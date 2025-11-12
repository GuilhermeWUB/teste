(function () {
    const STATUS_ORDER = ['NOVO', 'EM_ANALISE', 'NEGOCIACAO', 'ACORDO_FIRMADO', 'ENCERRADO'];
    const PRIORIDADES = ['ALTA', 'MEDIA', 'BAIXA'];

    const statusLabels = {
        NOVO: 'Novas cobranças',
        EM_ANALISE: 'Em análise',
        NEGOCIACAO: 'Negociação ativa',
        ACORDO_FIRMADO: 'Acordo firmado',
        ENCERRADO: 'Encerrado'
    };

    const statusIcons = {
        NOVO: 'bi-flag',
        EM_ANALISE: 'bi-search',
        NEGOCIACAO: 'bi-repeat',
        ACORDO_FIRMADO: 'bi-hand-thumbs-up',
        ENCERRADO: 'bi-archive'
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
        }
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
        createModal: () => document.getElementById('create-cobranca-modal'),
        createForm: () => document.getElementById('create-cobranca-form'),
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
        if (!selectors.board()) {
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
            const response = await fetch('/juridico/api/cobranca-terceiro/board', {
                headers: { 'Accept': 'application/json', 'X-Requested-With': 'XMLHttpRequest' }
            });

            if (!response.ok) {
                throw new Error('Falha ao carregar dados remotos');
            }

            const payload = await response.json();
            const cards = normalizePayload(payload);
            state.cards = cards.length ? cards : buildSeedData();
        } catch (error) {
            console.warn('[COBRANCA-KANBAN] Usando dados locais devido a erro:', error);
            state.cards = buildSeedData();
        }

        render();
        updateFilterBadge();
    }

    function normalizePayload(payload) {
        if (!payload) {
            return [];
        }

        const rawCards = Array.isArray(payload) ? payload :
            (Array.isArray(payload.cards) ? payload.cards :
                (payload.cobrancas ? Object.values(payload.cobrancas).flat() : []));

        return rawCards.map(item => normalizeCard(item)).filter(Boolean);
    }

    function normalizeCard(raw) {
        if (!raw) {
            return null;
        }

        const status = (raw.status || raw.situacao || '').toString().toUpperCase();
        const prioridade = (raw.prioridade || raw.prioridadeNegociacao || '').toString().toUpperCase();
        const origem = (raw.origem || raw.tipoCobranca || '').toString().toUpperCase();

        const card = {
            id: raw.id || raw.codigo || cryptoRandomId(),
            referencia: raw.referencia || raw.titulo || raw.processo || 'Cobrança sem referência',
            devedor: raw.devedor || raw.debtor || raw.cliente || 'Não informado',
            valor: parseAmount(raw.valor || raw.montante || raw.amount),
            status: STATUS_ORDER.includes(status) ? status : 'NOVO',
            prioridade: PRIORIDADES.includes(prioridade) ? prioridade : 'MEDIA',
            responsavel: raw.responsavel || raw.owner || raw.advogado || '',
            origem: origem === 'JUDICIAL' || origem === 'EXTRAJUDICIAL' ? origem : 'JUDICIAL',
            prazo: normalizeDate(raw.prazo || raw.dataPrazo || raw.dueDate),
            ultimaAtualizacao: normalizeDateTime(raw.ultimaAtualizacao || raw.atualizacao || raw.updatedAt),
            detalhes: raw.detalhes || raw.observacoes || raw.description || '',
            contato: raw.contato || raw.telefone || raw.phone || '',
            email: raw.email || raw.contatoEmail || '',
            documento: raw.documento || raw.cpfCnpj || raw.document || ''
        };

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
                card.devedor,
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
        article.className = 'task-card cobranca-card';
        article.dataset.id = card.id;
        if (card.origem) {
            article.dataset.origem = card.origem;
        }

        const header = document.createElement('div');
        header.className = 'task-card-header';

        const title = document.createElement('h4');
        title.innerHTML = `<i class="bi ${statusIcons[card.status] || 'bi-kanban'}"></i> ${escapeHtml(card.referencia)}`;
        header.appendChild(title);

        const priority = document.createElement('span');
        priority.className = `badge priority-${card.prioridade}`;
        priority.textContent = priorityLabels[card.prioridade] || card.prioridade;
        header.appendChild(priority);

        article.appendChild(header);

        const body = document.createElement('div');
        body.className = 'task-card-body';

        const debtor = document.createElement('p');
        debtor.className = 'cobranca-card-row';
        debtor.innerHTML = `<span class="label"><i class="bi bi-person"></i> Devedor</span><span class="value">${escapeHtml(card.devedor || 'Não informado')}</span>`;
        body.appendChild(debtor);

        if (card.valor !== null) {
            const amount = document.createElement('p');
            amount.className = 'cobranca-card-row';
            amount.innerHTML = `<span class="label"><i class="bi bi-cash-coin"></i> Valor</span><span class="value">${formatCurrency(card.valor)}</span>`;
            body.appendChild(amount);
        }

        if (card.prazo) {
            const due = document.createElement('p');
            due.className = 'cobranca-card-row';
            due.innerHTML = `<span class="label"><i class="bi bi-calendar-event"></i> Prazo</span><span class="value">${formatDate(card.prazo)}</span>`;
            body.appendChild(due);
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
        wrapper.innerHTML = '<i class="bi bi-inbox"></i> Nenhuma cobrança aqui ainda';
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

    function handleDrop(event) {
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
        if (!card || card.status === targetStatus) {
            return;
        }

        card.status = targetStatus;
        card.ultimaAtualizacao = new Date().toISOString();
        render();
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
            { label: 'Referência', value: card.referencia },
            { label: 'Devedor', value: card.devedor },
            { label: 'Responsável', value: card.responsavel || '—' },
            { label: 'Origem', value: originLabels[card.origem] || '—' },
            { label: 'Status', value: statusLabels[card.status] || card.status },
            { label: 'Prioridade', value: priorityLabels[card.prioridade] || card.prioridade },
            { label: 'Valor', value: card.valor !== null ? formatCurrency(card.valor) : '—' },
            { label: 'Prazo', value: card.prazo ? formatDate(card.prazo) : '—' },
            { label: 'Atualizado em', value: card.ultimaAtualizacao ? formatDateTime(card.ultimaAtualizacao) : '—' },
            { label: 'Documento', value: card.documento || '—' },
            { label: 'Contato', value: card.contato || '—' },
            { label: 'E-mail', value: card.email || '—' }
        ];

        const list = details.map(item => `
            <div class="detail-row">
                <span class="detail-label">${escapeHtml(item.label)}</span>
                <span class="detail-value">${escapeHtml(item.value)}</span>
            </div>
        `).join('');

        const notes = card.detalhes ? `
            <div class="detail-notes">
                <h3>Histórico e observações</h3>
                <p>${escapeHtml(card.detalhes)}</p>
            </div>
        ` : '';

        return `
            <article class="cobranca-details">
                <header class="details-header">
                    <span class="status-badge status-${card.status}">${escapeHtml(statusLabels[card.status] || card.status)}</span>
                    <h2>${escapeHtml(card.referencia)}</h2>
                    <p class="subtitle">${escapeHtml(card.devedor || '—')}</p>
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

    function openCreateModal() {
        const modal = selectors.createModal();
        if (!modal) {
            return;
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
        }
        document.body.classList.remove('kanban-modal-open');
    }

    function handleCreateSubmit(event) {
        event.preventDefault();
        const form = event.currentTarget;
        const data = new FormData(form);

        const card = normalizeCard({
            id: cryptoRandomId(),
            referencia: data.get('referencia'),
            devedor: data.get('devedor'),
            prioridade: data.get('prioridade'),
            valor: data.get('valor'),
            prazo: data.get('prazo'),
            responsavel: data.get('responsavel'),
            origem: data.get('origem'),
            status: data.get('status'),
            ultimaAtualizacao: data.get('atualizacao') || new Date().toISOString(),
            detalhes: data.get('detalhes')
        });

        state.cards.unshift(card);
        render();
        closeCreateModal();
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
        return `cobranca-${Math.random().toString(36).slice(2, 10)}`;
    }

    function buildSeedData() {
        const now = new Date();
        return [
            {
                id: cryptoRandomId(),
                referencia: 'PROC-2024-001',
                devedor: 'Transportadora Alfa Ltda.',
                valor: 18500.32,
                status: 'NOVO',
                prioridade: 'ALTA',
                responsavel: 'Maria Barbosa',
                origem: 'JUDICIAL',
                prazo: normalizeDate(new Date(now.getFullYear(), now.getMonth(), now.getDate() + 15)),
                ultimaAtualizacao: normalizeDateTime(now),
                detalhes: 'Cobrança referente a contrato de prestação de serviço cancelado. Aguardando documentação do cliente.',
                contato: '(11) 98888-1234',
                email: 'financeiro@transportadoraalfa.com',
                documento: '27.123.456/0001-90'
            },
            {
                id: cryptoRandomId(),
                referencia: 'ACORDO-NEG-233',
                devedor: 'João Batista de Souza',
                valor: 7200.00,
                status: 'NEGOCIACAO',
                prioridade: 'MEDIA',
                responsavel: 'Caroline Ribeiro',
                origem: 'EXTRAJUDICIAL',
                prazo: normalizeDate(new Date(now.getFullYear(), now.getMonth(), now.getDate() + 7)),
                ultimaAtualizacao: normalizeDateTime(new Date(now.getTime() - 1000 * 60 * 60 * 20)),
                detalhes: 'Em negociação de parcelamento em 4x. Cliente aguarda envio do termo de acordo revisado.',
                contato: '(41) 97777-8899',
                email: 'joao.souza@email.com',
                documento: '123.456.789-00'
            },
            {
                id: cryptoRandomId(),
                referencia: 'PROC-2023-887',
                devedor: 'Construtora Vértice S/A',
                valor: 54890.47,
                status: 'EM_ANALISE',
                prioridade: 'ALTA',
                responsavel: 'Ricardo Ferraz',
                origem: 'JUDICIAL',
                prazo: normalizeDate(new Date(now.getFullYear(), now.getMonth(), now.getDate() + 30)),
                ultimaAtualizacao: normalizeDateTime(new Date(now.getTime() - 1000 * 60 * 60 * 48)),
                detalhes: 'Processo com audiência marcada. Avaliando proposta de acordo apresentada pelo advogado da parte contrária.',
                contato: '(31) 95555-6677',
                email: 'contato@construtoravertice.com',
                documento: '15.456.789/0001-12'
            },
            {
                id: cryptoRandomId(),
                referencia: 'NEG-2024-054',
                devedor: 'Mercado Nova Era ME',
                valor: 3950.90,
                status: 'ACORDO_FIRMADO',
                prioridade: 'MEDIA',
                responsavel: 'Patrícia Gomes',
                origem: 'EXTRAJUDICIAL',
                prazo: normalizeDate(new Date(now.getFullYear(), now.getMonth(), now.getDate() + 3)),
                ultimaAtualizacao: normalizeDateTime(new Date(now.getTime() - 1000 * 60 * 60 * 3)),
                detalhes: 'Acordo firmado com pagamento em 2 parcelas. Primeira parcela vence em 5 dias.',
                contato: '(21) 96666-7788',
                email: 'contato@mercadonnovaera.com',
                documento: '09.987.654/0001-44'
            },
            {
                id: cryptoRandomId(),
                referencia: 'ENC-2023-410',
                devedor: 'Logística Horizonte LTDA',
                valor: 12800.00,
                status: 'ENCERRADO',
                prioridade: 'BAIXA',
                responsavel: 'Eduardo Martins',
                origem: 'JUDICIAL',
                prazo: normalizeDate(new Date(now.getFullYear(), now.getMonth() - 2, now.getDate())),
                ultimaAtualizacao: normalizeDateTime(new Date(now.getTime() - 1000 * 60 * 60 * 24 * 12)),
                detalhes: 'Cobrança encerrada após quitação integral. Arquivar documentação.',
                contato: '(51) 93333-4455',
                email: 'contato@logisticahorizonte.com',
                documento: '38.654.321/0001-77'
            }
        ];
    }

    document.addEventListener('DOMContentLoaded', () => {
        window.cobrancaBoard = {
            applyFilters,
            resetFilters,
            closeFiltersModal,
            openCreateModal,
            closeCreateModal,
            closeModal
        };

        init();
    });
})();
