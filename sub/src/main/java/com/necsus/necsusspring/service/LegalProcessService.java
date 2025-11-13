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
        return legalProcessRepository.save(process);
    }

    public LegalProcess update(Long id, LegalProcessRequest request) {
        LegalProcess existing = findById(id);
        existing.setAutor(request.autor());
        existing.setReu(request.reu());
        existing.setMateria(request.materia());
        existing.setNumeroProcesso(request.numeroProcesso());
        existing.setValorCausa(request.valorCausa());
        existing.setPedidos(request.pedidos());
        LegalProcessType newType = resolveType(request.processType());
        existing.setProcessType(newType);
        if (!isStatusAllowedForType(existing.getStatus(), newType)) {
            existing.setStatus(defaultStatusFor(newType));
        }
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
