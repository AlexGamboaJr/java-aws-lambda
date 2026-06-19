package com.alexgamboa.lambda.service;

import com.alexgamboa.lambda.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService — Testes de Lógica de Negócio")
class ProductServiceTest {

    @Mock
    private DynamoDbService dynamoDbService;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(dynamoDbService);
    }

    // ── createProduct ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("createProduct → atribui id (UUID) automaticamente")
    void testCreateProduct_setsId() {
        Product input = new Product();
        input.setName("Monitor LG");
        input.setDescription("4K 27\"");
        input.setPrice(2800.0);
        input.setStock(5);

        // DynamoDbService não deve retornar nada relevante
        when(dynamoDbService.putItem(any(Product.class))).thenReturn(Collections.emptyMap());

        Product result = productService.createProduct(input);

        assertNotNull(result.getId(), "O id deve ser preenchido automaticamente");
        assertFalse(result.getId().isBlank(), "O id não pode ser vazio");
        // UUID v4 tem 36 caracteres com hifens
        assertEquals(36, result.getId().length());
    }

    @Test
    @DisplayName("createProduct → atribui createdAt (ISO 8601) automaticamente")
    void testCreateProduct_setsCreatedAt() {
        Product input = new Product();
        input.setName("Webcam Logitech");
        input.setDescription("Full HD 1080p");
        input.setPrice(450.0);
        input.setStock(20);

        when(dynamoDbService.putItem(any(Product.class))).thenReturn(Collections.emptyMap());

        Product result = productService.createProduct(input);

        assertNotNull(result.getCreatedAt(), "O createdAt deve ser preenchido automaticamente");
        assertFalse(result.getCreatedAt().isBlank(), "O createdAt não pode ser vazio");
        // Verifica que é um timestamp ISO-8601 (contém 'T' e 'Z')
        assertTrue(result.getCreatedAt().contains("T"), "createdAt deve ser ISO 8601 (contém 'T')");
    }

    @Test
    @DisplayName("createProduct → persiste o produto no DynamoDB via putItem")
    void testCreateProduct_callsDynamoDb() {
        Product input = new Product();
        input.setName("SSD Kingston");
        input.setDescription("1TB NVMe");
        input.setPrice(550.0);
        input.setStock(15);

        when(dynamoDbService.putItem(any(Product.class))).thenReturn(Collections.emptyMap());

        productService.createProduct(input);

        // Verifica que putItem foi chamado exatamente uma vez com o produto correto
        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(dynamoDbService, times(1)).putItem(captor.capture());
        assertEquals("SSD Kingston", captor.getValue().getName());
    }

    @Test
    @DisplayName("createProduct → atribui stock=0 quando não informado")
    void testCreateProduct_defaultStock() {
        Product input = new Product();
        input.setName("Produto sem stock");
        input.setPrice(100.0);
        // stock não setado (null)

        when(dynamoDbService.putItem(any(Product.class))).thenReturn(Collections.emptyMap());

        Product result = productService.createProduct(input);

        assertEquals(0, result.getStock(), "Stock deve ser 0 quando não informado");
    }

    @Test
    @DisplayName("createProduct → lança exceção quando name é nulo")
    void testCreateProduct_nullName_throwsException() {
        Product input = new Product();
        input.setPrice(100.0);
        // name não setado

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> productService.createProduct(input)
        );
        assertTrue(exception.getMessage().contains("name"));
        verify(dynamoDbService, never()).putItem(any());
    }

    @Test
    @DisplayName("createProduct → lança exceção quando price é negativo")
    void testCreateProduct_negativePrice_throwsException() {
        Product input = new Product();
        input.setName("Produto inválido");
        input.setPrice(-10.0);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> productService.createProduct(input)
        );
        assertTrue(exception.getMessage().contains("price"));
        verify(dynamoDbService, never()).putItem(any());
    }

    // ── getProduct ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getProduct → retorna produto quando existe no DynamoDB")
    void testGetProduct_found() {
        String id = "uuid-existente";
        Product expected = new Product(id, "Headset Sony", "Sem fio", 600.0, 8, "2024-03-01T00:00:00Z");
        when(dynamoDbService.getItem(id)).thenReturn(Optional.of(expected));

        Optional<Product> result = productService.getProduct(id);

        assertTrue(result.isPresent());
        assertEquals("Headset Sony", result.get().getName());
        assertEquals(id, result.get().getId());
    }

    @Test
    @DisplayName("getProduct → retorna Optional.empty() quando produto não existe")
    void testGetProduct_notFound() {
        when(dynamoDbService.getItem("id-fantasma")).thenReturn(Optional.empty());

        Optional<Product> result = productService.getProduct("id-fantasma");

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("getProduct → lança exceção quando id é nulo")
    void testGetProduct_nullId_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> productService.getProduct(null));
        verify(dynamoDbService, never()).getItem(any());
    }

    @Test
    @DisplayName("getProduct → lança exceção quando id é vazio")
    void testGetProduct_blankId_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> productService.getProduct("   "));
        verify(dynamoDbService, never()).getItem(any());
    }

    // ── listProducts ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("listProducts → retorna lista com todos os produtos")
    void testListProducts_returnsAll() {
        List<Product> produtos = List.of(
                new Product("id-1", "Produto A", "Desc A", 100.0, 5, "2024-01-01T00:00:00Z"),
                new Product("id-2", "Produto B", "Desc B", 200.0, 10, "2024-01-02T00:00:00Z"),
                new Product("id-3", "Produto C", "Desc C", 300.0, 15, "2024-01-03T00:00:00Z")
        );
        when(dynamoDbService.scanItems()).thenReturn(produtos);

        List<Product> result = productService.listProducts();

        assertEquals(3, result.size());
        assertEquals("Produto A", result.get(0).getName());
        verify(dynamoDbService, times(1)).scanItems();
    }

    @Test
    @DisplayName("listProducts → retorna lista vazia quando não há produtos")
    void testListProducts_empty() {
        when(dynamoDbService.scanItems()).thenReturn(Collections.emptyList());

        List<Product> result = productService.listProducts();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ── deleteProduct ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteProduct → retorna true quando produto existia")
    void testDeleteProduct_success() {
        when(dynamoDbService.deleteItem("id-valido")).thenReturn(true);

        boolean result = productService.deleteProduct("id-valido");

        assertTrue(result);
        verify(dynamoDbService, times(1)).deleteItem("id-valido");
    }

    @Test
    @DisplayName("deleteProduct → retorna false quando produto não existia")
    void testDeleteProduct_notFound() {
        when(dynamoDbService.deleteItem("id-inexistente")).thenReturn(false);

        boolean result = productService.deleteProduct("id-inexistente");

        assertFalse(result);
    }

    @Test
    @DisplayName("deleteProduct → lança exceção quando id é nulo")
    void testDeleteProduct_nullId_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> productService.deleteProduct(null));
        verify(dynamoDbService, never()).deleteItem(any());
    }
}
