(function () {
    'use strict';

    const fileInput = document.getElementById('notaFiscalPdf');
    if (!fileInput) {
        return;
    }

    const numeroNotaInput = document.getElementById('numeroNota');
    const dataEmissaoInput = document.getElementById('dataEmissao');
    const valorInput = document.getElementById('valorNota');
    const placaInput = document.getElementById('placa');
    const feedbackElement = document.getElementById('notaFiscalAutoFillFeedback');

    const PDF_JS_WORKER = 'https://cdnjs.cloudflare.com/ajax/libs/pdf.js/4.2.67/pdf.worker.min.js';

    if (typeof pdfjsLib !== 'undefined' && pdfjsLib.GlobalWorkerOptions) {
        pdfjsLib.GlobalWorkerOptions.workerSrc = PDF_JS_WORKER;
    }

    fileInput.addEventListener('change', handlePdfUpload);

    function setFeedback(message, status = 'info') {
        if (!feedbackElement) {
            return;
        }

        feedbackElement.classList.remove('d-none', 'alert-info', 'alert-success', 'alert-warning', 'alert-danger');
        feedbackElement.textContent = message;
        feedbackElement.classList.add(`alert-${status}`);
    }

    function clearFeedback() {
        if (!feedbackElement) {
            return;
        }
        feedbackElement.classList.add('d-none');
    }

    async function handlePdfUpload(event) {
        const file = event.target.files && event.target.files[0];
        if (!file) {
            clearFeedback();
            return;
        }

        if (file.type !== 'application/pdf') {
            setFeedback('Envie apenas arquivos em PDF para tentar preencher os campos automaticamente.', 'warning');
            return;
        }

        setFeedback('Lendo o PDF e procurando pelos dados da nota...', 'info');

        try {
            const text = await readPdfText(file);
            const extracted = extractInvoiceData(text);

            const filledFields = applyExtractedData(extracted);

            if (filledFields.length) {
                setFeedback(`Campos preenchidos automaticamente: ${filledFields.join(', ')}.`, 'success');
            } else {
                setFeedback('Não foi possível identificar automaticamente os dados da nota. Preencha os campos manualmente.', 'warning');
            }
        } catch (error) {
            console.error('Erro ao processar PDF da nota fiscal:', error);
            setFeedback('Não conseguimos ler este PDF. Verifique o arquivo e tente novamente.', 'danger');
        }
    }

    async function readPdfText(file) {
        if (typeof pdfjsLib === 'undefined') {
            throw new Error('pdf.js não carregado');
        }

        const arrayBuffer = await file.arrayBuffer();
        const pdf = await pdfjsLib.getDocument({ data: arrayBuffer }).promise;
        let combinedText = '';

        const pagesToRead = Math.min(pdf.numPages, 5);
        for (let pageNumber = 1; pageNumber <= pagesToRead; pageNumber += 1) {
            const page = await pdf.getPage(pageNumber);
            const textContent = await page.getTextContent();
            const pageText = textContent.items.map(item => item.str).join(' ');
            combinedText += `\n${pageText}`;
        }

        return combinedText;
    }

    function extractInvoiceData(text) {
        if (!text) {
            return {};
        }

        const sanitizedText = text.replace(/\s+/g, ' ').trim();

        return {
            numeroNota: findFirstMatch(sanitizedText, [
                /(?:n[úu]mero\s+da\s+nota|nota\s+fiscal)\s*[:\-]?\s*([A-Za-z0-9\-\.\/]+)/i,
                /\bNF[-\s:]?\s*([A-Za-z0-9]+)\b/i,
                /\bNFS-?e?\s*[:\-]?\s*([A-Za-z0-9]+)/i
            ]),
            dataEmissao: formatDate(findFirstMatch(sanitizedText, [
                /data\s+de\s+emiss[aã]o\s*[:\-]?\s*(\d{2}[\/\-]\d{2}[\/\-]\d{4})/i,
                /emiss[aã]o\s*[:\-]?\s*(\d{2}[\/\-]\d{2}[\/\-]\d{4})/i
            ])),
            valor: normalizeCurrency(findFirstMatch(sanitizedText, [
                /valor\s+(?:total\s+da\s+nota|total)\s*[:\-]?\s*R?\$?\s*([\d\.\s]+,\d{2})/i,
                /total\s*R?\$?\s*([\d\.\s]+,\d{2})/i,
                /R\$\s*([\d\.\s]+,\d{2})/i
            ])),
            placa: findFirstMatch(sanitizedText, [
                /\b[A-Z]{3}-?[0-9][A-Z0-9][0-9]{2}\b/, // padrão Mercosul
                /\b[A-Z]{3}-?[0-9]{4}\b/ // padrão antigo
            ])
        };
    }

    function findFirstMatch(text, patterns) {
        for (const pattern of patterns) {
            const result = text.match(pattern);
            if (result && result[1]) {
                return result[1].trim();
            }
        }
        return null;
    }

    function formatDate(dateString) {
        if (!dateString) {
            return null;
        }
        const match = dateString.match(/(\d{2})[\/-](\d{2})[\/-](\d{4})/);
        if (!match) {
            return null;
        }
        const [, day, month, year] = match;
        return `${year}-${month}-${day}`;
    }

    function normalizeCurrency(value) {
        if (!value) {
            return null;
        }
        const numeric = value.replace(/\./g, '').replace(/\s/g, '').replace(',', '.');
        const parsed = Number.parseFloat(numeric);
        if (Number.isNaN(parsed)) {
            return null;
        }
        return parsed.toFixed(2);
    }

    function applyExtractedData(data) {
        const filled = [];

        if (data.numeroNota && numeroNotaInput) {
            numeroNotaInput.value = data.numeroNota;
            filled.push('Número da nota');
        }

        if (data.dataEmissao && dataEmissaoInput) {
            dataEmissaoInput.value = data.dataEmissao;
            filled.push('Data de emissão');
        }

        if (data.valor && valorInput) {
            valorInput.value = data.valor;
            filled.push('Valor');
        }

        if (data.placa && placaInput) {
            placaInput.value = data.placa.replace('-', '').toUpperCase();
            filled.push('Placa');
        }

        return filled;
    }
})();
