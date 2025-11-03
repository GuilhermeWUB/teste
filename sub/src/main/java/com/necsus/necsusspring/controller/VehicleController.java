package com.necsus.necsusspring.controller;

import com.necsus.necsusspring.config.ApiBrasilConfig;
import com.necsus.necsusspring.dto.FipeResponseDTO;
import com.necsus.necsusspring.model.Payment;
import com.necsus.necsusspring.model.Partner;
import com.necsus.necsusspring.model.Vehicle;
import com.necsus.necsusspring.service.PartnerService;
import com.necsus.necsusspring.service.VehicleService;
import com.necsus.necsusspring.service.FileStorageService;
import com.necsus.necsusspring.service.FipeService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.text.NumberFormat;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;

@Controller
@RequestMapping("/vehicles")
public class VehicleController {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(VehicleController.class);

    private final VehicleService vehicleService;
    private final PartnerService partnerService;
    private final FileStorageService fileStorageService;
    private final FipeService fipeService;
    private final ApiBrasilConfig apiBrasilConfig;
    private final RestTemplate restTemplate;

    public VehicleController(VehicleService vehicleService, PartnerService partnerService, FileStorageService fileStorageService, FipeService fipeService, ApiBrasilConfig apiBrasilConfig, RestTemplate restTemplate) {
        this.vehicleService = vehicleService;
        this.partnerService = partnerService;
        this.fileStorageService = fileStorageService;
        this.fipeService = fipeService;
        this.apiBrasilConfig = apiBrasilConfig;
        this.restTemplate = restTemplate;
    }

    // ... (código existente sem alterações)
    @ModelAttribute("partners")
    public List<Partner> partners() {
        return partnerService.getAllPartners();
    }

    @GetMapping
    public String listVehicles(@RequestParam(value = "partnerId", required = false) Long partnerId,
                               Model model) {
        List<Vehicle> vehicles = vehicleService.listAll(partnerId);
        model.addAttribute("vehicles", vehicles);
        model.addAttribute("partnerId", partnerId);
        if (partnerId != null) {
            partnerService.getPartnerById(partnerId)
                    .ifPresent(partner -> model.addAttribute("selectedPartner", partner));
        }
        return "lista_veiculos";
    }

    @GetMapping("/new")
    public String showCreateForm(@RequestParam(value = "partnerId", required = false) Long partnerId,
                                 @RequestParam(value = "codigoFipe", required = false) String codigoFipe,
                                 Model model) {
        Vehicle vehicle = new Vehicle();
        vehicle.setPayment(new Payment());
        if (partnerId != null) {
            vehicle.setPartnerId(partnerId);
        }

        // Se código FIPE foi fornecido, buscar dados automaticamente
        if (codigoFipe != null && !codigoFipe.trim().isEmpty()) {
            try {
                FipeResponseDTO fipeData = fipeService.buscarVeiculoPorCodigoFipe(codigoFipe);
                vehicle.setCodigo_fipe(fipeData.getCodeFipe());
                Double fipeValue = parseValorMonetario(fipeData.getPrice());
                vehicle.setFipe_value(fipeValue);
                model.addAttribute("fipeValueFormatted", formatCurrency(fipeValue));
                vehicle.setMaker(fipeData.getBrand());
                vehicle.setModel(fipeData.getModel());
                vehicle.setYear_mod(fipeData.getModelYear() != null ? String.valueOf(fipeData.getModelYear()) : null);
                vehicle.setTipo_combustivel(fipeData.getFuel());
                vehicle.setType_vehicle(mapearTipoVeiculo(fipeData.getVehicleType()));
                model.addAttribute("fipeSuccess", "Dados da FIPE carregados com sucesso!");
            } catch (Exception e) {
                model.addAttribute("fipeError", "Erro ao buscar dados da FIPE: " + e.getMessage());
                vehicle.setCodigo_fipe(codigoFipe);
            }
        }

        model.addAttribute("vehicle", vehicle);
        model.addAttribute("partnerId", partnerId);
        return "cadastro_veiculo";
    }

    @PostMapping(params = "acao=buscarFipe")
    public String buscarDadosFipeNoFormulario(@ModelAttribute("vehicle") Vehicle vehicle,
                                              @RequestParam(value = "contextPartnerId", required = false) Long contextPartnerId,
                                              @RequestParam(value = "tabelaReferencia", required = false) Integer tabelaReferencia,
                                              Model model) {
        if (vehicle.getPayment() == null) {
            vehicle.setPayment(new Payment());
        }

        Long partnerContext = contextPartnerId != null ? contextPartnerId : vehicle.getPartnerId();
        if (contextPartnerId != null && vehicle.getPartnerId() == null) {
            vehicle.setPartnerId(contextPartnerId);
        }
        model.addAttribute("partnerId", partnerContext);

        String codigoFipe = vehicle.getCodigo_fipe();
        if (codigoFipe == null || codigoFipe.trim().isEmpty()) {
            model.addAttribute("fipeErrorMessage", "Informe o código FIPE para realizar a busca.");
            return "cadastro_veiculo";
        }

        try {
            FipeResponseDTO fipeData = fipeService.buscarVeiculoPorCodigoFipe(codigoFipe.trim(), tabelaReferencia);
            aplicarDadosFipeNoVeiculo(vehicle, fipeData);
            model.addAttribute("fipeValueFormatted", formatCurrency(vehicle.getFipe_value()));
            model.addAttribute("fipeSuccessMessage", "Dados da Fipe carregados com sucesso!");
        } catch (Exception ex) {
            // Mostrar a mensagem específica do erro ao usuário
            String errorMessage = ex.getMessage();
            if (errorMessage != null && !errorMessage.isEmpty()) {
                model.addAttribute("fipeErrorMessage", errorMessage);
            } else {
                model.addAttribute("fipeErrorMessage", "Não foi possível buscar os dados da Fipe. Verifique o código informado.");
            }
        }

        return "cadastro_veiculo";
    }

    @PostMapping
    public String createVehicle(@Valid @ModelAttribute("vehicle") Vehicle vehicle,
                                BindingResult result,
                                @RequestParam(value = "inspectionPhotos", required = false) MultipartFile[] inspectionPhotos,
                                @RequestParam(value = "documentPhotos", required = false) MultipartFile[] documentPhotos,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        if (vehicle.getPayment() == null) {
            vehicle.setPayment(new Payment());
        }
        if (result.hasErrors()) {
            model.addAttribute("partnerId", vehicle.getPartnerId());
            return "cadastro_veiculo";
        }

        // Processar upload de fotos de inspeção
        if (inspectionPhotos != null && inspectionPhotos.length > 0) {
            List<String> inspectionPaths = fileStorageService.storeFiles(inspectionPhotos);
            vehicle.setInspectionPhotoPaths(inspectionPaths);
        }

        // Processar upload de fotos de documentação
        if (documentPhotos != null && documentPhotos.length > 0) {
            List<String> documentPaths = fileStorageService.storeFiles(documentPhotos);
            vehicle.setDocumentPhotoPaths(documentPaths);
        }

        Vehicle savedVehicle = vehicleService.create(vehicle);
        redirectAttributes.addFlashAttribute("successMessage", "Veículo cadastrado com sucesso!");
        return buildRedirect(savedVehicle.getPartnerId());
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id,
                               @RequestParam(value = "partnerId", required = false) Long partnerId,
                               @RequestParam(value = "codigoFipe", required = false) String codigoFipe,
                               Model model) {
        Vehicle vehicle = vehicleService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehicle not found"));
        if (vehicle.getPayment() == null) {
            vehicle.setPayment(new Payment());
        }

        // Se código FIPE foi fornecido, buscar dados automaticamente
        if (codigoFipe != null && !codigoFipe.trim().isEmpty()) {
            try {
                FipeResponseDTO fipeData = fipeService.buscarVeiculoPorCodigoFipe(codigoFipe);
                vehicle.setCodigo_fipe(fipeData.getCodeFipe());
                Double fipeValue = parseValorMonetario(fipeData.getPrice());
                vehicle.setFipe_value(fipeValue);
                model.addAttribute("fipeValueFormatted", formatCurrency(fipeValue));
                vehicle.setMaker(fipeData.getBrand());
                vehicle.setModel(fipeData.getModel());
                vehicle.setYear_mod(fipeData.getModelYear() != null ? String.valueOf(fipeData.getModelYear()) : null);
                vehicle.setTipo_combustivel(fipeData.getFuel());
                vehicle.setType_vehicle(mapearTipoVeiculo(fipeData.getVehicleType()));
                model.addAttribute("fipeSuccess", "Dados da FIPE carregados com sucesso!");
            } catch (Exception e) {
                model.addAttribute("fipeError", "Erro ao buscar dados da FIPE: " + e.getMessage());
                vehicle.setCodigo_fipe(codigoFipe);
            }
        } else {
            model.addAttribute("fipeValueFormatted", formatCurrency(vehicle.getFipe_value()));
        }

        model.addAttribute("vehicle", vehicle);
        model.addAttribute("partnerId", partnerId != null ? partnerId : vehicle.getPartnerId());
        return "update_veiculo";
    }

    @PostMapping("/update/{id}")
    public String updateVehicle(@PathVariable Long id,
                                @Valid @ModelAttribute("vehicle") Vehicle vehicle,
                                BindingResult result,
                                @RequestParam(value = "inspectionPhotos", required = false) MultipartFile[] inspectionPhotos,
                                @RequestParam(value = "documentPhotos", required = false) MultipartFile[] documentPhotos,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        if (vehicle.getPayment() == null) {
            vehicle.setPayment(new Payment());
        }
        if (result.hasErrors()) {
            model.addAttribute("partnerId", vehicle.getPartnerId());
            return "update_veiculo";
        }

        // Buscar veículo existente para manter fotos antigas
        Vehicle existingVehicle = vehicleService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehicle not found"));

        // Adicionar novas fotos de inspeção às existentes
        List<String> existingInspectionPhotos = existingVehicle.getInspectionPhotoPaths();
        if (existingInspectionPhotos == null) {
            existingInspectionPhotos = new ArrayList<>();
        }
        if (inspectionPhotos != null && inspectionPhotos.length > 0) {
            List<String> newInspectionPaths = fileStorageService.storeFiles(inspectionPhotos);
            existingInspectionPhotos.addAll(newInspectionPaths);
        }
        vehicle.setInspectionPhotoPaths(existingInspectionPhotos);

        // Adicionar novas fotos de documentação às existentes
        List<String> existingDocumentPhotos = existingVehicle.getDocumentPhotoPaths();
        if (existingDocumentPhotos == null) {
            existingDocumentPhotos = new ArrayList<>();
        }
        if (documentPhotos != null && documentPhotos.length > 0) {
            List<String> newDocumentPaths = fileStorageService.storeFiles(documentPhotos);
            existingDocumentPhotos.addAll(newDocumentPaths);
        }
        vehicle.setDocumentPhotoPaths(existingDocumentPhotos);

        vehicle.setId(id);
        Vehicle updatedVehicle = vehicleService.update(id, vehicle);
        redirectAttributes.addFlashAttribute("successMessage", "Veículo atualizado com sucesso!");
        return buildRedirect(updatedVehicle.getPartnerId());
    }

    @PostMapping("/delete/{id}")
    public String deleteVehicle(@PathVariable Long id,
                                @RequestParam(value = "partnerId", required = false) Long partnerId,
                                RedirectAttributes redirectAttributes) {
        vehicleService.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "Veículo removido com sucesso!");
        return buildRedirect(partnerId);
    }

    private String buildRedirect(Long partnerId) {
        if (partnerId != null) {
            return "redirect:/vehicles?partnerId=" + partnerId;
        }
        return "redirect:/vehicles";
    }

    private void aplicarDadosFipeNoVeiculo(Vehicle vehicle, FipeResponseDTO fipeData) {
        if (fipeData == null) {
            return;
        }

        if (isBlank(vehicle.getMaker())) {
            vehicle.setMaker(fipeData.getBrand());
        }

        if (isBlank(vehicle.getModel())) {
            vehicle.setModel(fipeData.getModel());
        }

        if (isBlank(vehicle.getYear_mod()) && fipeData.getModelYear() != null) {
            vehicle.setYear_mod(String.valueOf(fipeData.getModelYear()));
        }

        if (isBlank(vehicle.getTipo_combustivel())) {
            vehicle.setTipo_combustivel(fipeData.getFuel());
        }

        if (isBlank(vehicle.getType_vehicle())) {
            vehicle.setType_vehicle(mapearTipoVeiculo(fipeData.getVehicleType()));
        }

        if (vehicle.getFipe_value() == null) {
            vehicle.setFipe_value(parseValorMonetario(fipeData.getPrice()));
        }
    }

    private String mapearTipoVeiculo(Integer tipo) {
        if (tipo == null) {
            return null;
        }
        return switch (tipo) {
            case 1 -> "Carro";
            case 2 -> "Moto";
            case 3 -> "Caminhão";
            case 4 -> "Ônibus";
            case 5 -> "Micro-ônibus";
            default -> null;
        };
    }

    private Double parseValorMonetario(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }

        String normalizado = valor.replaceAll("[^0-9,.-]", "");

        if (normalizado.contains(",") && normalizado.lastIndexOf(',') > normalizado.lastIndexOf('.')) {
            normalizado = normalizado.replace(".", "");
            normalizado = normalizado.replace(',', '.');
        } else if (normalizado.indexOf(',') >= 0 && normalizado.indexOf('.') < 0) {
            normalizado = normalizado.replace(',', '.');
        }

        try {
            return Double.parseDouble(normalizado);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String formatCurrency(Double value) {
        if (value == null) {
            return "";
        }
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        return currencyFormatter.format(value);
    }

    @GetMapping("/api/placa/{placa}")
    @ResponseBody
    public ResponseEntity<?> buscarPorPlaca(@PathVariable String placa) {
        String url = "https://gateway.apibrasil.io/api/v2/vehicles/fipe";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiBrasilConfig.getBearerToken());
        headers.set("DeviceToken", apiBrasilConfig.getDeviceToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = new HashMap<>();
        body.put("placa", placa);

        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Object responseData = response.getBody().get("response");

                if (responseData instanceof Map) {
                    Map<String, Object> vehicleData = (Map<String, Object>) responseData;
                    return ResponseEntity.ok(prepareSuccessResponse(vehicleData));
                }
            }

            Map<String, Object> error = new HashMap<>();
            error.put("message", "Nenhum veículo encontrado para a placa informada.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);

        } catch (HttpClientErrorException e) {
            logger.error("Erro ao consultar a API da ApiBrasil para a placa {}: {} - {}", placa, e.getStatusCode(), e.getResponseBodyAsString(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Erro ao consultar a placa. Verifique se a placa está correta.");
            return ResponseEntity.status(e.getStatusCode()).body(error);
        } catch (Exception e) {
            logger.error("Erro inesperado ao processar a placa {}: Tipo de exceção: {}, Mensagem: {}", placa, e.getClass().getName(), e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Ocorreu um erro inesperado. Tente novamente mais tarde.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    private Map<String, Object> prepareSuccessResponse(Map<String, Object> vehicleData) {
        Map<String, Object> result = new HashMap<>();
        result.put("maker", vehicleData.get("marca"));
        result.put("model", vehicleData.get("modelo"));
        result.put("fipe_value", vehicleData.get("valor"));
        result.put("year_mod", vehicleData.get("anoModelo"));
        result.put("tipo_combustivel", vehicleData.get("combustivel"));
        result.put("color", vehicleData.get("cor"));
        return result;
    }

    /**
     * Endpoint REST para buscar dados da Fipe
     * Usado para debug e testes
     */
    @GetMapping("/fipe/test")
    @ResponseBody
    public ResponseEntity<?> testarBuscaFipe(@RequestParam String codigoFipe) {
        try {
            System.out.println("=== TESTE DE BUSCA FIPE ===");
            System.out.println("Código fornecido: " + codigoFipe);
            System.out.println("URL que será consultada: https://brasilapi.com.br/api/fipe/preco/v2/" + codigoFipe);

            FipeResponseDTO fipeData = fipeService.buscarVeiculoPorCodigoFipe(codigoFipe);

            System.out.println("Sucesso! Dados encontrados:");
            System.out.println("- Marca: " + fipeData.getBrand());
            System.out.println("- Modelo: " + fipeData.getModel());
            System.out.println("- Ano: " + fipeData.getModelYear());

            return ResponseEntity.ok(fipeData);
        } catch (Exception e) {
            System.err.println("ERRO: " + e.getMessage());
            if (e.getCause() != null) {
                System.err.println("Causa: " + e.getCause().getMessage());
            }
            e.printStackTrace();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "erro", e.getMessage(),
                            "codigo", codigoFipe,
                            "sugestao", "Verifique se o código FIPE está no formato correto (ex: 001004-9) e se existe na base de dados da FIPE"
                    ));
        }
    }
}
