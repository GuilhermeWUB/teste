(function () {
    console.log('[PROCESSOS-KANBAN] Script carregado');
    const API_BASE = '/juridico/api/processos';

    const STATUS_ORDER = [
        'EM_ABERTO_7_0',
        'EM_CONTATO_7_1',
        'PROCESSO_JUDICIAL_7_2',
        'ACORDO_ASSINADO_7_3'
    ];
    const PRIORIDADES = ['ALTA', 'MEDIA', 'BAIXA'];

    const statusLabels = {
        EM_ABERTO_7_0: 'Em Aberto 7.0',
        EM_CONTATO_7_1: 'Em Contato 7.1',
        PROCESSO_JUDICIAL_7_2: 'Processo Judicial 7.2',
        ACORDO_ASSINADO_7_3: 'Acordo Assinado 7.3'
    };

    const statusIcons = {
        EM_ABERTO_7_0: 'bi-flag',
        EM_CONTATO_7_1: 'bi-telephone-outbound',
        PROCESSO_JUDICIAL_7_2: 'bi-bank',
        ACORDO_ASSINADO_7_3: 'bi-pen'
    };

    const priorityLabels = {
        ALTA: 'Alta',
        MEDIA: 'Média',
        BAIXA: 'Baixa'
    };

    const originLabels = {
        JUDICIAL: 'Judicial',
        EXTRAJUDICIAL: 'Extrajudicial'
    };

    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

    function buildHeaders(extra = {}) {
        const headers = {
            'X-Requested-With': 'XMLHttpRequest',
            ...extra
        };

        if (csrfToken && csrfHeader) {
            headers[csrfHeader] = csrfToken;
        }

        return headers;
    }

    const state = {
        cards: [],
        search: '',
        filters: {
            prioridade: [...PRIORIDADES],
            status: [...STATUS_ORDER],
            origem: 'TODAS',
            responsavel: '',
            dateFrom: null,
            dateTo: null
        },
        pendingCreateStatus: STATUS_ORDER[0]
    };

    const dragState = {
        cardId: null,
        originStatus: null
    };

    const selectors = {
        board: () => document.querySelector('.kanban-board'),
        columns: () => Array.from(document.querySelectorAll('.kanban-column')),
        searchInput: () => document.getElementById('kanban-search'),
        btnAdvancedFilters: () => document.getElementById('btn-advanced-filters'),
        btnClearFilters: () => document.getElementById('btn-clear-filters'),
        filtersModal: () => document.getElementById('filters-modal'),
        modal: () => document.getElementById('kanban-modal'),
        modalBody: () => document.querySelector('#kanban-modal .kanban-modal-body'),
        createModal: () => document.getElementById('create-processo-modal'),
        createForm: () => document.getElementById('create-processo-form'),
        createError: () => document.getElementById('create-processo-error'),
        createSubmit: () => document.querySelector('#create-processo-form button[type="submit"]'),
        filterResponsavel: () => document.getElementById('filter-responsavel'),
        filterDateFrom: () => document.getElementById('filter-date-from'),
        filterDateTo: () => document.getElementById('filter-date-to'),
        filterCount: () => document.querySelector('#btn-advanced-filters .filter-count')
    };

    const currencyFormatter = new Intl.NumberFormat('pt-BR', {
        style: 'currency',
        currency: 'BRL'
    });

    const dateFormatter = new Intl.DateTimeFormat('pt-BR', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric'
    });

    const dateTimeFormatter = new Intl.DateTimeFormat('pt-BR', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });

    function init() {
        console.log('[PROCESSOS-KANBAN] init() chamado');
        const board = selectors.board();
        console.log('[PROCESSOS-KANBAN] Board encontrado:', board);

        if (!board) {
            console.error('[PROCESSOS-KANBAN] Board não encontrado! Cancelando inicialização.');
            return;
        }

        bindEvents();
        loadBoard();
    }

    function bindEvents() {
        const search = selectors.searchInput();
        if (search) {
            search.addEventListener('input', () => {
                state.search = (search.value || '').toLowerCase();
                render();
            });
        }

        const btnFilters = selectors.btnAdvancedFilters();
        if (btnFilters) {
            btnFilters.addEventListener('click', openFiltersModal);
        }

        const btnClearFilters = selectors.btnClearFilters();
        if (btnClearFilters) {
            btnClearFilters.addEventListener('click', clearAllFilters);
        }

        const filtersModal = selectors.filtersModal();
        if (filtersModal) {
            filtersModal.addEventListener('click', event => {
                if (event.target === filtersModal) {
                    closeFiltersModal();
                }
            });
        }

        const detailsModal = selectors.modal();
        if (detailsModal) {
            detailsModal.addEventListener('click', event => {
                if (event.target === detailsModal) {
                    closeModal();
                }
            });
        }

        const createModal = selectors.createModal();
        if (createModal) {
            createModal.addEventListener('click', event => {
                if (event.target === createModal) {
                    closeCreateModal();
                }
            });
        }

        const createForm = selectors.createForm();
        if (createForm) {
            createForm.addEventListener('submit', handleCreateSubmit);
        }

        const filterResponsavel = selectors.filterResponsavel();
        if (filterResponsavel) {
            filterResponsavel.addEventListener('input', event => {
                state.filters.responsavel = (event.target.value || '').trim();
                updateFilterBadge();
            });
        }

        const dateFrom = selectors.filterDateFrom();
        if (dateFrom) {
            dateFrom.addEventListener('change', event => {
                state.filters.dateFrom = parseDate(event.target.value);
                updateFilterBadge();
            });
        }

        const dateTo = selectors.filterDateTo();
        if (dateTo) {
            dateTo.addEventListener('change', event => {
                state.filters.dateTo = parseDate(event.target.value, true);
                updateFilterBadge();
            });
        }

        document.querySelectorAll('input[name="prioridade"]').forEach(input => {
            input.addEventListener('change', () => {
                const checked = Array.from(document.querySelectorAll('input[name="prioridade"]:checked'))
                    .map(el => el.value);
                state.filters.prioridade = checked;
                updateFilterBadge();
            });
        });

        document.querySelectorAll('input[name="status"]').forEach(input => {
            input.addEventListener('change', () => {
                const checked = Array.from(document.querySelectorAll('input[name="status"]:checked'))
                    .map(el => el.value);
                state.filters.status = checked;
                updateFilterBadge();
            });
        });

        document.querySelectorAll('input[name="origem"]').forEach(input => {
            input.addEventListener('change', event => {
                if (event.target.checked) {
                    state.filters.origem = event.target.value || 'TODAS';
                    updateFilterBadge();
                }
            });
        });

        document.addEventListener('keydown', event => {
            if (event.key !== 'Escape') {
                return;
            }
            if (selectors.filtersModal()?.classList.contains('active')) {
                closeFiltersModal();
                return;
            }
            if (selectors.modal()?.classList.contains('active')) {
                closeModal();
                return;
            }
            if (selectors.createModal()?.classList.contains('active')) {
                closeCreateModal();
            }
        });
    }

    async function loadBoard() {
        try {
            const response = await fetch(API_BASE, {
                headers: buildHeaders({ 'Accept': 'application/json' })
            });

            if (!response.ok) {
                throw new Error('Falha ao carregar dados remotos');
            }

            const payload = await response.json();
            state.cards = normalizePayload(payload);
        } catch (error) {
            console.error('[PROCESSOS-KANBAN] Falha ao carregar processos:', error);
            state.cards = [];
        }

        render();
        updateFilterBadge();
    }

    function normalizePayload(payload) {
        if (!payload) {
            return [];
        }

        let rawCards = [];

        if (Array.isArray(payload)) {
            rawCards = payload;
        } else if (Array.isArray(payload.cards)) {
            rawCards = payload.cards;
        } else if (Array.isArray(payload.content)) {
            rawCards = payload.content;
        } else if (payload.processos) {
            rawCards = Object.values(payload.processos).flat();
        }

        return rawCards.map(item => normalizeCard(item)).filter(Boolean);
    }

    function normalizeCard(raw) {
        if (!raw) {
            return null;
        }

        const numeroProcesso = (raw.numeroProcesso || raw.referencia || raw.processo || '').toString().trim();
        const autor = (raw.autor || raw.responsavel || raw.devedor || '').toString().trim();
        const reu = (raw.reu || raw.parteContraria || raw.cliente || '').toString().trim();
        const materia = (raw.materia || raw.assunto || '').toString().trim();
        const pedidos = (raw.pedidos || raw.detalhes || raw.observacoes || raw.description || '').toString().trim();
        const status = (raw.status || '').toString().toUpperCase();
        const prioridade = (raw.prioridade || '').toString().toUpperCase();
        const origem = (raw.origem || raw.tipoProcesso || raw.tipoProcessos || 'JUDICIAL').toString().toUpperCase();
        const valor = parseAmount(raw.valorCausa ?? raw.valor ?? raw.montante ?? raw.amount);

        const card = {
            id: raw.id || raw.codigo || cryptoRandomId(),
            referencia: numeroProcesso || 'Processo sem número',
            numeroProcesso: numeroProcesso || '—',
            autor: autor || 'Não informado',
            reu: reu || 'Não informado',
            materia: materia || 'Matéria não informada',
            pedidos: pedidos || '—',
            valor,
            valorCausa: valor,
            status: STATUS_ORDER.includes(status) ? status : 'EM_ABERTO_7_0',
            prioridade: PRIORIDADES.includes(prioridade) ? prioridade : 'MEDIA',
            responsavel: autor || '',
            origem: origem === 'EXTRAJUDICIAL' ? 'EXTRAJUDICIAL' : 'JUDICIAL',
            prazo: null,
            ultimaAtualizacao: normalizeDateTime(raw.ultimaAtualizacao || raw.atualizacao || raw.updatedAt),
            detalhes: pedidos || '',
            contato: raw.contato || raw.telefone || raw.phone || '',
            email: raw.email || raw.contatoEmail || '',
            documento: numeroProcesso || ''
        };

        if (!card.ultimaAtualizacao) {
            card.ultimaAtualizacao = new Date().toISOString();
        }

        return card;
    }

    function parseAmount(value) {
        if (value === null || value === undefined || value === '') {
            return null;
        }
        const number = typeof value === 'number' ? value : parseFloat(String(value).replace(/[^0-9,-]/g, '').replace(',', '.'));
        return Number.isFinite(number) ? number : null;
    }

    function normalizeDate(value) {
        if (!value) {
            return null;
        }
        const date = new Date(value);
        if (Number.isNaN(date.getTime())) {
            return null;
        }
        return date.toISOString().slice(0, 10);
    }

    function normalizeDateTime(value) {
        if (!value) {
            return null;
        }
        const date = new Date(value);
        if (Number.isNaN(date.getTime())) {
            return null;
        }
        return date.toISOString();
    }

    function render() {
        const columns = selectors.columns();
        if (!columns.length) {
            return;
        }

        const filteredCards = applyFilters(state.cards);
        const grouped = groupByStatus(filteredCards);

        columns.forEach(column => {
            const status = column.dataset.status;
            const container = column.querySelector('.tasks-container');
            const counter = column.querySelector('.column-count');

            const items = grouped[status] || [];
            if (counter) {
                counter.textContent = items.length;
            }

            if (!container) {
                return;
            }

            bindColumnDropZone(column);
            container.innerHTML = '';

            if (!items.length) {
                container.appendChild(createEmptyState());
                return;
            }

            items.forEach(card => container.appendChild(createCard(card)));
        });

        enableDragAndDrop();
    }

    function applyFilters(cards) {
        const term = state.search;
        const filters = state.filters;

        return cards.filter(card => {
            if (!filters.prioridade.length || !filters.prioridade.includes(card.prioridade)) {
                return false;
            }

            if (!filters.status.length || !filters.status.includes(card.status)) {
                return false;
            }

            if (filters.origem !== 'TODAS' && card.origem !== filters.origem) {
                return false;
            }

            if (filters.responsavel) {
                const responsavel = (card.responsavel || '').toLowerCase();
                if (!responsavel.includes(filters.responsavel.toLowerCase())) {
                    return false;
                }
            }

            if (filters.dateFrom && card.ultimaAtualizacao) {
                const updated = new Date(card.ultimaAtualizacao);
                if (updated < filters.dateFrom) {
                    return false;
                }
            }

            if (filters.dateTo && card.ultimaAtualizacao) {
                const updated = new Date(card.ultimaAtualizacao);
                if (updated > filters.dateTo) {
                    return false;
                }
            }

            if (!term) {
                return true;
            }

            const haystack = [
                card.referencia,
                card.numeroProcesso,
                card.autor,
                card.reu,
                card.materia,
                card.pedidos,
                card.responsavel,
                card.detalhes,
                originLabels[card.origem]
            ].filter(Boolean).join(' ').toLowerCase();

            return haystack.includes(term);
        });
    }

    function groupByStatus(cards) {
        return cards.reduce((acc, card) => {
            const status = card.status || 'NOVO';
            acc[status] = acc[status] || [];
            acc[status].push(card);
            return acc;
        }, {});
    }

    function createCard(card) {
        const article = document.createElement('article');
        article.className = 'task-card processos-card';
        article.dataset.id = card.id;
        if (card.origem) {
            article.dataset.origem = card.origem;
        }

        const header = document.createElement('div');
        header.className = 'task-card-header';

        const title = document.createElement('h4');
        title.innerHTML = `<i class="bi ${statusIcons[card.status] || 'bi-kanban'}"></i> ${escapeHtml(card.numeroProcesso)}`;
        header.appendChild(title);

        const priority = document.createElement('span');
        priority.className = `badge priority-${card.prioridade}`;
        priority.textContent = priorityLabels[card.prioridade] || card.prioridade;
        header.appendChild(priority);

        article.appendChild(header);

        const body = document.createElement('div');
        body.className = 'task-card-body';

        const author = document.createElement('p');
        author.className = 'processos-card-row';
        author.innerHTML = `<span class="label"><i class="bi bi-person-badge"></i> Autor</span><span class="value">${escapeHtml(card.autor)}</span>`;
        body.appendChild(author);

        const defendant = document.createElement('p');
        defendant.className = 'processos-card-row';
        defendant.innerHTML = `<span class="label"><i class="bi bi-building"></i> Réu</span><span class="value">${escapeHtml(card.reu)}</span>`;
        body.appendChild(defendant);

        const subject = document.createElement('p');
        subject.className = 'processos-card-row';
        subject.innerHTML = `<span class="label"><i class="bi bi-journal-text"></i> Matéria</span><span class="value">${escapeHtml(card.materia)}</span>`;
        body.appendChild(subject);

        if (card.valorCausa !== null && card.valorCausa !== undefined) {
            const amount = document.createElement('p');
            amount.className = 'processos-card-row';
            amount.innerHTML = `<span class="label"><i class="bi bi-cash-coin"></i> Valor da causa</span><span class="value">${formatCurrency(card.valorCausa)}</span>`;
            body.appendChild(amount);
        }

        if (card.pedidos && card.pedidos !== '—') {
            const pedidos = document.createElement('p');
            pedidos.className = 'processos-card-row processos-card-pedidos';
            pedidos.innerHTML = `<span class="label"><i class="bi bi-list-check"></i> Pedidos</span><span class="value">${escapeHtml(card.pedidos)}</span>`;
            body.appendChild(pedidos);
        }

        article.appendChild(body);

        const footer = document.createElement('div');
        footer.className = 'task-card-footer';

        const meta = document.createElement('div');
        meta.className = 'task-card-meta';

        const origin = document.createElement('span');
        origin.className = 'badge badge-origin';
        if (card.origem) {
            origin.dataset.origin = card.origem;
        }
        origin.textContent = originLabels[card.origem] || 'Origem não informada';
        meta.appendChild(origin);

        if (card.responsavel) {
            const owner = document.createElement('span');
            owner.className = 'date';
            owner.innerHTML = `<i class="bi bi-person-check"></i> ${escapeHtml(card.responsavel)}`;
            meta.appendChild(owner);
        }

        if (card.ultimaAtualizacao) {
            const updated = document.createElement('span');
            updated.className = 'date';
            updated.innerHTML = `<i class="bi bi-clock"></i> ${formatDateTime(card.ultimaAtualizacao)}`;
            meta.appendChild(updated);
        }

        footer.appendChild(meta);
        article.appendChild(footer);

        article.addEventListener('click', () => openModal(card));

        return article;
    }

    function createEmptyState() {
        const wrapper = document.createElement('div');
        wrapper.className = 'empty-state';
        wrapper.innerHTML = '<i class="bi bi-inbox"></i> Nenhum processo aqui ainda';
        return wrapper;
    }

    function enableDragAndDrop() {
        document.querySelectorAll('.task-card').forEach(card => {
            card.setAttribute('draggable', 'true');
            card.addEventListener('dragstart', handleDragStart);
            card.addEventListener('dragend', handleDragEnd);
        });
    }

    function bindColumnDropZone(column) {
        const container = column.querySelector('.tasks-container');
        if (!container || container.dataset.dndBound) {
            return;
        }

        container.addEventListener('dragover', handleDragOver);
        container.addEventListener('drop', handleDrop);
        container.dataset.dndBound = 'true';
    }

    function handleDragStart(event) {
        const card = event.currentTarget;
        dragState.cardId = card.dataset.id || null;
        dragState.originStatus = card.closest('.kanban-column')?.dataset.status || null;

        if (event.dataTransfer) {
            event.dataTransfer.effectAllowed = 'move';
            event.dataTransfer.setData('text/plain', dragState.cardId || '');
        }

        card.classList.add('is-dragging');
    }

    function handleDragEnd(event) {
        event.currentTarget.classList.remove('is-dragging');
        dragState.cardId = null;
        dragState.originStatus = null;
    }

    function handleDragOver(event) {
        event.preventDefault();
        if (event.dataTransfer) {
            event.dataTransfer.dropEffect = 'move';
        }
    }

    async function handleDrop(event) {
        event.preventDefault();
        const container = event.currentTarget;
        const column = container.closest('.kanban-column');
        const targetStatus = column?.dataset.status;
        const cardId = dragState.cardId || event.dataTransfer?.getData('text/plain');
        const normalizedCardId = cardId ? String(cardId).trim() : '';

        if (!normalizedCardId || !targetStatus) {
            return;
        }

        const card = state.cards.find(item => String(item.id) === normalizedCardId);
        if (!card) {
            return;
        }

        const previousStatus = card.status || dragState.originStatus;
        if (previousStatus === targetStatus) {
            return;
        }

        // Atualização otimista da UI
        card.status = targetStatus;
        card.ultimaAtualizacao = new Date().toISOString();
        render();

        // Persiste no backend
        try {
            const response = await persistCardStatusChange(normalizedCardId, targetStatus);
            if (response && response.process) {
                // Sincroniza dados do backend
                Object.assign(card, normalizeCard(response.process));
                render();
            }
        } catch (error) {
            console.error('[KANBAN] Falha ao atualizar status:', error);
            // Reverte em caso de erro
            card.status = previousStatus;
            render();
            alert('Não foi possível atualizar o status do processo. Tente novamente.');
        } finally {
            dragState.cardId = null;
            dragState.originStatus = null;
        }
    }

    async function persistCardStatusChange(cardId, newStatus) {
        const response = await fetch(`/juridico/api/processos/${cardId}/status`, {
            method: 'PUT',
            headers: buildHeaders({
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            }),
            body: JSON.stringify({ status: newStatus })
        });

        if (!response.ok) {
            const message = await response.text();
            throw new Error(message || 'Erro ao atualizar status');
        }

        try {
            return await response.json();
        } catch (error) {
            return null;
        }
    }

    function openModal(card) {
        const modal = selectors.modal();
        const modalBody = selectors.modalBody();
        if (!modal || !modalBody) {
            return;
        }

        modalBody.innerHTML = buildModalContent(card);
        modal.classList.add('active');
        document.body.classList.add('kanban-modal-open');
    }

    function buildModalContent(card) {
        const details = [
            { label: 'Número do processo', value: card.numeroProcesso },
            { label: 'Autor', value: card.autor },
            { label: 'Réu', value: card.reu },
            { label: 'Matéria', value: card.materia },
            { label: 'Status', value: statusLabels[card.status] || card.status },
            { label: 'Prioridade', value: priorityLabels[card.prioridade] || card.prioridade },
            { label: 'Valor da causa', value: card.valorCausa !== null && card.valorCausa !== undefined ? formatCurrency(card.valorCausa) : '—' },
            { label: 'Origem', value: originLabels[card.origem] || '—' },
            { label: 'Atualizado em', value: card.ultimaAtualizacao ? formatDateTime(card.ultimaAtualizacao) : '—' }
        ];

        const list = details.map(item => `
            <div class="detail-row">
                <span class="detail-label">${escapeHtml(item.label)}</span>
                <span class="detail-value">${escapeHtml(item.value)}</span>
            </div>
        `).join('');

        const notes = card.pedidos && card.pedidos !== '—' ? `
            <div class="detail-notes">
                <h3>Pedidos</h3>
                <p>${escapeHtml(card.pedidos)}</p>
            </div>
        ` : '';

        return `
            <article class="processos-details">
                <header class="details-header">
                    <span class="status-badge status-${card.status}">${escapeHtml(statusLabels[card.status] || card.status)}</span>
                    <h2>${escapeHtml(card.numeroProcesso)}</h2>
                    <p class="subtitle">${escapeHtml(`${card.autor} vs ${card.reu}`)}</p>
                </header>
                <section class="details-grid">${list}</section>
                ${notes}
            </article>
        `;
    }

    function closeModal() {
        const modal = selectors.modal();
        if (!modal) {
            return;
        }
        modal.classList.remove('active');
        document.body.classList.remove('kanban-modal-open');
    }

    function openFiltersModal() {
        selectors.filtersModal()?.classList.add('active');
        document.body.classList.add('kanban-modal-open');
    }

    function closeFiltersModal() {
        selectors.filtersModal()?.classList.remove('active');
        document.body.classList.remove('kanban-modal-open');
    }

    function openCreateModal(status = STATUS_ORDER[0]) {
        console.log('[PROCESSOS-KANBAN] openCreateModal chamado com status:', status);
        const modal = selectors.createModal();
        const form = selectors.createForm();
        const statusSummary = document.getElementById('create-processo-target');
        const normalizedStatus = STATUS_ORDER.includes(status) ? status : STATUS_ORDER[0];
        state.pendingCreateStatus = normalizedStatus;

        console.log('[PROCESSOS-KANBAN] Modal encontrado:', modal);
        console.log('[PROCESSOS-KANBAN] Form encontrado:', form);

        if (!modal) {
            console.error('[PROCESSOS-KANBAN] Modal não encontrado!');
            return;
        }

        showCreateError('');
        setCreateLoading(false);

        if (form) {
            form.reset();
            delete form.dataset.submitting;
            form.dataset.status = normalizedStatus;

            const focusable = form.querySelector('input, textarea');
            if (focusable) {
                requestAnimationFrame(() => focusable.focus());
            }
        }

        if (statusSummary) {
            const label = statusLabels[normalizedStatus] || normalizedStatus;
            const value = statusSummary.querySelector('.value');
            if (value) {
                value.textContent = label;
            }
            statusSummary.classList.add('active');
        }

        modal.classList.add('active');
        document.body.classList.add('kanban-modal-open');
    }

    function closeCreateModal() {
        const modal = selectors.createModal();
        const form = selectors.createForm();
        if (modal) {
            modal.classList.remove('active');
        }
        if (form) {
            form.reset();
            delete form.dataset.submitting;
            delete form.dataset.status;
        }
        const statusSummary = document.getElementById('create-processo-target');
        if (statusSummary) {
            statusSummary.classList.remove('active');
            const value = statusSummary.querySelector('.value');
            if (value) {
                value.textContent = '';
            }
        }
        state.pendingCreateStatus = STATUS_ORDER[0];
        showCreateError('');
        setCreateLoading(false);
        document.body.classList.remove('kanban-modal-open');
    }

    function setCreateLoading(isLoading) {
        const submit = selectors.createSubmit();
        if (!submit) {
            return;
        }

        submit.disabled = Boolean(isLoading);
        submit.classList.toggle('is-loading', Boolean(isLoading));
    }

    function showCreateError(message) {
        const error = selectors.createError();
        if (!error) {
            return;
        }

        if (!message) {
            error.textContent = '';
            error.classList.remove('active');
            return;
        }

        error.textContent = message;
        error.classList.add('active');
    }

    async function handleCreateSubmit(event) {
        event.preventDefault();
        const form = event.currentTarget;
        if (!form || form.dataset.submitting === 'true') {
            return;
        }

        showCreateError('');
        form.dataset.submitting = 'true';
        setCreateLoading(true);

        try {
            const data = new FormData(form);

            // Validação dos campos obrigatórios
            const autor = (data.get('autor') || '').toString().trim();
            const reu = (data.get('reu') || '').toString().trim();
            const materia = (data.get('materia') || '').toString().trim();
            const numeroProcesso = (data.get('numeroProcesso') || '').toString().trim();
            const pedidos = (data.get('pedidos') || '').toString().trim();

            if (!autor || !reu || !materia || !numeroProcesso || !pedidos) {
                showCreateError('Todos os campos são obrigatórios.');
                return;
            }

            const valorString = (data.get('valorCausa') || '').toString().replace(',', '.');
            const valorCausa = Number.parseFloat(valorString);

            if (!Number.isFinite(valorCausa) || valorCausa < 0) {
                showCreateError('Informe um valor da causa válido.');
                return;
            }

            const payload = {
                autor: autor,
                reu: reu,
                materia: materia,
                numeroProcesso: numeroProcesso,
                valorCausa: valorCausa,
                pedidos: pedidos
            };

            console.log('[PROCESSOS-KANBAN] Criando processo com payload:', payload);

            const response = await fetch(API_BASE, {
                method: 'POST',
                headers: buildHeaders({
                    'Content-Type': 'application/json',
                    'Accept': 'application/json'
                }),
                body: JSON.stringify(payload)
            });

            console.log('[PROCESSOS-KANBAN] Response status:', response.status);

            if (!response.ok) {
                let message = 'Não foi possível registrar o processo.';
                try {
                    const errorBody = await response.json();
                    console.error('[PROCESSOS-KANBAN] Erro do servidor:', errorBody);
                    if (errorBody?.message) {
                        message = errorBody.message;
                    } else if (errorBody?.error) {
                        message = errorBody.error;
                    }
                } catch (parseError) {
                    const textError = await response.text();
                    console.error('[PROCESSOS-KANBAN] Erro como texto:', textError);
                }
                showCreateError(message);
                return;
            }

            const created = await response.json();
            console.log('[PROCESSOS-KANBAN] Processo criado com sucesso:', created);

            const card = normalizeCard(created);
            if (card) {
                const targetStatus = (() => {
                    if (form?.dataset?.status && STATUS_ORDER.includes(form.dataset.status)) {
                        return form.dataset.status;
                    }
                    if (state.pendingCreateStatus && STATUS_ORDER.includes(state.pendingCreateStatus)) {
                        return state.pendingCreateStatus;
                    }
                    return null;
                })();

                if (targetStatus) {
                    card.status = targetStatus;
                }
                state.cards.unshift(card);
                render();
            }

            closeCreateModal();
        } catch (error) {
            console.error('[PROCESSOS-KANBAN] Erro ao registrar processo:', error);
            showCreateError('Ocorreu um erro ao registrar o processo. Tente novamente: ' + error.message);
        } finally {
            setCreateLoading(false);
            form.dataset.submitting = 'false';
        }
    }

    function applyFilters() {
        updateFilterBadge();
        render();
        closeFiltersModal();
    }

    function resetFilters() {
        state.filters = {
            prioridade: [...PRIORIDADES],
            status: [...STATUS_ORDER],
            origem: 'TODAS',
            responsavel: '',
            dateFrom: null,
            dateTo: null
        };

        document.querySelectorAll('input[name="prioridade"]').forEach(input => {
            input.checked = true;
        });
        document.querySelectorAll('input[name="status"]').forEach(input => {
            input.checked = true;
        });
        const origemTodas = document.querySelector('input[name="origem"][value="TODAS"]');
        if (origemTodas) {
            origemTodas.checked = true;
        }
        const responsavel = selectors.filterResponsavel();
        if (responsavel) {
            responsavel.value = '';
        }
        const dateFrom = selectors.filterDateFrom();
        if (dateFrom) {
            dateFrom.value = '';
        }
        const dateTo = selectors.filterDateTo();
        if (dateTo) {
            dateTo.value = '';
        }

        updateFilterBadge();
        render();
    }

    function clearAllFilters() {
        resetFilters();
        closeFiltersModal();
    }

    function updateFilterBadge() {
        const badge = selectors.filterCount();
        const btnClear = selectors.btnClearFilters();
        if (!badge || !btnClear) {
            return;
        }

        let active = 0;
        if (state.filters.prioridade.length && state.filters.prioridade.length !== PRIORIDADES.length) {
            active += 1;
        }
        if (state.filters.status.length && state.filters.status.length !== STATUS_ORDER.length) {
            active += 1;
        }
        if (state.filters.origem !== 'TODAS') {
            active += 1;
        }
        if (state.filters.responsavel) {
            active += 1;
        }
        if (state.filters.dateFrom) {
            active += 1;
        }
        if (state.filters.dateTo) {
            active += 1;
        }

        if (active > 0) {
            badge.textContent = String(active);
            badge.style.display = 'inline-flex';
            btnClear.style.display = 'inline-flex';
        } else {
            badge.style.display = 'none';
            btnClear.style.display = 'none';
        }
    }

    function formatCurrency(value) {
        return currencyFormatter.format(value || 0);
    }

    function formatDate(value) {
        if (!value) {
            return '—';
        }
        const date = new Date(value);
        return Number.isNaN(date.getTime()) ? '—' : dateFormatter.format(date);
    }

    function formatDateTime(value) {
        if (!value) {
            return '—';
        }
        const date = new Date(value);
        return Number.isNaN(date.getTime()) ? '—' : dateTimeFormatter.format(date);
    }

    function parseDate(value, isEndOfDay = false) {
        if (!value) {
            return null;
        }
        const date = new Date(value);
        if (Number.isNaN(date.getTime())) {
            return null;
        }
        if (isEndOfDay) {
            date.setHours(23, 59, 59, 999);
        }
        return date;
    }

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

    function cryptoRandomId() {
        if (window.crypto?.randomUUID) {
            return window.crypto.randomUUID();
        }
        return `processos-${Math.random().toString(36).slice(2, 10)}`;
    }

    document.addEventListener('DOMContentLoaded', () => {
        console.log('[PROCESSOS-KANBAN] DOMContentLoaded - Inicializando');

        window.processosBoard = {
            applyFilters,
            resetFilters,
            closeFiltersModal,
            openCreateModal,
            closeCreateModal,
            closeModal
        };

        console.log('[PROCESSOS-KANBAN] window.processosBoard criado:', window.processosBoard);

        init();

        console.log('[PROCESSOS-KANBAN] Inicialização completa');
    });
})();
