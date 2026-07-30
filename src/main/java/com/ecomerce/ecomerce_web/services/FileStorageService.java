package com.ecomerce.ecomerce_web.services;

import com.ecomerce.ecomerce_web.exception.InvalidRequestException;
import com.ecomerce.ecomerce_web.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.rmi.RemoteException;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {
    @Value("${file.upload-dir}")
    private String uploadDir;
    private final Tika tika = new Tika();

    private static final List<String> ALLOWED_MIME_TYPES = List.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );
    private static final List<String> ALLOWED_EXTENSIONS = List.of(
            ".jpg",".jpeg",".png",".webp"
    );

    private static final long MAX_FILE_SIZE = 2 * 1024 * 1024;

    public String saveFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty())
            throw new InvalidRequestException("File is Empty");

        if (file.getSize() > MAX_FILE_SIZE)
            throw new InvalidRequestException("File too large , max file size is 2MB");

        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || originalFileName.isBlank())
            throw new InvalidRequestException("Invalid fileName");

        String sanitizedFilename = Paths.get(originalFileName)
                .getFileName()
                .toString()
                .replaceAll("[^a-zA-Z0-9._-]", "");

        if (sanitizedFilename.contains("\0"))
            throw new InvalidRequestException("Invalid fileName");

        String extension = getExtension(sanitizedFilename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension))
            throw new InvalidRequestException("File type not allowed. Only JPG, PNG, WEBP");

        String detectedMimeType = tika.detect(file.getInputStream());
        if (!ALLOWED_MIME_TYPES.contains(detectedMimeType))
            throw new InvalidRequestException("Invalid file content. Detected: "+detectedMimeType);

        String declaredMimeType = file.getContentType();
        if (!ALLOWED_MIME_TYPES.contains(declaredMimeType))
            throw new InvalidRequestException("invalid content type header");

        if (!detectedMimeType.equals(declaredMimeType))
            throw new InvalidRequestException(
                    "Content type mismatch: declared=" + declaredMimeType
                            + " detected=" + detectedMimeType);

        String newFilename = UUID.randomUUID() + extension;
        Path uploadBase = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(uploadBase);
        Path targetPath = uploadBase.resolve(newFilename).normalize();

        if (!targetPath.startsWith(uploadBase))
            throw new InvalidRequestException("Path traversal detected");

        Files.copy(file.getInputStream(),targetPath, StandardCopyOption.REPLACE_EXISTING);
        log.info("file saved securely: {} ", newFilename);
        return "/api/files/" + newFilename;

    }

    public Resource loadFile(String filename){

        try{
            if (filename.contains("..")
                    || filename.contains("/")
                    || filename.contains("\\"))
                throw new InvalidRequestException("Invalid filename");

            if (!filename.matches("^[a-f0-9-]{36}\\.(jpg|jpeg|png|webp)$"))
                throw new InvalidRequestException("Invalid filename format");
            Path uploadBase = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path filePath = uploadBase.resolve(filename).normalize();
            if (!filePath.startsWith(uploadBase))
                throw new InvalidRequestException("Path traversal detected");

            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists())
                throw new ResourceNotFoundException("File not Found");

            return resource;
        } catch (Exception e) {
            throw new ResourceNotFoundException("File not found: " + filename);
        }

    }

    public void deleteFile(String fileUrl){
      try {
          String filename = fileUrl.substring(
                  fileUrl.lastIndexOf("/") + 1
          );
          Path uploadBase = Paths.get(uploadDir).toAbsolutePath().normalize();
          Path filePath  = uploadBase.resolve(filename).normalize();
          if (!filePath.startsWith(uploadBase))
              throw new InvalidRequestException("Path traversal detected on delete");

          Files.deleteIfExists(filePath);
          log.info("File deleted: {}", filename);
      }catch (IOException e){
          log.error("Failed to delete file: {}", e.getMessage());
      }
    }
    private String getExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot == -1 || lastDot == filename.length() -1 )
            throw new InvalidRequestException("No file extension found");

        String fullExtension  = filename.substring(lastDot).toLowerCase();
        String beforeExtension = filename.substring(0,lastDot).toLowerCase();

        List<String> dangerousExtensions = List.of(
                ".php", ".jsp", ".exe", ".sh", ".bat",
                ".py",".java" ,".js", ".html", ".xml", ".svg"
        );
        for (String dangerous : dangerousExtensions){
            if (beforeExtension.contains(dangerous))
                throw new InvalidRequestException("Double extension attack detected");
        }
 return fullExtension;
    }
}
