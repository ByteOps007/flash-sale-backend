package com.anshbhardwaj.flashsale.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Published to Kafka on every confirmed purchase. Consumed by
 * StockUpdateListener, which forwards it to any connected frontend
 * over WebSocket - this is what makes stock counts update live on screen
 * without the browser needing to poll.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderEvent {
    private Long productId;
    private String productName;
    private String userId;
    private int quantityPurchased;
    private long remainingStock;
    private LocalDateTime timestamp;
}
