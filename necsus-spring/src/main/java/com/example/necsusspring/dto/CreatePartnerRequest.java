package com.example.necsusspring.dto;

import com.example.necsusspring.model.Address;
import com.example.necsusspring.model.Adhesion;
import com.example.necsusspring.model.Partner;

public class CreatePartnerRequest {

    private Partner partner;
    private Address address;
    private Adhesion adhesion;

    public Partner getPartner() {
        return partner;
    }

    public void setPartner(Partner partner) {
        this.partner = partner;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public Adhesion getAdhesion() {
        return adhesion;
    }

    public void setAdhesion(Adhesion adhesion) {
        this.adhesion = adhesion;
    }
}
