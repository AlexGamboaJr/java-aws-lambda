package com.alexgamboa.lambda.model;

/**
 * POJO representando um Produto armazenado no DynamoDB.
 * Sem anotações JPA — compatível com AWS SDK v2 AttributeValue.
 */
public class Product {

    private String id;
    private String name;
    private String description;
    private Double price;
    private Integer stock;
    private String createdAt;

    // Construtor vazio necessário para deserialização Jackson
    public Product() {
    }

    // Construtor completo
    public Product(String id, String name, String description,
                   Double price, Integer stock, String createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.stock = stock;
        this.createdAt = createdAt;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Double getPrice() {
        return price;
    }

    public Integer getStock() {
        return stock;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    // ── Setters ──────────────────────────────────────────────────────────────

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Product{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", price=" + price +
                ", stock=" + stock +
                ", createdAt='" + createdAt + '\'' +
                '}';
    }
}
