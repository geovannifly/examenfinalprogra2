package com.miumg.chatbottelegram.Service;


import com.miumg.chatbottelegram.model.ChatMessage;
import com.miumg.chatbottelegram.repository.ChatMessageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class MessageService {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    public ChatMessage saveMessage(ChatMessage message) {
        return chatMessageRepository.save(message);
    }

    public boolean deleteById(Long productId) {
        if(chatMessageRepository.existsById(productId)) {
            chatMessageRepository.deleteById(productId);
            return true;
        }
        return false;
    }

    public List<ChatMessage> getLastMessages(String cliente) {

        List<ChatMessage> response = chatMessageRepository.findLast3MessagesByClient(cliente);
        if(!response.isEmpty()){
            return response.subList(0, 3);
        }else{
            return response;
        }

    }

}
