package baijun.example.demo.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ChatMessage {

    private String sender;
    private String receiver;
    private String content;
    private String type;
    private String fileUrl;
    private long timestamp;

    public ChatMessage() {
    }

    public ChatMessage(String sender, String receiver, String content, String type, long timestamp) {
        this.sender = sender;
        this.receiver = receiver;
        this.content = content;
        this.type = type;
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "[" + type + "] " + sender + " -> " + receiver + ": " + content;
    }
}
