package com.necsus.necsusspring.repository;

import com.necsus.necsusspring.model.Regional;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RegionalRepository extends JpaRepository<Regional, Long> {

    Optional<Regional> findByCode(String code);

    boolean existsByCode(String code);

    boolean existsByName(String name);

    List<Regional> findByActive(Boolean active);

    List<Regional> findAllByOrderByNameAsc();
}
