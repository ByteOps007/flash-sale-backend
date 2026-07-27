package com.anshbhardwaj.flashsale.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PurchaseRequest {

    @NotNull
    private Long productId;

    @NotBlank
    private String userId; // will come from Clerk auth later; manually passed for now

    @Min(1)
    private int quantity;
}
