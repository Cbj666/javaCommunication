package baijun.example.demo.listener;

import baijun.example.demo.dto.ChatMessage;
import baijun.example.demo.service.SseService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class MessageListener {

    private final SseService sseService;

    public MessageListener(SseService sseService) {
        this.sseService = sseService;
    }

    @RabbitListener(queues = "#{publicQueue.name}")
    public void onPublicMessage(ChatMessage message) {
        sseService.broadcast(message);
    }
}
