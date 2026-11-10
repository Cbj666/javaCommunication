package baijun.example.demo.service;

import baijun.example.demo.config.RabbitConfig;
import baijun.example.demo.dto.ChatMessage;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatSessionService {

    private final ConnectionFactory connectionFactory;
    private final AmqpAdmin amqpAdmin;
    private final MessageConverter messageConverter;
    private final SseService sseService;

    private final Map<String, SimpleMessageListenerContainer> containers = new ConcurrentHashMap<>();

    public ChatSessionService(ConnectionFactory connectionFactory,
                              AmqpAdmin amqpAdmin,
                              MessageConverter messageConverter,
                              SseService sseService) {
        this.connectionFactory = connectionFactory;
        this.amqpAdmin = amqpAdmin;
        this.messageConverter = messageConverter;
        this.sseService = sseService;
    }

    public synchronized void startSession(String username) {
        if (containers.containsKey(username)) {
            return;
        }
        String queueName = "chat.user." + username;
        amqpAdmin.declareQueue(new Queue(queueName, true));
        amqpAdmin.declareBinding(new Binding(queueName, Binding.DestinationType.QUEUE,
                RabbitConfig.PRIVATE_EXCHANGE, "user." + username, null));

        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);
        container.setQueueNames(queueName);
        container.setMessageListener(message -> {
            Object body = messageConverter.fromMessage(message);
            if (body instanceof ChatMessage chatMessage) {
                sseService.pushToUser(chatMessage.getReceiver(), chatMessage);
            }
        });
        container.start();
        containers.put(username, container);
    }

    public synchronized void stopSession(String username) {
        SimpleMessageListenerContainer container = containers.remove(username);
        if (container != null) {
            container.stop();
        }
    }
}
