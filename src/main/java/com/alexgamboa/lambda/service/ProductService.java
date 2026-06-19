package com.alexgamboa.lambda.service;

import com.alexgamboa.lambda.model.Product;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Camada de serviço que orquestra a lógica de negócio para operações com Produtos.
 * Depende do DynamoDbService para persistência.
 */
public class ProductService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ProductService.class);

    private final DynamoDbService dynamoDbService;

    /**
     * Construtor padrão — usa a instância singleton do DynamoDbService.
     * Usado em produção (Lambda).
     */
    public ProductService() {
        this.dynamoDbService = DynamoDbService.getInstance();
    }

    /**
     * Construtor com injeção de dependência — usado nos testes unitários.
     *
     * @param dynamoDbService Instância mockada do serviço DynamoDB.
     */
    public ProductService(DynamoDbService dynamoDbService) {
        this.dynamoDbService = dynamoDbService;
    }

    // ── Operações de negócio ──────────────────────────────────────────────────

    /**
     * Cria um novo produto.
     * Gera automaticamente o id (UUID v4) e o createdAt (ISO 8601).
     *
     * @param product Dados do produto vindos do request body.
     * @return Produto criado com id e createdAt preenchidos.
     */
    public Product createProduct(Product product) {
        if (product.getName() == null || product.getName().isBlank()) {
            throw new IllegalArgumentException("O campo 'name' é obrigatório.");
        }
        if (product.getPrice() == null || product.getPrice() < 0) {
            throw new IllegalArgumentException("O campo 'price' deve ser um número positivo.");
        }

        log.info("Criando produto: {}", product.getName());

        // Atribui id único e timestamp de criação
        product.setId(UUID.randomUUID().toString());
        product.setCreatedAt(Instant.now().toString());

        // Garante stock padrão 0 se não informado
        if (product.getStock() == null) {
            product.setStock(0);
        }

        dynamoDbService.putItem(product);
        return product;
    }

    /**
     * Busca um produto pelo id.
     *
     * @param id UUID do produto.
     * @return Optional com o produto, ou Optional.empty() se não existir.
     */
    public Optional<Product> getProduct(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("O parâmetro 'id' é obrigatório.");
        }
        return dynamoDbService.getItem(id);
    }

    /**
     * Lista todos os produtos armazenados.
     *
     * @return Lista de produtos (pode ser vazia).
     */
    public List<Product> listProducts() {
        return dynamoDbService.scanItems();
    }

    /**
     * Remove um produto pelo id.
     *
     * @param id UUID do produto.
     * @return true se removido com sucesso, false se não encontrado.
     */
    public boolean deleteProduct(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("O parâmetro 'id' é obrigatório.");
        }
        return dynamoDbService.deleteItem(id);
    }
}
