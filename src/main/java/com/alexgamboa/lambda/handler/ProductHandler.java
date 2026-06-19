package com.alexgamboa.lambda.handler;

import com.alexgamboa.lambda.model.Product;
import com.alexgamboa.lambda.service.ProductService;
import com.alexgamboa.lambda.util.ResponseBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Handler principal da Lambda que gerencia o CRUD de Produtos.
 * Recebe eventos do API Gateway e roteia para o ProductService.
 *
 * Rotas suportadas:
 *   GET    /products        → lista todos os produtos
 *   GET    /products/{id}   → busca produto por id
 *   POST   /products        → cria novo produto
 *   DELETE /products/{id}   → remove produto por id
 */
public class ProductHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ProductHandler.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ProductService productService;

    /**
     * Construtor padrão — usado pela AWS Lambda em produção.
     */
    public ProductHandler() {
        this.productService = new ProductService();
    }

    /**
     * Construtor com injeção de dependência — usado nos testes.
     */
    public ProductHandler(ProductService productService) {
        this.productService = productService;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        String httpMethod = event.getHttpMethod();
        String path = event.getPath();
        Map<String, String> pathParameters = event.getPathParameters();

        log.info("Recebendo {} {}", event.getHttpMethod(), event.getPath());

        if (context != null) {
            context.getLogger().log(String.format("[ProductHandler] %s %s", httpMethod, path));
        }

        try {
            Map<String, Object> result;

            if ("GET".equals(httpMethod) && pathParameters == null) {
                // GET /products → lista todos
                result = handleListProducts();

            } else if ("GET".equals(httpMethod) && pathParameters != null && pathParameters.containsKey("id")) {
                // GET /products/{id} → busca por id
                result = handleGetProduct(pathParameters.get("id"));

            } else if ("POST".equals(httpMethod)) {
                // POST /products → cria produto
                result = handleCreateProduct(event.getBody());

            } else if ("DELETE".equals(httpMethod) && pathParameters != null && pathParameters.containsKey("id")) {
                // DELETE /products/{id} → remove produto
                result = handleDeleteProduct(pathParameters.get("id"));

            } else {
                result = ResponseBuilder.methodNotAllowed(
                        "Método " + httpMethod + " não suportado neste endpoint.");
            }

            return buildResponse(result);

        } catch (IllegalArgumentException e) {
            return buildResponse(ResponseBuilder.badRequest(e.getMessage()));
        } catch (Exception e) {
            if (context != null) {
                context.getLogger().log("[ProductHandler] Erro inesperado: " + e.getMessage());
            }
            return buildResponse(ResponseBuilder.serverError("Erro interno no servidor: " + e.getMessage()));
        }
    }

    // ── Handlers internos ─────────────────────────────────────────────────────

    private Map<String, Object> handleListProducts() {
        List<Product> products = productService.listProducts();
        return ResponseBuilder.ok(products);
    }

    private Map<String, Object> handleGetProduct(String id) {
        Optional<Product> product = productService.getProduct(id);
        if (product.isPresent()) {
            return ResponseBuilder.ok(product.get());
        }
        return ResponseBuilder.notFound("Produto com id '" + id + "' não encontrado.");
    }

    private Map<String, Object> handleCreateProduct(String body) throws Exception {
        if (body == null || body.isBlank()) {
            return ResponseBuilder.badRequest("O corpo da requisição não pode ser vazio.");
        }
        Product input = MAPPER.readValue(body, Product.class);
        Product created = productService.createProduct(input);
        return ResponseBuilder.created(created);
    }

    private Map<String, Object> handleDeleteProduct(String id) {
        boolean deleted = productService.deleteProduct(id);
        if (deleted) {
            return ResponseBuilder.ok(Map.of("message", "Produto '" + id + "' removido com sucesso."));
        }
        return ResponseBuilder.notFound("Produto com id '" + id + "' não encontrado.");
    }

    // ── Conversão para APIGatewayProxyResponseEvent ───────────────────────────

    /**
     * Converte o Map do ResponseBuilder para o objeto de resposta do API Gateway,
     * adicionando os headers CORS obrigatórios.
     */
    private APIGatewayProxyResponseEvent buildResponse(Map<String, Object> responseMap) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Access-Control-Allow-Origin", "*");
        headers.put("Access-Control-Allow-Methods", "GET,POST,DELETE,OPTIONS");
        headers.put("Access-Control-Allow-Headers", "Content-Type,Authorization");

        return new APIGatewayProxyResponseEvent()
                .withStatusCode((Integer) responseMap.get("statusCode"))
                .withHeaders(headers)
                .withBody((String) responseMap.get("body"));
    }
}
