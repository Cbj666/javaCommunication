package baijun.example.demo.controller;

import baijun.example.demo.service.SseService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class StreamController {

    private final SseService sseService;

    public StreamController(SseService sseService) {
        this.sseService = sseService;
    }

    @GetMapping("/stream")
    public SseEmitter stream(HttpSession session) {
        Object user = session.getAttribute("user");
        return sseService.register(user == null ? null : user.toString());
    }
}
