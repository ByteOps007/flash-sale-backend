package com.anshbhardwaj.flashsale.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class StockUpdateListener {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Listens to every order event published to Kafka and rebroadcasts it
     * to all clients subscribed to /topic/stock-updates over WebSocket.
     * This is the bridge between "backend event happened" and
     * "frontend UI updates live" - no polling needed.
     */
    @KafkaListener(topics = "order-events", groupId = "stock-broadcaster")
    public void onOrderEvent(String message) {
        log.info("Broadcasting stock update to WebSocket clients: {}", message);
        messagingTemplate.convertAndSend("/topic/stock-updates", message);
    }
}
