package com.necsus.necsusspring.service;

import com.necsus.necsusspring.dto.CrmActivityResponse;
import com.necsus.necsusspring.dto.CreateCrmActivityRequest;
import com.necsus.necsusspring.model.CrmActivity;
import com.necsus.necsusspring.repository.CrmActivityRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CrmActivityService {

    private final CrmActivityRepository crmActivityRepository;

    public CrmActivityService(CrmActivityRepository crmActivityRepository) {
        this.crmActivityRepository = crmActivityRepository;
    }

    public List<CrmActivityResponse> listAll() {
        return crmActivityRepository.findAll(Sort.by(Sort.Direction.ASC, "dueAt"))
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public CrmActivityResponse create(CreateCrmActivityRequest request) {
        CrmActivity activity = new CrmActivity();
        activity.setTitle(request.title());
        activity.setDescription(request.description());
        activity.setStatus(request.status());
        activity.setType(request.type());
        activity.setResponsible(request.responsible());
        activity.setLeadSource(request.leadSource());
        activity.setCity(request.city());
        activity.setState(request.state());
        activity.setDueAt(LocalDateTime.of(request.dueDate(), request.dueTime()));

        CrmActivity saved = crmActivityRepository.save(activity);
        return toResponse(saved);
    }

    private CrmActivityResponse toResponse(CrmActivity activity) {
        return new CrmActivityResponse(
                activity.getId(),
                activity.getTitle(),
                activity.getDescription(),
                activity.getStatus(),
                activity.getType(),
                activity.getLeadSource(),
                activity.getResponsible(),
                activity.getCity(),
                activity.getState(),
                activity.getDueAt(),
                activity.getCreatedAt()
        );
    }
}
