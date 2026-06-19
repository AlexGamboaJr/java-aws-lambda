package com.alexgamboa.lambda.service;

import com.alexgamboa.lambda.model.Product;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Camada de acesso ao DynamoDB usando AWS SDK v2.
 * Implementa o padrão Singleton para reutilizar a conexão entre invocações Lambda.
 */
public class DynamoDbService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DynamoDbService.class);

    private static DynamoDbService instance;

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    // Construtor privado — use getInstance()
    private DynamoDbService() {
        String region = System.getenv("AWS_REGION") != null
                ? System.getenv("AWS_REGION")
                : "us-east-1";

        this.tableName = System.getenv("TABLE_NAME") != null
                ? System.getenv("TABLE_NAME")
                : "Products";

        this.dynamoDbClient = DynamoDbClient.builder()
                .region(Region.of(region))
                .build();
    }

    // Construtor package-private para testes (injeção do client mockado)
    DynamoDbService(DynamoDbClient dynamoDbClient, String tableName) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableName;
    }

    /**
     * Retorna a instância singleton do serviço.
     */
    public static synchronized DynamoDbService getInstance() {
        if (instance == null) {
            instance = new DynamoDbService();
        }
        return instance;
    }

    // ── Operações DynamoDB ────────────────────────────────────────────────────

    /**
     * Persiste um produto no DynamoDB.
     *
     * @param product Produto a ser salvo.
     * @return Mapa de atributos enviado ao DynamoDB.
     */
    public Map<String, AttributeValue> putItem(Product product) {
        Map<String, AttributeValue> item = productToItem(product);

        PutItemRequest request = PutItemRequest.builder()
                .tableName(tableName)
                .item(item)
                .build();

        dynamoDbClient.putItem(request);
        return item;
    }

    /**
     * Busca um produto pelo id (partition key).
     *
     * @param id UUID do produto.
     * @return Optional com o produto, ou Optional.empty() se não encontrado.
     */
    public Optional<Product> getItem(String id) {
        log.info("Buscando item id={} na tabela {}", id, tableName);

        Map<String, AttributeValue> key = new HashMap<>();
        key.put("id", AttributeValue.builder().s(id).build());

        GetItemRequest request = GetItemRequest.builder()
                .tableName(tableName)
                .key(key)
                .build();

        GetItemResponse response = dynamoDbClient.getItem(request);

        if (!response.hasItem() || response.item().isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(itemToProduct(response.item()));
    }

    /**
     * Retorna todos os produtos via Scan (adequado para tabelas pequenas).
     *
     * @return Lista de produtos.
     */
    public List<Product> scanItems() {
        ScanRequest request = ScanRequest.builder()
                .tableName(tableName)
                .build();

        ScanResponse response = dynamoDbClient.scan(request);

        return response.items().stream()
                .map(this::itemToProduct)
                .collect(Collectors.toList());
    }

    /**
     * Remove um produto pelo id.
     *
     * @param id UUID do produto.
     * @return true se o item existia e foi removido, false caso contrário.
     */
    public boolean deleteItem(String id) {
        log.info("Deletando item id={}", id);

        // Verifica se o item existe antes de deletar
        Optional<Product> existing = getItem(id);
        if (existing.isEmpty()) {
            return false;
        }

        Map<String, AttributeValue> key = new HashMap<>();
        key.put("id", AttributeValue.builder().s(id).build());

        DeleteItemRequest request = DeleteItemRequest.builder()
                .tableName(tableName)
                .key(key)
                .build();

        dynamoDbClient.deleteItem(request);
        return true;
    }

    // ── Conversões privadas ───────────────────────────────────────────────────

    /**
     * Converte um objeto Product para o mapa de AttributeValue do DynamoDB.
     */
    private Map<String, AttributeValue> productToItem(Product product) {
        Map<String, AttributeValue> item = new HashMap<>();

        item.put("id", AttributeValue.builder().s(product.getId()).build());
        item.put("name", AttributeValue.builder().s(safeString(product.getName())).build());
        item.put("description", AttributeValue.builder().s(safeString(product.getDescription())).build());
        item.put("price", AttributeValue.builder()
                .n(product.getPrice() != null ? String.valueOf(product.getPrice()) : "0.0")
                .build());
        item.put("stock", AttributeValue.builder()
                .n(product.getStock() != null ? String.valueOf(product.getStock()) : "0")
                .build());
        item.put("createdAt", AttributeValue.builder().s(safeString(product.getCreatedAt())).build());

        return item;
    }

    /**
     * Converte um mapa de AttributeValue do DynamoDB para um objeto Product.
     */
    private Product itemToProduct(Map<String, AttributeValue> item) {
        Product product = new Product();

        if (item.containsKey("id")) {
            product.setId(item.get("id").s());
        }
        if (item.containsKey("name")) {
            product.setName(item.get("name").s());
        }
        if (item.containsKey("description")) {
            product.setDescription(item.get("description").s());
        }
        if (item.containsKey("price")) {
            product.setPrice(Double.parseDouble(item.get("price").n()));
        }
        if (item.containsKey("stock")) {
            product.setStock(Integer.parseInt(item.get("stock").n()));
        }
        if (item.containsKey("createdAt")) {
            product.setCreatedAt(item.get("createdAt").s());
        }

        return product;
    }

    private String safeString(String value) {
        return value != null ? value : "";
    }
}
