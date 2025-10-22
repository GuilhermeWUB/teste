package com.necsus.necsusspring.service;

import com.necsus.necsusspring.model.Address;
import com.necsus.necsusspring.model.Adhesion;
import com.necsus.necsusspring.model.Partner;
import com.necsus.necsusspring.repository.AddressRepository;
import com.necsus.necsusspring.repository.AdhesionRepository;
import com.necsus.necsusspring.repository.PartnerRepository;
import org.springframework.beans.factory.annotation.Autowired;
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

    public Optional<Partner> getPartnerById(Long id) {
        return partnerRepository.findById(id);
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
                        existingPartner.getAddress().setZipcode(partner.getAddress().getZipcode());
                        existingPartner.getAddress().setAddress(partner.getAddress().getAddress());
                        existingPartner.getAddress().setNeighborhood(partner.getAddress().getNeighborhood());
                        existingPartner.getAddress().setNumber(partner.getAddress().getNumber());
                        existingPartner.getAddress().setComplement(partner.getAddress().getComplement());
                        existingPartner.getAddress().setCity(partner.getAddress().getCity());
                        existingPartner.getAddress().setStates(partner.getAddress().getStates());
                    }
                    return partnerRepository.save(existingPartner);
                })
                .orElseThrow(() -> new RuntimeException("Partner not found with id " + partner.getId()));
    }

    public void deletePartner(Long id) {
        partnerRepository.deleteById(id);
    }
}
