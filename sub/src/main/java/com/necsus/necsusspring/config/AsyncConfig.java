package com.necsus.necsusspring.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Configuração para habilitar processamento assíncrono na aplicação.
 * Necessário para que os listeners de notificação funcionem de forma assíncrona.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    // Esta classe habilita o suporte a @Async na aplicação
    // Os listeners de notificação usam @Async para não bloquear
    // o thread principal ao enviar notificações
}
