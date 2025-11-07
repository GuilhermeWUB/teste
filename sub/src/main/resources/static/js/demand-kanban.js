(function () {
    const ALL_PRIORIDADES = ['URGENTE', 'ALTA', 'MEDIA', 'BAIXA'];
    const ALL_STATUS = ['PENDENTE', 'EM_ANDAMENTO', 'CONCLUIDA', 'CANCELADA'];

    const state = {
        cards: [],
        search: '',
        advancedFilters: {
            prioridade: [...ALL_PRIORIDADES],
            status: [...ALL_STATUS],
            targetRoles: [],
            assigned: '',
            creator: '',
            dateFrom: null,
            dateTo: null
        }
    };

    const dragState = {
        cardId: null,
        originStatus: null
    };

    const statusLabels = {
        PENDENTE: 'Pendente',
        EM_ANDAMENTO: 'Em andamento',
        CONCLUIDA: 'Concluída',
        CANCELADA: 'Cancelada'
    };

    const priorityLabels = {
        URGENTE: 'Urgente',
        ALTA: 'Alta',
        MEDIA: 'Média',
        BAIXA: 'Baixa'
    };

    let allTargetRoles = [];

    const selectors = {
        columns: () => Array.from(document.querySelectorAll('.kanban-column')),
        searchInput: () => document.getElementById('kanban-search'),
        btnAdvancedFilters: () => document.getElementById('btn-advanced-filters'),
        btnClearFilters: () => document.getElementById('btn-clear-filters'),
        filtersModal: () => document.getElementById('filters-modal'),
        modal: () => document.getElementById('kanban-modal'),
        modalBody: () => document.querySelector('#kanban-modal .kanban-modal-body'),
        createModal: () => document.getElementById('create-demand-modal')
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

    function init() {
        if (!document.querySelector('.kanban-board')) {
            return;
        }

        collectAllRoles();
        bindEvents();
        updateFilterBadge();
        loadBoard();
    }

    function collectAllRoles() {
        const modal = selectors.filtersModal();
        if (!modal) {
            return;
        }
        allTargetRoles = Array.from(modal.querySelectorAll('input[name="targetRoles"]')).map(cb => cb.value);
        state.advancedFilters.targetRoles = [];
    }

    function bindEvents() {
        const search = selectors.searchInput();
        if (search) {
            search.addEventListener('input', () => {
                state.search = search.value.toLowerCase();
                render();
            });
        }

        const btnAdvancedFilters = selectors.btnAdvancedFilters();
        if (btnAdvancedFilters) {
            btnAdvancedFilters.addEventListener('click', openFiltersModal);
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

        const modal = selectors.modal();
        if (modal) {
            modal.addEventListener('click', event => {
                if (event.target === modal) {
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

        document.addEventListener('keydown', event => {
            if (event.key !== 'Escape') {
                return;
            }

            const filtersModalRef = selectors.filtersModal();
            if (filtersModalRef && filtersModalRef.classList.contains('active')) {
                closeFiltersModal();
                return;
            }

            const modalRef = selectors.modal();
            if (modalRef && modalRef.classList.contains('active')) {
                closeModal();
                return;
            }

            const createRef = selectors.createModal();
            if (createRef && createRef.classList.contains('active')) {
                closeCreateModal();
            }
        });
    }

    async function loadBoard() {
        try {
            const response = await fetch('/demands/api/board', {
                headers: buildHeaders({ 'Accept': 'application/json' })
            });

            if (!response.ok) {
                throw new Error('Erro ao carregar demandas');
            }

            const payload = await response.json();
            if (payload && Array.isArray(payload.cards)) {
                state.cards = payload.cards;
            } else if (payload && payload.demandsByStatus) {
                state.cards = Object.values(payload.demandsByStatus).flat();
            } else {
                state.cards = [];
            }

            render();
        } catch (error) {
            console.error('[KANBAN] Falha ao carregar demandas:', error);
            renderError();
        }
    }

    function render() {
        const columns = selectors.columns();
        if (columns.length === 0) {
            return;
        }

        const filteredCards = applyFilters(state.cards);
        const grouped = groupByStatus(filteredCards);

        columns.forEach(column => {
            const status = column.dataset.status;
            const container = column.querySelector('.tasks-container');
            const counter = column.querySelector('.column-count');

            if (counter) {
                counter.textContent = grouped[status] ? grouped[status].length : 0;
            }

            if (!container) {
                return;
            }

            bindColumnDropZone(column);
            container.innerHTML = '';

            const items = grouped[status] || [];
            if (items.length === 0) {
                container.appendChild(createEmptyState());
                return;
            }

            items.forEach(card => container.appendChild(createCard(card)));
        });

        enableDragAndDrop();
    }

    function renderError() {
        selectors.columns().forEach(column => {
            const container = column.querySelector('.tasks-container');
            const counter = column.querySelector('.column-count');
            if (counter) {
                counter.textContent = '0';
            }
            if (container) {
                container.innerHTML = '';
                const error = document.createElement('div');
                error.className = 'empty-state';
                error.textContent = 'Não foi possível carregar as demandas.';
                container.appendChild(error);
            }
        });
    }

    function applyFilters(cards) {
        const term = state.search.trim();
        const filters = state.advancedFilters;

        return cards.filter(card => {
            if (filters.prioridade.length === 0) {
                return false;
            }

            const prioridade = card.prioridade || null;
            if (prioridade && !filters.prioridade.includes(prioridade)) {
                return false;
            }

            if (filters.status.length === 0) {
                return false;
            }

            const status = card.status || 'PENDENTE';
            if (!filters.status.includes(status)) {
                return false;
            }

            if (filters.targetRoles.length) {
                if (filters.targetRoles.length === 1 && filters.targetRoles[0] === '__NONE__') {
                    return false;
                }

                const cardRoles = Array.isArray(card.targetRoles) ? card.targetRoles : [];
                if (cardRoles.length === 0) {
                    return false;
                }
                const matchesRole = cardRoles.some(role => filters.targetRoles.includes(role));
                if (!matchesRole) {
                    return false;
                }
            }

            if (filters.assigned) {
                const assigned = (card.assignedToName || '').toLowerCase();
                if (!assigned.includes(filters.assigned.toLowerCase())) {
                    return false;
                }
            }

            if (filters.creator) {
                const creator = (card.createdByName || '').toLowerCase();
                if (!creator.includes(filters.creator.toLowerCase())) {
                    return false;
                }
            }

            if (filters.dateFrom && card.dueDate) {
                const dueDate = parseDate(card.dueDate);
                if (dueDate && dueDate < filters.dateFrom) {
                    return false;
                }
            }

            if (filters.dateTo && card.dueDate) {
                const dueDate = parseDate(card.dueDate);
                if (dueDate && dueDate > filters.dateTo) {
                    return false;
                }
            }

            if (!term) {
                return true;
            }

            const haystack = [
                card.titulo,
                card.descricao,
                card.assignedToName,
                card.createdByName,
                ...(Array.isArray(card.targetRolesDisplay) ? card.targetRolesDisplay : [])
            ]
                .filter(Boolean)
                .join(' ')
                .toLowerCase();

            return haystack.includes(term.toLowerCase());
        });
    }

    function groupByStatus(cards) {
        return cards.reduce((acc, card) => {
            const status = card.status || 'PENDENTE';
            acc[status] = acc[status] || [];
            acc[status].push(card);
            return acc;
        }, {});
    }

    function createCard(card) {
        const article = document.createElement('article');
        article.className = 'task-card';
        article.dataset.id = card.id;

        const header = document.createElement('div');
        header.className = 'task-card-header';

        const title = document.createElement('h4');
        title.textContent = card.titulo || 'Demanda sem título';
        header.appendChild(title);

        const menu = document.createElement('button');
        menu.type = 'button';
        menu.className = 'task-card-menu';
        menu.innerHTML = '<i class="bi bi-three-dots"></i>';
        menu.addEventListener('click', event => {
            event.stopPropagation();
            openModal(card);
        });
        header.appendChild(menu);

        article.appendChild(header);

        if (card.descricao) {
            const description = document.createElement('p');
            description.className = 'task-card-description';
            description.textContent = truncate(card.descricao, 140);
            article.appendChild(description);
        }

        const footer = document.createElement('div');
        footer.className = 'task-card-footer';

        const badges = document.createElement('div');
        badges.className = 'task-card-badges';

        if (card.prioridade) {
            const priority = document.createElement('span');
            priority.className = `badge priority-${card.prioridade}`;
            priority.textContent = priorityLabels[card.prioridade] || card.prioridadeLabel || card.prioridade;
            if (card.prioridadeColor) {
                priority.style.background = card.prioridadeColor;
                priority.style.color = '#fff';
            }
            badges.appendChild(priority);
        }

        if (Array.isArray(card.targetRolesDisplay) && card.targetRolesDisplay.length) {
            card.targetRolesDisplay.forEach(role => {
                const badge = document.createElement('span');
                badge.className = 'badge badge-role';
                badge.innerHTML = `<i class="bi bi-people"></i> ${escapeHtml(role)}`;
                badges.appendChild(badge);
            });
        }

        footer.appendChild(badges);

        const meta = document.createElement('div');
        meta.className = 'task-card-meta';

        if (card.dueDate) {
            const due = document.createElement('span');
            due.className = 'date';
            due.innerHTML = `<i class="bi bi-calendar-event"></i> ${formatDateTime(card.dueDate)}`;
            meta.appendChild(due);
        }

        if (card.assignedToName) {
            const assigned = document.createElement('span');
            assigned.className = 'date';
            assigned.innerHTML = `<i class="bi bi-person-check"></i> ${escapeHtml(card.assignedToName)}`;
            meta.appendChild(assigned);
        }

        if (card.createdByName) {
            const creator = document.createElement('span');
            creator.className = 'date';
            creator.innerHTML = `<i class="bi bi-person"></i> ${escapeHtml(card.createdByName)}`;
            meta.appendChild(creator);
        }

        footer.appendChild(meta);
        article.appendChild(footer);

        article.addEventListener('click', () => openModal(card));

        return article;
    }

    function truncate(value, maxLength) {
        if (!value) {
            return '';
        }
        if (value.length <= maxLength) {
            return value;
        }
        return `${value.substring(0, maxLength - 1)}…`;
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

    function enableDragAndDrop() {
        document.querySelectorAll('.task-card').forEach(card => {
            card.setAttribute('draggable', 'true');
            card.addEventListener('dragstart', handleDragStart);
            card.addEventListener('dragend', handleDragEnd);
        });
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

        const previousLabel = card.statusLabel;
        card.status = targetStatus;
        card.statusLabel = statusLabels[targetStatus] || card.statusLabel;

        render();

        try {
            const response = await persistCardStatusChange(normalizedCardId, targetStatus);
            if (response && response.demand) {
                syncCardFromResponse(normalizedCardId, response.demand);
                render();
            }
        } catch (error) {
            console.error('[KANBAN] Falha ao atualizar status:', error);
            card.status = previousStatus;
            card.statusLabel = previousLabel || statusLabels[previousStatus] || card.statusLabel;
            render();
            alert('Não foi possível atualizar o status da demanda.');
        } finally {
            dragState.cardId = null;
            dragState.originStatus = null;
        }
    }

    async function persistCardStatusChange(cardId, newStatus) {
        const response = await fetch(`/demands/api/${cardId}/status`, {
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

    function syncCardFromResponse(cardId, payloadDemand) {
        const card = state.cards.find(item => String(item.id) === String(cardId));
        if (!card || !payloadDemand) {
            return;
        }

        Object.assign(card, payloadDemand);
    }

    function createEmptyState() {
        const empty = document.createElement('div');
        empty.className = 'empty-state';
        empty.textContent = 'Nenhuma demanda neste status.';
        return empty;
    }

    function parseDate(value) {
        if (!value) {
            return null;
        }
        const date = new Date(value);
        return Number.isNaN(date.getTime()) ? null : date;
    }

    function formatDateTime(value) {
        const date = parseDate(value);
        if (!date) {
            return '';
        }
        const day = String(date.getDate()).padStart(2, '0');
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const year = date.getFullYear();
        const hours = String(date.getHours()).padStart(2, '0');
        const minutes = String(date.getMinutes()).padStart(2, '0');
        return `${day}/${month}/${year} ${hours}:${minutes}`;
    }

    function openModal(card) {
        const modal = selectors.modal();
        const modalBody = selectors.modalBody();
        if (!modal || !modalBody) {
            return;
        }

        modalBody.innerHTML = buildModalContent(card);
        modal.classList.add('active');
        document.body.style.overflow = 'hidden';
    }

    function closeModal() {
        const modal = selectors.modal();
        if (!modal) {
            return;
        }
        modal.classList.remove('active');
        document.body.style.overflow = '';
    }

    function openFiltersModal() {
        const modal = selectors.filtersModal();
        if (!modal) {
            return;
        }

        syncFiltersToModal(modal);
        modal.classList.add('active');
        document.body.style.overflow = 'hidden';
    }

    function closeFiltersModal() {
        const modal = selectors.filtersModal();
        if (!modal) {
            return;
        }
        modal.classList.remove('active');
        document.body.style.overflow = '';
    }

    function syncFiltersToModal(modal) {
        ['prioridade', 'status', 'targetRoles'].forEach(filterType => {
            const checkboxes = modal.querySelectorAll(`input[name="${filterType}"]`);
            checkboxes.forEach(checkbox => {
                if (filterType === 'targetRoles') {
                    if (state.advancedFilters.targetRoles.length === 0) {
                        checkbox.checked = true;
                    } else {
                        checkbox.checked = state.advancedFilters.targetRoles.includes(checkbox.value);
                    }
                } else {
                    checkbox.checked = state.advancedFilters[filterType].includes(checkbox.value);
                }
            });
        });

        const dateFrom = modal.querySelector('#filter-date-from');
        const dateTo = modal.querySelector('#filter-date-to');
        if (dateFrom) {
            dateFrom.value = state.advancedFilters.dateFrom ? formatDateForInput(state.advancedFilters.dateFrom) : '';
        }
        if (dateTo) {
            dateTo.value = state.advancedFilters.dateTo ? formatDateForInput(state.advancedFilters.dateTo) : '';
        }

        const assigned = modal.querySelector('#filter-assigned');
        if (assigned) {
            assigned.value = state.advancedFilters.assigned;
        }

        const creator = modal.querySelector('#filter-creator');
        if (creator) {
            creator.value = state.advancedFilters.creator;
        }
    }

    function formatDateForInput(date) {
        if (!(date instanceof Date)) {
            return '';
        }
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        return `${year}-${month}-${day}`;
    }

    function applyFiltersFromModal() {
        const modal = selectors.filtersModal();
        if (!modal) {
            return;
        }

        ['prioridade', 'status'].forEach(filterType => {
            const checkboxes = modal.querySelectorAll(`input[name="${filterType}"]:checked`);
            state.advancedFilters[filterType] = Array.from(checkboxes).map(cb => cb.value);
        });

        const selectedRoles = Array.from(modal.querySelectorAll('input[name="targetRoles"]:checked')).map(cb => cb.value);
        if (selectedRoles.length === 0 && allTargetRoles.length > 0) {
            state.advancedFilters.targetRoles = ['__NONE__'];
        } else if (selectedRoles.length === allTargetRoles.length) {
            state.advancedFilters.targetRoles = [];
        } else {
            state.advancedFilters.targetRoles = selectedRoles;
        }

        const dateFrom = modal.querySelector('#filter-date-from');
        const dateTo = modal.querySelector('#filter-date-to');
        state.advancedFilters.dateFrom = dateFrom && dateFrom.value ? new Date(dateFrom.value) : null;
        state.advancedFilters.dateTo = dateTo && dateTo.value ? new Date(dateTo.value) : null;

        const assigned = modal.querySelector('#filter-assigned');
        state.advancedFilters.assigned = assigned ? assigned.value.trim() : '';

        const creator = modal.querySelector('#filter-creator');
        state.advancedFilters.creator = creator ? creator.value.trim() : '';

        updateFilterBadge();
        render();
        closeFiltersModal();
    }

    function resetFiltersInModal() {
        state.advancedFilters = {
            prioridade: [...ALL_PRIORIDADES],
            status: [...ALL_STATUS],
            targetRoles: [],
            assigned: '',
            creator: '',
            dateFrom: null,
            dateTo: null
        };

        const modal = selectors.filtersModal();
        if (modal) {
            modal.querySelectorAll('input[type="checkbox"]').forEach(checkbox => {
                checkbox.checked = true;
            });

            const assigned = modal.querySelector('#filter-assigned');
            if (assigned) {
                assigned.value = '';
            }

            const creator = modal.querySelector('#filter-creator');
            if (creator) {
                creator.value = '';
            }

            const dateFrom = modal.querySelector('#filter-date-from');
            const dateTo = modal.querySelector('#filter-date-to');
            if (dateFrom) {
                dateFrom.value = '';
            }
            if (dateTo) {
                dateTo.value = '';
            }
        }

        updateFilterBadge();
        render();
    }

    function clearAllFilters() {
        resetFiltersInModal();
    }

    function updateFilterBadge() {
        const btnAdvancedFilters = selectors.btnAdvancedFilters();
        const btnClearFilters = selectors.btnClearFilters();
        const filterCount = btnAdvancedFilters ? btnAdvancedFilters.querySelector('.filter-count') : null;

        let activeFiltersCount = 0;

        if (state.advancedFilters.prioridade.length < ALL_PRIORIDADES.length) {
            activeFiltersCount++;
        }
        if (state.advancedFilters.status.length < ALL_STATUS.length) {
            activeFiltersCount++;
        }
        if (state.advancedFilters.targetRoles.length > 0) {
            activeFiltersCount++;
        }
        if (state.advancedFilters.dateFrom) {
            activeFiltersCount++;
        }
        if (state.advancedFilters.dateTo) {
            activeFiltersCount++;
        }
        if (state.advancedFilters.assigned) {
            activeFiltersCount++;
        }
        if (state.advancedFilters.creator) {
            activeFiltersCount++;
        }

        if (filterCount) {
            if (activeFiltersCount > 0) {
                filterCount.textContent = activeFiltersCount;
                filterCount.style.display = 'inline-flex';
                if (btnAdvancedFilters) {
                    btnAdvancedFilters.classList.add('active');
                }
            } else {
                filterCount.style.display = 'none';
                if (btnAdvancedFilters) {
                    btnAdvancedFilters.classList.remove('active');
                }
            }
        }

        if (btnClearFilters) {
            btnClearFilters.style.display = activeFiltersCount > 0 ? 'inline-flex' : 'none';
        }
    }

    function buildModalContent(card) {
        const rows = [
            { label: 'Título', value: card.titulo },
            { label: 'Descrição', value: card.descricao },
            { label: 'Status', value: card.statusLabel || statusLabels[card.status] },
            { label: 'Prioridade', value: card.prioridadeLabel || priorityLabels[card.prioridade] },
            { label: 'Responsável', value: card.assignedToName },
            { label: 'Criado por', value: card.createdByName },
            { label: 'Prazo', value: card.dueDate ? formatDateTime(card.dueDate) : null },
            { label: 'Atualizado em', value: card.updatedAt ? formatDateTime(card.updatedAt) : null },
            { label: 'Concluído em', value: card.completedAt ? formatDateTime(card.completedAt) : null },
            { label: 'Cargos destinatários', value: Array.isArray(card.targetRolesDisplay) ? card.targetRolesDisplay.join(', ') : null }
        ];

        const html = [
            '<div class="kanban-modal-grid">',
            ...rows.filter(row => Boolean(row.value)).map(row => `  <div class="kanban-modal-row"><span>${row.label}:</span><strong>${escapeHtml(row.value)}</strong></div>`),
            '</div>'
        ];

        if (card.detailsUrl) {
            html.push('<div class="kanban-modal-actions">');
            html.push(`  <a class="btn-primary" href="${card.detailsUrl}"><i class="bi bi-box-arrow-up-right"></i> Ver detalhes completos</a>`);
            if (card.editUrl) {
                html.push(`  <a class="btn-secondary" href="${card.editUrl}"><i class="bi bi-pencil"></i> Editar</a>`);
            }
            html.push('</div>');
        }

        return html.join('\n');
    }

    function escapeHtml(value) {
        return String(value)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
    }

    function openCreateModal() {
        const modal = selectors.createModal();
        if (!modal) {
            return;
        }
        modal.classList.add('active');
        document.body.style.overflow = 'hidden';
    }

    function closeCreateModal() {
        const modal = selectors.createModal();
        if (!modal) {
            return;
        }
        modal.classList.remove('active');
        document.body.style.overflow = '';
    }

    window.demandBoard = {
        init,
        closeModal,
        closeFiltersModal,
        applyFilters: applyFiltersFromModal,
        resetFilters: resetFiltersInModal,
        openCreateModal,
        closeCreateModal
    };

    document.addEventListener('DOMContentLoaded', init);
})();
