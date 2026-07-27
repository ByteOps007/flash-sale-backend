package com.anshbhardwaj.flashsale.service;

import com.anshbhardwaj.flashsale.dto.PurchaseResult;
import com.anshbhardwaj.flashsale.entity.Order;
import com.anshbhardwaj.flashsale.event.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final StringRedisTemplate redisTemplate;
    private final OrderConfirmationService orderConfirmationService;
    private final KafkaProducerService kafkaProducerService;

    private static final String STOCK_KEY_PREFIX = "stock:";

    /**
     * Call this once when a product is created (or a sale is (re)started) to
     * seed Redis with the current DB stock count. Redis and Postgres must
     * start in sync, or the fast-path check below is meaningless.
     */
    public void seedStock(Long productId, int stock) {
        redisTemplate.opsForValue().set(STOCK_KEY_PREFIX + productId, String.valueOf(stock));
    }

    /**
     * Two-layer purchase flow:
     *
     * Layer 1 (Redis fast-path): Redis's DECR is atomic even with thousands
     * of concurrent requests, because Redis is single-threaded. This cheaply
     * rejects most over-capacity requests WITHOUT touching Postgres at all,
     * which is what lets this survive a "thundering herd" of buyers.
     *
     * Layer 2 (Postgres source of truth): Even though Redis said "yes",
     * OrderConfirmationService.confirmInDatabase does a single atomic
     * conditional UPDATE against Postgres. If Redis and Postgres somehow
     * drifted out of sync (cache eviction, restart, etc.), that UPDATE's
     * WHERE clause catches it and we roll back the Redis decrement.
     */
    public PurchaseResult purchase(Long productId, String userId, int quantity) {
        String stockKey = STOCK_KEY_PREFIX + productId;

        Long remaining = redisTemplate.opsForValue().decrement(stockKey, quantity);

        if (remaining == null) {
            // Key didn't exist in Redis at all - stock was never seeded.
            return PurchaseResult.failed("ERROR", "Product stock not initialized in cache.");
        }

        if (remaining < 0) {
            // Oversold in Redis - roll back our own decrement and reject.
            redisTemplate.opsForValue().increment(stockKey, quantity);
            log.info("Purchase rejected (Redis): productId={} userId={} qty={} - out of stock", productId, userId, quantity);
            return PurchaseResult.failed("OUT_OF_STOCK", "This item is sold out.");
        }

        try {
            Order order = orderConfirmationService.confirmInDatabase(productId, userId, quantity);
            log.info("Purchase confirmed: productId={} userId={} qty={} orderId={}",
                    productId, userId, quantity, order.getId());

            kafkaProducerService.publishOrderEvent(new OrderEvent(
                    productId,
                    order.getProduct().getName(),
                    userId,
                    quantity,
                    remaining, // remaining stock per Redis, reflects this purchase immediately
                    LocalDateTime.now()
            ));

            return PurchaseResult.success(order.getId());
        } catch (IllegalStateException e) {
            // Genuinely out of stock at the DB level (e.g. Redis/DB drift) -
            // roll back the Redis decrement and reject cleanly.
            redisTemplate.opsForValue().increment(stockKey, quantity);
            log.warn("Purchase rejected (DB): productId={} userId={} qty={} reason={}",
                    productId, userId, quantity, e.getMessage());
            return PurchaseResult.failed("OUT_OF_STOCK", e.getMessage());
        }
    }
}

