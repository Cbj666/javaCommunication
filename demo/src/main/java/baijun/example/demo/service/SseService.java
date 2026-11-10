package baijun.example.demo.service;

import baijun.example.demo.dto.ChatMessage;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class SseService {

    /**
     * SSE 连接最长存活时间（兜底）。
     * 到期后 Spring 会结束该异步请求，触发 onTimeout 清理；
     * 浏览器 EventSource 会自动重连，建立一条新连接。
     */
    private static final long EMITTER_TIMEOUT_MS = 30 * 60 * 1000L;

    private final List<SseEmitter> all = new CopyOnWriteArrayList<>();
    private final Map<String, List<SseEmitter>> byUser = new ConcurrentHashMap<>();

    public SseEmitter register(String username) {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
        all.add(emitter);
        if (username != null) {
            byUser.computeIfAbsent(username, key -> new CopyOnWriteArrayList<>()).add(emitter);
        }
        Runnable cleanup = () -> remove(username, emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(error -> cleanup.run());
        return emitter;
    }

    public void broadcast(ChatMessage message) {
        for (SseEmitter emitter : all) {
            send(emitter, message);
        }
    }

    public void pushToUser(String username, ChatMessage message) {
        if (username == null) {
            return;
        }
        List<SseEmitter> emitters = byUser.get(username);
        if (emitters == null) {
            return;
        }
        for (SseEmitter emitter : emitters) {
            send(emitter, message);
        }
    }

    public List<String> onlineUsers() {
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, List<SseEmitter>> entry : byUser.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    /**
     * 心跳：定期向所有 SSE 连接写入一个注释帧（:ping）。
     * <p>
     * 这是在线状态能正确下线的关键。浏览器关闭、断网或连接已死时，
     * 这里的 send 会抛异常，随即把该连接从在线集合中移除。
     * 若没有心跳，服务器不写数据就永远发现不了死连接，用户会一直显示在线。
     * EventSource 会忽略注释帧，不会把它当成消息。
     */
    @Scheduled(fixedDelayString = "${app.sse.heartbeat-ms:20000}")
    public void heartbeat() {
        for (SseEmitter emitter : all) {
            try {
                emitter.send(SseEmitter.event().comment("ping"));
            } catch (Exception e) {
                removeEmitter(emitter);
                try {
                    emitter.completeWithError(e);
                } catch (Exception ignored) {
                    // 连接可能已经关闭，忽略
                }
            }
        }
    }

    private void remove(String username, SseEmitter emitter) {
        all.remove(emitter);
        if (username != null) {
            // computeIfPresent 保证“移除连接 + 列表为空则删 key”是一个原子操作
            byUser.computeIfPresent(username, (key, emitters) -> {
                emitters.remove(emitter);
                return emitters.isEmpty() ? null : emitters;
            });
        }
    }

    /** 不知道用户名时（心跳/发送失败），按 emitter 反查并移除。 */
    private void removeEmitter(SseEmitter emitter) {
        all.remove(emitter);
        byUser.entrySet().removeIf(entry -> {
            List<SseEmitter> emitters = entry.getValue();
            emitters.remove(emitter);
            return emitters.isEmpty();
        });
    }

    private void send(SseEmitter emitter, ChatMessage message) {
        try {
            emitter.send(SseEmitter.event().data(message, MediaType.APPLICATION_JSON));
        } catch (Exception e) {
            // 不依赖回调，立即从在线集合移除，避免死连接残留
            removeEmitter(emitter);
            try {
                emitter.completeWithError(e);
            } catch (Exception ignored) {
                // 连接可能已经关闭，忽略
            }
        }
    }
}
