package com.yulaf.stock.domain;

import jakarta.persistence.*;

@Entity
public class Stock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long productId;

    private Long quantity;

    @Version
    private Long version;

    public Stock() {

    }

    public void decreaseQuantity(Long quantity) {
        if (this.quantity - quantity < 0) {
            throw new RuntimeException("재고는 0 미만이 될 수없다.");
        }

        this.quantity -= quantity;
    }

    public Stock(Long productId, Long quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }

    public Long getQuantity() {
        return quantity;
    }
}
