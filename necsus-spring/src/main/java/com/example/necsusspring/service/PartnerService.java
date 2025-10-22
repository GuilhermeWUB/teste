package com.example.necsusspring.service;

import com.example.necsusspring.model.Address;
import com.example.necsusspring.model.Adhesion;
import com.example.necsusspring.model.Partner;
import com.example.necsusspring.repository.AddressRepository;
import com.example.necsusspring.repository.AdhesionRepository;
import com.example.necsusspring.repository.PartnerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
}
