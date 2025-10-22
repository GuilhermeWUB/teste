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
        //TODO: Implement this method correctly
        return new byte[0];
    }

    private Boleto toStellaBoleto(BankSlip bankSlip) {
        //TODO: Implement this method correctly
        return null;
    }

    private Calendar toCalendar(java.util.Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar;
    }
}