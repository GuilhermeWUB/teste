package com.necsus.necsusspring.service;

import com.necsus.necsusspring.dto.LegalProcessRequest;
import com.necsus.necsusspring.model.LegalProcess;
import com.necsus.necsusspring.repository.LegalProcessRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class LegalProcessService {

    private final LegalProcessRepository legalProcessRepository;

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
        LegalProcess process = new LegalProcess(
                request.autor(),
                request.reu(),
                request.materia(),
                request.numeroProcesso(),
                request.valorCausa(),
                request.pedidos()
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
        return legalProcessRepository.save(existing);
    }

    public void delete(Long id) {
        LegalProcess existing = findById(id);
        legalProcessRepository.delete(existing);
    }
}
