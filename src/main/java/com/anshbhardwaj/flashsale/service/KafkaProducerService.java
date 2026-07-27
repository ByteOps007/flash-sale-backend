package com.anshbhardwaj.flashsale.service;

import com.anshbhardwaj.flashsale.event.OrderEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {

    private static final String TOPIC = "order-events";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publishOrderEvent(OrderEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(TOPIC, String.valueOf(event.getProductId()), payload);
        } catch (Exception e) {
            // Publishing failures shouldn't roll back the purchase itself -
            // the order already succeeded and is safely in Postgres.
            // We just log it; in production you might retry or use an outbox pattern.
            log.error("Failed to publish order event for productId={}", event.getProductId(), e);
        }
    }
}
