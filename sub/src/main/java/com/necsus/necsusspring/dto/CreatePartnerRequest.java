package com.necsus.necsusspring.dto;

import com.necsus.necsusspring.model.Address;
import com.necsus.necsusspring.model.Adhesion;
import com.necsus.necsusspring.model.Partner;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreatePartnerRequest {

    private Partner partner;
    private Address address;
    private Adhesion adhesion;
}
