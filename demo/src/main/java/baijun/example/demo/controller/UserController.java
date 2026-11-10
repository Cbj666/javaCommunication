package baijun.example.demo.controller;

import baijun.example.demo.service.SseService;
import baijun.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class UserController {

    private final UserService userService;
    private final SseService sseService;
    private final String owner;

    public UserController(UserService userService, SseService sseService, @Value("${app.owner:}") String owner) {
        this.userService = userService;
        this.sseService = sseService;
        this.owner = owner;
    }

    @GetMapping("/users")
    public List<String> users() {
        return userService.listUsers();
    }

    @GetMapping("/online")
    public ResponseEntity<List<String>> online() {
        // 在线列表必须实时，禁止浏览器/代理缓存
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(sseService.onlineUsers());
    }

    @GetMapping("/owner")
    public String owner() {
        return owner;
    }
}
