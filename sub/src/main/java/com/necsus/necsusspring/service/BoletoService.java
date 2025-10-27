package com.necsus.necsusspring.service;

import br.com.caelum.stella.boleto.Banco;
import br.com.caelum.stella.boleto.Boleto;
import br.com.caelum.stella.boleto.Datas;
import br.com.caelum.stella.boleto.Emissor;
import br.com.caelum.stella.boleto.Sacado;
import br.com.caelum.stella.boleto.bancos.BancoDoBrasil;
import br.com.caelum.stella.boleto.transformer.GeradorDeBoleto;
import com.necsus.necsusspring.model.BankSlip;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Calendar;

@Service
public class BoletoService {

    public byte[] generateBoleto(BankSlip bankSlip) throws IOException {
        Boleto boleto = toStellaBoleto(bankSlip);
        GeradorDeBoleto gerador = new GeradorDeBoleto(boleto);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        gerador.geraPDF(baos);
        return baos.toByteArray();
    }

    private Boleto toStellaBoleto(BankSlip bankSlip) {
        if (bankSlip == null) {
            throw new IllegalArgumentException("BankSlip cannot be null");
        }

        Datas datas = Datas.novasDatas();
        if (bankSlip.getDataDocumento() != null) {
            datas.comDocumento(toCalendar(bankSlip.getDataDocumento()));
        }
        if (bankSlip.getDataProcessamento() != null) {
            datas.comProcessamento(toCalendar(bankSlip.getDataProcessamento()));
        }
        if (bankSlip.getVencimento() != null) {
            datas.comVencimento(toCalendar(bankSlip.getVencimento()));
        }

        Emissor emissor = Emissor.novoEmissor();
        if (bankSlip.getPayment() != null && bankSlip.getPayment().getVehicle() != null && bankSlip.getPayment().getVehicle().getPartner() != null && bankSlip.getPayment().getVehicle().getPartner().getCompany() != null) {
            emissor.comCedente(bankSlip.getPayment().getVehicle().getPartner().getCompany().getCompanyName());
        }
        if (bankSlip.getSlipsBriefing() != null && bankSlip.getSlipsBriefing().getBankAccount() != null && bankSlip.getSlipsBriefing().getBankAccount().getBankAgency() != null) {
            emissor.comAgencia(Integer.parseInt(bankSlip.getSlipsBriefing().getBankAccount().getBankAgency().getAgencyCode()));
        }
        if (bankSlip.getSlipsBriefing() != null && bankSlip.getSlipsBriefing().getBankAccount() != null) {
            emissor.comContaCorrente(Long.parseLong(bankSlip.getSlipsBriefing().getBankAccount().getAccountCode()));
        }
        if (bankSlip.getNossoNumero() != null) {
            emissor.comNossoNumero(Long.parseLong(bankSlip.getNossoNumero()));
        }

        Sacado sacado = Sacado.novoSacado();
        if (bankSlip.getPartner() != null) {
            sacado.comNome(bankSlip.getPartner().getName());
            sacado.comCpf(bankSlip.getPartner().getCpf());
        }

        Banco banco = new BancoDoBrasil();

        Boleto boleto = Boleto.novoBoleto()
                .comBanco(banco)
                .comDatas(datas)
                .comEmissor(emissor)
                .comSacado(sacado);

        if (bankSlip.getValor() != null) {
            boleto.comValorBoleto(bankSlip.getValor().toString());
        }
        if (bankSlip.getNumeroDocumento() != null) {
            boleto.comNumeroDoDocumento(bankSlip.getNumeroDocumento());
        }

        return boleto;
    }

    private Calendar toCalendar(java.util.Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar;
    }
}