document.addEventListener('DOMContentLoaded', function() {
    console.log('Script relatorio-associados.js carregado');

    const page = document.querySelector('.report-page');
    if (!page) {
        console.error('Elemento .report-page não encontrado');
        return;
    }
    console.log('Elemento .report-page encontrado:', page);

    const form = page.querySelector('.report-filters');
    if (!form) {
        console.error('Elemento .report-filters não encontrado');
        return;
    }
    console.log('Elemento .report-filters encontrado:', form);

    const overlay = page.querySelector('[data-report-overlay]');
    const overlayDialog = overlay ? overlay.querySelector('.report-overlay__dialog') : null;
    const overlayBody = overlay ? overlay.querySelector('[data-report-overlay-content]') : null;
    const overlaySubtitle = overlay ? overlay.querySelector('[data-overlay-subtitle]') : null;
    const resultsWrapper = page.querySelector('[data-report-results-wrapper]');
    const resultsContainer = resultsWrapper ? resultsWrapper.querySelector('[data-report-results]') : null;
    const resultsTitle = resultsWrapper ? resultsWrapper.querySelector('[data-report-results-title]') : null;
    const resultsSubtitle = resultsWrapper ? resultsWrapper.querySelector('[data-report-results-subtitle]') : null;
    const emptyState = page.querySelector('[data-report-empty]');
    const errorBox = page.querySelector('[data-report-error]');
    const feedback = page.querySelector('[data-report-feedback]');
    const scrollButton = page.querySelector('[data-scroll-to-form]');
    const submitButton = form.querySelector('[data-report-submit]');
    const previewButton = page.querySelector('[data-report-view-details]');
    const clearButton = page.querySelector('[data-report-clear]');

    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
    const FETCH_HEADERS = {
        'X-Requested-With': 'XMLHttpRequest'
    };

    if (csrfToken && csrfHeader) {
        FETCH_HEADERS[csrfHeader] = csrfToken;
    }

    const DATE_TIME_FORMATTER = new Intl.DateTimeFormat('pt-BR', {
        dateStyle: 'short',
        timeStyle: 'short'
    });

    let overlayCloseHandler = null;
    let overlayKeyHandler = null;
    let lastReportData = null;
    let lastGeneratedAt = null;
    let feedbackTimeoutId = null;

    function escapeHtml(value) {
        if (value === null || value === undefined) {
            return '';
        }
        return String(value)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    function toDisplay(value, fallback = '—') {
        if (value === null || value === undefined || value === '') {
            return escapeHtml(fallback);
        }
        return escapeHtml(value);
    }

    function showInlineError(message) {
        if (!errorBox) {
            return;
        }
        errorBox.innerHTML = `<i class="bi bi-exclamation-triangle-fill me-2"></i>${escapeHtml(message)}`;
        errorBox.classList.remove('d-none');
    }

    function hideInlineError() {
        if (!errorBox) {
            return;
        }
        errorBox.classList.add('d-none');
        errorBox.textContent = '';
    }

    function toggleSubmitState(isLoading) {
        if (!submitButton) {
            return;
        }
        submitButton.disabled = Boolean(isLoading);
        submitButton.setAttribute('aria-busy', isLoading ? 'true' : 'false');
    }

    function showFeedback(message, type = 'success') {
        if (!feedback) {
            return;
        }

        feedback.removeAttribute('hidden');
        feedback.textContent = message;
        feedback.classList.remove('is-error', 'is-visible');
        if (type === 'error') {
            feedback.classList.add('is-error');
        }

        void feedback.offsetWidth;
        feedback.classList.add('is-visible');

        if (feedbackTimeoutId) {
            clearTimeout(feedbackTimeoutId);
        }
        feedbackTimeoutId = setTimeout(function () {
            feedback.classList.remove('is-visible');
        }, 4000);
    }

    function openOverlay() {
        if (!overlay) {
            return;
        }
        overlay.removeAttribute('hidden');
        overlay.classList.add('is-visible');

        if (overlayDialog) {
            overlayDialog.setAttribute('aria-busy', 'true');
        }

        if (!overlayCloseHandler) {
            overlayCloseHandler = function (event) {
                if (event.target.closest('[data-overlay-close]')) {
                    event.preventDefault();
                    closeOverlay();
                } else if (event.target === overlay) {
                    closeOverlay();
                }
            };
            overlay.addEventListener('click', overlayCloseHandler);
        }

        if (!overlayKeyHandler) {
            overlayKeyHandler = function (event) {
                if (event.key === 'Escape') {
                    closeOverlay();
                }
            };
            document.addEventListener('keydown', overlayKeyHandler);
        }
    }

    function closeOverlay() {
        if (!overlay) {
            return;
        }

        overlay.classList.remove('is-visible');
        if (overlayDialog) {
            overlayDialog.setAttribute('aria-busy', 'false');
        }

        setTimeout(function () {
            overlay.setAttribute('hidden', 'hidden');
            if (overlayBody) {
                overlayBody.innerHTML = '';
            }
        }, 220);

        if (overlayCloseHandler) {
            overlay.removeEventListener('click', overlayCloseHandler);
            overlayCloseHandler = null;
        }

        if (overlayKeyHandler) {
            document.removeEventListener('keydown', overlayKeyHandler);
            overlayKeyHandler = null;
        }
    }

    function setOverlayContent(content, title, subtitle) {
        if (!overlay) {
            return;
        }
        if (overlayBody) {
            overlayBody.innerHTML = content;
        }
        const titleElement = overlay.querySelector('#partnerReportOverlayTitle');
        if (titleElement && title) {
            titleElement.textContent = title;
        }
        if (overlaySubtitle && subtitle) {
            overlaySubtitle.textContent = subtitle;
        }
    }

    function showLoadingOverlay(message) {
        const content = `
            <div class="report-loading-state">
                <div class="spinner-border text-primary mb-3" role="status" aria-hidden="true"></div>
                <strong class="d-block mb-1">${toDisplay(message || 'Gerando relatório...')}</strong>
                <p class="text-muted mb-0">Por favor, aguarde enquanto processamos os dados.</p>
            </div>
        `;
        setOverlayContent(content, 'Gerando relatório de associados', 'Estamos preparando os dados em tempo real.');
    }

    function buildSummarySection(summary) {
        if (!summary) {
            return '';
        }

        const cards = [
            {
                label: 'Associados cadastrados',
                value: toDisplay(summary.totalPartners ?? 0, '0'),
                icon: 'bi-people-fill',
                accent: 'text-primary',
                description: 'Total de associados ativos no sistema.'
            },
            {
                label: 'Com veículos',
                value: toDisplay(summary.partnersWithVehicles ?? 0, '0'),
                icon: 'bi-car-front-fill',
                accent: 'text-success',
                description: 'Associados com pelo menos um veículo vinculado.'
            },
            {
                label: 'Sem veículos',
                value: toDisplay(summary.partnersWithoutVehicles ?? 0, '0'),
                icon: 'bi-person-dash-fill',
                accent: 'text-warning',
                description: 'Associados sem veículos cadastrados.'
            },
            {
                label: 'Média de veículos',
                value: toDisplay(summary.averageVehiclesPerPartner, '0,00'),
                icon: 'bi-speedometer2',
                accent: 'text-primary',
                description: 'Quantidade média de veículos por associado.'
            }
        ];

        const cardsMarkup = cards.map(function (card) {
            return `
                <div class="col-sm-6 col-lg-3">
                    <div class="card border-0 shadow-sm h-100">
                        <div class="card-body">
                            <span class="text-muted text-uppercase small">${card.label}</span>
                            <div class="d-flex align-items-center justify-content-between mt-3">
                                <h3 class="fw-bold mb-0">${card.value}</h3>
                                <span class="display-6 ${card.accent}"><i class="bi ${card.icon}"></i></span>
                            </div>
                            <p class="text-muted small mb-0 mt-2">${card.description}</p>
                        </div>
                    </div>
                </div>
            `;
        }).join('');

        return `
            <div class="report-section">
                <div class="row g-4">
                    ${cardsMarkup}
                </div>
            </div>
        `;
    }

    function buildTopCitiesSection(items, limit) {
        if (!Array.isArray(items)) {
            return '';
        }
        const hasItems = items.length > 0;
        const sliced = typeof limit === 'number' ? items.slice(0, limit) : items;
        const listItems = sliced.map(function (item) {
            return `
                <li class="list-group-item d-flex justify-content-between align-items-center">
                    <span>${toDisplay(item.label, 'Não informado')}</span>
                    <span class="badge bg-primary rounded-pill">${toDisplay(item.value, '0')}</span>
                </li>
            `;
        }).join('');

        const footerNote = limit && items.length > limit
            ? `<p class="text-muted small mb-0 mt-3">Mostrando as ${limit} principais cidades. Visualize os detalhes para conferir a lista completa.</p>`
            : '';

        return `
            <div class="col-lg-6">
                <div class="card border-0 shadow-sm h-100">
                    <div class="card-header bg-transparent border-0 pb-0">
                        <h5 class="card-title mb-0 d-flex align-items-center gap-2">
                            <i class="bi bi-geo-alt-fill text-primary"></i>
                            <span>Cidades com mais associados</span>
                        </h5>
                    </div>
                    <div class="card-body">
                        ${hasItems ? `<ul class="list-group list-group-flush">${listItems}</ul>${footerNote}` : '<div class="text-muted">Nenhuma informação disponível.</div>'}
                    </div>
                </div>
            </div>
        `;
    }

    function buildIndicatorsSection(indicators) {
        if (!indicators) {
            return '';
        }
        return `
            <div class="col-lg-6">
                <div class="card border-0 shadow-sm h-100">
                    <div class="card-header bg-transparent border-0 pb-0">
                        <h5 class="card-title mb-0 d-flex align-items-center gap-2">
                            <i class="bi bi-graph-up text-primary"></i>
                            <span>Indicadores gerais</span>
                        </h5>
                    </div>
                    <div class="card-body">
                        <div class="row g-3">
                            <div class="col-sm-6">
                                <div class="border rounded-3 p-3 h-100">
                                    <span class="text-muted text-uppercase small">Total de veículos</span>
                                    <h4 class="fw-bold mt-2 mb-1">${toDisplay(indicators.totalVehicles ?? 0, '0')}</h4>
                                    <p class="text-muted small mb-0">Veículos vinculados a todos os associados.</p>
                                </div>
                            </div>
                            <div class="col-sm-6">
                                <div class="border rounded-3 p-3 h-100">
                                    <span class="text-muted text-uppercase small">Percentual ativo</span>
                                    <h4 class="fw-bold mt-2 mb-1">${toDisplay(indicators.activePercentage, '0%')}</h4>
                                    <p class="text-muted small mb-0">Associados com veículos cadastrados.</p>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        `;
    }

    function buildPartnersTable(items, options) {
        const settings = Object.assign({
            limit: null,
            context: 'overlay'
        }, options || {});

        if (!Array.isArray(items) || items.length === 0) {
            return `
                <div class="report-section">
                    <div class="card border-0 shadow-sm">
                        <div class="card-body text-center py-5 text-muted">
                            Nenhum associado encontrado para os filtros selecionados.
                        </div>
                    </div>
                </div>
            `;
        }

        const limit = typeof settings.limit === 'number' ? settings.limit : null;
        const sliced = limit ? items.slice(0, limit) : items;
        const hasMore = limit && items.length > limit;
        const tableClass = settings.context === 'overlay' ? 'table table-hover align-middle mb-0' : 'table align-middle mb-0';

        const rows = sliced.map(function (item) {
            return `
                <tr>
                    <td>
                        <div class="fw-semibold">${toDisplay(item.name, 'Não informado')}</div>
                        <small class="text-muted">${toDisplay(item.addressSummary, 'Endereço não informado')}</small>
                    </td>
                    <td>${toDisplay(item.cpf, '—')}</td>
                    <td>${toDisplay(item.email, '—')}</td>
                    <td>${toDisplay(item.city, '—')}</td>
                    <td>${toDisplay(item.vehicleCount ?? 0, '0')}</td>
                    <td><span class="badge bg-light text-dark">${toDisplay(item.status, 'Não informado')}</span></td>
                    <td>${toDisplay(item.registrationDate, '—')}</td>
                </tr>
            `;
        }).join('');

        const note = hasMore
            ? `<div class="p-3 text-muted small border-top">Exibindo ${limit} de ${items.length} associados. Clique em &ldquo;Ver detalhes&rdquo; para acessar a lista completa.</div>`
            : '';

        return `
            <div class="report-section">
                <div class="card border-0 shadow-sm">
                    <div class="card-header bg-transparent border-0 pb-0">
                        <h5 class="card-title mb-0 d-flex align-items-center gap-2">
                            <i class="bi bi-list-check text-primary"></i>
                            <span>Lista de associados</span>
                        </h5>
                    </div>
                    <div class="card-body p-0">
                        <div class="table-responsive">
                            <table class="${tableClass}">
                                <thead>
                                    <tr>
                                        <th>Associado</th>
                                        <th>CPF</th>
                                        <th>E-mail</th>
                                        <th>Cidade</th>
                                        <th>Veículos</th>
                                        <th>Status</th>
                                        <th>Cadastro</th>
                                    </tr>
                                </thead>
                                <tbody>${rows}</tbody>
                            </table>
                        </div>
                        ${note}
                    </div>
                </div>
            </div>
        `;
    }

    function buildOverlayContent(data) {
        if (!data || data.hasPartners === false) {
            const message = data && data.message ? data.message : 'Não há associados cadastrados para gerar o relatório.';
            return `
                <div class="report-section">
                    <div class="alert alert-warning mb-0 d-flex align-items-start gap-3">
                        <i class="bi bi-exclamation-triangle-fill fs-4"></i>
                        <div>
                            <strong class="d-block mb-1">Nenhum dado disponível</strong>
                            <span>${toDisplay(message)}</span>
                        </div>
                    </div>
                </div>
            `;
        }

        const sections = [];

        if (data.summary) {
            sections.push(buildSummarySection(data.summary));
        }

        if (data.topCities || data.generalIndicators) {
            const insights = [];
            if (data.topCities) {
                insights.push(buildTopCitiesSection(data.topCities));
            }
            if (data.generalIndicators) {
                insights.push(buildIndicatorsSection(data.generalIndicators));
            }
            if (insights.length) {
                sections.push(`<div class="report-section"><div class="row g-4">${insights.join('')}</div></div>`);
            }
        }

        if (data.partners) {
            sections.push(buildPartnersTable(data.partners, {context: 'overlay'}));
        }

        if (!sections.length) {
            sections.push(`
                <div class="alert alert-info mb-0">
                    Nenhuma seção foi selecionada para exibição. Marque pelo menos uma opção e tente novamente.
                </div>
            `);
        }

        return sections.join('');
    }

    function buildPreviewContent(data) {
        if (!data || data.hasPartners === false) {
            return '';
        }

        const previewSections = [];

        if (data.summary) {
            previewSections.push(buildSummarySection(data.summary));
        }

        if (data.topCities || data.generalIndicators) {
            const insights = [];
            if (data.topCities) {
                insights.push(buildTopCitiesSection(data.topCities, 3));
            }
            if (data.generalIndicators) {
                insights.push(buildIndicatorsSection(data.generalIndicators));
            }
            if (insights.length) {
                previewSections.push(`<div class="report-section"><div class="row g-4">${insights.join('')}</div></div>`);
            }
        }

        if (data.partners) {
            previewSections.push(buildPartnersTable(data.partners, {limit: 5, context: 'preview'}));
        }

        previewSections.push('<p class="text-muted small mb-0 mt-3 text-end">Use o botão &ldquo;Ver detalhes&rdquo; para explorar todo o relatório.</p>');

        return previewSections.join('');
    }

    function renderResults(data, generatedAt) {
        if (!resultsContainer || !resultsWrapper) {
            return;
        }

        if (data && data.hasPartners === false) {
            resultsContainer.innerHTML = `
                <div class="alert alert-warning mb-0 d-flex align-items-start gap-3">
                    <i class="bi bi-exclamation-triangle-fill fs-4"></i>
                    <div>
                        <strong class="d-block mb-1">Nenhum dado disponível</strong>
                        <span>${toDisplay(data.message || 'Não há associados cadastrados para gerar o relatório.')}</span>
                    </div>
                </div>
            `;
            resultsWrapper.classList.remove('d-none');
            if (emptyState) {
                emptyState.classList.add('d-none');
            }
            return;
        }

        const preview = buildPreviewContent(data);
        if (!preview) {
            resultsContainer.innerHTML = `
                <div class="alert alert-info mb-0">Nenhuma seção foi selecionada para exibição.</div>
            `;
        } else {
            resultsContainer.innerHTML = preview;
        }

        if (resultsTitle) {
            resultsTitle.textContent = 'Relatório de Associados';
        }
        if (resultsSubtitle && generatedAt) {
            resultsSubtitle.textContent = 'Consulta realizada em ' + DATE_TIME_FORMATTER.format(generatedAt) + '.';
        }

        resultsWrapper.classList.remove('d-none');
        if (emptyState) {
            emptyState.classList.add('d-none');
        }
    }

    function handleSubmit(event) {
        console.log('handleSubmit chamado', event);
        event.preventDefault();

        const selectedSections = form.querySelectorAll('input[name="sections"]:checked');
        console.log('Seções selecionadas:', selectedSections.length);

        if (!selectedSections.length) {
            console.warn('Nenhuma seção selecionada');
            showInlineError('Selecione pelo menos uma seção para gerar o relatório.');
            return;
        }
        hideInlineError();

        toggleSubmitState(true);
        openOverlay();
        showLoadingOverlay('Gerando relatório...');

        const params = new URLSearchParams(new FormData(form));
        if (!params.has('generate')) {
            params.set('generate', 'true');
        }

        const url = '/reports/partners/data?' + params.toString();
        console.log('Fazendo requisição para:', url);

        fetch(url, {
            method: 'GET',
            headers: FETCH_HEADERS
        })
            .then(function (response) {
                console.log('Resposta recebida:', response);
                if (!response.ok) {
                    throw new Error('Erro ao buscar dados do relatório. Status ' + response.status);
                }
                return response.json();
            })
            .then(function (data) {
                console.log('Dados recebidos:', data);
                lastReportData = data;
                lastGeneratedAt = new Date();

                const overlayContent = buildOverlayContent(data);
                const subtitle = data && data.hasPartners === false
                    ? 'Nenhum dado foi encontrado para os filtros selecionados.'
                    : 'Consulta realizada em ' + DATE_TIME_FORMATTER.format(lastGeneratedAt) + '.';

                setOverlayContent(overlayContent, 'Relatório de Associados', subtitle);
                if (overlayDialog) {
                    overlayDialog.setAttribute('aria-busy', 'false');
                }

                renderResults(data, lastGeneratedAt);
                showFeedback('Relatório gerado com sucesso!');
            })
            .catch(function (error) {
                console.error(error);
                setOverlayContent(`
                    <div class="report-section">
                        <div class="alert alert-danger mb-0 d-flex align-items-start gap-3">
                            <i class="bi bi-exclamation-octagon-fill fs-4"></i>
                            <div>
                                <strong class="d-block mb-1">Não foi possível gerar o relatório</strong>
                                <p class="mb-2">Ocorreu um erro ao processar sua solicitação. Por favor, tente novamente.</p>
                                <button type="button" class="btn btn-outline-danger btn-sm" data-overlay-close>Fechar</button>
                            </div>
                        </div>
                    </div>
                `, 'Erro ao gerar relatório', 'Verifique sua conexão e tente novamente.');
                showFeedback('Não foi possível gerar o relatório. Tente novamente.', 'error');
            })
            .finally(function () {
                toggleSubmitState(false);
            });
    }

    function handlePreviewButton() {
        if (!lastReportData) {
            showFeedback('Gere um relatório para visualizar os detalhes.', 'error');
            return;
        }
        openOverlay();
        const subtitle = lastReportData.hasPartners === false
            ? 'Nenhum dado foi encontrado para os filtros selecionados.'
            : 'Consulta realizada em ' + DATE_TIME_FORMATTER.format(lastGeneratedAt || new Date()) + '.';
        setOverlayContent(buildOverlayContent(lastReportData), 'Relatório de Associados', subtitle);
        if (overlayDialog) {
            overlayDialog.setAttribute('aria-busy', 'false');
        }
    }

    function handleClearResults(event) {
        event.preventDefault();
        lastReportData = null;
        lastGeneratedAt = null;
        if (resultsContainer) {
            resultsContainer.innerHTML = '';
        }
        if (resultsWrapper) {
            resultsWrapper.classList.add('d-none');
        }
        if (emptyState) {
            emptyState.classList.remove('d-none');
        }
        if (resultsSubtitle) {
            resultsSubtitle.textContent = 'Consulta dinâmica do relatório.';
        }
        showFeedback('Visualização limpa com sucesso.');
    }

    function handleScrollButton() {
        form.scrollIntoView({behavior: 'smooth', block: 'start'});
    }

    form.addEventListener('submit', handleSubmit);

    if (previewButton) {
        previewButton.addEventListener('click', function (event) {
            event.preventDefault();
            handlePreviewButton();
        });
    }

    if (clearButton) {
        clearButton.addEventListener('click', handleClearResults);
    }

    if (scrollButton) {
        scrollButton.addEventListener('click', handleScrollButton);
    }

    console.log('Event listeners registrados com sucesso');
});
