package com.necsus.necsusspring.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "slips_briefing")
public class SlipsBriefing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "briefing_01")
    private String briefing01;

    @Column(name = "wallet")
    private String wallet;

    @Column(name = "aceite")
    private String aceite;

    @Column(name = "type_doc")
    private String typeDoc;

    @ManyToOne
    @JoinColumn(name = "bank_accont_id")
    private BankAccount bankAccount;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBriefing01() {
        return briefing01;
    }

    public void setBriefing01(String briefing01) {
        this.briefing01 = briefing01;
    }

    public String getWallet() {
        return wallet;
    }

    public void setWallet(String wallet) {
        this.wallet = wallet;
    }

    public String getAceite() {
        return aceite;
    }

    public void setAceite(String aceite) {
        this.aceite = aceite;
    }

    public String getTypeDoc() {
        return typeDoc;
    }

    public void setTypeDoc(String typeDoc) {
        this.typeDoc = typeDoc;
    }

    public BankAccount getBankAccount() {
        return bankAccount;
    }

    public void setBankAccount(BankAccount bankAccount) {
        this.bankAccount = bankAccount;
    }
}