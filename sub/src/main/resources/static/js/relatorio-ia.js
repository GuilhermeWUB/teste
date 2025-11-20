(function() {
    'use strict';

    document.addEventListener('DOMContentLoaded', initializeAIReports);

    function initializeAIReports() {
        const exampleCards = document.querySelectorAll('[data-ai-example]');
        const submitButton = document.getElementById('aiBtn');
        const questionInput = document.getElementById('aiPergunta');

        exampleCards.forEach(card => {
            card.addEventListener('click', () => applyExample(card.dataset.aiExample));
        });

        if (submitButton) {
            submitButton.addEventListener('click', event => {
                event.preventDefault();
                generateReport();
            });
        }

        if (questionInput) {
            questionInput.addEventListener('keypress', event => {
                if (event.key === 'Enter') {
                    event.preventDefault();
                    generateReport();
                }
            });
        }
    }

    function applyExample(example) {
        const questionInput = document.getElementById('aiPergunta');
        if (!questionInput) return;

        questionInput.value = example;
        questionInput.focus();
        questionInput.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }

    async function generateReport() {
        const questionInput = document.getElementById('aiPergunta');
        const resultsContainer = document.getElementById('aiResultados');
        const submitButton = document.getElementById('aiBtn');

        if (!questionInput || !resultsContainer || !submitButton) return;

        const question = questionInput.value.trim();

        if (!question) {
            resultsContainer.innerHTML = createAlert('error', 'Por favor, descreva o que deseja analisar.');
            return;
        }

        setLoading(submitButton, true);
        resultsContainer.innerHTML = createAlert('info', 'Processando sua pergunta com IA...');

        try {
            const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
            const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

            const headers = { 'Content-Type': 'application/json' };
            if (csrfToken && csrfHeader) {
                headers[csrfHeader] = csrfToken;
            }

            const response = await fetch('/api/relatorios-ia/gerar', {
                method: 'POST',
                headers,
                body: JSON.stringify({ pergunta: question })
            });

            const data = await response.json();
            resultsContainer.innerHTML = '';

            if (data.sucesso && Array.isArray(data.dados) && data.dados.length > 0) {
                const status = `${data.mensagem} - ${data.totalLinhas} linha(s) encontrada(s)`;
                resultsContainer.insertAdjacentHTML('beforeend', createAlert('success', status));
                resultsContainer.insertAdjacentHTML('beforeend', renderTable(data.dados));
            } else if (data.sucesso && Array.isArray(data.dados) && data.dados.length === 0) {
                resultsContainer.innerHTML = createAlert('info', 'Nenhum resultado encontrado para sua pergunta.');
            } else {
                resultsContainer.innerHTML = createAlert('error', data.mensagem || 'Erro ao gerar relatório.');
            }
        } catch (error) {
            resultsContainer.innerHTML = createAlert('error', `Erro de conexão: ${error.message}`);
        } finally {
            setLoading(submitButton, false);
        }
    }

    function renderTable(rows) {
        const columns = Object.keys(rows[0]);
        let tableHTML = '<div class="ai-table-wrapper">';
        tableHTML += '<table class="ai-table">';
        tableHTML += '<thead><tr>';
        columns.forEach(column => {
            tableHTML += `<th>${formatColumnName(column)}</th>`;
        });
        tableHTML += '</tr></thead>';
        tableHTML += '<tbody>';
        rows.forEach(row => {
            tableHTML += '<tr>';
            columns.forEach(column => {
                tableHTML += `<td>${formatValue(row[column])}</td>`;
            });
            tableHTML += '</tr>';
        });
        tableHTML += '</tbody></table></div>';
        return tableHTML;
    }

    function formatColumnName(name) {
        return name
            .split('_')
            .map(word => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase())
            .join(' ');
    }

    function formatValue(value) {
        if (value === null || value === undefined) {
            return '<em style="color: var(--ai-muted);">-</em>';
        }

        if (typeof value === 'number') {
            if (Number.isInteger(value) && value > 1000000) {
                return value.toLocaleString('pt-BR');
            }
            if (!Number.isInteger(value)) {
                return 'R$ ' + value.toLocaleString('pt-BR', {
                    minimumFractionDigits: 2,
                    maximumFractionDigits: 2
                });
            }
            return value.toLocaleString('pt-BR');
        }

        if (typeof value === 'string' && /^\d{4}-\d{2}-\d{2}/.test(value)) {
            const date = new Date(value);
            if (!Number.isNaN(date.getTime())) {
                return date.toLocaleDateString('pt-BR');
            }
        }

        const div = document.createElement('div');
        div.textContent = value;
        return div.innerHTML;
    }

    function createAlert(type, message) {
        const icons = {
            success: 'bi-check-circle-fill',
            error: 'bi-exclamation-triangle-fill',
            info: 'bi-info-circle-fill'
        };

        const icon = icons[type] || icons.info;
        return `<div class="ai-alert ai-alert-${type}"><i class="bi ${icon}"></i><span>${message}</span></div>`;
    }

    function setLoading(button, isLoading) {
        if (isLoading) {
            button.disabled = true;
            button.innerHTML = '<span class="ai-loading"></span><span>Gerando...</span>';
        } else {
            button.disabled = false;
            button.innerHTML = '<i class="bi bi-stars"></i><span>Gerar Relatório</span>';
        }
    }
})();
