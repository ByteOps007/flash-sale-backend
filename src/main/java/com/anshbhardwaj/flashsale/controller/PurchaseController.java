package com.anshbhardwaj.flashsale.controller;

import com.anshbhardwaj.flashsale.dto.PurchaseRequest;
import com.anshbhardwaj.flashsale.dto.PurchaseResult;
import com.anshbhardwaj.flashsale.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/purchase")
@RequiredArgsConstructor
public class PurchaseController {

    private final InventoryService inventoryService;

    @PostMapping
    public ResponseEntity<PurchaseResult> purchase(@Valid @RequestBody PurchaseRequest request) {
        PurchaseResult result = inventoryService.purchase(
                request.getProductId(), request.getUserId(), request.getQuantity());

        if (result.isSuccess()) {
            return ResponseEntity.ok(result);
        }

        // 409 Conflict for stock/version issues, so load-test tools (k6/JMeter)
        // can distinguish "expected rejection" from a real server error.
        return ResponseEntity.status(HttpStatus.CONFLICT).body(result);
    }
}
