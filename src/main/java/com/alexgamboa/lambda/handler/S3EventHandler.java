package com.alexgamboa.lambda.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3EventNotificationRecord;

import java.util.List;

/**
 * Handler Lambda acionado por eventos do Amazon S3.
 *
 * Casos de uso típicos:
 *   - Processar arquivos enviados ao bucket (imagens, CSVs, JSONs)
 *   - Gerar thumbnails, validar dados, acionar pipelines
 *   - Registrar metadados no DynamoDB após upload
 *
 * Este handler demonstra como receber e processar notificações S3
 * dentro de uma arquitetura serverless.
 */
public class S3EventHandler implements RequestHandler<S3Event, String> {

    @Override
    public String handleRequest(S3Event event, Context context) {
        List<S3EventNotificationRecord> records = event.getRecords();

        if (records == null || records.isEmpty()) {
            log(context, "Nenhum record recebido no evento S3.");
            return "Processed 0 records";
        }

        int processedCount = 0;

        for (S3EventNotificationRecord record : records) {
            try {
                String bucketName = record.getS3().getBucket().getName();
                String objectKey = record.getS3().getObject().getKey();
                String eventName = record.getEventName();

                log(context, String.format(
                        "Evento S3 recebido | EventType: %s | Bucket: %s | Key: %s",
                        eventName, bucketName, objectKey
                ));

                processS3Record(bucketName, objectKey, eventName, context);
                processedCount++;

            } catch (Exception e) {
                log(context, "Erro ao processar record S3: " + e.getMessage());
                // Continua processando os demais records mesmo em caso de erro parcial
            }
        }

        String summary = String.format("Processed %d records", processedCount);
        log(context, summary);
        return summary;
    }

    /**
     * Processa individualmente cada notificação S3.
     * Aqui você adicionaria lógica real: redimensionar imagem, parsear CSV, etc.
     *
     * @param bucketName Nome do bucket S3.
     * @param objectKey  Chave (path) do objeto no bucket.
     * @param eventName  Tipo do evento (ex: "ObjectCreated:Put").
     * @param context    Contexto da Lambda (para logging).
     */
    private void processS3Record(String bucketName, String objectKey,
                                  String eventName, Context context) {
        // Identifica o tipo de evento para tratamento diferenciado
        if (eventName != null && eventName.startsWith("ObjectCreated")) {
            log(context, String.format(
                    "Novo arquivo detectado no S3 — Bucket: %s | Key: %s | Ação: processar conteúdo",
                    bucketName, objectKey
            ));
            // TODO (produção): usar S3Client para fazer getObject e processar o arquivo
            // Exemplo:
            //   S3Client s3 = S3Client.builder().region(Region.US_EAST_1).build();
            //   GetObjectResponse obj = s3.getObject(GetObjectRequest.builder()
            //       .bucket(bucketName).key(objectKey).build(), ResponseTransformer.toInputStream());

        } else if (eventName != null && eventName.startsWith("ObjectRemoved")) {
            log(context, String.format(
                    "Arquivo removido do S3 — Bucket: %s | Key: %s | Ação: limpar referências",
                    bucketName, objectKey
            ));
            // TODO (produção): remover referências no DynamoDB ou outro armazenamento
        } else {
            log(context, String.format(
                    "Evento não mapeado: %s — Bucket: %s | Key: %s",
                    eventName, bucketName, objectKey
            ));
        }
    }

    /**
     * Helper de log que funciona tanto com Context real quanto em testes (context null).
     */
    private void log(Context context, String message) {
        if (context != null) {
            context.getLogger().log("[S3EventHandler] " + message);
        } else {
            System.out.println("[S3EventHandler] " + message);
        }
    }
}
