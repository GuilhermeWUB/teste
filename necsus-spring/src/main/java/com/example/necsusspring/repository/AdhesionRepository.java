package com.example.necsusspring.repository;

import com.example.necsusspring.model.Adhesion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdhesionRepository extends JpaRepository<Adhesion, Long> {
}
