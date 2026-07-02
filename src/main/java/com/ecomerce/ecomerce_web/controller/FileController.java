package com.ecomerce.ecomerce_web.controller;

import com.ecomerce.ecomerce_web.services.FileStorageService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

@RestController
@RequestMapping("/api/files")
@AllArgsConstructor
@Slf4j
public class FileController {

    private final FileStorageService fileStorageService;

    @PostMapping("/uplaod")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String>uploadFile(
            @RequestParam("file") MultipartFile file
    )throws IOException
    {
     String fileUrl = fileStorageService.saveFile(file);
     return ResponseEntity.ok(fileUrl);

    }
    @GetMapping("/{filename}")
    public ResponseEntity<Resource> getFile(
            @PathVariable String filename
    ){
        Resource resource = fileStorageService.loadFile(filename);
        String contentType = "application/octet-stream";
        try{
            contentType = Files.probeContentType(Paths.get(Objects.requireNonNull(resource.getFilename())));
            if (contentType == null) contentType = "image/jpeg";


        }catch (IOException e){
            log.warn("Could not determine file type");
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,"inline; filename=\""
                        + resource.getFilename() + "\"")
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Security-Policy",
                        "default-src 'none'; img-src 'self'")
                .header("X-Frame-Options", "DENY")
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }
}
