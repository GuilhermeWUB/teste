package com.necsus.necsusspring.service;

import com.necsus.necsusspring.model.AmbienteNfe;
import com.necsus.necsusspring.model.NfeConfig;
import com.necsus.necsusspring.repository.NfeConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

/**
 * Service para gerenciar as configurações da NFe
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NfeConfigService {

    private final NfeConfigRepository nfeConfigRepository;

    /**
     * Diretório onde os certificados serão armazenados
     */
    private static final String CERTIFICADOS_DIR = "certificados/nfe/";

    /**
     * Retorna a configuração ativa ou a primeira configuração encontrada
     */
    @Transactional(readOnly = true)
    public Optional<NfeConfig> getConfig() {
        return nfeConfigRepository.findFirstByAtivoTrue()
                .or(() -> nfeConfigRepository.findAll().stream().findFirst());
    }

    /**
     * Busca configuração por ID
     */
    @Transactional(readOnly = true)
    public Optional<NfeConfig> getConfigById(Long id) {
        return nfeConfigRepository.findById(id);
    }

    /**
     * Salva ou atualiza a configuração da NFe
     */
    @Transactional
    public NfeConfig saveConfig(NfeConfig config) {
        log.info("Salvando configuração NFe para CNPJ: {}", config.getCnpj());
        return nfeConfigRepository.save(config);
    }

    /**
     * Salva ou atualiza a configuração com upload de certificado
     */
    @Transactional
    public NfeConfig saveConfigWithCertificate(
            String cnpj,
            String senha,
            String uf,
            AmbienteNfe ambiente,
            MultipartFile certificadoFile
    ) throws IOException {

        log.info("Salvando configuração NFe com certificado para CNPJ: {}", cnpj);

        // Cria o diretório se não existir
        Path certificadosPath = Paths.get(CERTIFICADOS_DIR);
        if (!Files.exists(certificadosPath)) {
            Files.createDirectories(certificadosPath);
            log.info("Diretório de certificados criado: {}", CERTIFICADOS_DIR);
        }

        // Salva o arquivo do certificado
        String fileName = cnpj + "_" + System.currentTimeMillis() + ".pfx";
        Path targetPath = certificadosPath.resolve(fileName);

        Files.copy(certificadoFile.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        log.info("Certificado salvo em: {}", targetPath.toAbsolutePath());

        // Busca ou cria nova configuração
        NfeConfig config = nfeConfigRepository.findByCnpj(cnpj)
                .orElse(new NfeConfig());

        config.setCnpj(cnpj);
        config.setCertificadoPath(targetPath.toAbsolutePath().toString());
        config.setCertificadoSenha(senha);
        config.setUf(uf != null ? uf : "SP");
        config.setAmbiente(ambiente != null ? ambiente : AmbienteNfe.HOMOLOGACAO);
        config.setAtivo(true);

        // Se é a primeira configuração, inicializa o NSU
        if (config.getId() == null) {
            config.setUltimoNsu("0");
        }

        return nfeConfigRepository.save(config);
    }

    /**
     * Atualiza o último NSU consultado
     */
    @Transactional
    public void atualizarUltimoNsu(Long configId, String novoNsu) {
        nfeConfigRepository.findById(configId).ifPresent(config -> {
            log.info("Atualizando último NSU de {} para {}", config.getUltimoNsu(), novoNsu);
            config.setUltimoNsu(novoNsu);
            nfeConfigRepository.save(config);
        });
    }

    /**
     * Ativa ou desativa a consulta automática
     */
    @Transactional
    public void toggleAtivo(Long configId, boolean ativo) {
        nfeConfigRepository.findById(configId).ifPresent(config -> {
            log.info("Alterando status ativo de {} para {}", config.getAtivo(), ativo);
            config.setAtivo(ativo);
            nfeConfigRepository.save(config);
        });
    }

    /**
     * Valida se o certificado existe no disco
     */
    public boolean validarCertificado(NfeConfig config) {
        if (config == null || config.getCertificadoPath() == null) {
            return false;
        }

        File certificado = new File(config.getCertificadoPath());
        boolean existe = certificado.exists() && certificado.canRead();

        if (!existe) {
            log.error("Certificado não encontrado ou sem permissão de leitura: {}",
                     config.getCertificadoPath());
        }

        return existe;
    }

    /**
     * Deleta a configuração
     */
    @Transactional
    public void deleteConfig(Long id) {
        nfeConfigRepository.deleteById(id);
        log.info("Configuração NFe deletada: {}", id);
    }
}
