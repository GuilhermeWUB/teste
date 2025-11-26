package com.necsus.necsusspring.service;

import com.necsus.necsusspring.dto.ActivityRequest;
import com.necsus.necsusspring.model.*;
import com.necsus.necsusspring.repository.CrmActivityRepository;
import com.necsus.necsusspring.repository.SaleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CrmActivityService {

    private final CrmActivityRepository activityRepository;
    private final SaleRepository saleRepository;

    public CrmActivityService(CrmActivityRepository activityRepository, SaleRepository saleRepository) {
        this.activityRepository = activityRepository;
        this.saleRepository = saleRepository;
    }

    public List<CrmActivity> findAll() {
        return activityRepository.findAllByOrderByCreatedAtDesc();
    }

    public Optional<CrmActivity> findById(Long id) {
        return activityRepository.findById(id);
    }

    public List<CrmActivity> findByStatus(ActivityStatus status) {
        return activityRepository.findByStatus(status);
    }

    public List<CrmActivity> findByTipo(ActivityType tipo) {
        return activityRepository.findByTipo(tipo);
    }

    public List<CrmActivity> findBySaleId(Long saleId) {
        return activityRepository.findBySaleId(saleId);
    }

    public List<CrmActivity> findByResponsavel(String responsavel) {
        return activityRepository.findByResponsavel(responsavel);
    }

    public List<CrmActivity> findByDataAgendadaBetween(LocalDateTime start, LocalDateTime end) {
        return activityRepository.findByDataAgendadaBetween(start, end);
    }

    public CrmActivity create(ActivityRequest request) {
        CrmActivity activity = new CrmActivity();
        activity.setTitulo(request.titulo());
        activity.setDescricao(request.descricao());
        activity.setTipo(request.tipo());
        activity.setPrioridade(request.prioridade());
        activity.setContatoNome(request.contatoNome());
        activity.setContatoEmail(request.contatoEmail());
        activity.setContatoTelefone(request.contatoTelefone());
        activity.setDataAgendada(request.dataAgendada());
        activity.setResponsavel(request.responsavel());
        activity.setResultado(request.resultado());
        activity.setStatus(ActivityStatus.AGENDADA);

        if (request.saleId() != null) {
            Sale sale = saleRepository.findById(request.saleId())
                .orElseThrow(() -> new RuntimeException("Venda não encontrada: " + request.saleId()));
            activity.setSale(sale);
        }

        return activityRepository.save(activity);
    }

    public CrmActivity update(Long id, ActivityRequest request) {
        CrmActivity activity = activityRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Atividade não encontrada: " + id));

        activity.setTitulo(request.titulo());
        activity.setDescricao(request.descricao());
        activity.setTipo(request.tipo());
        activity.setPrioridade(request.prioridade());
        activity.setContatoNome(request.contatoNome());
        activity.setContatoEmail(request.contatoEmail());
        activity.setContatoTelefone(request.contatoTelefone());
        activity.setDataAgendada(request.dataAgendada());
        activity.setResponsavel(request.responsavel());
        activity.setResultado(request.resultado());

        if (request.saleId() != null) {
            Sale sale = saleRepository.findById(request.saleId())
                .orElseThrow(() -> new RuntimeException("Venda não encontrada: " + request.saleId()));
            activity.setSale(sale);
        }

        return activityRepository.save(activity);
    }

    public CrmActivity updateStatus(Long id, ActivityStatus newStatus) {
        CrmActivity activity = activityRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Atividade não encontrada: " + id));

        activity.setStatus(newStatus);

        if (newStatus == ActivityStatus.CONCLUIDA && activity.getDataRealizada() == null) {
            activity.setDataRealizada(LocalDateTime.now());
        }

        return activityRepository.save(activity);
    }

    public void delete(Long id) {
        if (!activityRepository.existsById(id)) {
            throw new RuntimeException("Atividade não encontrada: " + id);
        }
        activityRepository.deleteById(id);
    }

    public Long countByStatus(ActivityStatus status) {
        return activityRepository.countByStatus(status);
    }

    public Long countByTipo(ActivityType tipo) {
        return activityRepository.countByTipo(tipo);
    }

    public List<CrmActivity> findRecent() {
        return activityRepository.findTop10ByOrderByCreatedAtDesc();
    }
}
