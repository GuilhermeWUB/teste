(function () {
    'use strict';

    // Seleciona os elementos do formulário
    const fileInput = document.getElementById('notaFiscalPdf');
    if (!fileInput) {
        return; // Se o input de arquivo não existir, não faz nada
    }

    const numeroNotaInput = document.getElementById('numeroNota');
    const dataEmissaoInput = document.getElementById('dataEmissao');
    const valorInput = document.getElementById('valorNota');
    const placaInput = document.getElementById('placa');
    const feedbackElement = document.getElementById('notaFiscalAutoFillFeedback');

    // URL do novo endpoint no backend
    const EXTRACTION_ENDPOINT_URL = '/financeiro/lancamentos/contas/extrair-dados-nota';

    /**
     * Exibe uma mensagem de feedback para o usuário.
     * @param {string} message - A mensagem a ser exibida.
     * @param {'info'|'success'|'warning'|'danger'} status - O tipo de alerta (cor).
     */
    function setFeedback(message, status = 'info') {
        if (!feedbackElement) return;

        // Reseta as classes de status
        feedbackElement.className = 'alert mt-3 d-none';
        feedbackElement.classList.remove('d-none', 'alert-info', 'alert-success', 'alert-warning', 'alert-danger');

        // Define a nova mensagem e status
        feedbackElement.textContent = message;
        feedbackElement.classList.add(`alert-${status}`);
        feedbackElement.classList.remove('d-none');
    }

    /**
     * Limpa a mensagem de feedback.
     */
    function clearFeedback() {
        if (!feedbackElement) return;
        feedbackElement.classList.add('d-none');
    }

    /**
     * Preenche os campos do formulário com os dados extraídos.
     * @param {object} data - O objeto com os dados retornados pela API.
     * @returns {string[]} Uma lista com os nomes dos campos que foram preenchidos.
     */
    function applyExtractedData(data) {
        const filled = [];

        if (data.numeroNota && numeroNotaInput) {
            numeroNotaInput.value = data.numeroNota;
            filled.push('Número da nota');
        }

        if (data.dataEmissao && dataEmissaoInput) {
            // A API já deve retornar no formato YYYY-MM-DD
            dataEmissaoInput.value = data.dataEmissao;
            filled.push('Data de emissão');
        }

        if (data.valor && valorInput) {
            // A API já deve retornar o valor normalizado
            valorInput.value = data.valor;
            filled.push('Valor');
        }

        if (data.placa && placaInput) {
            placaInput.value = data.placa.replace(/[^A-Z0-9]/g, '').toUpperCase();
            filled.push('Placa');
        }

        return filled;
    }

    /**
     * Lida com o evento de seleção de um arquivo PDF.
     * Envia o arquivo para o backend para extração de dados.
     * @param {Event} event - O evento 'change' do input de arquivo.
     */
    async function handlePdfUpload(event) {
        const file = event.target.files?.[0];
        if (!file) {
            clearFeedback();
            return;
        }

        if (file.type !== 'application/pdf') {
            setFeedback('Envie apenas arquivos em PDF.', 'warning');
            return;
        }

        // Exibe o feedback de "carregando"
        setFeedback('Analisando a nota fiscal com a IA... Por favor, aguarde.', 'info');
        // Desabilita o input para evitar novos envios enquanto processa
        fileInput.disabled = true;

        const formData = new FormData();
        formData.append('notaFiscalPdf', file);

        try {
            const response = await fetch(EXTRACTION_ENDPOINT_URL, {
                method: 'POST',
                body: formData,
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(errorText || `Erro no servidor: ${response.status}`);
            }

            const extractedData = await response.json();
            const filledFields = applyExtractedData(extractedData);

            if (filledFields.length > 0) {
                setFeedback(`Dados extraídos com sucesso: ${filledFields.join(', ')}.`, 'success');
            } else {
                setFeedback('Não foi possível encontrar dados na nota fiscal. Por favor, preencha manualmente.', 'warning');
            }

        } catch (error) {
            console.error('Erro ao extrair dados da nota fiscal:', error);
            setFeedback(`Falha na extração: ${error.message}`, 'danger');
        } finally {
            // Reabilita o input ao final do processo
            fileInput.disabled = false;
        }
    }

    // Adiciona o listener de evento ao input de arquivo
    fileInput.addEventListener('change', handlePdfUpload);

})();
