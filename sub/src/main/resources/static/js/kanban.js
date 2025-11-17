(function () {
    const PRIORIDADE_VALUES = ['URGENTE', 'ALTA', 'MEDIA', 'BAIXA', 'NULL'];
    const STATUS_VALUES = [
        // Fase 1 - Comunicação
        'COMUNICADO', 'ABERTO',
        // Fase 2 - Análise
        'VISTORIA', 'ANALISE', 'SINDICANCIA', 'DESISTENCIA',
        // Fase 3 - Negociação
        'ORCAMENTO', 'COTA_PARTICIPACAO', 'ACORDO_ANDAMENTO',
        // Fase 4 - Execução
        'COMPRA', 'AGENDADO', 'REPAROS_LIBERADOS', 'COMPLEMENTOS', 'ENTREGUES', 'PESQUISA_SATISFACAO',
        // Fase 5 - Garantia
        'ABERTURA_GARANTIA', 'VISTORIA_GARANTIA', 'GARANTIA_AUTORIZADA', 'GARANTIA_ENTREGUE'
    ];
    const ENVOLVIMENTO_VALUES = ['CAUSADOR', 'VITIMA', 'NAO_INFORMADO'];
    const MOTIVO_VALUES = ['COLISAO', 'ROUBO', 'FURTO', 'INCENDIO', 'VANDALISMO', 'FENOMENO_NATURAL', 'QUEBRA_PECA', 'OUTROS', 'NAO_INFORMADO_MOTIVO'];

    const state = {
        cards: [],
        search: "",
        advancedFilters: {
            prioridade: [...PRIORIDADE_VALUES],
            status: [...STATUS_VALUES],
            envolvimento: [...ENVOLVIMENTO_VALUES],
            motivo: [...MOTIVO_VALUES],
            dateFrom: null,
            dateTo: null,
            analista: ''
        }
    };

    const dragState = {
        cardId: null,
        originStatus: null,
        pendingCardId: null,
        pendingTargetStatus: null,
        pendingPreviousStatus: null,
        pendingTriggerButton: null,
        pendingTriggerButtonLabel: null
    };

    const highlightState = {
        eventId: null,
        hasOpened: false
    };

    const statusLabels = {
        // Fase 1 - Comunicação
        COMUNICADO: '1.0 Comunicado',
        ABERTO: '1.1 Aberto',
        // Fase 2 - Análise
        VISTORIA: '2.0 Vistoria',
        ANALISE: '2.1 Análise',
        SINDICANCIA: '2.2 Sindicância',
        DESISTENCIA: '2.8 Desistência',
        // Fase 3 - Negociação
        ORCAMENTO: '3.0 Orçamento',
        COTA_PARTICIPACAO: '3.1 Cota de Participação',
        ACORDO_ANDAMENTO: '3.2 Acordo em Andamento',
        // Fase 4 - Execução
        COMPRA: '4.0 Compra',
        AGENDADO: '4.1 Agendado',
        REPAROS_LIBERADOS: '4.2 Reparos Liberados',
        COMPLEMENTOS: '4.3 Complementos',
        ENTREGUES: '4.7 Entregues',
        PESQUISA_SATISFACAO: '4.8 Pesquisa de Satisfação',
        // Fase 5 - Garantia
        ABERTURA_GARANTIA: '5.0 Abertura de Garantia',
        VISTORIA_GARANTIA: '5.1 Vistoria de Garantia',
        GARANTIA_AUTORIZADA: '5.2 Garantia Autorizada',
        GARANTIA_ENTREGUE: '5.7 Garantia Entregue'
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

    const selectors = {
        columns: () => Array.from(document.querySelectorAll('.kanban-column')),
        searchInput: () => document.getElementById('kanban-search'),
        btnAdvancedFilters: () => document.getElementById('btn-advanced-filters'),
        btnClearFilters: () => document.getElementById('btn-clear-filters'),
        filtersModal: () => document.getElementById('filters-modal'),
        modal: () => document.getElementById('kanban-modal'),
        modalBody: () => document.querySelector('#kanban-modal .kanban-modal-body')
    };

    function init() {
        if (!document.querySelector('.kanban-board')) {
            return;
        }
        highlightState.eventId = readHighlightParam();
        bindEvents();
        loadBoard();
    }

    function readHighlightParam() {
        try {
            const params = new URLSearchParams(window.location.search);
            const highlight = params.get('highlight');

            if (!highlight) {
                return null;
            }

            params.delete('highlight');
            const newQuery = params.toString();
            const newUrl = window.location.pathname + (newQuery ? `?${newQuery}` : '') + window.location.hash;
            window.history.replaceState({}, '', newUrl);

            return highlight;
        } catch (error) {
            console.warn('[KANBAN] Não foi possível ler o parâmetro de destaque:', error);
            return null;
        }
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

            document.addEventListener('keydown', event => {
                if (event.key === 'Escape') {
                    if (filtersModal && filtersModal.classList.contains('active')) {
                        closeFiltersModal();
                    } else if (modal.classList.contains('active')) {
                        closeModal();
                    }
                }
            });
        }
    }

    async function loadBoard() {
        try {
            const response = await fetch('/events/api/board', {
                headers: buildHeaders({ 'Accept': 'application/json' })
            });

            if (!response.ok) {
                throw new Error('Erro ao carregar eventos.');
            }

            const payload = await response.json();
            if (payload && Array.isArray(payload.cards)) {
                state.cards = payload.cards;
            } else if (payload && payload.eventsByStatus) {
                state.cards = Object.values(payload.eventsByStatus).flat();
            } else {
                state.cards = [];
            }

            render();
            handleHighlight();
        } catch (error) {
            console.error('[KANBAN] Falha ao carregar eventos:', error);
            renderError();
        }
    }

    function handleHighlight() {
        if (!highlightState.eventId || highlightState.hasOpened) {
            return;
        }

        const targetId = highlightState.eventId;
        const card = state.cards.find(item => String(item.id) === String(targetId));

        if (!card) {
            return;
        }

        highlightState.hasOpened = true;

        const cardElement = document.querySelector(`.task-card[data-id="${card.id}"]`);
        if (cardElement) {
            cardElement.classList.add('task-card-highlight');
            cardElement.scrollIntoView({ behavior: 'smooth', block: 'center' });
            setTimeout(() => cardElement.classList.remove('task-card-highlight'), 4000);
        }

        setTimeout(() => openModal(card), 250);
    }

    function render() {
        const columns = selectors.columns();
        if (columns.length === 0) {
            return;
        }

        const cards = applyFilters(state.cards);
        const grouped = groupByStatus(cards);

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
                error.textContent = 'Não foi possível carregar os eventos.';
                container.appendChild(error);
            }
        });
    }

    function applyFilters(cards) {
        const term = state.search.trim();
        const filters = state.advancedFilters;

        return cards.filter(card => {
            // Filtro de prioridade
            const cardPrioridade = card.prioridade || 'NULL';
            if (!filters.prioridade.includes(cardPrioridade)) {
                return false;
            }

            // Filtro de status
            if (!filters.status.includes(card.status)) {
                return false;
            }

            // Filtro de envolvimento
            if (!filters.envolvimento.includes(card.envolvimento)) {
                return false;
            }

            // Filtro de motivo
            if (!filters.motivo.includes(card.motivo)) {
                return false;
            }

            // Filtro de data de vencimento
            if (filters.dateFrom && card.dataVencimento) {
                const cardDate = new Date(card.dataVencimento);
                const fromDate = new Date(filters.dateFrom);
                if (cardDate < fromDate) {
                    return false;
                }
            }

            if (filters.dateTo && card.dataVencimento) {
                const cardDate = new Date(card.dataVencimento);
                const toDate = new Date(filters.dateTo);
                if (cardDate > toDate) {
                    return false;
                }
            }

            // Filtro de analista
            if (filters.analista && card.analistaResponsavel) {
                const analistaLower = filters.analista.toLowerCase();
                const cardAnalistaLower = card.analistaResponsavel.toLowerCase();
                if (!cardAnalistaLower.includes(analistaLower)) {
                    return false;
                }
            }

            // Filtro de busca textual
            if (!term) {
                return true;
            }

            const haystack = [
                card.titulo,
                card.descricao,
                card.partnerName,
                card.vehiclePlate,
                card.analistaResponsavel,
                card.statusLabel,
                card.motivoLabel,
                card.envolvimentoLabel
            ]
                .filter(Boolean)
                .join(' ')
                .toLowerCase();

            return haystack.includes(term);
        });
    }

    function groupByStatus(cards) {
        return cards.reduce((acc, card) => {
            const status = card.status || 'COMUNICADO';
            acc[status] = acc[status] || [];
            acc[status].push(card);
            return acc;
        }, {});
    }

    function createCard(card) {
        const article = document.createElement('article');
        article.className = 'task-card';
        article.dataset.id = card.id;

        if (highlightState.eventId && String(card.id) === String(highlightState.eventId)) {
            article.classList.add('task-card-highlight');
            setTimeout(() => article.classList.remove('task-card-highlight'), 4000);
        }

        const header = document.createElement('div');
        header.className = 'task-card-header';

        const title = document.createElement('h4');
        title.textContent = card.titulo || 'Evento sem título';
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
            description.textContent = card.descricao;
            article.appendChild(description);
        }

        const footer = document.createElement('div');
        footer.className = 'task-card-footer';

        const badges = document.createElement('div');
        badges.className = 'task-card-badges';

        if (card.prioridade) {
            const priority = document.createElement('span');
            priority.className = `badge priority-${card.prioridade}`;
            priority.textContent = card.prioridadeLabel || card.prioridade;
            if (card.prioridadeColor) {
                priority.style.background = card.prioridadeColor;
                priority.style.color = '#fff';
            }
            badges.appendChild(priority);
        }

        if (card.partnerName) {
            const partner = document.createElement('span');
            partner.className = 'badge';
            partner.innerHTML = `<i class="bi bi-person"></i> ${card.partnerName}`;
            badges.appendChild(partner);
        }

        if (card.envolvimentoLabel) {
            const envolvimento = document.createElement('span');
            envolvimento.className = 'badge';
            envolvimento.innerHTML = `<i class="bi bi-people"></i> ${card.envolvimentoLabel}`;
            badges.appendChild(envolvimento);
        }

        footer.appendChild(badges);

        const meta = document.createElement('div');
        meta.className = 'task-card-meta';

        if (card.dataVencimento) {
            const date = document.createElement('span');
            date.className = 'date';
            date.innerHTML = `<i class="bi bi-calendar-event"></i> ${formatDate(card.dataVencimento)}`;
            meta.appendChild(date);
        }

        if (card.analistaResponsavel) {
            const analyst = document.createElement('span');
            analyst.className = 'date';
            analyst.innerHTML = `<i class="bi bi-person-badge"></i> ${card.analistaResponsavel}`;
            meta.appendChild(analyst);
        }

        footer.appendChild(meta);

        article.appendChild(footer);

        article.addEventListener('click', () => openModal(card));

        return article;
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

        // Interceptar quando mover para "3.2 Acordo em Andamento"
        if (targetStatus === 'ACORDO_ANDAMENTO') {
            dragState.pendingCardId = normalizedCardId;
            dragState.pendingTargetStatus = targetStatus;
            dragState.pendingPreviousStatus = previousStatus;
            showLegalTypeModal();
            return;
        }

        const previousLabel = card.statusLabel;
        card.status = targetStatus;
        card.statusLabel = statusLabels[targetStatus] || card.statusLabel;

        render();

        try {
            const response = await persistCardStatusChange(normalizedCardId, targetStatus);
            if (response && response.event) {
                syncCardFromResponse(normalizedCardId, response.event);
                render();
            }
        } catch (error) {
            console.error('[KANBAN] Falha ao atualizar status:', error);
            card.status = previousStatus;
            card.statusLabel = previousLabel || statusLabels[previousStatus] || card.statusLabel;
            render();
            alert('Não foi possível atualizar o status do evento. Tente novamente.');
        } finally {
            dragState.cardId = null;
            dragState.originStatus = null;
        }
    }

    async function persistCardStatusChange(cardId, newStatus) {
        const response = await fetch(`/events/api/${cardId}/status`, {
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

    function syncCardFromResponse(cardId, payloadEvent) {
        const card = state.cards.find(item => String(item.id) === String(cardId));
        if (!card || !payloadEvent) {
            return;
        }

        if (payloadEvent.status) {
            card.status = payloadEvent.status;
        }

        if (payloadEvent.statusLabel) {
            card.statusLabel = payloadEvent.statusLabel;
        } else if (payloadEvent.status) {
            card.statusLabel = statusLabels[payloadEvent.status] || card.statusLabel;
        }
    }

    function createEmptyState() {
        const empty = document.createElement('div');
        empty.className = 'empty-state';
        empty.textContent = 'Nenhum evento neste status.';
        return empty;
    }

    function formatDate(value) {
        if (!value) {
            return '';
        }
        try {
            const [year, month, day] = value.split('-').map(Number);
            if (!year || !month || !day) {
                return value;
            }
            return `${String(day).padStart(2, '0')}/${String(month).padStart(2, '0')}/${year}`;
        } catch (error) {
            return value;
        }
    }

    function openModal(card) {
        const modal = selectors.modal();
        const modalBody = selectors.modalBody();
        if (!modal || !modalBody) {
            return;
        }

        modalBody.innerHTML = buildModalContent(card);
        bindModalActions(card);

        // Carrega históricos e fotos de vistoria de forma assíncrona
        loadObservationHistory(card.id);
        loadDescriptionHistory(card.id);
        loadVistoriaPhotos(card.id);

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

    function showLegalTypeModal() {
        const modal = document.getElementById('legal-type-modal');
        if (!modal) {
            console.error('[KANBAN] Modal de tipo jurídico não encontrado');
            return;
        }
        modal.style.display = 'flex';
        document.body.style.overflow = 'hidden';
    }

    function closeLegalTypeModal(options = {}) {
        const { resetState = true } = options;
        const modal = document.getElementById('legal-type-modal');
        if (!modal) {
            return;
        }
        modal.style.display = 'none';
        document.body.style.overflow = '';

        if (!resetState) {
            return;
        }

        resetLegalModalState();
    }

    async function selectLegalType(legalType) {
        const cardId = dragState.pendingCardId;
        const triggerButton = dragState.pendingTriggerButton;
        const originalLabel = dragState.pendingTriggerButtonLabel;

        if (!cardId) {
            console.error('[KANBAN] Nenhum card pendente para enviar ao jurídico');
            closeLegalTypeModal();
            return;
        }

        console.log('[KANBAN] Enviando evento para jurídico - CardID:', cardId, 'ProcessType:', legalType);
        closeLegalTypeModal();

        try {
            const requestPayload = { processType: legalType };
            console.log('[KANBAN] Payload:', JSON.stringify(requestPayload));

            // Enviar evento para o jurídico com o tipo selecionado
            const response = await fetch(`/events/api/${cardId}/send-to-legal`, {
                method: 'POST',
                headers: buildHeaders({
                    'Content-Type': 'application/json',
                    'Accept': 'application/json'
                }),
                body: JSON.stringify(requestPayload)
            });

            const responseText = await response.text();
            let responsePayload = null;
            if (responseText) {
                try {
                    responsePayload = JSON.parse(responseText);
                } catch (jsonError) {
                    responsePayload = { raw: responseText };
                }
            }

            if (response.status === 404) {
                alert(responsePayload?.error || 'Evento não encontrado. Ele pode ter sido removido.');
                return;
            }

            if (response.status === 409) {
                alert(responsePayload?.error || 'Este evento já foi enviado para o jurídico.');
                return;
            }

            if (response.status === 400) {
                alert(responsePayload?.error || 'Não foi possível enviar o evento. Verifique os dados.');
                return;
            }

            if (!response.ok) {
                throw new Error(responsePayload?.error || `Erro ao enviar para jurídico: ${response.status}`);
            }

            const result = responsePayload || {};
            console.log('[KANBAN] Resposta do servidor:', result);
            if (result.processType) {
                console.log('[KANBAN] Processo criado com tipo:', result.processType);
            }

            // Remover o card da tela
            state.cards = state.cards.filter(card => String(card.id) !== String(cardId));
            render();

            // Fechar o modal de detalhes se estiver aberto
            closeModal();

            alert(`Evento enviado com sucesso para Jurídico/Cobrança (${result.processType || legalType})!\nNúmero do processo: ${result.numeroProcesso || 'N/A'}`);

        } catch (error) {
            console.error('[KANBAN] Erro ao enviar para jurídico:', error);
            alert('Não foi possível enviar o evento para o jurídico. Tente novamente.');
        } finally {
            if (triggerButton) {
                triggerButton.disabled = false;
                triggerButton.innerHTML = originalLabel || triggerButton.innerHTML;
            }

            resetLegalModalState();
            dragState.cardId = null;
            dragState.originStatus = null;
        }
    }

    function bindModalActions(card) {
        const modalBody = selectors.modalBody();
        if (!modalBody) {
            return;
        }

        const deleteButton = modalBody.querySelector('[data-action="delete-event"]');
        if (deleteButton) {
            deleteButton.addEventListener('click', () => {
                const confirmed = window.confirm('Tem certeza que deseja excluir este evento? Esta ação não poderá ser desfeita.');
                if (!confirmed) {
                    return;
                }

                deleteEvent(card.id, deleteButton);
            });
        }

        const sendLegalButton = modalBody.querySelector('[data-action="send-legal"]');
        if (sendLegalButton) {
            sendLegalButton.addEventListener('click', () => {
                sendEventToLegal(card.id, sendLegalButton);
            });
        }
    }

    async function deleteEvent(eventId, triggerButton) {
        if (!eventId) {
            return;
        }

        const button = triggerButton;
        const originalLabel = button ? button.innerHTML : null;

        try {
            if (button) {
                button.disabled = true;
                button.innerHTML = '<i class="bi bi-hourglass-split"></i> Removendo...';
            }

            const response = await fetch(`/events/api/${eventId}`, {
                method: 'DELETE',
                headers: buildHeaders({ 'Accept': 'application/json' })
            });

            if (response.status === 404) {
                alert('Evento não encontrado. Ele pode já ter sido removido.');
                return;
            }

            if (!response.ok) {
                throw new Error('Falha ao remover evento.');
            }

            state.cards = state.cards.filter(card => card.id !== eventId);
            render();
            closeModal();
            alert('Evento removido com sucesso!');
        } catch (error) {
            console.error('[KANBAN] Erro ao remover evento:', error);
            alert('Não foi possível remover o evento. Tente novamente mais tarde.');
        } finally {
            if (button) {
                button.disabled = false;
                button.innerHTML = originalLabel;
            }
        }
    }

    async function sendEventToLegal(eventId, triggerButton) {
        if (!eventId) {
            return;
        }

        dragState.pendingCardId = String(eventId);
        dragState.pendingPreviousStatus = null;
        dragState.pendingTargetStatus = null;

        if (triggerButton) {
            dragState.pendingTriggerButton = triggerButton;
            dragState.pendingTriggerButtonLabel = triggerButton.innerHTML;
        } else {
            dragState.pendingTriggerButton = null;
            dragState.pendingTriggerButtonLabel = null;
        }

        showLegalTypeModal();
    }

    function resetLegalModalState() {
        if (dragState.pendingTriggerButton) {
            dragState.pendingTriggerButton.disabled = false;
            if (dragState.pendingTriggerButtonLabel !== null) {
                dragState.pendingTriggerButton.innerHTML = dragState.pendingTriggerButtonLabel;
            }
        }

        dragState.pendingTriggerButton = null;
        dragState.pendingTriggerButtonLabel = null;
        dragState.pendingCardId = null;
        dragState.pendingTargetStatus = null;
        dragState.pendingPreviousStatus = null;
    }

    function buildModalContent(card) {
        // Formata hora no formato HH:MM
        const formatHora = (hora) => {
            if (!hora) return null;
            const horaStr = String(hora).padStart(4, '0');
            return `${horaStr.substring(0, 2)}:${horaStr.substring(2, 4)}`;
        };

        const fields = [
            { label: 'Título', value: card.titulo },
            { label: 'Descrição', value: card.descricao },
            { label: 'Status', value: card.statusLabel },
            { label: 'Prioridade', value: card.prioridadeLabel },
            { label: 'Associado', value: card.partnerName },
            { label: 'Placa', value: card.vehiclePlate || card.placaManual },
            { label: 'Motivo', value: card.motivoLabel },
            { label: 'Envolvimento', value: card.envolvimentoLabel },
            { label: 'Analista Responsável', value: card.analistaResponsavel },
            { label: 'Data do Ocorrido', value: card.dataAconteceu ? formatDate(card.dataAconteceu) : null },
            { label: 'Hora do Ocorrido', value: formatHora(card.horaAconteceu) },
            { label: 'Data da Comunicação', value: card.dataComunicacao ? formatDate(card.dataComunicacao) : null },
            { label: 'Hora da Comunicação', value: formatHora(card.horaComunicacao) },
            { label: 'Data de Vencimento', value: card.dataVencimento ? formatDate(card.dataVencimento) : null },
            { label: 'Observações', value: card.observacoes }
        ];

        const html = [
            '<div class="kanban-modal-grid">',
            ...fields
                .filter(field => Boolean(field.value))
                .map(field => `  <div class="kanban-modal-row"><span>${field.label}:</span><strong>${escapeHtml(field.value)}</strong></div>`),
            '</div>'
        ];

        html.push('<div class="kanban-modal-actions">');
        html.push(`<button type="button" class="btn-primary" data-action="send-legal" data-event-id="${card.id}"><i class="bi bi-send"></i> Enviar para Jurídico</button>`);
        html.push(`<button type="button" class="btn-danger" data-action="delete-event" data-event-id="${card.id}"><i class="bi bi-trash"></i> Excluir Evento</button>`);
        html.push('</div>');

        // Adicionar seção de documentos se houver algum documento anexado
        const documents = [
            { type: 'crlv', label: 'CRLV', has: card.hasCrlv },
            { type: 'cnh', label: 'CNH', has: card.hasCnh },
            { type: 'bo', label: 'B.O.', has: card.hasBo },
            { type: 'comprovante_residencia', label: 'Comprovante de Residência', has: card.hasComprovanteResidencia },
            { type: 'termo_abertura', label: 'Termo de Abertura', has: card.hasTermoAbertura }
        ];

        const availableDocs = documents.filter(doc => doc.has);

        if (availableDocs.length > 0) {
            html.push('<div class="kanban-modal-section" style="margin-top: 20px; padding-top: 20px; border-top: 1px solid var(--border-color, #dee2e6);">');
            html.push('<h5 style="margin-bottom: 12px; font-size: 14px; font-weight: 600; color: var(--text-primary, #2c3e50);">Documentos Anexados</h5>');
            html.push('<div style="display: flex; flex-direction: column; gap: 8px;">');

            availableDocs.forEach(doc => {
                const downloadUrl = `/events/${card.id}/download/${doc.type}`;
                html.push(`  <a href="${downloadUrl}" target="_blank" style="display: inline-flex; align-items: center; gap: 8px; padding: 8px 12px; background: var(--bg-secondary, #f8f9fa); border: 1px solid var(--border-color, #dee2e6); border-radius: 6px; color: var(--text-primary, #2c3e50); text-decoration: none; font-size: 13px; transition: all 0.2s;"><i class="bi bi-file-earmark-arrow-down" style="font-size: 16px;"></i><span>${escapeHtml(doc.label)}</span></a>`);
            });

            html.push('</div>');
            html.push('</div>');
        }

        // Adicionar seção de fotos da vistoria (será preenchida dinamicamente)
        html.push('<div id="vistoria-photos-section" class="kanban-modal-section" style="margin-top: 20px; padding-top: 20px; border-top: 1px solid var(--border-color, #dee2e6); display: none;">');
        html.push('<h5 style="margin-bottom: 12px; font-size: 14px; font-weight: 600; color: var(--text-primary, #2c3e50);"><i class="bi bi-camera-fill me-2"></i>Fotos da Vistoria</h5>');
        html.push('<div id="vistoria-photos-content" style="display: grid; grid-template-columns: repeat(auto-fill, minmax(150px, 1fr)); gap: 12px;"></div>');
        html.push('</div>');

        // Adicionar seção de histórico de descrições (será preenchida dinamicamente)
        html.push('<div id="description-history-section" class="kanban-modal-section" style="margin-top: 20px; padding-top: 20px; border-top: 1px solid var(--border-color, #dee2e6); display: none;">');
        html.push('<h5 style="margin-bottom: 12px; font-size: 14px; font-weight: 600; color: var(--text-primary, #2c3e50);">Histórico de Alterações da Descrição</h5>');
        html.push('<div id="description-history-content" style="display: flex; flex-direction: column; gap: 12px;"></div>');
        html.push('</div>');

        // Adicionar seção de histórico de observações (será preenchida dinamicamente)
        html.push('<div id="observation-history-section" class="kanban-modal-section" style="margin-top: 20px; padding-top: 20px; border-top: 1px solid var(--border-color, #dee2e6); display: none;">');
        html.push('<h5 style="margin-bottom: 12px; font-size: 14px; font-weight: 600; color: var(--text-primary, #2c3e50);">Histórico de Alterações das Observações</h5>');
        html.push('<div id="observation-history-content" style="display: flex; flex-direction: column; gap: 12px;"></div>');
        html.push('</div>');

        return html.join('\n');
    }

    async function loadDescriptionHistory(eventId) {
        try {
            const response = await fetch(`/events/${eventId}/description-history`, {
                headers: buildHeaders({ 'Accept': 'application/json' })
            });

            if (!response.ok) {
                console.warn('[KANBAN] Erro ao carregar histórico de descrições:', response.status);
                return;
            }

            const history = await response.json();

            if (!history || history.length === 0) {
                return; // Não mostra a seção se não houver histórico
            }

            const section = document.getElementById('description-history-section');
            const content = document.getElementById('description-history-content');

            if (!section || !content) {
                return;
            }

            // Limpa conteúdo anterior
            content.innerHTML = '';

            // Adiciona cada entrada do histórico
            history.forEach(entry => {
                const entryDiv = document.createElement('div');
                entryDiv.style.cssText = 'padding: 12px; background: var(--bg-secondary, #f8f9fa); border-left: 3px solid var(--success-color, #198754); border-radius: 4px;';

                const headerDiv = document.createElement('div');
                headerDiv.style.cssText = 'display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; font-size: 12px; color: var(--text-secondary, #6c757d);';

                const modifiedBy = document.createElement('span');
                modifiedBy.innerHTML = `<i class="bi bi-person"></i> <strong>${escapeHtml(entry.modifiedBy || 'Sistema')}</strong>`;

                const modifiedAt = document.createElement('span');
                const date = new Date(entry.modifiedAt);
                modifiedAt.innerHTML = `<i class="bi bi-clock"></i> ${date.toLocaleString('pt-BR')}`;

                headerDiv.appendChild(modifiedBy);
                headerDiv.appendChild(modifiedAt);

                const changesDiv = document.createElement('div');
                changesDiv.style.cssText = 'display: grid; grid-template-columns: 1fr; gap: 8px; font-size: 13px;';

                if (entry.previousDescription) {
                    const prevDiv = document.createElement('div');
                    prevDiv.innerHTML = `<strong style="color: var(--danger-color, #dc3545);">Anterior:</strong> <span style="color: var(--text-secondary, #6c757d); font-style: italic;">${escapeHtml(entry.previousDescription)}</span>`;
                    changesDiv.appendChild(prevDiv);
                }

                if (entry.newDescription) {
                    const newDiv = document.createElement('div');
                    newDiv.innerHTML = `<strong style="color: var(--success-color, #198754);">Nova:</strong> <span style="color: var(--text-primary, #2c3e50);">${escapeHtml(entry.newDescription)}</span>`;
                    changesDiv.appendChild(newDiv);
                }

                entryDiv.appendChild(headerDiv);
                entryDiv.appendChild(changesDiv);
                content.appendChild(entryDiv);
            });

            // Mostra a seção
            section.style.display = 'block';

        } catch (error) {
            console.error('[KANBAN] Erro ao carregar histórico de descrições:', error);
        }
    }

    async function loadObservationHistory(eventId) {
        try {
            const response = await fetch(`/events/${eventId}/observation-history`, {
                headers: buildHeaders({ 'Accept': 'application/json' })
            });

            if (!response.ok) {
                console.warn('[KANBAN] Erro ao carregar histórico de observações:', response.status);
                return;
            }

            const history = await response.json();

            if (!history || history.length === 0) {
                return; // Não mostra a seção se não houver histórico
            }

            const section = document.getElementById('observation-history-section');
            const content = document.getElementById('observation-history-content');

            if (!section || !content) {
                return;
            }

            // Limpa conteúdo anterior
            content.innerHTML = '';

            // Adiciona cada entrada do histórico
            history.forEach(entry => {
                const entryDiv = document.createElement('div');
                entryDiv.style.cssText = 'padding: 12px; background: var(--bg-secondary, #f8f9fa); border-left: 3px solid var(--primary-color, #0d6efd); border-radius: 4px;';

                const headerDiv = document.createElement('div');
                headerDiv.style.cssText = 'display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; font-size: 12px; color: var(--text-secondary, #6c757d);';

                const modifiedBy = document.createElement('span');
                modifiedBy.innerHTML = `<i class="bi bi-person"></i> <strong>${escapeHtml(entry.modifiedBy || 'Sistema')}</strong>`;

                const modifiedAt = document.createElement('span');
                const date = new Date(entry.modifiedAt);
                modifiedAt.innerHTML = `<i class="bi bi-clock"></i> ${date.toLocaleString('pt-BR')}`;

                headerDiv.appendChild(modifiedBy);
                headerDiv.appendChild(modifiedAt);

                const changesDiv = document.createElement('div');
                changesDiv.style.cssText = 'display: grid; grid-template-columns: 1fr; gap: 8px; font-size: 13px;';

                if (entry.previousObservation) {
                    const prevDiv = document.createElement('div');
                    prevDiv.innerHTML = `<strong style="color: var(--danger-color, #dc3545);">Anterior:</strong> <span style="color: var(--text-secondary, #6c757d); font-style: italic;">${escapeHtml(entry.previousObservation)}</span>`;
                    changesDiv.appendChild(prevDiv);
                }

                if (entry.newObservation) {
                    const newDiv = document.createElement('div');
                    newDiv.innerHTML = `<strong style="color: var(--success-color, #198754);">Nova:</strong> <span style="color: var(--text-primary, #2c3e50);">${escapeHtml(entry.newObservation)}</span>`;
                    changesDiv.appendChild(newDiv);
                }

                entryDiv.appendChild(headerDiv);
                entryDiv.appendChild(changesDiv);
                content.appendChild(entryDiv);
            });

            // Mostra a seção
            section.style.display = 'block';

        } catch (error) {
            console.error('[KANBAN] Erro ao carregar histórico de observações:', error);
        }
    }

    async function loadVistoriaPhotos(eventId) {
        try {
            console.log('[KANBAN] Carregando fotos da vistoria para evento ID:', eventId);
            const response = await fetch(`/vistorias/api/event/${eventId}`, {
                headers: buildHeaders({ 'Accept': 'application/json' })
            });

            if (!response.ok) {
                console.warn('[KANBAN] Erro ao carregar fotos da vistoria. Status:', response.status);
                return;
            }

            const vistorias = await response.json();
            console.log('[KANBAN] Vistorias recebidas:', vistorias);

            if (!vistorias || vistorias.length === 0) {
                console.log('[KANBAN] Nenhuma vistoria encontrada para o evento');
                return; // Não mostra a seção se não houver vistoria
            }

            const section = document.getElementById('vistoria-photos-section');
            const content = document.getElementById('vistoria-photos-content');

            if (!section || !content) {
                console.warn('[KANBAN] Elementos vistoria-photos-section ou vistoria-photos-content não encontrados');
                return;
            }

            // Limpa conteúdo anterior
            content.innerHTML = '';

            // Pega a vistoria mais recente (última do array)
            const vistoria = vistorias[vistorias.length - 1];
            console.log('[KANBAN] Vistoria selecionada:', vistoria);

            // Verifica se há fotos na vistoria
            const fotos = vistoria.fotos || [];
            console.log('[KANBAN] Número de fotos encontradas:', fotos.length);

            if (fotos.length === 0) {
                console.log('[KANBAN] Nenhuma foto encontrada na vistoria');
                return; // Não mostra a seção se não houver fotos
            }

            // Ordena fotos pela ordem
            fotos.sort((a, b) => (a.ordem || 0) - (b.ordem || 0));

            // Adiciona cada foto
            fotos.forEach(foto => {
                console.log('[KANBAN] Renderizando foto:', foto);
                const photoDiv = document.createElement('div');
                photoDiv.style.cssText = 'position: relative; overflow: hidden; border-radius: 8px; aspect-ratio: 1; background: var(--bg-secondary, #f8f9fa); border: 1px solid var(--border-color, #dee2e6); cursor: pointer;';

                const img = document.createElement('img');
                img.src = `/vistorias/${vistoria.id}/download/${foto.id}`;
                img.alt = `Foto ${foto.ordem || ''}`;
                img.style.cssText = 'width: 100%; height: 100%; object-fit: cover; transition: transform 0.2s;';

                console.log('[KANBAN] URL da foto:', img.src);

                // Efeito de hover
                photoDiv.addEventListener('mouseenter', () => {
                    img.style.transform = 'scale(1.05)';
                });
                photoDiv.addEventListener('mouseleave', () => {
                    img.style.transform = 'scale(1)';
                });

                // Abre a imagem em uma nova aba utilizando AJAX para evitar download automático
                photoDiv.addEventListener('click', () => {
                    openVistoriaPhotoPreview(vistoria.id, foto.id);
                });

                photoDiv.appendChild(img);
                content.appendChild(photoDiv);
            });

            // Adiciona observações da vistoria se existirem
            if (vistoria.observacoes) {
                const obsDiv = document.createElement('div');
                obsDiv.style.cssText = 'grid-column: 1 / -1; margin-top: 8px; padding: 12px; background: var(--bg-secondary, #f8f9fa); border-left: 3px solid var(--warning-color, #ffc107); border-radius: 4px; font-size: 13px;';
                obsDiv.innerHTML = `<strong>Observações da Vistoria:</strong><br>${escapeHtml(vistoria.observacoes)}`;
                content.appendChild(obsDiv);
            }

            // Mostra a seção
            section.style.display = 'block';
            console.log('[KANBAN] Seção de fotos da vistoria exibida com sucesso');

        } catch (error) {
            console.error('[KANBAN] Erro ao carregar fotos da vistoria:', error);
        }
    }

    async function openVistoriaPhotoPreview(vistoriaId, fotoId) {
        const photoUrl = `/vistorias/${vistoriaId}/download/${fotoId}`;

        try {
            const response = await fetch(photoUrl, {
                headers: buildHeaders({ 'Accept': 'image/*' })
            });

            if (!response.ok) {
                console.warn('[KANBAN] Não foi possível pré-visualizar a foto:', response.status);
                window.open(photoUrl, '_blank');
                return;
            }

            const blob = await response.blob();
            const objectUrl = URL.createObjectURL(blob);
            const previewWindow = window.open('', '_blank');

            if (!previewWindow) {
                console.warn('[KANBAN] Navegador bloqueou a abertura da nova aba. Fazendo download padrão.');
                window.open(photoUrl, '_blank');
                URL.revokeObjectURL(objectUrl);
                return;
            }

            previewWindow.document.write(`
                <!DOCTYPE html>
                <html lang="pt-BR">
                <head>
                    <meta charset="UTF-8">
                    <title>Foto da Vistoria</title>
                    <style>
                        body {
                            margin: 0;
                            background: #111;
                            display: flex;
                            align-items: center;
                            justify-content: center;
                            min-height: 100vh;
                        }
                        img {
                            max-width: 100vw;
                            max-height: 100vh;
                            object-fit: contain;
                        }
                    </style>
                </head>
                <body>
                    <img src="${objectUrl}" alt="Foto da Vistoria" />
                </body>
                </html>
            `);
            previewWindow.document.close();

            previewWindow.addEventListener('beforeunload', () => {
                URL.revokeObjectURL(objectUrl);
            });
        } catch (error) {
            console.error('[KANBAN] Erro ao pré-visualizar foto da vistoria:', error);
            window.open(photoUrl, '_blank');
        }
    }

    function escapeHtml(value) {
        return String(value)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
    }

    function openFiltersModal() {
        const modal = selectors.filtersModal();
        if (!modal) {
            return;
        }

        // Sincronizar o estado dos checkboxes com o state atual
        ['prioridade', 'status', 'envolvimento', 'motivo'].forEach(filterType => {
            const checkboxes = modal.querySelectorAll(`input[name="${filterType}"]`);
            checkboxes.forEach(checkbox => {
                checkbox.checked = state.advancedFilters[filterType].includes(checkbox.value);
            });
        });

        // Sincronizar datas
        const dateFrom = modal.querySelector('#filter-date-from');
        const dateTo = modal.querySelector('#filter-date-to');
        if (dateFrom) {
            dateFrom.value = state.advancedFilters.dateFrom || '';
        }
        if (dateTo) {
            dateTo.value = state.advancedFilters.dateTo || '';
        }

        // Sincronizar analista
        const analista = modal.querySelector('#filter-analista');
        if (analista) {
            analista.value = state.advancedFilters.analista || '';
        }

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

    function applyFiltersFromModal() {
        const modal = selectors.filtersModal();
        if (!modal) {
            return;
        }

        // Coletar valores dos checkboxes
        ['prioridade', 'status', 'envolvimento', 'motivo'].forEach(filterType => {
            const checkboxes = modal.querySelectorAll(`input[name="${filterType}"]:checked`);
            state.advancedFilters[filterType] = Array.from(checkboxes).map(cb => cb.value);
        });

        // Coletar datas
        const dateFrom = modal.querySelector('#filter-date-from');
        const dateTo = modal.querySelector('#filter-date-to');
        state.advancedFilters.dateFrom = dateFrom ? dateFrom.value : null;
        state.advancedFilters.dateTo = dateTo ? dateTo.value : null;

        // Coletar analista
        const analista = modal.querySelector('#filter-analista');
        state.advancedFilters.analista = analista ? analista.value.trim() : '';

        // Atualizar UI
        updateFilterBadge();
        render();
        closeFiltersModal();
    }

    function resetFiltersInModal() {
        const modal = selectors.filtersModal();
        if (!modal) {
            return;
        }

        // Resetar checkboxes - marcar todos
        ['prioridade', 'status', 'envolvimento', 'motivo'].forEach(filterType => {
            const checkboxes = modal.querySelectorAll(`input[name="${filterType}"]`);
            checkboxes.forEach(checkbox => {
                checkbox.checked = true;
            });
        });

        // Resetar datas
        const dateFrom = modal.querySelector('#filter-date-from');
        const dateTo = modal.querySelector('#filter-date-to');
        if (dateFrom) {
            dateFrom.value = '';
        }
        if (dateTo) {
            dateTo.value = '';
        }

        // Resetar analista
        const analista = modal.querySelector('#filter-analista');
        if (analista) {
            analista.value = '';
        }

        // Resetar state
        state.advancedFilters = {
            prioridade: [...PRIORIDADE_VALUES],
            status: [...STATUS_VALUES],
            envolvimento: [...ENVOLVIMENTO_VALUES],
            motivo: [...MOTIVO_VALUES],
            dateFrom: null,
            dateTo: null,
            analista: ''
        };

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

        // Contar filtros ativos
        const allPrioridades = PRIORIDADE_VALUES;
        const allStatus = STATUS_VALUES;
        const allEnvolvimentos = ENVOLVIMENTO_VALUES;
        const allMotivos = MOTIVO_VALUES;

        if (state.advancedFilters.prioridade.length < allPrioridades.length) {
            activeFiltersCount++;
        }
        if (state.advancedFilters.status.length < allStatus.length) {
            activeFiltersCount++;
        }
        if (state.advancedFilters.envolvimento.length < allEnvolvimentos.length) {
            activeFiltersCount++;
        }
        if (state.advancedFilters.motivo.length < allMotivos.length) {
            activeFiltersCount++;
        }
        if (state.advancedFilters.dateFrom) {
            activeFiltersCount++;
        }
        if (state.advancedFilters.dateTo) {
            activeFiltersCount++;
        }
        if (state.advancedFilters.analista) {
            activeFiltersCount++;
        }

        // Atualizar badge
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

        // Mostrar/ocultar botão de limpar filtros
        if (btnClearFilters) {
            btnClearFilters.style.display = activeFiltersCount > 0 ? 'inline-flex' : 'none';
        }
    }

    window.kanbanBoard = {
        init,
        closeModal,
        closeFiltersModal,
        closeLegalTypeModal,
        selectLegalType,
        applyFilters: applyFiltersFromModal,
        resetFilters: resetFiltersInModal
    };

    document.addEventListener('DOMContentLoaded', init);
})();
