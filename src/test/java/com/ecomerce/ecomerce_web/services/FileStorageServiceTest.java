package com.ecomerce.ecomerce_web.services;

import com.ecomerce.ecomerce_web.exception.InvalidRequestException;
import com.ecomerce.ecomerce_web.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

class FileStorageServiceTest {

    private FileStorageService fileStorageService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        fileStorageService = new FileStorageService();
        ReflectionTestUtils.setField(fileStorageService, "uploadDir", tempDir.toString());
    }

    private byte[] realPngBytes() throws IOException {
        // minimal valid 1x1 PNG so Apache Tika detects image/png correctly
        try (InputStream is = getClass().getResourceAsStream("/test-files/sample.png")) {
            if (is == null) throw new IllegalStateException(
                    "Add a real 1x1 PNG to src/test/resources/test-files/sample.png for this test");
            return is.readAllBytes();
        }
    }

    @Test
    @DisplayName("should save a valid PNG file and return its URL")
    void shouldSaveValidFile() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.png", "image/png", realPngBytes());

        String url = fileStorageService.saveFile(file);

        assertThat(url).startsWith("/api/files/").endsWith(".png");
    }

    @Test
    @DisplayName("should reject empty file")
    void shouldRejectEmptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> fileStorageService.saveFile(file))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("empty");
    }

    @Test
    @DisplayName("should reject file exceeding 2MB")
    void shouldRejectOversizedFile() {
        byte[] tooLarge = new byte[3 * 1024 * 1024];
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", tooLarge);

        assertThatThrownBy(() -> fileStorageService.saveFile(file))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("too large");
    }

    @Test
    @DisplayName("should reject disallowed extension")
    void shouldRejectDisallowedExtension() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "malware.exe", "application/octet-stream", realPngBytes());

        assertThatThrownBy(() -> fileStorageService.saveFile(file))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("not allowed");
    }

    @Test
    @DisplayName("should reject double-extension attack (photo.php.png)")
    void shouldRejectDoubleExtensionAttack() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.php.png", "image/png", realPngBytes());

        assertThatThrownBy(() -> fileStorageService.saveFile(file))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Double extension");
    }

    @Test
    @DisplayName("should reject content-type spoofing (declared PNG, actual content differs)")
    void shouldRejectMimeTypeMismatch() {
        // Real content is plain text, but declared as image/png
        byte[] fakeContent = "this is not really a png".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", fakeContent);

        assertThatThrownBy(() -> fileStorageService.saveFile(file))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    @DisplayName("loadFile should reject path traversal attempts")
    void shouldRejectPathTraversalOnLoad() {
        assertThatThrownBy(() -> fileStorageService.loadFile("../../etc/passwd"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("loadFile should reject filenames not matching UUID pattern")
    void shouldRejectInvalidFilenameFormat() {
        assertThatThrownBy(() -> fileStorageService.loadFile("not-a-valid-uuid.png"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("loadFile should throw when file does not exist on disk")
    void shouldThrowWhenFileMissing() {
        String fakeUuid = "550e8400-e29b-41d4-a716-446655440000.png";

        assertThatThrownBy(() -> fileStorageService.loadFile(fakeUuid))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("saved file should be loadable afterward")
    void shouldRoundTripSaveAndLoad() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.png", "image/png", realPngBytes());

        String url = fileStorageService.saveFile(file);
        String filename = url.substring(url.lastIndexOf("/") + 1);

        Resource resource = fileStorageService.loadFile(filename);

        assertThat(resource.exists()).isTrue();
    }

    @Test
    @DisplayName("deleteFile should remove file and not throw if already gone")
    void shouldDeleteFileSilentlyIfMissing() {
        assertThatCode(() -> fileStorageService.deleteFile("/api/files/nonexistent.png"))
                .doesNotThrowAnyException();
    }
}