package com.anshbhardwaj.flashsale.controller;

import com.anshbhardwaj.flashsale.entity.Product;
import com.anshbhardwaj.flashsale.repository.ProductRepository;
import com.anshbhardwaj.flashsale.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductRepository productRepository;
    private final InventoryService inventoryService;

    @GetMapping
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProduct(@PathVariable Long id) {
        return productRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Product createProduct(@RequestBody Product product) {
        Product saved = productRepository.save(product);
        // Keep Redis's fast-path stock count in sync with Postgres from the start
        inventoryService.seedStock(saved.getId(), saved.getStock());
        return saved;
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
