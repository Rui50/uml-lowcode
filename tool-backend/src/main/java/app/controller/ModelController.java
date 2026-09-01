package app.controller;

import app.dto.AnnotationsDTO;
import app.dto.GenerateRequestDTO;
import app.dto.ParseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import app.service.ToolService;

import java.io.File;
import java.nio.file.Files;

@RestController
@RequestMapping("/api/model")
@RequiredArgsConstructor
public class ModelController {
    private final ToolService toolService;

    @PostMapping("/parse")
    public ParseResponse parse(@RequestParam("file") MultipartFile file) throws Exception {
        File temp = File.createTempFile("upload-", ".xmi");
        try {
            file.transferTo(temp);
            return toolService.parse(temp.getAbsolutePath());
        } finally {
            Files.deleteIfExists(temp.toPath());
        }
    }

    @PostMapping("/generate")
    public ResponseEntity<byte[]> generate(
            @RequestParam(name = "target", defaultValue = "plain") String target,
            @RequestBody(required = false) GenerateRequestDTO request
    ) throws Exception {
        if (request == null) request = new GenerateRequestDTO();
        byte[] zip = toolService.generate(target, request.getAnnotations(), request.getSettings());

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"generated-" + target + ".zip\"")
                .body(zip);
    }

    // erros de cliente (OCL invalid/vazia, target desconhecido, modelo não carregado)
    @ExceptionHandler({ IllegalArgumentException.class, IllegalStateException.class })
    public ResponseEntity<String> handleBadRequest(RuntimeException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}
