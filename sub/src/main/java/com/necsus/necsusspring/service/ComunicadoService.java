package com.necsus.necsusspring.service;

import com.necsus.necsusspring.model.Comunicado;
import com.necsus.necsusspring.repository.ComunicadoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ComunicadoService {

    private final ComunicadoRepository comunicadoRepository;

    public ComunicadoService(ComunicadoRepository comunicadoRepository) {
        this.comunicadoRepository = comunicadoRepository;
    }

    @Transactional(readOnly = true)
    public List<Comunicado> listAll() {
        return comunicadoRepository.findAllByOrderByDataCriacaoDesc();
    }

    @Transactional(readOnly = true)
    public List<Comunicado> listAtivos() {
        return comunicadoRepository.findByAtivoTrueOrderByDataCriacaoDesc();
    }

    @Transactional(readOnly = true)
    public List<Comunicado> listVisiveis() {
        return comunicadoRepository.findComunicadosVisiveis();
    }

    @Transactional(readOnly = true)
    public Optional<Comunicado> findById(Long id) {
        return comunicadoRepository.findById(id);
    }

    @Transactional
    public Comunicado create(Comunicado comunicado) {
        return comunicadoRepository.save(comunicado);
    }

    @Transactional
    public Comunicado update(Long id, Comunicado comunicadoPayload) {
        Comunicado existing = comunicadoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Comunicado não encontrado com id " + id));

        existing.setTitulo(comunicadoPayload.getTitulo());
        existing.setMensagem(comunicadoPayload.getMensagem());
        existing.setDataExpiracao(comunicadoPayload.getDataExpiracao());
        existing.setAutor(comunicadoPayload.getAutor());
        existing.setAtivo(comunicadoPayload.isAtivo());

        return comunicadoRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        if (comunicadoRepository.existsById(id)) {
            comunicadoRepository.deleteById(id);
        }
    }

    @Transactional
    public Comunicado toggleAtivo(Long id) {
        Comunicado comunicado = comunicadoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Comunicado não encontrado com id " + id));

        comunicado.setAtivo(!comunicado.isAtivo());
        return comunicadoRepository.save(comunicado);
    }
}
