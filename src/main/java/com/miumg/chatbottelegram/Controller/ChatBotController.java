package com.miumg.chatbottelegram.Controller;

import com.miumg.chatbottelegram.model.Request;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import com.miumg.chatbottelegram.Service.OpenAIService;

@Slf4j
@RestController
@RequestMapping("/api")
public class ChatBotController {

    private final OpenAIService openAIService;

    public ChatBotController(OpenAIService openAIService) {
        this.openAIService = openAIService;
    }

    @PostMapping("/preguntar")
    public String preguntar(@RequestBody Request message) {
        log.info("Estes es el body de la peticion: " + message.getRequest() );
        String requestMessage = message.getRequest();
        try {
            return openAIService.obtenerRespuesta(requestMessage);
        } catch (Exception e) {
            return "Error al obtener la respuesta: " + e.getMessage();
        }
    }
}
