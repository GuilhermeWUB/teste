/**
 * Script para gerar relatórios de associados via AJAX
 */

document.addEventListener('DOMContentLoaded', function() {
    const form = document.querySelector('form[action*="/reports/partners"]');
    const generateButton = form.querySelector('button[type="submit"]');
    const resultsContainer = document.querySelector('.report-results');
    const emptyState = document.querySelector('.report-empty');
    const errorAlert = document.querySelector('.alert-warning');

    // Interceptar o submit do formulário
    form.addEventListener('submit', function(event) {
        event.preventDefault();
        generateReport();
    });

    function generateReport() {
        // Mostrar indicador de carregamento
        showLoadingState();

        // Coletar seções selecionadas
        const selectedSections = getSelectedSections();

        // Construir URL com parâmetros
        const url = buildReportUrl(selectedSections);

        // Fazer requisição AJAX
        fetch(url)
            .then(response => response.json())
            .then(data => {
                hideLoadingState();
                renderReport(data, selectedSections);
            })
            .catch(error => {
                hideLoadingState();
                showError('Erro ao gerar o relatório. Por favor, tente novamente.');
                console.error('Erro ao gerar relatório:', error);
            });
    }

    function getSelectedSections() {
        const checkboxes = form.querySelectorAll('input[name="sections"]:checked');
        return Array.from(checkboxes).map(cb => cb.value);
    }

    function buildReportUrl(sections) {
        const baseUrl = '/reports/partners/data';
        if (sections.length === 0) {
            return baseUrl;
        }
        const params = sections.map(s => `sections=${encodeURIComponent(s)}`).join('&');
        return `${baseUrl}?${params}`;
    }

    function showLoadingState() {
        generateButton.disabled = true;
        generateButton.innerHTML = '<span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>Gerando...';

        if (resultsContainer) {
            resultsContainer.style.display = 'none';
        }
        if (emptyState) {
            emptyState.style.display = 'none';
        }
        if (errorAlert) {
            errorAlert.style.display = 'none';
        }
    }

    function hideLoadingState() {
        generateButton.disabled = false;
        generateButton.innerHTML = '<i class="bi bi-file-earmark-bar-graph"></i><span>Gerar Relatório</span>';
    }

    function showError(message) {
        if (errorAlert) {
            errorAlert.querySelector('span').textContent = message;
            errorAlert.style.display = 'block';
        } else {
            // Criar alerta se não existir
            const alert = document.createElement('div');
            alert.className = 'alert alert-warning';
            alert.role = 'alert';
            alert.innerHTML = `<i class="bi bi-exclamation-triangle-fill"></i><span>${message}</span>`;
            form.parentNode.insertBefore(alert, resultsContainer || emptyState);
        }

        if (resultsContainer) {
            resultsContainer.style.display = 'none';
        }
        if (emptyState) {
            emptyState.style.display = 'none';
        }
    }

    function renderReport(data, selectedSections) {
        // Verificar se há dados
        if (!data.hasPartners) {
            showError(data.message || 'Não há associados cadastrados para gerar o relatório.');
            return;
        }

        // Limpar container de resultados ou criar se não existir
        let container = resultsContainer;
        if (!container) {
            container = createResultsContainer();
        }

        container.innerHTML = '';

        // Renderizar cabeçalho
        container.appendChild(createResultsHeader());

        // Renderizar seções
        if (selectedSections.includes('summary') && data.summary) {
            container.appendChild(createSummarySection(data.summary));
        }

        if (selectedSections.includes('topCities') && data.topCities) {
            container.appendChild(createTopCitiesSection(data.topCities));
        }

        if (selectedSections.includes('generalIndicators') && data.generalIndicators) {
            container.appendChild(createGeneralIndicatorsSection(data.generalIndicators));
        }

        if (selectedSections.includes('partnerList') && data.partners) {
            container.appendChild(createPartnerListSection(data.partners));
        }

        // Mostrar resultados
        container.style.display = 'block';

        if (emptyState) {
            emptyState.style.display = 'none';
        }
        if (errorAlert) {
            errorAlert.style.display = 'none';
        }

        // Scroll suave até os resultados
        container.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }

    function createResultsContainer() {
        const container = document.createElement('div');
        container.className = 'report-card report-results';
        form.parentNode.insertBefore(container, emptyState);
        return container;
    }

    function createResultsHeader() {
        const header = document.createElement('div');
        header.className = 'd-flex flex-column flex-lg-row align-items-lg-center justify-content-between gap-3';
        header.innerHTML = `
            <div>
                <h2 class="h4 mb-1">Resultados do Relatório</h2>
                <p class="text-muted mb-0">Resumo das informações conforme as seções marcadas acima.</p>
            </div>
            <div class="d-flex gap-2">
                <a href="/reports/partners/excel" class="btn btn-success btn-sm">
                    <i class="bi bi-file-earmark-excel"></i>
                    <span>Excel Completo</span>
                </a>
                <a href="/reports/partners/excel/summary" class="btn btn-success btn-sm">
                    <i class="bi bi-file-earmark-excel"></i>
                    <span>Excel Resumido</span>
                </a>
                <button type="button" onclick="window.print()" class="btn btn-outline-secondary btn-sm">
                    <i class="bi bi-printer"></i>
                    <span>Imprimir</span>
                </button>
            </div>
        `;
        return header;
    }

    function createSummarySection(summary) {
        const section = document.createElement('div');
        section.className = 'report-section';
        section.innerHTML = `
            <h3 class="h5 mb-3 d-flex align-items-center gap-2">
                <i class="bi bi-clipboard-data"></i>
                <span>Resumo Geral</span>
            </h3>
            <div class="row g-3">
                <div class="col-sm-6 col-lg-3">
                    <div class="stat-card h-100">
                        <div class="stat-label">Total de Associados</div>
                        <div class="stat-value">${summary.totalPartners}</div>
                    </div>
                </div>
                <div class="col-sm-6 col-lg-3">
                    <div class="stat-card h-100">
                        <div class="stat-label">Com Veículos</div>
                        <div class="stat-value">${summary.partnersWithVehicles}</div>
                    </div>
                </div>
                <div class="col-sm-6 col-lg-3">
                    <div class="stat-card h-100">
                        <div class="stat-label">Sem Veículos</div>
                        <div class="stat-value">${summary.partnersWithoutVehicles}</div>
                    </div>
                </div>
                <div class="col-sm-6 col-lg-3">
                    <div class="stat-card h-100">
                        <div class="stat-label">Média por Associado</div>
                        <div class="stat-value">${summary.averageVehiclesPerPartner}</div>
                    </div>
                </div>
            </div>
        `;
        return section;
    }

    function createTopCitiesSection(topCities) {
        const section = document.createElement('div');
        section.className = 'report-section';

        let itemsHtml = '';
        if (topCities.length === 0) {
            itemsHtml = '<div class="text-muted text-center pt-3">Nenhum dado disponível para os filtros selecionados.</div>';
        } else {
            itemsHtml = topCities.map(item => `
                <div class="distribution-item">
                    <span>${escapeHtml(item.label)}</span>
                    <strong>${item.value}</strong>
                </div>
            `).join('');
        }

        section.innerHTML = `
            <h3 class="h5 mb-3 d-flex align-items-center gap-2">
                <i class="bi bi-geo-alt"></i>
                <span>Principais Cidades</span>
            </h3>
            ${itemsHtml}
        `;
        return section;
    }

    function createGeneralIndicatorsSection(indicators) {
        const section = document.createElement('div');
        section.className = 'report-section';
        section.innerHTML = `
            <h3 class="h5 mb-3 d-flex align-items-center gap-2">
                <i class="bi bi-graph-up"></i>
                <span>Indicadores Gerais</span>
            </h3>
            <div class="row g-3">
                <div class="col-md-6">
                    <div class="stat-card h-100">
                        <div class="stat-label">Total de Veículos</div>
                        <div class="stat-value">${indicators.totalVehicles}</div>
                    </div>
                </div>
                <div class="col-md-6">
                    <div class="stat-card h-100">
                        <div class="stat-label">Taxa de Ativação</div>
                        <div class="stat-value">${indicators.activePercentage}</div>
                    </div>
                </div>
            </div>
        `;
        return section;
    }

    function createPartnerListSection(partners) {
        const section = document.createElement('div');
        section.className = 'report-section';

        let tableContent = '';
        if (partners.length === 0) {
            tableContent = '<div class="text-muted text-center pt-3">Nenhum associado encontrado para os filtros aplicados.</div>';
        } else {
            const rows = partners.map(partner => `
                <tr>
                    <td>${partner.id}</td>
                    <td>${escapeHtml(partner.name) || '-'}</td>
                    <td>${escapeHtml(partner.email) || '-'}</td>
                    <td>${escapeHtml(partner.cpf) || '-'}</td>
                    <td>${escapeHtml(partner.city) || '-'}</td>
                    <td>${escapeHtml(partner.status) || '-'}</td>
                    <td>${partner.vehicleCount}</td>
                    <td>${escapeHtml(partner.registrationDate) || '-'}</td>
                </tr>
            `).join('');

            tableContent = `
                <div class="report-table-wrapper">
                    <table class="table table-striped table-hover mb-0">
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>Nome</th>
                                <th>Email</th>
                                <th>CPF</th>
                                <th>Cidade</th>
                                <th>Status</th>
                                <th>Veículos</th>
                                <th>Cadastro</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${rows}
                        </tbody>
                    </table>
                </div>
            `;
        }

        section.innerHTML = `
            <h3 class="h5 mb-3 d-flex align-items-center gap-2">
                <i class="bi bi-list-ul"></i>
                <span>Listagem Completa de Associados</span>
            </h3>
            ${tableContent}
        `;
        return section;
    }

    function escapeHtml(text) {
        if (text === null || text === undefined) {
            return '';
        }
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
});
