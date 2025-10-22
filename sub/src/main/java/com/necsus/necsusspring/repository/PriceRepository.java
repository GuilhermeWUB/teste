package com.necsus.necsusspring.repository;

import com.necsus.necsusspring.model.Price;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PriceRepository extends JpaRepository<Price, Long> {
}