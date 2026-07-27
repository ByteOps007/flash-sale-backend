package com.anshbhardwaj.flashsale.repository;

import com.anshbhardwaj.flashsale.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Atomically decrements stock in a single conditional UPDATE, instead
     * of JPA's default read-then-write @Version pattern. This matters
     * under heavy contention: read-then-write requires every concurrent
     * transaction to first read the same version, so only one write wins
     * and everyone else must retry from scratch. A single UPDATE ... WHERE
     * stock >= :qty lets Postgres serialize the writes itself at the row
     * level - each transaction just queues briefly and then succeeds or
     * fails based on the CURRENT stock, with no wasted round-trips.
     *
     * Returns the number of rows updated: 1 if there was enough stock,
     * 0 if not (which we treat as "insufficient stock").
     */
    @Modifying
    @Query("UPDATE Product p SET p.stock = p.stock - :qty, p.version = p.version + 1 " +
            "WHERE p.id = :id AND p.stock >= :qty")
    int decrementStock(@Param("id") Long id, @Param("qty") int qty);
}
