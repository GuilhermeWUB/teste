package com.necsus.necsusspring.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path uploadDir = Paths.get("uploads");

    // Tipos de arquivo permitidos (imagens)
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".gif");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
        "image/jpeg", "image/jpg", "image/png", "image/gif"
    );

    // Tamanho máximo: 10MB
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

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
            // Validação de tamanho
            if (file.getSize() > MAX_FILE_SIZE) {
                throw new IllegalArgumentException("Arquivo muito grande. Tamanho máximo: 10MB");
            }

            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.trim().isEmpty()) {
                throw new IllegalArgumentException("Nome de arquivo inválido");
            }

            // Prevenir path traversal
            if (originalFilename.contains("..") || originalFilename.contains("/") || originalFilename.contains("\\")) {
                throw new IllegalArgumentException("Nome de arquivo contém caracteres inválidos");
            }

            String extension = "";
            if (originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
            }

            // Validação de extensão
            if (!ALLOWED_EXTENSIONS.contains(extension)) {
                throw new IllegalArgumentException("Tipo de arquivo não permitido. Apenas imagens JPG, PNG e GIF são aceitas");
            }

            // Validação de content type
            String contentType = file.getContentType();
            if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
                throw new IllegalArgumentException("Tipo de conteúdo inválido");
            }

            String filename = UUID.randomUUID().toString() + extension;
            Path targetLocation = uploadDir.resolve(filename);

            // Garantir que o arquivo está dentro do diretório de uploads
            if (!targetLocation.normalize().startsWith(uploadDir.normalize())) {
                throw new IllegalArgumentException("Tentativa de path traversal detectada");
            }

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

            // Validação de segurança: garantir que o path está dentro do diretório de uploads
            if (!path.normalize().startsWith(uploadDir.normalize())) {
                throw new IllegalArgumentException("Tentativa de deletar arquivo fora do diretório permitido");
            }

            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao deletar arquivo: " + filePath, e);
        }
    }
}
