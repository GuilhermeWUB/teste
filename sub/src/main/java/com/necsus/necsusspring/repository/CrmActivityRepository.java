package com.necsus.necsusspring.repository;

import com.necsus.necsusspring.model.ActivityStatus;
import com.necsus.necsusspring.model.ActivityType;
import com.necsus.necsusspring.model.CrmActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CrmActivityRepository extends JpaRepository<CrmActivity, Long> {

    List<CrmActivity> findAllByOrderByCreatedAtDesc();

    List<CrmActivity> findByStatus(ActivityStatus status);

    List<CrmActivity> findByTipo(ActivityType tipo);

    List<CrmActivity> findBySaleId(Long saleId);

    List<CrmActivity> findByResponsavel(String responsavel);

    List<CrmActivity> findByDataAgendadaBetween(LocalDateTime start, LocalDateTime end);

    Long countByStatus(ActivityStatus status);

    Long countByTipo(ActivityType tipo);

    List<CrmActivity> findTop10ByOrderByCreatedAtDesc();
}
