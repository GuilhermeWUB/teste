(function() {
    'use strict';

    // Aguarda o DOM carregar para não dar erro de elemento null
    document.addEventListener('DOMContentLoaded', initializeAIReports);

    function initializeAIReports() {
        // SELETORES ATUALIZADOS PARA O SEU NOVO HTML
        const exampleCards = document.querySelectorAll('.ai-quick-card'); // Era .ai-example
        const submitButton = document.getElementById('aiBtn');
        const questionInput = document.getElementById('aiPergunta');

        // 1. Configura clique nos cards de exemplo
        exampleCards.forEach(card => {
            card.addEventListener('click', () => {
                // Pega o texto do atributo correto (data-ai-example)
                const pergunta = card.getAttribute('data-ai-example');
                applyExample(pergunta);
            });
        });

        // 2. Configura clique no botão "Gerar Relatório"
        if (submitButton) {
            submitButton.addEventListener('click', event => {
                event.preventDefault(); // Evita reload se estiver num form
                generateReport();
            });
        }

        // 3. Configura o "Enter" no campo de texto
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

        // Efeito visual de flash para mostrar que foi selecionado
        questionInput.parentElement.style.borderColor = '#8b5cf6'; // Cor roxa do seu tema
        setTimeout(() => {
            questionInput.parentElement.style.borderColor = '';
        }, 300);

        // Opcional: Já disparar a busca ao clicar no exemplo (descomente se quiser)
        // generateReport();
    }

    async function generateReport() {
        const questionInput = document.getElementById('aiPergunta');
        const resultsContainer = document.getElementById('aiResultados');
        const submitButton = document.getElementById('aiBtn');

        if (!questionInput || !resultsContainer || !submitButton) return;

        const question = questionInput.value.trim();

        if (!question) {
            // Usa seu estilo de alerta de erro
            resultsContainer.innerHTML = `
                <div class="ai-alert ai-alert-error">
                    <i class="bi bi-exclamation-triangle-fill"></i>
                    <span>Por favor, digite uma pergunta para a IA analisar.</span>
                </div>`;
            return;
        }

        // --- ESTADO DE LOADING ---
        const originalBtnText = submitButton.innerHTML;
        submitButton.disabled = true;
        submitButton.innerHTML = '<span class="ai-loading"></span> Analisando...';

        resultsContainer.innerHTML = `
            <div class="ai-alert ai-alert-info fade show">
                <span class="ai-loading" style="border-color: #3b82f6; border-top-color: transparent; margin-right: 0.5rem;"></span>
                <span>A IA está lendo os dados e gerando seu relatório...</span>
            </div>
        `;

        try {
            // Pega Tokens CSRF do Spring Security (se existirem na página)
            const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
            const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

            const headers = { 'Content-Type': 'application/json' };
            if (csrfToken && csrfHeader) {
                headers[csrfHeader] = csrfToken;
            }

            // --- CHAMADA AO BACKEND (RAG) ---
            const response = await fetch('/api/relatorios-ia/analisar', {
                method: 'POST',
                headers: headers,
                body: JSON.stringify({ pergunta: question })
            });

            const data = await response.json();

            // Limpa o container
            resultsContainer.innerHTML = '';

            if (response.ok && data.sucesso && data.html) {
                // SUCESSO: Renderiza o HTML que a IA mandou
                // Adiciona uma div wrapper para animação se quiser
                const resultWrapper = document.createElement('div');
                resultWrapper.className = 'animate__animated animate__fadeIn'; // Se tiver animate.css
                resultWrapper.innerHTML = `
                    <div class="ai-table-wrapper p-4" style="background: var(--ai-surface);">
                        ${data.html}
                        <div class="text-end mt-3">
                            <small style="color: var(--ai-muted); font-size: 0.85rem;">
                                <i class="bi bi-stars"></i> Gerado por Gemini AI em ${new Date().toLocaleTimeString()}
                            </small>
                        </div>
                    </div>
                `;
                resultsContainer.appendChild(resultWrapper);
            } else {
                // ERRO DO BACKEND
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
                    <span>Erro de conexão: Verifique sua internet ou contate o suporte.</span>
                </div>`;
        } finally {
            // Restaura o botão
            submitButton.disabled = false;
            submitButton.innerHTML = originalBtnText;
        }
    }
})();