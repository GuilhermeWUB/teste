package com.necsus.necsusspring.service;

import com.necsus.necsusspring.dto.LegalProcessRequest;
import com.necsus.necsusspring.model.LegalProcess;
import com.necsus.necsusspring.model.LegalProcessStatus;
import com.necsus.necsusspring.model.LegalProcessType;
import com.necsus.necsusspring.repository.LegalProcessRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
@Transactional
public class LegalProcessService {

    private final LegalProcessRepository legalProcessRepository;

    private static final Map<LegalProcessType, EnumSet<LegalProcessStatus>> STATUSES_BY_TYPE = Map.of(
            LegalProcessType.RASTREADOR, EnumSet.of(
                    LegalProcessStatus.RASTREADOR_EM_ABERTO,
                    LegalProcessStatus.RASTREADOR_EM_CONTATO,
                    LegalProcessStatus.RASTREADOR_ACORDO_ASSINADO,
                    LegalProcessStatus.RASTREADOR_DEVOLVIDO,
                    LegalProcessStatus.RASTREADOR_REATIVACAO
            ),
            LegalProcessType.FIDELIDADE, EnumSet.of(
                    LegalProcessStatus.FIDELIDADE_EM_ABERTO,
                    LegalProcessStatus.FIDELIDADE_EM_CONTATO,
                    LegalProcessStatus.FIDELIDADE_ACORDO_ASSINADO,
                    LegalProcessStatus.FIDELIDADE_REATIVACAO
            ),
            LegalProcessType.TERCEIROS, EnumSet.of(
                    LegalProcessStatus.EM_ABERTO_7_0,
                    LegalProcessStatus.EM_CONTATO_7_1,
                    LegalProcessStatus.PROCESSO_JUDICIAL_7_2,
                    LegalProcessStatus.ACORDO_ASSINADO_7_3
            )
    );

    public LegalProcessService(LegalProcessRepository legalProcessRepository) {
        this.legalProcessRepository = legalProcessRepository;
    }

    @Transactional(readOnly = true)
    public List<LegalProcess> findAll() {
        return legalProcessRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    @Transactional(readOnly = true)
    public Optional<LegalProcess> findByNumeroProcesso(String numeroProcesso) {
        return legalProcessRepository.findByNumeroProcesso(numeroProcesso);
    }

    @Transactional(readOnly = true)
    public Optional<LegalProcess> findBySourceEventId(Long sourceEventId) {
        if (sourceEventId == null) {
            return Optional.empty();
        }
        return legalProcessRepository.findBySourceEventId(sourceEventId);
    }

    @Transactional(readOnly = true)
    public LegalProcess findById(Long id) {
        return legalProcessRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Processo não encontrado"));
    }

    public LegalProcess create(LegalProcessRequest request) {
        LegalProcessType processType = resolveType(request.processType());
        LegalProcess process = new LegalProcess(
                request.autor(),
                request.reu(),
                request.materia(),
                request.numeroProcesso(),
                request.valorCausa(),
                request.pedidos(),
                processType,
                defaultStatusFor(processType)
        );
        process.setSourceEventId(request.sourceEventId());
        process.setSourceEventSnapshot(request.sourceEventSnapshot());
        normalizeProcessTypeAndStatus(process, processType);
        return legalProcessRepository.save(process);
    }

    public LegalProcess linkEventToProcess(LegalProcess process,
                                           Long sourceEventId,
                                           String sourceEventSnapshot,
                                           LegalProcessType type) {
        if (process == null) {
            throw new EntityNotFoundException("Processo não encontrado");
        }

        boolean changed = false;

        if (sourceEventId != null && !Objects.equals(process.getSourceEventId(), sourceEventId)) {
            process.setSourceEventId(sourceEventId);
            changed = true;
        }

        if (sourceEventSnapshot != null && !sourceEventSnapshot.isBlank()
                && !Objects.equals(process.getSourceEventSnapshot(), sourceEventSnapshot)) {
            process.setSourceEventSnapshot(sourceEventSnapshot);
            changed = true;
        }

        LegalProcessType previousType = process.getProcessType();
        LegalProcessStatus previousStatus = process.getStatus();
        normalizeProcessTypeAndStatus(process, type != null ? type : process.getProcessType());
        if (process.getProcessType() != previousType || process.getStatus() != previousStatus) {
            changed = true;
        }

        return changed ? legalProcessRepository.save(process) : process;
    }

    public LegalProcess update(Long id, LegalProcessRequest request) {
        LegalProcess existing = findById(id);
        existing.setAutor(request.autor());
        existing.setReu(request.reu());
        existing.setMateria(request.materia());
        existing.setNumeroProcesso(request.numeroProcesso());
        existing.setValorCausa(request.valorCausa());
        existing.setPedidos(request.pedidos());
        if (request.sourceEventId() != null) {
            existing.setSourceEventId(request.sourceEventId());
        }
        if (request.sourceEventSnapshot() != null) {
            existing.setSourceEventSnapshot(request.sourceEventSnapshot());
        }
        LegalProcessType newType = resolveType(request.processType());
        normalizeProcessTypeAndStatus(existing, newType);
        return legalProcessRepository.save(existing);
    }

    public void delete(Long id) {
        LegalProcess existing = findById(id);
        legalProcessRepository.delete(existing);
    }

    /**
     * Atualiza o status de um processo jurídico no Kanban.
     *
     * @param id ID do processo
     * @param newStatus Novo status
     * @return Processo atualizado
     */
    public LegalProcess updateStatus(Long id, LegalProcessStatus newStatus) {
        LegalProcess process = findById(id);
        process.setStatus(newStatus);
        process.setProcessType(inferTypeFromStatus(newStatus));
        return legalProcessRepository.save(process);
    }

    private LegalProcessType resolveType(LegalProcessType type) {
        return type != null ? type : LegalProcessType.TERCEIROS;
    }

    private void normalizeProcessTypeAndStatus(LegalProcess process, LegalProcessType requestedType) {
        if (process == null) {
            return;
        }

        LegalProcessType resolvedType = resolveType(requestedType != null ? requestedType : process.getProcessType());
        process.setProcessType(resolvedType);

        if (!isStatusAllowedForType(process.getStatus(), resolvedType)) {
            process.setStatus(defaultStatusFor(resolvedType));
        }
    }

    private LegalProcessStatus defaultStatusFor(LegalProcessType type) {
        return switch (resolveType(type)) {
            case RASTREADOR -> LegalProcessStatus.RASTREADOR_EM_ABERTO;
            case FIDELIDADE -> LegalProcessStatus.FIDELIDADE_EM_ABERTO;
            case TERCEIROS -> LegalProcessStatus.EM_ABERTO_7_0;
        };
    }

    private boolean isStatusAllowedForType(LegalProcessStatus status, LegalProcessType type) {
        if (status == null) {
            return false;
        }
        return STATUSES_BY_TYPE.getOrDefault(resolveType(type), EnumSet.noneOf(LegalProcessStatus.class))
                .contains(status);
    }

    private LegalProcessType inferTypeFromStatus(LegalProcessStatus status) {
        if (status == null) {
            return LegalProcessType.TERCEIROS;
        }
        for (Map.Entry<LegalProcessType, EnumSet<LegalProcessStatus>> entry : STATUSES_BY_TYPE.entrySet()) {
            if (entry.getValue().contains(status)) {
                return entry.getKey();
            }
        }
        return LegalProcessType.TERCEIROS;
    }
}
