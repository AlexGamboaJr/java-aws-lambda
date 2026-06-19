package com.alexgamboa.lambda.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

/**
 * Utilitário para construir respostas HTTP padronizadas no formato
 * esperado pelo API Gateway (statusCode + body JSON).
 */
public final class ResponseBuilder {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // Impede instanciação — classe utilitária com métodos estáticos
    private ResponseBuilder() {
    }

    // ── Métodos de resposta ───────────────────────────────────────────────────

    /**
     * 200 OK — resposta bem-sucedida com corpo.
     */
    public static Map<String, Object> ok(Object body) {
        return build(200, body);
    }

    /**
     * 201 Created — recurso criado com sucesso.
     */
    public static Map<String, Object> created(Object body) {
        return build(201, body);
    }

    /**
     * 404 Not Found — recurso não encontrado.
     */
    public static Map<String, Object> notFound(String message) {
        return build(404, Map.of("error", message));
    }

    /**
     * 400 Bad Request — dados inválidos na requisição.
     */
    public static Map<String, Object> badRequest(String message) {
        return build(400, Map.of("error", message));
    }

    /**
     * 405 Method Not Allowed — método HTTP não suportado.
     */
    public static Map<String, Object> methodNotAllowed(String message) {
        return build(405, Map.of("error", message));
    }

    /**
     * 500 Internal Server Error — erro inesperado no servidor.
     */
    public static Map<String, Object> serverError(String message) {
        return build(500, Map.of("error", message));
    }

    // ── Builder interno ───────────────────────────────────────────────────────

    /**
     * Constrói o mapa de resposta com statusCode e body serializado como JSON.
     */
    private static Map<String, Object> build(int statusCode, Object body) {
        Map<String, Object> response = new HashMap<>();
        response.put("statusCode", statusCode);

        try {
            response.put("body", MAPPER.writeValueAsString(body));
        } catch (JsonProcessingException e) {
            // Fallback seguro se a serialização falhar
            response.put("body", "{\"error\":\"Falha ao serializar a resposta\"}");
        }

        return response;
    }
}
