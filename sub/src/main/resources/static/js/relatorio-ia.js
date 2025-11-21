(function() {
    'use strict';

    document.addEventListener('DOMContentLoaded', initializeAIReports);

    function initializeAIReports() {
        const exampleCards = document.querySelectorAll('.ai-quick-card');
        const submitButton = document.getElementById('aiBtn');
        const questionInput = document.getElementById('aiPergunta');
        const toneChips = document.querySelectorAll('.ai-chip');
        const datasetSelect = document.getElementById('aiDataset');
        const outputSelect = document.getElementById('aiOutput');

        setupPlaceholderRotation(questionInput);
        setupToneChips(toneChips);

        exampleCards.forEach(card => {
            card.addEventListener('click', () => {
                applyExample({
                    text: card.getAttribute('data-ai-example'),
                    dataset: card.getAttribute('data-dataset'),
                    tone: card.getAttribute('data-tone')
                });
            });
        });

        if (submitButton) {
            submitButton.addEventListener('click', event => {
                event.preventDefault();
                generateReport();
            });
        }

        if (questionInput) {
            questionInput.addEventListener('keydown', event => {
                if (event.key === 'Enter' && !event.shiftKey) {
                    event.preventDefault();
                    generateReport();
                }
            });
        }

        if (datasetSelect) {
            datasetSelect.addEventListener('change', () => pulseElement(datasetSelect));
        }

        if (outputSelect) {
            outputSelect.addEventListener('change', () => pulseElement(outputSelect));
        }
    }

    function setupPlaceholderRotation(input) {
        if (!input) return;
        const examples = [
            'Monte um painel com KPIs de faturamento e churn dos últimos 90 dias',
            'Quais veículos ativos estão sem vistoria válida e quem são os responsáveis?',
            'Resuma ações priorizadas para reduzir inadimplência neste trimestre',
            'Gere um comparativo de processos jurídicos abertos x resolvidos por mês'
        ];
        let index = 0;
        setInterval(() => {
            index = (index + 1) % examples.length;
            input.setAttribute('placeholder', examples[index]);
        }, 7000);
    }

    function setupToneChips(chips) {
        chips.forEach(chip => {
            chip.addEventListener('click', () => {
                chips.forEach(c => c.classList.remove('active'));
                chip.classList.add('active');
            });
        });
    }

    function applyExample({ text, dataset, tone }) {
        const questionInput = document.getElementById('aiPergunta');
        const datasetSelect = document.getElementById('aiDataset');
        const toneChips = document.querySelectorAll('.ai-chip');

        if (!questionInput || !text) return;

        questionInput.value = text;
        questionInput.focus();
        pulseElement(questionInput.parentElement);

        if (datasetSelect && dataset) {
            datasetSelect.value = dataset;
            pulseElement(datasetSelect);
        }

        if (tone && toneChips.length) {
            toneChips.forEach(chip => {
                chip.classList.toggle('active', chip.dataset.tone === tone);
            });
        }
    }

    async function generateReport() {
        const questionInput = document.getElementById('aiPergunta');
        const resultsContainer = document.getElementById('aiResultados');
        const submitButton = document.getElementById('aiBtn');
        const datasetSelect = document.getElementById('aiDataset');
        const outputSelect = document.getElementById('aiOutput');
        const toneChip = document.querySelector('.ai-chip.active');
        const evidenceToggle = document.getElementById('aiEvidence');
        const explainToggle = document.getElementById('aiExplain');

        if (!questionInput || !resultsContainer || !submitButton) return;

        const question = questionInput.value.trim();

        if (!question) {
            resultsContainer.classList.remove('ai-results--empty');
            resultsContainer.innerHTML = `
                <div class="ai-alert ai-alert-error">
                    <i class="bi bi-exclamation-triangle-fill"></i>
                    <span>Por favor, descreva o relatório que deseja gerar.</span>
                </div>`;
            return;
        }

        const context = buildContext({
            dataset: datasetSelect?.value,
            output: outputSelect?.value,
            tone: toneChip?.dataset.tone,
            evidence: evidenceToggle?.checked,
            explain: explainToggle?.checked
        });

        const enrichedQuestion = context ? `${question}\n\n${context}` : question;

        const originalBtnText = submitButton.innerHTML;
        submitButton.disabled = true;
        submitButton.innerHTML = '<span class="ai-loading"></span> Refinando consulta...';

        resultsContainer.classList.remove('ai-results--empty');
        resultsContainer.innerHTML = `
            <div class="ai-alert ai-alert-info fade show">
                <span class="ai-loading" style="border-color: #3b82f6; border-top-color: transparent; margin-right: 0.5rem;"></span>
                <span>Conectando às bases e gerando um relatório com as evidências solicitadas...</span>
            </div>
        `;

        try {
            const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
            const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

            const headers = { 'Content-Type': 'application/json' };
            if (csrfToken && csrfHeader) {
                headers[csrfHeader] = csrfToken;
            }

            const response = await fetch('/api/relatorios-ia/analisar', {
                method: 'POST',
                headers: headers,
                body: JSON.stringify({ pergunta: enrichedQuestion })
            });

            const data = await response.json();
            resultsContainer.innerHTML = '';

            if (response.ok && data.sucesso && data.html) {
                const resultWrapper = document.createElement('div');
                resultWrapper.className = 'animate__animated animate__fadeIn';
                resultWrapper.innerHTML = `
                    <div class="ai-table-wrapper p-4" style="background: var(--ai-surface);">
                        ${data.html}
                        <div class="text-end mt-3">
                            <small style="color: var(--ai-muted); font-size: 0.85rem;">
                                <i class="bi bi-stars"></i> Gerado às ${new Date().toLocaleTimeString()} com contexto seguro
                            </small>
                        </div>
                    </div>
                `;
                resultsContainer.appendChild(resultWrapper);
            } else {
                resultsContainer.innerHTML = `
                    <div class="ai-alert ai-alert-error">
                        <i class="bi bi-x-circle-fill"></i>
                        <span>${data.mensagem || 'Não consegui analisar os dados. Tente reformular.'}</span>
                    </div>`;
            }

        } catch (error) {
            console.error('Erro ao conectar:', error);
            resultsContainer.innerHTML = `
                <div class="ai-alert ai-alert-error">
                    <i class="bi bi-wifi-off"></i>
                    <span>Erro de conexão: verifique sua internet ou contate o suporte.</span>
                </div>`;
        } finally {
            submitButton.disabled = false;
            submitButton.innerHTML = originalBtnText;
        }
    }

    function buildContext({ dataset, output, tone, evidence, explain }) {
        const parts = [];
        if (dataset) parts.push(`Foque na base: ${dataset}.`);
        if (output) parts.push(`Entregue no formato: ${output}.`);
        if (tone) parts.push(`Use tom ${tone}.`);
        if (evidence) parts.push('Liste as evidências, campos e cálculos utilizados.');
        if (explain) parts.push('Inclua próximos passos recomendados.');
        return parts.length ? `Contexto: ${parts.join(' ')}` : '';
    }

    function pulseElement(element) {
        if (!element) return;
        element.classList.add('ai-pulse');
        setTimeout(() => element.classList.remove('ai-pulse'), 450);
    }
})();
