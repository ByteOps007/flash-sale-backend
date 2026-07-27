package com.anshbhardwaj.flashsale.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseResult {

    private boolean success;
    private String status;   // CONFIRMED, OUT_OF_STOCK, CONFLICT_RETRY, ERROR
    private String message;
    private Long orderId;    // null if the purchase failed

    public static PurchaseResult success(Long orderId) {
        return new PurchaseResult(true, "CONFIRMED", "Order placed successfully.", orderId);
    }

    public static PurchaseResult failed(String status, String message) {
        return new PurchaseResult(false, status, message, null);
    }
}
