package com.necsus.necsusspring.repository;

import com.necsus.necsusspring.model.Demand;
import com.necsus.necsusspring.model.DemandStatus;
import com.necsus.necsusspring.model.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DemandRepository extends JpaRepository<Demand, Long> {

    /**
     * Busca demandas criadas por um usuário específico
     */
    List<Demand> findByCreatedByOrderByCreatedAtDesc(UserAccount createdBy);

    /**
     * Busca demandas atribuídas a um usuário específico
     */
    List<Demand> findByAssignedToOrderByCreatedAtDesc(UserAccount assignedTo);

    /**
     * Busca demandas por status
     */
    List<Demand> findByStatusOrderByCreatedAtDesc(DemandStatus status);

    /**
     * Busca demandas onde o role está incluído nos targetRoles
     */
    @Query("SELECT d FROM Demand d WHERE d.targetRoles LIKE %:role% ORDER BY d.createdAt DESC")
    List<Demand> findByTargetRolesContaining(@Param("role") String role);

    /**
     * Busca demandas que um usuário pode ver (criadas por ele ou direcionadas ao seu role)
     */
    @Query("SELECT d FROM Demand d WHERE d.createdBy = :user OR d.assignedTo = :user OR d.targetRoles LIKE %:role% ORDER BY d.createdAt DESC")
    List<Demand> findAccessibleByUser(@Param("user") UserAccount user, @Param("role") String role);
}
