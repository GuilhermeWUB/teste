package com.necsus.necsusspring.repository;

import com.necsus.necsusspring.model.PriceGroup;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PriceGroupRepository extends JpaRepository<PriceGroup, Long> {
}