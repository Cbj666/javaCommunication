package baijun.example.demo.controller;

import baijun.example.demo.entity.ChatRecord;
import baijun.example.demo.repository.ChatRecordRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@RestController
public class ChatHistoryController {

    private final ChatRecordRepository chatRecordRepository;

    public ChatHistoryController(ChatRecordRepository chatRecordRepository) {
        this.chatRecordRepository = chatRecordRepository;
    }

    @GetMapping("/history/public")
    public List<ChatRecord> publicHistory() {
        List<ChatRecord> list = chatRecordRepository.findTop100ByReceiverIsNullOrderByIdDesc();
        Collections.reverse(list);
        return list;
    }

    @GetMapping("/history/private")
    public List<ChatRecord> privateHistory(@RequestParam String with, HttpSession session) {
        Object me = session.getAttribute("user");
        if (me == null) {
            return List.of();
        }
        List<ChatRecord> list = chatRecordRepository.findPrivateBetween(me.toString(), with, PageRequest.of(0, 200));
        Collections.reverse(list);
        return list;
    }
}
