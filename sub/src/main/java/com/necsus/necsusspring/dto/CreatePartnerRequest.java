package com.necsus.necsusspring.dto;

import com.necsus.necsusspring.model.Address;
import com.necsus.necsusspring.model.Adhesion;
import com.necsus.necsusspring.model.Partner;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatePartnerRequest {

    private Partner partner = new Partner();
    private Address address = new Address();
    private Adhesion adhesion = new Adhesion();
}
