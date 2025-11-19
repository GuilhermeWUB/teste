package com.necsus.necsusspring.service;

import com.necsus.necsusspring.model.FiscalDocument;
import com.necsus.necsusspring.repository.FiscalDocumentRepository;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class FiscalDocumentService {

    private final FiscalDocumentRepository fiscalDocumentRepository;

    public FiscalDocumentService(FiscalDocumentRepository fiscalDocumentRepository) {
        this.fiscalDocumentRepository = fiscalDocumentRepository;
    }

    public FiscalDocument save(FiscalDocument document) {
        document.setDataUpload(new Date());
        return fiscalDocumentRepository.save(document);
    }

    public List<FiscalDocument> listRecentDocuments() {
        return fiscalDocumentRepository.findTop5ByOrderByDataUploadDesc();
    }

    public FiscalDocument findById(Long id) {
        return fiscalDocumentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Nota fiscal não encontrada"));
    }
}
