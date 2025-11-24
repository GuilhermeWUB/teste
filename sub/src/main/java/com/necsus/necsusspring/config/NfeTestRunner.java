package com.necsus.necsusspring.config;

import com.necsus.necsusspring.service.NfeIntegrationService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NfeTestRunner {

    @Bean
    public CommandLineRunner testarNfe(NfeIntegrationService nfeService) {
        return args -> {
            System.out.println("=================================================");
            System.out.println("🔥 INICIANDO TESTE DE NFE (CERTIFICADO FALSO) 🔥");
            System.out.println("=================================================");

            try {
                // Chama o método que dispara a consulta na SEFAZ
                nfeService.consultarNotasSefaz();
            } catch (Exception e) {
                System.out.println("❌ Erro capturado no teste: " + e.getMessage());
                e.printStackTrace();
            }

            System.out.println("=================================================");
            System.out.println("🔥 FIM DO TESTE 🔥");
            System.out.println("=================================================");
        };
    }
}
