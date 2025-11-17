package com.necsus.necsusspring.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);

    private final Path uploadDir = Paths.get("uploads");

    public FileStorageService() {
        try {
            Files.createDirectories(uploadDir);
            logger.info("Diretório de uploads criado/verificado: {}", uploadDir.toAbsolutePath());
        } catch (IOException e) {
            logger.error("Erro ao criar diretório de uploads", e);
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
            logger.warn("Arquivo nulo ou vazio recebido");
            return null;
        }

        try {
            String originalFilename = file.getOriginalFilename();
            logger.info("Salvando arquivo: {}, tamanho: {} bytes", originalFilename, file.getSize());

            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            String filename = UUID.randomUUID().toString() + extension;
            Path targetLocation = uploadDir.resolve(filename);

            logger.info("Path de destino: {}", targetLocation.toAbsolutePath());

            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            logger.info("Arquivo salvo com sucesso: {}", targetLocation.toString());
            return targetLocation.toString();
        } catch (IOException e) {
            logger.error("Erro ao salvar arquivo: {}", file.getOriginalFilename(), e);
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
