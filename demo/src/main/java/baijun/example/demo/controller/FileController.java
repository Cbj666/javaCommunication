package baijun.example.demo.controller;

import baijun.example.demo.dto.ChatMessage;
import baijun.example.demo.service.MessageSender;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@RestController
public class FileController {

    private static final Map<String, MediaType> IMAGE_TYPES = Map.of(
            "jpg", MediaType.IMAGE_JPEG,
            "jpeg", MediaType.IMAGE_JPEG,
            "png", MediaType.IMAGE_PNG,
            "gif", MediaType.IMAGE_GIF,
            "webp", MediaType.parseMediaType("image/webp"),
            "bmp", MediaType.parseMediaType("image/bmp"),
            "svg", MediaType.parseMediaType("image/svg+xml")
    );

    private final MessageSender messageSender;
    private final Path uploadDir = Paths.get("uploads").toAbsolutePath().normalize();

    public FileController(MessageSender messageSender) {
        this.messageSender = messageSender;
    }

    @PostMapping("/sendFile")
    public String sendFile(@RequestParam("to") String to, @RequestParam("file") MultipartFile file, HttpSession session) throws IOException {
        String sender = currentUser(session);
        if (sender == null) {
            return "未登录";
        }
        if (file.isEmpty()) {
            return "文件为空";
        }
        StoredFile stored = storeFile(file);
        ChatMessage message = new ChatMessage(sender, to, stored.originalName(), "file", System.currentTimeMillis());
        message.setFileUrl(stored.url());
        messageSender.sendPrivate(message);
        return "ok";
    }

    @PostMapping("/sendPublicFile")
    public String sendPublicFile(@RequestParam("file") MultipartFile file, HttpSession session) throws IOException {
        String sender = currentUser(session);
        if (sender == null) {
            return "未登录";
        }
        if (file.isEmpty()) {
            return "文件为空";
        }
        StoredFile stored = storeFile(file);
        ChatMessage message = new ChatMessage(sender, null, stored.originalName(), "file", System.currentTimeMillis());
        message.setFileUrl(stored.url());
        messageSender.sendPublic(message);
        return "ok";
    }

    @GetMapping("/files/{name}")
    public ResponseEntity<Resource> download(@PathVariable String name) {
        Path target = uploadDir.resolve(name).normalize();
        if (!target.startsWith(uploadDir) || !Files.exists(target)) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = new FileSystemResource(target);
        String downloadName = name.contains("__") ? name.substring(name.indexOf("__") + 2) : name;

        MediaType imageType = imageType(downloadName);
        if (imageType != null) {
            ContentDisposition disposition = ContentDisposition.inline().filename(downloadName, StandardCharsets.UTF_8).build();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                    .contentType(imageType)
                    .body(resource);
        }

        ContentDisposition disposition = ContentDisposition.attachment().filename(downloadName, StandardCharsets.UTF_8).build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    private StoredFile storeFile(MultipartFile file) throws IOException {
        Files.createDirectories(uploadDir);
        String rawName = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String originalName = Paths.get(rawName).getFileName().toString();
        String storedName = UUID.randomUUID() + "__" + originalName;
        Path target = uploadDir.resolve(storedName).normalize();
        file.transferTo(target);
        String url = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/files/").path(storedName).toUriString();
        return new StoredFile(originalName, url);
    }

    private String currentUser(HttpSession session) {
        Object user = session.getAttribute("user");
        return user == null ? null : user.toString();
    }

    private MediaType imageType(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0) {
            return null;
        }
        String ext = fileName.substring(dot + 1).toLowerCase();
        return IMAGE_TYPES.get(ext);
    }

    private record StoredFile(String originalName, String url) {
    }
}
