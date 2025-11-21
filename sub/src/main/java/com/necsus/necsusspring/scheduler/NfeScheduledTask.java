package com.necsus.necsusspring.scheduler;

import com.necsus.necsusspring.service.NfeIntegrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job agendado para consultar automaticamente as notas na SEFAZ
 * Executa a cada hora
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NfeScheduledTask {

    private final NfeIntegrationService nfeIntegrationService;

    /**
     * Consulta as notas na SEFAZ a cada hora
     * Cron: 0 0 * * * * = A cada hora, no minuto 0
     */
    @Scheduled(cron = "0 0 * * * *")
    public void consultarNotasPeriodicamente() {
        log.info("========================================");
        log.info("Job agendado: Iniciando consulta de NFe na SEFAZ");
        log.info("========================================");

        try {
            int notasImportadas = nfeIntegrationService.consultarNotasSefaz();

            if (notasImportadas > 0) {
                log.info("Job concluído com sucesso! {} notas importadas", notasImportadas);
            } else {
                log.info("Job concluído. Nenhuma nota nova encontrada.");
            }

        } catch (Exception e) {
            log.error("Erro no job agendado de NFe: {}", e.getMessage(), e);
        }

        log.info("========================================");
    }

    /**
     * Job alternativo que roda a cada 6 horas (desabilitado por padrão)
     * Descomente se preferir menos frequência
     */
    // @Scheduled(cron = "0 0 */6 * * *")
    public void consultarNotasCada6Horas() {
        consultarNotasPeriodicamente();
    }

    /**
     * Job para teste que roda a cada 5 minutos (desabilitado por padrão)
     * Útil para testes em ambiente de desenvolvimento
     */
    // @Scheduled(cron = "0 */5 * * * *")
    public void consultarNotasParaTeste() {
        log.info("[TESTE] Executando consulta de teste a cada 5 minutos");
        consultarNotasPeriodicamente();
    }
}
