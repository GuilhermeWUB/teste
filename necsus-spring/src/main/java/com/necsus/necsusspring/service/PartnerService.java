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

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private AdhesionRepository adhesionRepository;

    public Partner createPartner(Partner partner, Address address, Adhesion adhesion) {
        Address savedAddress = addressRepository.save(address);
        partner.setAddressId(savedAddress.getId());
        Partner savedPartner = partnerRepository.save(partner);
        adhesion.setPartners_id(savedPartner.getId());
        adhesionRepository.save(adhesion);
        return savedPartner;
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
                    existingPartner.setZipcode(partner.getZipcode());
                    existingPartner.setAddress(partner.getAddress());
                    existingPartner.setNeighborhood(partner.getNeighborhood());
                    existingPartner.setNumResid(partner.getNumResid());
                    existingPartner.setComplement(partner.getComplement());
                    existingPartner.setEmail(partner.getEmail());
                    existingPartner.setCity(partner.getCity());
                    existingPartner.setUf(partner.getUf());
                    existingPartner.setCpf(partner.getCpf());
                    existingPartner.setPhone(partner.getPhone());
                    existingPartner.setCell(partner.getCell());
                    existingPartner.setRg(partner.getRg());
                    existingPartner.setFax(partner.getFax());
                    return partnerRepository.save(existingPartner);
                })
                .orElseThrow(() -> new RuntimeException("Partner not found with id " + partner.getId()));
    }

    public void deletePartner(Long id) {
        partnerRepository.deleteById(id);
    }
}
