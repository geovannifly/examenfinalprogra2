package com.miumg.chatbottelegram.Service;

import java.time.LocalDateTime;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.miumg.chatbottelegram.model.ChatMessage;
import com.miumg.chatbottelegram.model.ErrorLog;
import com.miumg.chatbottelegram.repository.ChatMessageRepository;
import com.miumg.chatbottelegram.repository.ErrorLogRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class OpenAIService {

    @Autowired
    private ErrorLogRepository errorLogRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.api.url}")
    private String apiUrl;

    public String obtenerRespuesta(String prompt) throws Exception {

        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost request = new HttpPost(apiUrl);
        request.addHeader("Content-Type", "application/json");
        request.addHeader("Authorization", "Bearer " + apiKey);

        // Construcción del cuerpo de la solicitud
        String jsonInputString = String.format("""
                                               {
                                                 "messages": [
                                                   {
                                                     "role": "user",
                                                     "content": "%s"
                                                   }
                                                 ],
                                                 "model": "llama3-8b-8192",
                                                 "temperature": 1,
                                                 "max_tokens": 1024,
                                                 "top_p": 1,
                                                 "stream": false,
                                                 "stop": null
                                               }""",
                prompt);
        log.info("Esto te mando: " + jsonInputString);
        request.setEntity(new StringEntity(jsonInputString));

        // Envío de la solicitud y manejo de la respuesta
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            String jsonResponse = EntityUtils.toString(response.getEntity());
            log.info("Respuesta de la API: {}", jsonResponse); // Agregar log para depuración
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(jsonResponse);

            // Verificar que la respuesta contiene los datos esperados
            if (jsonNode.has("choices") && jsonNode.get("choices").isArray() && jsonNode.get("choices").size() > 0) {
                JsonNode choice = jsonNode.get("choices").get(0);
                if (choice.has("message") && choice.get("message").has("content")) {
                    return choice.get("message").get("content").asText();
                } else {
                    log.error("La respuesta no contiene el campo 'message' o 'content'.");
                    return "No se pudo obtener la respuesta.";
                }
            } else {
                log.error("La respuesta no contiene 'choices' o está vacía.");
                return "No se pudo obtener la respuesta.";
            }

        }

    }

    public String obtenerRespuesta2(String prompt) throws Exception {
        // Crear un nuevo mensaje de chat y guardar la pregunta
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setQuestion(prompt);
        chatMessage.setCreationDate(LocalDateTime.now());
        chatMessageRepository.save(chatMessage);

        // Crear un cliente HTTP y configurar la solicitud POST
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost request = new HttpPost(apiUrl);
        request.addHeader("Content-Type", "application/json");
        request.addHeader("Authorization", "Bearer " + apiKey);

        // Construir el cuerpo de la solicitud en formato JSON
        String jsonInputString = String.format("""
                {
                  "messages": [
                    {
                      "role": "user",
                      "content": "%s"
                    }
                  ],
                  "model": "llama3-8b-8192",
                  "temperature": 1,
                  "max_tokens": 1024,
                  "top_p": 1,
                  "stream": false,
                  "stop": null
                }""",
                prompt);
        log.info("Esto te mando: " + jsonInputString);
        request.setEntity(new StringEntity(jsonInputString));

        // Enviar la solicitud y manejar la respuesta
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            String jsonResponse = EntityUtils.toString(response.getEntity());
            log.info("Respuesta de la API: {}", jsonResponse);
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(jsonResponse);

            // Verificar que la respuesta contiene los datos esperados
            if (jsonNode.has("choices") && jsonNode.get("choices").isArray() && jsonNode.get("choices").size() > 0) {
                JsonNode choice = jsonNode.get("choices").get(0);
                if (choice.has("message") && choice.get("message").has("content")) {
                    String answer = choice.get("message").get("content").asText();
                    // Guardar la respuesta en el mensaje de chat y devolverla
                    chatMessage.setAnswer(answer);
                    chatMessageRepository.save(chatMessage);
                    return answer;
                } else {
                    // Registrar un error si la respuesta no contiene el campo esperado
                    logError(chatMessage, "La respuesta no contiene el campo 'message' o 'content'.");
                    return "No se pudo obtener la respuesta.";
                }
            } else {
                // Registrar un error si la respuesta no contiene 'choices' o está vacía
                logError(chatMessage, "La respuesta no contiene 'choices' o está vacía.");
                return "No se pudo obtener la respuesta.";
            }
        } catch (Exception e) {
            // Registrar un error si ocurre una excepción durante la solicitud
            logError(chatMessage, e.getMessage());
            throw e;
        }
    }

    // Método para registrar errores en el repositorio de errores
    private void logError(ChatMessage chatMessage, String errorMessage) {
        ErrorLog errorLog = new ErrorLog();
        errorLog.setChatMessage(chatMessage);
        errorLog.setErrorMessage(errorMessage);
        errorLog.setTimestamp(LocalDateTime.now());
        errorLogRepository.save(errorLog);
    }

}
