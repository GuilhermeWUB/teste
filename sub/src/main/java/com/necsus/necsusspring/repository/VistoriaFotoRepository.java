package com.necsus.necsusspring.repository;

import com.necsus.necsusspring.model.VistoriaFoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VistoriaFotoRepository extends JpaRepository<VistoriaFoto, Long> {

    List<VistoriaFoto> findByVistoriaIdOrderByOrdemAsc(Long vistoriaId);

    void deleteByVistoriaId(Long vistoriaId);
}
