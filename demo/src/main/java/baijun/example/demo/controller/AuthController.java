package baijun.example.demo.controller;

import baijun.example.demo.service.ChatSessionService;
import baijun.example.demo.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    private final UserService userService;
    private final ChatSessionService chatSessionService;

    public AuthController(UserService userService, ChatSessionService chatSessionService) {
        this.userService = userService;
        this.chatSessionService = chatSessionService;
    }

    @PostMapping("/register")
    public String register(@RequestParam String username, @RequestParam String password) {
        try {
            userService.register(username, password);
            return "ok";
        } catch (IllegalArgumentException e) {
            return "注册失败: " + e.getMessage();
        }
    }

    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password, HttpSession session) {
        try {
            userService.login(username, password);
        } catch (IllegalArgumentException e) {
            return "登录失败: " + e.getMessage();
        }
        session.setAttribute("user", username);
        chatSessionService.startSession(username);
        return "ok";
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        Object user = session.getAttribute("user");
        if (user != null) {
            chatSessionService.stopSession(user.toString());
        }
        session.invalidate();
        return "ok";
    }

    @GetMapping("/me")
    public String me(HttpSession session) {
        Object user = session.getAttribute("user");
        return user == null ? "" : user.toString();
    }
}
