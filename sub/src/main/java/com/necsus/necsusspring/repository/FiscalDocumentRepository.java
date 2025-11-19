package com.necsus.necsusspring.repository;

import com.necsus.necsusspring.model.FiscalDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FiscalDocumentRepository extends JpaRepository<FiscalDocument, Long> {

    List<FiscalDocument> findTop5ByOrderByDataUploadDesc();
}
