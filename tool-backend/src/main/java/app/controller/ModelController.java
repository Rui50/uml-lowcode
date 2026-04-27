package app.controller;

import app.dto.ParseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
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

}
