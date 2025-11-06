package com.necsus.necsusspring.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class FileStorageServiceTest {

    private FileStorageService fileStorageService;
    private Path testUploadDir = Paths.get("test-uploads");

    @BeforeEach
    public void setUp() throws IOException {
        // Clean up test directory if it exists
        if (Files.exists(testUploadDir)) {
            Files.walk(testUploadDir)
                    .sorted((a, b) -> -a.compareTo(b))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            // Ignore
                        }
                    });
        }

        fileStorageService = new FileStorageService();
    }

    @AfterEach
    public void tearDown() throws IOException {
        // Clean up after tests
        Path uploadDir = Paths.get("uploads");
        if (Files.exists(uploadDir)) {
            Files.walk(uploadDir)
                    .filter(Files::isRegularFile)
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            // Ignore
                        }
                    });
        }
    }

    @Test
    public void testStoreFiles_WithValidFiles_ShouldReturnFilePaths() {
        MockMultipartFile file1 = new MockMultipartFile(
                "file1",
                "test1.txt",
                "text/plain",
                "Test content 1".getBytes()
        );
        MockMultipartFile file2 = new MockMultipartFile(
                "file2",
                "test2.pdf",
                "application/pdf",
                "Test content 2".getBytes()
        );

        MultipartFile[] files = {file1, file2};
        List<String> result = fileStorageService.storeFiles(files);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.get(0).contains(".txt"));
        assertTrue(result.get(1).contains(".pdf"));

        // Verify files were actually created
        assertTrue(Files.exists(Paths.get(result.get(0))));
        assertTrue(Files.exists(Paths.get(result.get(1))));
    }

    @Test
    public void testStoreFiles_WithEmptyFile_ShouldSkipEmptyFile() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "empty",
                "empty.txt",
                "text/plain",
                new byte[0]
        );
        MockMultipartFile validFile = new MockMultipartFile(
                "valid",
                "valid.txt",
                "text/plain",
                "Valid content".getBytes()
        );

        MultipartFile[] files = {emptyFile, validFile};
        List<String> result = fileStorageService.storeFiles(files);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).contains(".txt"));
    }

    @Test
    public void testStoreFiles_WithNullFiles_ShouldReturnEmptyList() {
        List<String> result = fileStorageService.storeFiles(null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testStoreFiles_WithEmptyArray_ShouldReturnEmptyList() {
        MultipartFile[] emptyArray = new MultipartFile[0];
        List<String> result = fileStorageService.storeFiles(emptyArray);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testStoreFiles_WithFileWithoutExtension_ShouldStoreWithoutExtension() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "noextension",
                "text/plain",
                "Test content".getBytes()
        );

        MultipartFile[] files = {file};
        List<String> result = fileStorageService.storeFiles(files);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertFalse(result.get(0).contains("."));
    }

    @Test
    public void testStoreFiles_ShouldGenerateUniqueFilenames() {
        MockMultipartFile file1 = new MockMultipartFile(
                "file1",
                "test.txt",
                "text/plain",
                "Content 1".getBytes()
        );
        MockMultipartFile file2 = new MockMultipartFile(
                "file2",
                "test.txt",
                "text/plain",
                "Content 2".getBytes()
        );

        MultipartFile[] files = {file1, file2};
        List<String> result = fileStorageService.storeFiles(files);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertNotEquals(result.get(0), result.get(1));
    }

    @Test
    public void testDeleteFile_WhenFileExists_ShouldDeleteSuccessfully() {
        // First create a file
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "delete-test.txt",
                "text/plain",
                "Test content".getBytes()
        );
        MultipartFile[] files = {file};
        List<String> storedFiles = fileStorageService.storeFiles(files);
        String filePath = storedFiles.get(0);

        // Verify file exists
        assertTrue(Files.exists(Paths.get(filePath)));

        // Delete the file
        assertDoesNotThrow(() -> fileStorageService.deleteFile(filePath));

        // Verify file was deleted
        assertFalse(Files.exists(Paths.get(filePath)));
    }

    @Test
    public void testDeleteFile_WhenFileDoesNotExist_ShouldNotThrowException() {
        String nonExistentPath = "uploads/nonexistent-file.txt";

        assertDoesNotThrow(() -> fileStorageService.deleteFile(nonExistentPath));
    }

    @Test
    public void testConstructor_ShouldCreateUploadDirectory() {
        Path uploadDir = Paths.get("uploads");
        assertTrue(Files.exists(uploadDir));
        assertTrue(Files.isDirectory(uploadDir));
    }
}
