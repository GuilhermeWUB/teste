package com.necsus.necsusspring.service;

import com.necsus.necsusspring.model.Payment;
import com.necsus.necsusspring.model.Partner;
import com.necsus.necsusspring.model.Vehicle;
import com.necsus.necsusspring.repository.EventRepository;
import com.necsus.necsusspring.repository.PaymentRepository;
import com.necsus.necsusspring.repository.PartnerRepository;
import com.necsus.necsusspring.repository.VehicleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final PartnerRepository partnerRepository;
    private final PaymentRepository paymentRepository;
    private final EventRepository eventRepository;

    public VehicleService(VehicleRepository vehicleRepository,
                          PartnerRepository partnerRepository,
                          PaymentRepository paymentRepository,
                          EventRepository eventRepository) {
        this.vehicleRepository = vehicleRepository;
        this.partnerRepository = partnerRepository;
        this.paymentRepository = paymentRepository;
        this.eventRepository = eventRepository;
    }

    @Transactional(readOnly = true)
    public List<Vehicle> listAll(Long partnerId) {
        List<Vehicle> vehicles = partnerId != null
                ? vehicleRepository.findByPartnerId(partnerId)
                : vehicleRepository.findAll();
        vehicles.forEach(this::ensurePaymentLoaded);
        return vehicles;
    }

    /**
     * Retorna veículos com paginação.
     * @param partnerId ID do associado (opcional)
     * @param page Número da página (começa em 0)
     * @param size Quantidade de itens por página (máximo 30)
     * @return Page contendo os veículos
     */
    @Transactional(readOnly = true)
    public Page<Vehicle> listAllPaginated(Long partnerId, int page, int size) {
        // Limita o tamanho máximo a 30 itens por página
        size = Math.min(size, 30);
        size = Math.max(size, 1); // Mínimo 1 item

        Pageable pageable = PageRequest.of(page, size, Sort.by("plaque").ascending());

        Page<Vehicle> vehiclePage;
        if (partnerId != null) {
            // Precisa criar método no repository ou usar Specification
            vehiclePage = vehicleRepository.findAll(
                    (root, query, cb) -> cb.equal(root.get("partnerId"), partnerId),
                    pageable
            );
        } else {
            vehiclePage = vehicleRepository.findAll(pageable);
        }

        // Carrega payment para cada veículo
        vehiclePage.forEach(this::ensurePaymentLoaded);

        return vehiclePage;
    }

    @Transactional(readOnly = true)
    public List<Vehicle> listByPartnerId(Long partnerId) {
        if (partnerId == null) {
            return List.of();
        }
        List<Vehicle> vehicles = vehicleRepository.findByPartnerId(partnerId);
        vehicles.forEach(this::ensurePaymentLoaded);
        return vehicles;
    }

    @Transactional(readOnly = true)
    public Optional<Vehicle> findById(Long id) {
        return vehicleRepository.findById(id).map(this::ensurePaymentLoaded);
    }

    @Transactional
    public Vehicle create(Vehicle vehicle) {
        Partner partner = resolvePartner(vehicle.getPartnerId());
        vehicle.setPartner(partner);
        vehicle.setPartnerId(partner.getId());

        Payment paymentPayload = vehicle.getPayment();
        vehicle.setPayment(null);

        Vehicle savedVehicle = vehicleRepository.save(vehicle);

        Payment payment = preparePayment(paymentPayload, partner);
        payment.setVehicle(savedVehicle);
        paymentRepository.save(payment);

        savedVehicle.setPayment(payment);
        return savedVehicle;
    }

    @Transactional
    public Vehicle update(Long id, Vehicle vehiclePayload) {
        Vehicle existing = vehicleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vehicle not found with id " + id));

        Partner partner = resolvePartner(vehiclePayload.getPartnerId());
        existing.setMaker(vehiclePayload.getMaker());
        if (vehiclePayload.getType_vehicle() != null) {
            existing.setType_vehicle(vehiclePayload.getType_vehicle());
        }
        existing.setPlaque(vehiclePayload.getPlaque());
        existing.setPartnerId(partner.getId());
        existing.setModel(vehiclePayload.getModel());
        existing.setColor(vehiclePayload.getColor());
        existing.setYear_mod(vehiclePayload.getYear_mod());
        existing.setTipo_combustivel(vehiclePayload.getTipo_combustivel());
        existing.setPartner(partner);

        Vehicle savedVehicle = vehicleRepository.save(existing);

        Payment payment = preparePayment(vehiclePayload.getPayment(), partner);
        payment.setVehicle(savedVehicle);

        paymentRepository.findByVehicleId(savedVehicle.getId())
                .ifPresent(existingPayment -> payment.setId(existingPayment.getId()));
        paymentRepository.save(payment);

        savedVehicle.setPayment(payment);
        return savedVehicle;
    }

    @Transactional
    public void delete(Long id) {
        if (!vehicleRepository.existsById(id)) {
            return;
        }
        eventRepository.deleteByVehicleId(id);
        paymentRepository.deleteByVehicleId(id);
        vehicleRepository.deleteById(id);
    }

    private Partner resolvePartner(Long partnerId) {
        return partnerRepository.findById(partnerId)
                .orElseThrow(() -> new RuntimeException("Partner not found with id " + partnerId));
    }

    private Payment preparePayment(Payment paymentPayload, Partner partner) {
        Payment payment = paymentPayload != null ? paymentPayload : new Payment();
        if (payment.getDateCreate() == null) {
            payment.setDateCreate(new Date());
        }
        if (payment.getMonthly() == null) {
            payment.setMonthly(BigDecimal.ONE);
        }
        if (payment.getVencimento() == null) {
            payment.setVencimento(1);
        }
        if (partner != null) {
            payment.setPartner(partner);
        }
        return payment;
    }

    private Vehicle ensurePaymentLoaded(Vehicle vehicle) {
        paymentRepository.findByVehicleId(vehicle.getId())
                .ifPresent(vehicle::setPayment);
        if (vehicle.getPayment() == null) {
            Partner partner = vehicle.getPartner();
            if (partner == null && vehicle.getPartnerId() != null) {
                partner = partnerRepository.findById(vehicle.getPartnerId()).orElse(null);
                vehicle.setPartner(partner);
            }
            Payment payment = preparePayment(null, partner);
            payment.setVehicle(vehicle);
            paymentRepository.save(payment);
            vehicle.setPayment(payment);
        }
        return vehicle;
    }
}
