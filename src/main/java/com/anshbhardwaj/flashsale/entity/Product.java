package com.anshbhardwaj.flashsale.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer stock;

    // Optimistic locking: Hibernate auto-checks this on every save().
    // If two requests read the same version and both try to update,
    // the second one throws OptimisticLockingFailureException instead
    // of silently overwriting the first (this is what prevents overselling).
    @Version
    private Integer version;

    private LocalDateTime saleStart;
    private LocalDateTime saleEnd;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
