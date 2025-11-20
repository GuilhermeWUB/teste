package com.necsus.necsusspring.controller;

import com.necsus.necsusspring.dto.ExtractedDataDto;
import com.necsus.necsusspring.dto.FinancialMovementView;
import com.necsus.necsusspring.model.BankSlip;
import com.necsus.necsusspring.model.BillToPay;
import com.necsus.necsusspring.model.FiscalDocument;
import com.necsus.necsusspring.service.BillToPayService;
import com.necsus.necsusspring.service.FileStorageService;
import com.necsus.necsusspring.service.FiscalDocumentService;
import com.necsus.necsusspring.service.GeminiService;
import com.necsus.necsusspring.service.JinjavaService;
import com.necsus.necsusspring.service.PaymentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.Date;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Controller
@RequestMapping("/financeiro")
public class FinanceiroController {

    private final JinjavaService jinjavaService;
    private final PaymentService paymentService; // INJETADO
    private final BillToPayService billToPayService; // INJETADO
    private final FileStorageService fileStorageService;
    private final FiscalDocumentService fiscalDocumentService;
    private final GeminiService geminiService;

    public FinanceiroController(JinjavaService jinjavaService,
                                PaymentService paymentService,
                                BillToPayService billToPayService,
                                FileStorageService fileStorageService,
                                FiscalDocumentService fiscalDocumentService,
                                GeminiService geminiService) {
        this.jinjavaService = jinjavaService;
        this.paymentService = paymentService; // INJETADO
        this.billToPayService = billToPayService; // INJETADO
        this.fileStorageService = fileStorageService;
        this.fiscalDocumentService = fiscalDocumentService;
        this.geminiService = geminiService;
    }

    @GetMapping
    public String dashboard(Model model) {
        configurePage(model, "dashboard", "Visão geral", "Acompanhe os principais indicadores financeiros do sistema.");

        // 1. CARREGAR DADOS REAIS DO MÓDULO DE PAGAMENTOS
        List<BankSlip> paidInvoices = paymentService.listPaidInvoices(); // Entradas Recebidas
        List<BillToPay> paidBills = billToPayService.listPaidBills(); // Saídas Pagas

        // 2. CALCULAR TOTAIS

        // Total Entradas (Recebido) - Soma o valor recebido (ou o valor nominal, se não houver valor recebido)
        BigDecimal totalEntradasBD = paidInvoices.stream()
                .map(b -> b.getValorRecebido() != null ? b.getValorRecebido() : b.getValor())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Total Saídas (Pago) - Soma o valor pago (ou o valor nominal, se não houver valor pago)
        BigDecimal totalSaidasBD = paidBills.stream()
                .map(b -> b.getValorPago() != null ? b.getValorPago() : b.getValor())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        double totalEntradas = totalEntradasBD.doubleValue();
        double totalSaidas = totalSaidasBD.doubleValue();

        // 3. PREPARAR CONTEXTO PARA O GRÁFICO 1: ENTRADAS VS SAÍDAS
        Map<String, Object> chartContext = new HashMap<>();
        chartContext.put("chartId", "chartEntradaSaida");
        chartContext.put("titulo", "Entradas vs Saídas (Real) - Recebidas/Pagas");
        chartContext.put("entradas", totalEntradas);
        chartContext.put("saidas", totalSaidas);

        String chartHtml = jinjavaService.render("chart-pizza-entrada-saida.html", chartContext);

        // 4. PASSAR DADOS REAIS PARA O TEMPLATE THYMELEAF
        model.addAttribute("chartEntradaSaida", chartHtml);
        model.addAttribute("totalEntradas", totalEntradas);
        model.addAttribute("totalSaidas", totalSaidas);
        model.addAttribute("saldo", totalEntradas - totalSaidas);

        // 5. PREPARAR CONTEXTO PARA O GRÁFICO 2: CATEGORIAS (Entradas agrupadas + Saídas por Categoria)
        List<Map<String, Object>> categorias = new ArrayList<>();

        // --- CATEGORIAS DE ENTRADAS: Agrupadas em uma só (Adaptado pela falta de categoria em BankSlip) ---
        Map<String, Object> totalEntradasMap = new HashMap<>();
        totalEntradasMap.put("nome", "Entradas Recebidas (Total)");
        totalEntradasMap.put("valor", totalEntradas);
        totalEntradasMap.put("cor", "rgba(40, 167, 69, 0.8)"); // Verde
        totalEntradasMap.put("borderCor", "rgba(40, 167, 69, 1)");
        categorias.add(totalEntradasMap);

        // --- CATEGORIAS DE SAÍDAS (BillToPay): Agrupadas pelo campo 'categoria' ---
        Map<String, BigDecimal> groupedPaidBills = paidBills.stream()
                .collect(Collectors.groupingBy(
                        b -> b.getCategoria() != null && !b.getCategoria().isBlank() ? b.getCategoria() : "Outras Despesas",
                        Collectors.mapping(
                                b -> b.getValorPago() != null ? b.getValorPago() : b.getValor(),
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                        )
                ));

        // Mapeamento simples de cores para as categorias de Saída mais comuns (para manter a consistência do visual)
        Map<String, String[]> corMap = Map.of(
                "Fornecedores", new String[]{"rgba(220, 53, 69, 0.8)", "rgba(220, 53, 69, 1)"}, // Vermelho
                "Salarios", new String[]{"rgba(255, 193, 7, 0.8)", "rgba(255, 193, 7, 1)"}, // Amarelo
                "Despesas Fixas", new String[]{"rgba(108, 117, 125, 0.8)", "rgba(108, 117, 125, 1)"}, // Cinza
                "Impostos", new String[]{"rgba(111, 66, 193, 0.8)", "rgba(111, 66, 193, 1)"} // Roxo
        );

        groupedPaidBills.forEach((categoriaNome, valor) -> {
            Map<String, Object> item = new HashMap<>();
            item.put("nome", categoriaNome);
            item.put("valor", valor.doubleValue());
            String[] cores = corMap.getOrDefault(categoriaNome, new String[]{"rgba(33, 37, 41, 0.8)", "rgba(33, 37, 41, 1)"}); // Default cinza escuro
            item.put("cor", cores[0]);
            item.put("borderCor", cores[1]);
            categorias.add(item);
        });

        // Contexto para o grafico de categorias
        Map<String, Object> categoriasContext = new HashMap<>();
        categoriasContext.put("chartId", "chartCategorias");
        categoriasContext.put("titulo", "Distribuição por Categorias (Entradas e Saídas Pagas)");
        categoriasContext.put("categorias", categorias);

        String chartCategoriasHtml = jinjavaService.render("chart-pizza-categorias.html", categoriasContext);
        model.addAttribute("chartCategorias", chartCategoriasHtml);

        return "financeiro/dashboard";
    }

    @GetMapping("/lancamentos")
    public String lancamentos() {
        return "redirect:/financeiro/lancamentos/contas";
    }

    @GetMapping("/lancamentos/contas")
    public String lancamentosContas(Model model) {
        configurePage(model, "lancamentos", "Contas a pagar e receber", "Registre entradas, saídas e mantenha o fluxo diário organizado.");
        populateLancamentosModel(model);
        model.addAttribute("recentFiscalDocuments", fiscalDocumentService.listRecentDocuments());
        return "financeiro/lancamentos-contas";
    }

    @GetMapping("/lancamentos/notas-fiscais")
    public String lancamentosNotasFiscais(Model model) {
        configurePage(model, "lancamentos", "Notas fiscais", "Centralize os documentos emitidos e acompanhe a evolução da integração fiscal.");
        return "financeiro/lancamentos-notas";
    }

    @PostMapping(value = "/lancamentos/contas/upload-nota", consumes = "multipart/form-data")
    public String uploadNotaFiscal(@RequestParam String descricao,
                                   @RequestParam(value = "numeroNota", required = false) String numeroNota,
                                   @RequestParam(value = "placa", required = false) String placa,
                                   @RequestParam(value = "dataEmissao", required = false)
                                   @DateTimeFormat(pattern = "yyyy-MM-dd") Date dataEmissao,
                                   @RequestParam(value = "valor", required = false) BigDecimal valor,
                                   @RequestParam("notaFiscalPdf") MultipartFile notaFiscalPdf,
                                   RedirectAttributes redirectAttributes) {
        if (notaFiscalPdf == null || notaFiscalPdf.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Selecione um PDF para enviar.");
            return "redirect:/financeiro/lancamentos/contas";
        }
        if (!isPdfFile(notaFiscalPdf)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Envie apenas arquivos em PDF.");
            return "redirect:/financeiro/lancamentos/contas";
        }

        try {
            String pdfPath = fileStorageService.storeFile(notaFiscalPdf);
            FiscalDocument document = new FiscalDocument();
            document.setDescricao(descricao);
            document.setNumeroNota(numeroNota);
            document.setPlaca(placa);
            document.setDataEmissao(dataEmissao);
            document.setValor(valor);
            document.setPdfPath(pdfPath);
            fiscalDocumentService.save(document);
            redirectAttributes.addFlashAttribute("successMessage", "Nota fiscal enviada com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erro ao enviar nota fiscal: " + e.getMessage());
        }
        return "redirect:/financeiro/lancamentos/contas";
    }

    @GetMapping("/lancamentos/contas/notas/{documentId}/download")
    public ResponseEntity<byte[]> downloadNotaFiscal(@PathVariable Long documentId) {
        FiscalDocument document = fiscalDocumentService.findById(documentId);
        byte[] fileBytes = fileStorageService.loadFile(document.getPdfPath());
        String fileName = fileStorageService.extractFileName(document.getPdfPath());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                .body(fileBytes);
    }

    /**
     * Endpoint para extrair dados de nota fiscal usando Gemini AI
     */
    @PostMapping("/lancamentos/contas/extrair-dados-nota")
    @ResponseBody
    public ResponseEntity<?> extrairDadosNota(@RequestParam("notaFiscalPdf") MultipartFile notaFiscalPdf) {
        try {
            if (notaFiscalPdf == null || notaFiscalPdf.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "PDF não fornecido"));
            }

            if (!isPdfFile(notaFiscalPdf)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Arquivo deve ser um PDF"));
            }

            ExtractedDataDto extractedData = geminiService.extractDataFromPdf(notaFiscalPdf);
            return ResponseEntity.ok(extractedData);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erro ao extrair dados: " + e.getMessage()));
        }
    }

    @GetMapping("/movimento-caixa")
    public String movimentoCaixa(Model model) {
        configurePage(model, "movimento-caixa", "Movimento de Caixa", "Visualize a evolução do caixa e acompanhe tendências.");
        return "financeiro/movimento-caixa";
    }

    @GetMapping("/cadastros")
    public String cadastros(Model model) {
        configurePage(model, "cadastros", "Cadastros", "Gerencie contas, categorias e configurações financeiras.");
        return "financeiro/cadastros";
    }

    @GetMapping("/ferramentas")
    public String ferramentas(Model model) {
        configurePage(model, "ferramentas", "Ferramentas", "Acesse utilitários e automações que aceleram seu dia a dia.");
        return "financeiro/ferramentas";
    }

    @GetMapping("/relatorios")
    public String relatorios(Model model) {
        configurePage(model, "relatorios", "Relatórios", "Centralize análises e exportações para apoiar decisões estratégicas.");
        return "financeiro/relatorios";
    }

    private void configurePage(Model model, String currentPage, String heroTitle, String heroSubtitle) {
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("pageTitle", "SUB - Financeiro | " + heroTitle);
        model.addAttribute("financeiroHeroTitle", heroTitle);
        model.addAttribute("financeiroHeroSubtitle", heroSubtitle);
    }

    private boolean isPdfFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }
        String contentType = file.getContentType();
        if (contentType != null && MediaType.APPLICATION_PDF_VALUE.equalsIgnoreCase(contentType)) {
            return true;
        }
        String originalFilename = file.getOriginalFilename();
        return originalFilename != null && originalFilename.toLowerCase().endsWith(".pdf");
    }

    private void populateLancamentosModel(Model model) {
        List<BankSlip> pendingReceivables = paymentService.listPendingInvoices();
        List<BankSlip> paidReceivables = paymentService.listPaidInvoices();
        List<BillToPay> pendingBills = billToPayService.listPendingBills();
        List<BillToPay> paidBills = billToPayService.listPaidBills();

        BigDecimal totalReceber = pendingReceivables.stream()
                .map(BankSlip::getValor)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRecebido = paidReceivables.stream()
                .map(b -> b.getValorRecebido() != null ? b.getValorRecebido() : b.getValor())
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPagar = pendingBills.stream()
                .map(BillToPay::getValor)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPago = paidBills.stream()
                .map(b -> b.getValorPago() != null ? b.getValorPago() : b.getValor())
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<BankSlip> upcomingReceivables = pendingReceivables.stream()
                .sorted(Comparator.comparing(BankSlip::getVencimento, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(5)
                .collect(Collectors.toList());

        List<BillToPay> upcomingPayables = pendingBills.stream()
                .sorted(Comparator.comparing(BillToPay::getDataVencimento, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(5)
                .collect(Collectors.toList());

        List<FinancialMovementView> recentMovements = Stream.concat(
                        paidReceivables.stream()
                                .filter(b -> b.getDataPagamento() != null)
                                .map(b -> new FinancialMovementView(
                                        "Entrada",
                                        b.getPartner() != null ? b.getPartner().getName() : "Recebimento",
                                        b.getNumeroDocumento() != null ? b.getNumeroDocumento() : "Boleto #" + b.getId(),
                                        b.getDataPagamento(),
                                        b.getValorRecebido() != null ? b.getValorRecebido() : b.getValor(),
                                        true
                                )),
                        paidBills.stream()
                                .filter(b -> b.getDataPagamento() != null)
                                .map(b -> new FinancialMovementView(
                                        "Saída",
                                        b.getDescricao(),
                                        b.getFornecedor() != null ? b.getFornecedor() : b.getCategoria(),
                                        b.getDataPagamento(),
                                        b.getValorPago() != null ? b.getValorPago() : b.getValor(),
                                        false
                                ))
                )
                .sorted(Comparator.comparing(FinancialMovementView::getData, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(6)
                .collect(Collectors.toList());

        model.addAttribute("totalReceber", totalReceber);
        model.addAttribute("totalRecebido", totalRecebido);
        model.addAttribute("totalPagar", totalPagar);
        model.addAttribute("totalPago", totalPago);
        model.addAttribute("quantidadeReceber", pendingReceivables.size());
        model.addAttribute("quantidadePagar", pendingBills.size());
        model.addAttribute("upcomingReceivables", upcomingReceivables);
        model.addAttribute("upcomingPayables", upcomingPayables);
        model.addAttribute("recentMovements", recentMovements);
    }
}
