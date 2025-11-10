package com.necsus.necsusspring.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path uploadDir = Paths.get("uploads");

    public FileStorageService() {
        try {
            Files.createDirectories(uploadDir);
        } catch (IOException e) {
            throw new RuntimeException("Não foi possível criar o diretório de uploads", e);
        }
    }

    /**
     * Armazena um único arquivo
     * @param file arquivo a ser armazenado
     * @return path do arquivo armazenado ou null se o arquivo estiver vazio
     */
    public String storeFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        try {
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            String filename = UUID.randomUUID().toString() + extension;
            Path targetLocation = uploadDir.resolve(filename);

            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            return targetLocation.toString();
        } catch (IOException e) {
            throw new RuntimeException("Erro ao salvar arquivo: " + file.getOriginalFilename(), e);
        }
    }

    public List<String> storeFiles(MultipartFile[] files) {
        List<String> filePaths = new ArrayList<>();

        if (files == null || files.length == 0) {
            return filePaths;
        }

        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                String filePath = storeFile(file);
                if (filePath != null) {
                    filePaths.add(filePath);
                }
            }
        }

        return filePaths;
    }

    public void deleteFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao deletar arquivo: " + filePath, e);
        }
    }
}
