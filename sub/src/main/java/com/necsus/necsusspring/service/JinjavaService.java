package com.necsus.necsusspring.service;

import com.hubspot.jinjava.Jinjava;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class JinjavaService {

    private final Jinjava jinjava;

    public JinjavaService() {
        this.jinjava = new Jinjava();
    }

    /**
     * Renderiza um template Jinjava com os dados fornecidos
     * @param templatePath caminho do template em resources/templates/jinjava/
     * @param context mapa com os dados para o template
     * @return HTML renderizado
     */
    public String render(String templatePath, Map<String, Object> context) {
        try {
            String template = loadTemplate(templatePath);
            return jinjava.render(template, context);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao carregar template: " + templatePath, e);
        }
    }

    /**
     * Renderiza um template string diretamente
     * @param template string do template
     * @param context mapa com os dados
     * @return HTML renderizado
     */
    public String renderString(String template, Map<String, Object> context) {
        return jinjava.render(template, context);
    }

    private String loadTemplate(String templatePath) throws IOException {
        ClassPathResource resource = new ClassPathResource("templates/jinjava/" + templatePath);
        return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }
}
