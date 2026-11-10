package baijun.example.demo.controller;

import baijun.example.demo.dto.ChatMessage;
import baijun.example.demo.service.MessageSender;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MessageController {

    private final MessageSender messageSender;

    public MessageController(MessageSender messageSender) {
        this.messageSender = messageSender;
    }

    @PostMapping("/sendPublic")
    public String sendPublic(@RequestParam String content, HttpSession session) {
        String sender = currentUser(session);
        if (sender == null) {
            return "未登录";
        }
        ChatMessage message = new ChatMessage(sender, null, content, "public", System.currentTimeMillis());
        messageSender.sendPublic(message);
        return "ok";
    }

    @PostMapping("/sendPrivate")
    public String sendPrivate(@RequestParam String to, @RequestParam String content, HttpSession session) {
        String sender = currentUser(session);
        if (sender == null) {
            return "未登录";
        }
        ChatMessage message = new ChatMessage(sender, to, content, "private", System.currentTimeMillis());
        messageSender.sendPrivate(message);
        return "ok";
    }

    private String currentUser(HttpSession session) {
        Object user = session.getAttribute("user");
        return user == null ? null : user.toString();
    }
}
