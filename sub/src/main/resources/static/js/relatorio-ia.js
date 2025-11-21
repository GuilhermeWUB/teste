(function() {
    'use strict';

    document.addEventListener('DOMContentLoaded', initializeAIReports);

    function initializeAIReports() {
        const exampleCards = document.querySelectorAll('.ai-quick-card');
        const submitButton = document.getElementById('aiBtn');
        const questionInput = document.getElementById('aiPergunta');

        exampleCards.forEach(card => {
            card.addEventListener('click', () => {
                const pergunta = card.getAttribute('data-ai-example');
                applyExample(pergunta);
            });
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

    function applyExample(exampleText) {
        const questionInput = document.getElementById('aiPergunta');
        if (!questionInput || !exampleText) return;

        questionInput.value = exampleText;
        questionInput.focus();

        questionInput.parentElement.classList.add('pulse');
        setTimeout(() => questionInput.parentElement.classList.remove('pulse'), 300);
    }

    async function generateReport() {
        const questionInput = document.getElementById('aiPergunta');
        const resultsContainer = document.getElementById('aiResultados');
        const submitButton = document.getElementById('aiBtn');

        if (!questionInput || !resultsContainer || !submitButton) return;

        const question = questionInput.value.trim();

        if (!question) {
            resultsContainer.innerHTML = `
                <div class="ai-alert ai-alert-error">
                    <i class="bi bi-exclamation-triangle-fill"></i>
                    <span>Digite uma pergunta para a IA gerar o relatório.</span>
                </div>`;
            return;
        }

        const originalBtnText = submitButton.innerHTML;
        submitButton.disabled = true;
        submitButton.innerHTML = '<span class="ai-loading"></span> Analisando...';

        resultsContainer.innerHTML = `
            <div class="ai-alert ai-alert-info">
                <span class="ai-loading" style="border-color: rgba(70,215,255,0.5); border-top-color: transparent;"></span>
                <span>A IA está lendo os dados e montando seu relatório...</span>
            </div>`;

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
                body: JSON.stringify({ pergunta: question })
            });

            const data = await response.json();
            resultsContainer.innerHTML = '';

            if (response.ok && data.sucesso && data.html) {
                const resultWrapper = document.createElement('div');
                resultWrapper.className = 'ai-table-wrapper p-4';
                resultWrapper.innerHTML = `
                    ${data.html}
                    <div class="text-end mt-3" style="color: var(--ai-muted); font-size: 0.9rem;">
                        <i class="bi bi-stars"></i> Gerado por Gemini AI em ${new Date().toLocaleTimeString()}
                    </div>
                `;
                resultsContainer.appendChild(resultWrapper);
            } else {
                resultsContainer.innerHTML = `
                    <div class="ai-alert ai-alert-error">
                        <i class="bi bi-x-circle-fill"></i>
                        <span>${data.mensagem || 'Não consegui analisar os dados. Tente reformular a pergunta.'}</span>
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
})();
