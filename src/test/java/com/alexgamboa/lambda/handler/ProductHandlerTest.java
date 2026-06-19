package com.alexgamboa.lambda.handler;

import com.alexgamboa.lambda.model.Product;
import com.alexgamboa.lambda.service.ProductService;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductHandler — Testes de Roteamento HTTP")
class ProductHandlerTest {

    @Mock
    private ProductService productService;

    private ProductHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ProductHandler(productService);
    }

    // ── GET /products ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /products → 200 com lista de produtos")
    void testListProducts_returnsOk() {
        Product p1 = new Product("id-1", "Notebook Dell", "i7 16GB", 4500.0, 10, "2024-01-01T00:00:00Z");
        Product p2 = new Product("id-2", "Mouse Logitech", "Sem fio", 150.0, 50, "2024-01-02T00:00:00Z");
        when(productService.listProducts()).thenReturn(List.of(p1, p2));

        APIGatewayProxyRequestEvent event = buildEvent("GET", "/products", null, null);
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, null);

        assertEquals(200, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("Notebook Dell"));
        assertTrue(response.getBody().contains("Mouse Logitech"));
        verify(productService, times(1)).listProducts();
    }

    @Test
    @DisplayName("GET /products → 200 com lista vazia")
    void testListProducts_emptyList() {
        when(productService.listProducts()).thenReturn(Collections.emptyList());

        APIGatewayProxyRequestEvent event = buildEvent("GET", "/products", null, null);
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, null);

        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("[]"));
    }

    // ── GET /products/{id} ────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /products/{id} → 200 quando produto existe")
    void testGetProduct_found() {
        String productId = "abc-123";
        Product product = new Product(productId, "Teclado Mecânico", "RGB", 350.0, 5, "2024-01-10T00:00:00Z");
        when(productService.getProduct(productId)).thenReturn(Optional.of(product));

        APIGatewayProxyRequestEvent event = buildEvent(
                "GET", "/products/" + productId, Map.of("id", productId), null);
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, null);

        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("Teclado Mecânico"));
        assertTrue(response.getBody().contains(productId));
    }

    @Test
    @DisplayName("GET /products/{id} → 404 quando produto não existe")
    void testGetProduct_notFound() {
        when(productService.getProduct("nao-existe")).thenReturn(Optional.empty());

        APIGatewayProxyRequestEvent event = buildEvent(
                "GET", "/products/nao-existe", Map.of("id", "nao-existe"), null);
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, null);

        assertEquals(404, response.getStatusCode());
        assertTrue(response.getBody().contains("não encontrado"));
    }

    // ── POST /products ────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /products → 201 com produto criado")
    void testCreateProduct_success() {
        String body = "{\"name\":\"Notebook Dell\",\"description\":\"Notebook i7 16GB\",\"price\":4500.00,\"stock\":10}";
        Product created = new Product("uuid-gerado", "Notebook Dell", "Notebook i7 16GB", 4500.0, 10, "2024-01-15T10:00:00Z");
        when(productService.createProduct(any(Product.class))).thenReturn(created);

        APIGatewayProxyRequestEvent event = buildEvent("POST", "/products", null, body);
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, null);

        assertEquals(201, response.getStatusCode());
        assertTrue(response.getBody().contains("uuid-gerado"));
        assertTrue(response.getBody().contains("Notebook Dell"));
        verify(productService, times(1)).createProduct(any(Product.class));
    }

    @Test
    @DisplayName("POST /products → 400 quando body é nulo")
    void testCreateProduct_nullBody() {
        APIGatewayProxyRequestEvent event = buildEvent("POST", "/products", null, null);
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, null);

        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody().contains("error"));
        verify(productService, never()).createProduct(any());
    }

    @Test
    @DisplayName("POST /products → 400 quando body é JSON inválido")
    void testCreateProduct_invalidJson() {
        APIGatewayProxyRequestEvent event = buildEvent("POST", "/products", null, "isso nao e json");
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, null);

        assertEquals(500, response.getStatusCode()); // Jackson lança exceção
        verify(productService, never()).createProduct(any());
    }

    // ── DELETE /products/{id} ─────────────────────────────────────────────────

    @Test
    @DisplayName("DELETE /products/{id} → 200 quando produto existe e é deletado")
    void testDeleteProduct_success() {
        String productId = "id-para-deletar";
        when(productService.deleteProduct(productId)).thenReturn(true);

        APIGatewayProxyRequestEvent event = buildEvent(
                "DELETE", "/products/" + productId, Map.of("id", productId), null);
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, null);

        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("removido com sucesso"));
        verify(productService, times(1)).deleteProduct(productId);
    }

    @Test
    @DisplayName("DELETE /products/{id} → 404 quando produto não existe")
    void testDeleteProduct_notFound() {
        String productId = "id-inexistente";
        when(productService.deleteProduct(productId)).thenReturn(false);

        APIGatewayProxyRequestEvent event = buildEvent(
                "DELETE", "/products/" + productId, Map.of("id", productId), null);
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, null);

        assertEquals(404, response.getStatusCode());
    }

    // ── Método não suportado ──────────────────────────────────────────────────

    @Test
    @DisplayName("PUT /products/{id} → 405 Method Not Allowed")
    void testUnsupportedMethod_returns405() {
        APIGatewayProxyRequestEvent event = buildEvent("PUT", "/products/id-1", null, null);
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, null);

        assertEquals(405, response.getStatusCode());
        assertTrue(response.getBody().contains("error"));
    }

    // ── Headers CORS ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Todas as respostas incluem header CORS Access-Control-Allow-Origin: *")
    void testCorsHeaders() {
        when(productService.listProducts()).thenReturn(Collections.emptyList());

        APIGatewayProxyRequestEvent event = buildEvent("GET", "/products", null, null);
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, null);

        assertNotNull(response.getHeaders());
        assertEquals("*", response.getHeaders().get("Access-Control-Allow-Origin"));
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private APIGatewayProxyRequestEvent buildEvent(String method, String path,
                                                    Map<String, String> pathParams, String body) {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setHttpMethod(method);
        event.setPath(path);
        event.setPathParameters(pathParams);
        event.setBody(body);
        return event;
    }
}
