package com.anshbhardwaj.flashsale.service;

import com.anshbhardwaj.flashsale.entity.Order;
import com.anshbhardwaj.flashsale.entity.Product;
import com.anshbhardwaj.flashsale.repository.OrderRepository;
import com.anshbhardwaj.flashsale.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Holds the single @Transactional DB write for a purchase, kept in its
 * OWN Spring bean deliberately.
 *
 * Why: @Transactional only works when a method is called THROUGH Spring's
 * proxy. If InventoryService called this same logic via "this.method()"
 * from within itself, the proxy is bypassed entirely and no transaction
 * is opened - which silently breaks @Modifying queries (they require an
 * active transaction to run at all). Injecting this as a separate bean
 * and calling it via that injected reference forces the call through the
 * real proxy, so the transaction actually applies.
 */
@Service
@RequiredArgsConstructor
public class OrderConfirmationService {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public Order confirmInDatabase(Long productId, String userId, int quantity) {
        int rowsUpdated = productRepository.decrementStock(productId, quantity);

        if (rowsUpdated == 0) {
            throw new IllegalStateException("Insufficient stock in database");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalStateException("Product not found"));

        Order order = new Order(null, userId, product, quantity, "CONFIRMED", null);
        return orderRepository.save(order);
    }
}
