package com.necsus.necsusspring.repository;

import com.necsus.necsusspring.model.CrmActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CrmActivityRepository extends JpaRepository<CrmActivity, Long> {
}
