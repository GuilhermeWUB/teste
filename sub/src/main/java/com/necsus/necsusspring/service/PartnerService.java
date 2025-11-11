package com.necsus.necsusspring.service;

import com.necsus.necsusspring.model.Address;
import com.necsus.necsusspring.model.Adhesion;
import com.necsus.necsusspring.model.Partner;
import com.necsus.necsusspring.repository.AddressRepository;
import com.necsus.necsusspring.repository.AdhesionRepository;
import com.necsus.necsusspring.repository.PartnerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PartnerService {

    @Autowired
    private PartnerRepository partnerRepository;

    public Partner createPartner(Partner partner, Address address, Adhesion adhesion) {
        partner.setAddress(address);
        partner.setAdhesion(adhesion);
        adhesion.setPartner(partner);
        return partnerRepository.save(partner);
    }

    public List<Partner> getAllPartners() {
        return partnerRepository.findAll();
    }

    /**
     * Retorna associados com paginação.
     * @param page Número da página (começa em 0)
     * @param size Quantidade de itens por página (máximo 30)
     * @return Page contendo os associados
     */
    public Page<Partner> getAllPartnersPaginated(int page, int size) {
        // Limita o tamanho máximo a 30 itens por página
        size = Math.min(size, 30);
        size = Math.max(size, 1); // Mínimo 1 item

        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        return partnerRepository.findAllWithVehicles(pageable);
    }

    /**
     * Pesquisa associados por nome ou CPF com paginação.
     * @param searchTerm Termo de pesquisa (nome ou CPF)
     * @param page Número da página (começa em 0)
     * @param size Quantidade de itens por página (máximo 30)
     * @return Page contendo os associados que correspondem à pesquisa
     */
    public Page<Partner> searchPartnersPaginated(String searchTerm, int page, int size) {
        // Limita o tamanho máximo a 30 itens por página
        size = Math.min(size, 30);
        size = Math.max(size, 1); // Mínimo 1 item

        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        return partnerRepository.searchByNameOrCpf(searchTerm, pageable);
    }

    public Optional<Partner> getPartnerById(Long id) {
        return partnerRepository.findByIdWithAllRelationships(id);
    }

    public Optional<Partner> getPartnerByEmail(String email) {
        if (email == null) {
            return Optional.empty();
        }
        return partnerRepository.findByEmailIgnoreCase(email);
    }

    public Partner updatePartner(Partner partner) {
        return partnerRepository.findById(partner.getId())
                .map(existingPartner -> {
                    existingPartner.setName(partner.getName());
                    existingPartner.setDateBorn(partner.getDateBorn());
                    existingPartner.setEmail(partner.getEmail());
                    existingPartner.setCpf(partner.getCpf());
                    existingPartner.setPhone(partner.getPhone());
                    existingPartner.setCell(partner.getCell());
                    existingPartner.setRg(partner.getRg());
                    existingPartner.setFax(partner.getFax());
                    if (partner.getAddress() != null) {
                        if (existingPartner.getAddress() != null) {
                            // Update existing address
                            existingPartner.getAddress().setZipcode(partner.getAddress().getZipcode());
                            existingPartner.getAddress().setAddress(partner.getAddress().getAddress());
                            existingPartner.getAddress().setNeighborhood(partner.getAddress().getNeighborhood());
                            existingPartner.getAddress().setNumber(partner.getAddress().getNumber());
                            existingPartner.getAddress().setComplement(partner.getAddress().getComplement());
                            existingPartner.getAddress().setCity(partner.getAddress().getCity());
                            existingPartner.getAddress().setStates(partner.getAddress().getStates());
                        } else {
                            // Create new address if existing partner doesn't have one
                            existingPartner.setAddress(partner.getAddress());
                        }
                    }
                    return partnerRepository.save(existingPartner);
                })
                .orElseThrow(() -> new RuntimeException("Partner not found with id " + partner.getId()));
    }

    public void deletePartner(Long id) {
        partnerRepository.deleteById(id);
    }
}
