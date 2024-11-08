package com.miumg.chatbottelegram.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import com.miumg.chatbottelegram.model.ChatMessage;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TelegramBotService extends TelegramLongPollingBot {

    private final String botName;
    private final Map<Long, String> userStates = new HashMap<>();
    @Autowired
    private OpenAIService openAIService;

    @Autowired
    private MessageService messageService;

    public TelegramBotService(String botName, String botToken) {
        super(botToken);
        this.botName = botName;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            Message message = update.getMessage();
            Long chatId = message.getChatId();
            String messageText = message.getText();
            String userNameTelegram = message.getFrom().getUserName();

            try {
                if (Objects.equals(messageText, "/start")) {
                    iniciarConversacion(chatId);
                } else if ("waitting_name".equals(userStates.get(chatId))) {
                    if (userNameTelegram == null) {
                        message.getFrom().setUserName(messageText);
                        userNameTelegram = message.getFrom().getUserName();
                    }
                    obtenerNombreUsuario(chatId, messageText, userNameTelegram);
                } else {
                    sendUserMessageAI(chatId, messageText, userNameTelegram);
                }
            } catch (Exception e) {
                log.error("Error durante la ejecución de la API de Telegram: {}", e.getMessage());
            }
        }
    }

    /**
     * Inicia la conversación y solicita el nombre del usuario.
     */
    private void iniciarConversacion(Long chatId) throws TelegramApiException {
        userStates.put(chatId, "waitting_name");
        sendMessage(chatId, "¡Hola! ¿Cuál es tu nombre?");
    }

    /**
     * Procesa el nombre del usuario y envía un saludo personalizado.
     */
    private void obtenerNombreUsuario(Long chatId, String name, String userName) throws TelegramApiException {
        userStates.remove(chatId); // Limpiamos el estado
        List<ChatMessage> lastMessages = getLastMessages(userName);
        if (!lastMessages.isEmpty()) {
            log.info("No esta vacio el retorno");
            sendMessage(chatId, "Veo que ya estuviste por aca antes, estos son tus ultimos respuestas:");
            for (ChatMessage chatMessage : lastMessages) {
                sendMessage(chatId, chatMessage.getAnswer());
            }

        } else {
            String response = "¡Mucho gusto, " + name + "! ¿En qué puedo ayudarte?";

            sendMessage(chatId, response);

        }
        log.info("Ultimos mensajes del usuario: {}", lastMessages);

    }

    /**
     * Envía un mensaje al usuario a través de Telegram.
     */
    private void sendMessage(Long chatId, String response) throws TelegramApiException {
        SendMessage mensaje = new SendMessage(chatId.toString(), response);
        execute(mensaje);
    }

    private void sendUserMessageAI(Long chatId, String userMessage, String userNameTelegram) throws Exception {
        try {
            String responseAI = openAIService.obtenerRespuesta(userMessage);
            ChatMessage chatMessageDB = new ChatMessage();

            chatMessageDB.setQuestion(userMessage);
            chatMessageDB.setAnswer(responseAI);
            chatMessageDB.setClient(userNameTelegram);
            chatMessageDB.setChatId(chatId);

            saveMessageDB(chatMessageDB);
            sendMessage(chatId, responseAI);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void saveMessageDB(ChatMessage chatMessage) {
        this.messageService.saveMessage(chatMessage);
    }

    private List<ChatMessage> getLastMessages(String client) {
        return this.messageService.getLastMessages(client);
    }

    @Override
    public String getBotUsername() {
        return this.botName;
    }
}