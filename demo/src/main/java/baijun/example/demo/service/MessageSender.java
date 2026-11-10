package baijun.example.demo.service;

import baijun.example.demo.config.RabbitConfig;
import baijun.example.demo.dto.ChatMessage;
import baijun.example.demo.entity.ChatRecord;
import baijun.example.demo.repository.ChatRecordRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class MessageSender {

    private final RabbitTemplate rabbitTemplate;
    private final ChatRecordRepository chatRecordRepository;
    private final SseService sseService;

    public MessageSender(RabbitTemplate rabbitTemplate, ChatRecordRepository chatRecordRepository, SseService sseService) {
        this.rabbitTemplate = rabbitTemplate;
        this.chatRecordRepository = chatRecordRepository;
        this.sseService = sseService;
    }

    public void sendPublic(ChatMessage message) {
        rabbitTemplate.convertAndSend(RabbitConfig.PUBLIC_EXCHANGE, "", message);
        save(message);
    }

    public void sendPrivate(ChatMessage message) {
        rabbitTemplate.convertAndSend(RabbitConfig.PRIVATE_EXCHANGE, "user." + message.getReceiver(), message);
        save(message);
        if (!message.getSender().equals(message.getReceiver())) {
            sseService.pushToUser(message.getSender(), message);
        }
    }

    private void save(ChatMessage message) {
        ChatRecord record = new ChatRecord();
        record.setSender(message.getSender());
        record.setReceiver(message.getReceiver());
        record.setContent(message.getContent());
        record.setType(message.getType());
        record.setFileUrl(message.getFileUrl());
        record.setCreatedAt(message.getTimestamp());
        chatRecordRepository.save(record);
    }
}
