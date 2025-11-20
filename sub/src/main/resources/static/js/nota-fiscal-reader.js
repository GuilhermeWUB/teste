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

    const PDF_JS_CDN = 'https://cdnjs.cloudflare.com/ajax/libs/pdf.js/3.11.174/pdf.min.js';
    const PDF_JS_WORKER = 'https://cdnjs.cloudflare.com/ajax/libs/pdf.js/3.11.174/pdf.worker.min.js';
    let pdfJsLoadingPromise = null;

    function configurePdfJs() {
        if (typeof pdfjsLib !== 'undefined' && pdfjsLib.GlobalWorkerOptions) {
            pdfjsLib.GlobalWorkerOptions.workerSrc = PDF_JS_WORKER;
            return true;
        }
        return false;
    }

    function loadPdfJsIfNeeded() {
        if (typeof pdfjsLib !== 'undefined') {
            configurePdfJs();
            return Promise.resolve();
        }

        if (!pdfJsLoadingPromise) {
            pdfJsLoadingPromise = new Promise((resolve, reject) => {
                const script = document.createElement('script');
                script.src = PDF_JS_CDN;
                script.async = true;

                script.onload = () => {
                    if (configurePdfJs()) {
                        resolve();
                    } else {
                        reject(new Error('pdf.js não carregado após tentar o fallback'));
                    }
                };

                script.onerror = () => reject(new Error('Não foi possível carregar a biblioteca PDF.'));

                document.head.appendChild(script);
            }).catch(error => {
                pdfJsLoadingPromise = null;
                throw error;
            });
        }

        return pdfJsLoadingPromise;
    }

    // Tenta configurar imediatamente
    configurePdfJs();

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
                setFeedback(`Lembre-se de confirmar as informações para que não haja erros`, 'success');
            } else {
                setFeedback('Não foi possível identificar automaticamente os dados da nota. Preencha os campos manualmente.', 'warning');
            }
        } catch (error) {
            console.error('Erro ao processar PDF da nota fiscal:', error);
            if (error.message && error.message.includes('pdf.js não carregado')) {
                setFeedback('Biblioteca PDF não carregou. Recarregue a página e tente novamente.', 'danger');
            } else if (error.name === 'InvalidPDFException') {
                setFeedback('Este arquivo não é um PDF válido.', 'danger');
            } else if (error.name === 'PasswordException') {
                setFeedback('Este PDF está protegido por senha.', 'danger');
            } else {
                setFeedback('Erro ao ler o PDF: ' + (error.message || 'Verifique o arquivo e tente novamente.'), 'danger');
            }
        }
    }

    async function readPdfText(file) {
        await loadPdfJsIfNeeded();

        const arrayBuffer = await file.arrayBuffer();

        const loadingTask = pdfjsLib.getDocument({ data: arrayBuffer });
        const pdf = await loadingTask.promise;

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
                /n[úu]mero\s+da\s+NFS-?e?\s*[:\-]?\s*(\d+)/i,
                /NFS-?e?\s*n[°º]?\s*[:\-]?\s*(\d+)/i,
                /N[°º]\s*(\d{3}[\.\s]?\d{3}[\.\s]?\d{3})/i,
                /\bNF-?e?\s*N[°º]?\s*(\d{3}[\.\s]?\d{3}[\.\s]?\d{3})/i,
                /(?:n[úu]mero\s+da\s+nota|nota\s+fiscal|n[úu]mero)\s*[:\-]?\s*([A-Za-z0-9\-\.\/]+)/i,
                /\bNF[-\s:.]?\s*([A-Za-z0-9\-\.\/]+)\b/i,
                /\bNFS-?e?\s*[:\-]?\s*([A-Za-z0-9\-\.\/]+)/i,
                /\bNFe?\s*n[°º]?\s*[:\-]?\s*([0-9]+)/i,
                /n[°º]\s*[:\-]?\s*([0-9]+)/i
            ]),
            dataEmissao: formatDate(findFirstMatch(sanitizedText, [
                /data\s+de\s+emiss[aã]o\s*[:\-]?\s*(\d{2}[\/\-]\d{2}[\/\-]\d{4})/i,
                /data[\s\/]hora\s+emiss[aã]o\s*[:\-]?\s*(\d{2}[\/\-]\d{2}[\/\-]\d{4})/i,
                /data\s+fator\s+gerador\s*[:\-]?\s*(\d{2}[\/\-]\d{2}[\/\-]\d{4})/i,
                /emiss[aã]o\s*[:\-]?\s*(\d{2}[\/\-]\d{2}[\/\-]\d{4})/i,
                /emitida?\s+em\s*[:\-]?\s*(\d{2}[\/\-]\d{2}[\/\-]\d{4})/i,
                /data\s*[:\-]?\s*(\d{2}[\/\-]\d{2}[\/\-]\d{4})/i
            ])),
            valor: normalizeCurrency(findFirstMatch(sanitizedText, [
                /valor\s+total\s+da\s+nota\s*[:\-]?\s*R?\$?\s*([\d\.\s]+,\d{2})/i,
                /valor\s+l[íi]quido\s*[:\-]?\s*R?\$?\s*([\d\.\s]+,\d{2})/i,
                /valor\s+total\s*[:\-]?\s*R?\$?\s*([\d\.\s]+,\d{2})/i,
                /total\s+(?:da\s+)?nota\s*[:\-]?\s*R?\$?\s*([\d\.\s]+,\d{2})/i,
                /total\s+geral\s*[:\-]?\s*R?\$?\s*([\d\.\s]+,\d{2})/i,
                /total\s*[:\-]?\s*R?\$?\s*([\d\.\s]+,\d{2})/i,
                /valor\s*[:\-]?\s*R?\$?\s*([\d\.\s]+,\d{2})/i,
                /R\$\s*([\d\.\s]+,\d{2})/i
            ])),
            placa: findFirstMatch(sanitizedText, [
                /placa\s*[:\-]?\s*([A-Z]{3}-?[0-9][A-Z0-9][0-9]{2})/i, // placa explícita Mercosul
                /placa\s*[:\-]?\s*([A-Z]{3}-?[0-9]{4})/i, // placa explícita antiga
                /\b([A-Z]{3}-?[0-9][A-Z0-9][0-9]{2})\b/i, // padrão Mercosul
                /\b([A-Z]{3}-?[0-9]{4})\b/i // padrão antigo
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
