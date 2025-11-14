(function () {
    console.log('[PROCESSOS-KANBAN] Script carregado com sucesso');

    const API_BASE = '/juridico/api/processos';
    const DEFAULT_TYPE = 'TERCEIROS';
    const STATUS_BY_TYPE = {
        TERCEIROS: ['EM_ABERTO_7_0', 'EM_CONTATO_7_1', 'PROCESSO_JUDICIAL_7_2', 'ACORDO_ASSINADO_7_3'],
        FIDELIDADE: ['FIDELIDADE_EM_ABERTO', 'FIDELIDADE_EM_CONTATO', 'FIDELIDADE_ACORDO_ASSINADO', 'FIDELIDADE_REATIVACAO'],
        RASTREADOR: ['RASTREADOR_EM_ABERTO', 'RASTREADOR_EM_CONTATO', 'RASTREADOR_ACORDO_ASSINADO', 'RASTREADOR_DEVOLVIDO', 'RASTREADOR_REATIVACAO']
    };

    const state = {
        cards: [],
        search: '',
        activeType: DEFAULT_TYPE
    };

    const dragState = {
        cardId: null,
        originStatus: null
    };

    const statusLabels = {
        EM_ABERTO_7_0: 'Em Aberto 7.0',
        EM_CONTATO_7_1: 'Em Contato 7.1',
        PROCESSO_JUDICIAL_7_2: 'Processo Judicial 7.2',
        ACORDO_ASSINADO_7_3: 'Acordo Assinado 7.3',
        FIDELIDADE_EM_ABERTO: 'Fidelidade - Em Aberto',
        FIDELIDADE_EM_CONTATO: 'Fidelidade - Em Contato',
        FIDELIDADE_ACORDO_ASSINADO: 'Fidelidade - Acordo Assinado',
        FIDELIDADE_REATIVACAO: 'Fidelidade - Reativação',
        RASTREADOR_EM_ABERTO: 'Rastreador - Em Aberto',
        RASTREADOR_EM_CONTATO: 'Rastreador - Em Contato',
        RASTREADOR_ACORDO_ASSINADO: 'Rastreador - Acordo Assinado',
        RASTREADOR_DEVOLVIDO: 'Rastreador - Devolvido',
        RASTREADOR_REATIVACAO: 'Rastreador - Reativação'
    };

    const typeLabels = {
        RASTREADOR: 'Rastreador',
        FIDELIDADE: 'Fidelidade',
        TERCEIROS: 'Terceiros'
    };

    const selectors = {
        board: () => document.querySelector('.kanban-board'),
        searchInput: () => document.getElementById('kanban-search'),
        typeFilter: () => document.getElementById('process-type-filter'),
        modal: () => document.getElementById('kanban-modal'),
        modalBody: () => document.querySelector('#kanban-modal .kanban-modal-body'),
        createModal: () => document.getElementById('create-processo-modal'),
        createForm: () => document.getElementById('create-processo-form'),
        createError: () => document.getElementById('create-processo-error'),
        createTypeSelect: () => document.getElementById('create-process-type')
    };

    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
    const STORAGE_KEYS = {
        activeType: 'processos-kanban.activeType'
    };

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

    function loadStoredActiveType() {
        try {
            const saved = window.localStorage?.getItem(STORAGE_KEYS.activeType);
            if (saved && STATUS_BY_TYPE[saved]) {
                return saved;
            }
        } catch (error) {
            console.warn('[PROCESSOS-KANBAN] Não foi possível carregar o tipo salvo:', error);
        }
        return DEFAULT_TYPE;
    }

    function persistActiveType(type) {
        try {
            window.localStorage?.setItem(STORAGE_KEYS.activeType, type);
        } catch (error) {
            console.warn('[PROCESSOS-KANBAN] Não foi possível salvar o tipo selecionado:', error);
        }
    }

    function init() {
        console.log('[PROCESSOS-KANBAN] Inicializando...');

        if (!selectors.board()) {
            console.error('[PROCESSOS-KANBAN] Board não encontrado!');
            return;
        }

        console.log('[PROCESSOS-KANBAN] Board encontrado, carregando eventos...');
        state.activeType = loadStoredActiveType();
        bindEvents();
        render();
        loadBoard();
    }

    function bindEvents() {
        const search = selectors.searchInput();
        if (search) {
            search.addEventListener('input', () => {
                state.search = search.value.toLowerCase();
                render();
            });
        }

        const typeFilter = selectors.typeFilter();
        if (typeFilter) {
            typeFilter.value = state.activeType;
            typeFilter.addEventListener('change', () => {
                const selected = typeFilter.value || DEFAULT_TYPE;
                state.activeType = STATUS_BY_TYPE[selected] ? selected : DEFAULT_TYPE;
                persistActiveType(state.activeType);
                render();
            });
        }

        const createForm = selectors.createForm();
        if (createForm) {
            createForm.addEventListener('submit', handleCreateSubmit);
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
            if (event.key === 'Escape') {
                closeCreateModal();
                closeModal();
            }
        });
    }

    async function loadBoard() {
        console.log('[PROCESSOS-KANBAN] Carregando processos...');
        try {
            const response = await fetch(API_BASE, {
                headers: buildHeaders({ 'Accept': 'application/json' })
            });

            if (!response.ok) {
                throw new Error('Erro ao carregar processos');
            }

            const data = await response.json();
            console.log('[PROCESSOS-KANBAN] Processos carregados:', data);

            if (Array.isArray(data)) {
                state.cards = data;
            } else {
                state.cards = [];
            }

            render();
        } catch (error) {
            console.error('[PROCESSOS-KANBAN] Erro ao carregar processos:', error);
            state.cards = [];
            render();
        }
    }

    function render() {
        const board = selectors.board();
        if (!board) {
            return;
        }

        const statuses = STATUS_BY_TYPE[state.activeType] || [];
        const filteredCards = applyFilters(state.cards);
        const grouped = groupByStatus(filteredCards, statuses);

        board.innerHTML = '';

        if (statuses.length === 0) {
            board.appendChild(createBoardEmptyState());
            return;
        }

        statuses.forEach(status => {
            const column = createColumn(status, grouped[status] || []);
            board.appendChild(column);
        });

        enableDragAndDrop();
    }

    function applyFilters(cards) {
        return cards.filter(card => {
            const processType = card.processType || DEFAULT_TYPE;
            if (processType !== state.activeType) {
                return false;
            }

            if (state.search) {
                const searchLower = state.search;
                const autor = (card.autor || '').toLowerCase();
                const reu = (card.reu || '').toLowerCase();
                const numeroProcesso = (card.numeroProcesso || '').toLowerCase();

                if (!autor.includes(searchLower) &&
                    !reu.includes(searchLower) &&
                    !numeroProcesso.includes(searchLower)) {
                    return false;
                }
            }

            return true;
        });
    }

    function groupByStatus(cards, statuses) {
        const grouped = {};
        statuses.forEach(status => {
            grouped[status] = [];
        });

        cards.forEach(card => {
            const status = card.status || statuses[0];
            if (!status) {
                return;
            }
            if (!grouped[status]) {
                grouped[status] = [];
            }
            grouped[status].push(card);
        });

        return grouped;
    }

    function createBoardEmptyState() {
        const container = document.createElement('div');
        container.className = 'kanban-empty-board';
        container.innerHTML = '<div class="empty-state"><i class="bi bi-kanban"></i> Nenhum fluxo configurado para exibir.</div>';
        return container;
    }

    function createColumn(status, items) {
        const column = document.createElement('div');
        column.className = 'kanban-column';
        column.dataset.status = status;
        column.dataset.type = state.activeType;

        const header = document.createElement('div');
        header.className = 'column-header';

        const title = document.createElement('div');
        title.className = 'column-title';

        const heading = document.createElement('h3');
        heading.textContent = statusLabels[status] || status;

        const counter = document.createElement('span');
        counter.className = 'column-count';
        counter.textContent = items.length;

        title.appendChild(heading);
        title.appendChild(counter);

        const addButton = document.createElement('button');
        addButton.className = 'column-add-btn';
        addButton.type = 'button';
        addButton.innerHTML = '<i class="bi bi-plus"></i> Nova';
        addButton.addEventListener('click', () => openCreateModal(state.activeType));

        header.appendChild(title);
        header.appendChild(addButton);

        const container = document.createElement('div');
        container.className = 'tasks-container';

        if (items.length === 0) {
            container.appendChild(createEmptyState());
        } else {
            items.forEach(card => container.appendChild(createCard(card)));
        }

        column.appendChild(header);
        column.appendChild(container);

        bindColumnDropZone(column);

        return column;
    }

    function createCard(card) {
        const article = document.createElement('article');
        article.className = 'task-card';
        article.dataset.id = card.id;

        const header = document.createElement('div');
        header.className = 'task-card-header';

        const title = document.createElement('h4');
        title.textContent = card.numeroProcesso || 'Sem número';
        header.appendChild(title);

        if (card.processType) {
            const badges = document.createElement('div');
            badges.className = 'task-card-badges';

            const badge = document.createElement('span');
            badge.className = 'badge';
            badge.textContent = typeLabels[card.processType] || card.processType;
            badges.appendChild(badge);

            header.appendChild(badges);
        }

        article.appendChild(header);

        const body = document.createElement('div');
        body.className = 'task-card-body';

        const autorP = document.createElement('p');
        autorP.innerHTML = '<strong>Autor:</strong> ' + escapeHtml(card.autor || 'N/A');
        body.appendChild(autorP);

        const reuP = document.createElement('p');
        reuP.innerHTML = '<strong>Réu:</strong> ' + escapeHtml(card.reu || 'N/A');
        body.appendChild(reuP);

        const materiaP = document.createElement('p');
        materiaP.innerHTML = '<strong>Matéria:</strong> ' + escapeHtml(card.materia || 'N/A');
        body.appendChild(materiaP);

        const statusP = document.createElement('p');
        statusP.innerHTML = '<strong>Status:</strong> ' + escapeHtml(statusLabels[card.status] || card.status || 'N/A');
        body.appendChild(statusP);

        if (card.valorCausa) {
            const valorP = document.createElement('p');
            valorP.innerHTML = '<strong>Valor:</strong> R$ ' + formatCurrency(card.valorCausa);
            body.appendChild(valorP);
        }

        article.appendChild(body);

        article.addEventListener('click', () => openModal(card));

        return article;
    }

    function createEmptyState() {
        const div = document.createElement('div');
        div.className = 'empty-state';
        div.innerHTML = '<i class="bi bi-inbox"></i> Nenhum processo aqui ainda';
        return div;
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

        if (!cardId || !targetStatus) {
            return;
        }

        const allowedStatuses = STATUS_BY_TYPE[state.activeType] || [];
        if (!allowedStatuses.includes(targetStatus)) {
            return;
        }

        const card = state.cards.find(item => String(item.id) === String(cardId));
        if (!card) {
            return;
        }

        if ((card.processType || DEFAULT_TYPE) !== state.activeType) {
            return;
        }

        const previousStatus = card.status;
        if (previousStatus === targetStatus) {
            return;
        }

        // Atualização otimista
        card.status = targetStatus;
        card.processType = state.activeType;
        render();

        try {
            const response = await fetch(API_BASE + '/' + cardId + '/status', {
                method: 'PUT',
                headers: buildHeaders({
                    'Content-Type': 'application/json',
                    'Accept': 'application/json'
                }),
                body: JSON.stringify({ status: targetStatus })
            });

            if (!response.ok) {
                throw new Error('Erro ao atualizar status');
            }

            console.log('[PROCESSOS-KANBAN] Status atualizado com sucesso');
        } catch (error) {
            console.error('[PROCESSOS-KANBAN] Erro ao atualizar status:', error);
            // Reverte em caso de erro
            card.status = previousStatus;
            render();
            alert('Não foi possível atualizar o status do processo.');
        } finally {
            dragState.cardId = null;
            dragState.originStatus = null;
        }
    }

    function openModal(card) {
        const modal = selectors.modal();
        const modalBody = selectors.modalBody();

        if (!modal || !modalBody) {
            return;
        }

        const eventData = parseEventSnapshot(card);
        const sections = [];

        if (eventData) {
            sections.push(buildEventDetailsSection(eventData, card.sourceEventId));
        }

        sections.push(buildProcessDetailsSection(card));

        modalBody.innerHTML = sections.join('');

        if (eventData && card.sourceEventId) {
            loadEventHistories(card.sourceEventId);
        }

        modal.classList.add('active');
        document.body.classList.add('kanban-modal-open');
    }

    function closeModal() {
        const modal = selectors.modal();
        if (modal) {
            modal.classList.remove('active');
        }
        document.body.classList.remove('kanban-modal-open');
    }

    function openCreateModal(defaultType) {
        console.log('[PROCESSOS-KANBAN] Abrindo modal de criação');

        const modal = selectors.createModal();
        const form = selectors.createForm();

        if (!modal) {
            console.error('[PROCESSOS-KANBAN] Modal de criação não encontrado!');
            return;
        }

        showCreateError('');

        const initialType = STATUS_BY_TYPE[defaultType] ? defaultType : state.activeType || DEFAULT_TYPE;

        if (form) {
            form.reset();
            delete form.dataset.editingId;

            const typeSelect = selectors.createTypeSelect();
            if (typeSelect) {
                typeSelect.value = initialType;
            }
        }

        modal.classList.add('active');
        document.body.classList.add('kanban-modal-open');

        console.log('[PROCESSOS-KANBAN] Modal aberto');
    }

    function closeCreateModal() {
        const modal = selectors.createModal();
        const form = selectors.createForm();

        if (modal) {
            modal.classList.remove('active');
        }

        if (form) {
            form.reset();
            delete form.dataset.editingId;

            // Restaura o título do modal
            const modalHeader = modal.querySelector('.kanban-modal-header h2');
            if (modalHeader) {
                modalHeader.innerHTML = '<i class="bi bi-plus-circle"></i> Novo Processo';
            }

            // Restaura o texto do botão de submit
            const submitBtn = form.querySelector('button[type="submit"]');
            if (submitBtn) {
                submitBtn.innerHTML = '<i class="bi bi-send-fill"></i> Registrar';
            }

            const typeSelect = selectors.createTypeSelect();
            if (typeSelect) {
                typeSelect.value = state.activeType || DEFAULT_TYPE;
            }
        }

        showCreateError('');
        document.body.classList.remove('kanban-modal-open');
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
        console.log('[PROCESSOS-KANBAN] Formulário submetido');

        const form = event.currentTarget;
        if (!form) {
            return;
        }

        const isEditing = !!form.dataset.editingId;
        const editingId = form.dataset.editingId;

        const data = new FormData(form);
        const autor = data.get('autor')?.toString().trim();
        const reu = data.get('reu')?.toString().trim();
        const materia = data.get('materia')?.toString().trim();
        const numeroProcesso = data.get('numeroProcesso')?.toString().trim();
        const pedidos = data.get('pedidos')?.toString().trim();
        const valorCausaStr = data.get('valorCausa')?.toString().replace(',', '.');
        const valorCausa = parseFloat(valorCausaStr);
        const processType = data.get('processType')?.toString();

        if (!autor || !reu || !materia || !numeroProcesso || !pedidos) {
            showCreateError('Todos os campos são obrigatórios.');
            return;
        }

        if (!valorCausa || valorCausa < 0) {
            showCreateError('Informe um valor da causa válido.');
            return;
        }

        if (!processType) {
            showCreateError('Selecione o tipo de cobrança.');
            return;
        }

        const payload = {
            autor: autor,
            reu: reu,
            materia: materia,
            numeroProcesso: numeroProcesso,
            valorCausa: valorCausa,
            pedidos: pedidos,
            processType: processType
        };

        console.log('[PROCESSOS-KANBAN] Enviando payload:', payload, 'Edição:', isEditing);

        try {
            const url = isEditing ? API_BASE + '/' + editingId : API_BASE;
            const method = isEditing ? 'PUT' : 'POST';

            const response = await fetch(url, {
                method: method,
                headers: buildHeaders({
                    'Content-Type': 'application/json',
                    'Accept': 'application/json'
                }),
                body: JSON.stringify(payload)
            });

            console.log('[PROCESSOS-KANBAN] Response status:', response.status);

            if (!response.ok) {
                let message = isEditing ? 'Não foi possível atualizar o processo.' : 'Não foi possível criar o processo.';
                try {
                    const errorData = await response.json();
                    message = errorData.message || errorData.error || message;
                } catch (e) {
                    const textError = await response.text();
                    console.error('[PROCESSOS-KANBAN] Erro:', textError);
                }
                showCreateError(message);
                return;
            }

            const savedProcess = await response.json();
            console.log('[PROCESSOS-KANBAN] Processo salvo:', savedProcess);

            if (isEditing) {
                // Atualiza o processo no estado
                const index = state.cards.findIndex(c => c.id === parseInt(editingId, 10));
                if (index !== -1) {
                    state.cards[index] = savedProcess;
                }
            } else {
                // Adiciona novo processo ao início
                state.cards.unshift(savedProcess);
            }

            const updatedType = savedProcess.processType || processType;
            if (updatedType && state.activeType !== updatedType && STATUS_BY_TYPE[updatedType]) {
                state.activeType = updatedType;
                persistActiveType(state.activeType);
                const typeFilter = selectors.typeFilter();
                if (typeFilter) {
                    typeFilter.value = state.activeType;
                }
            }

            render();
            closeCreateModal();

            if (isEditing) {
                alert('Processo atualizado com sucesso!');
            }

        } catch (error) {
            console.error('[PROCESSOS-KANBAN] Erro ao salvar processo:', error);
            showCreateError('Ocorreu um erro ao salvar o processo. Tente novamente.');
        }
    }

    function openEditModal(processId) {
        console.log('[PROCESSOS-KANBAN] Abrindo modal de edição para processo:', processId);

        const card = state.cards.find(c => c.id === processId);
        if (!card) {
            console.error('[PROCESSOS-KANBAN] Processo não encontrado:', processId);
            return;
        }

        closeModal();

        const modal = selectors.createModal();
        const form = selectors.createForm();

        if (!modal || !form) {
            console.error('[PROCESSOS-KANBAN] Modal ou form não encontrado!');
            return;
        }

        // Preenche o formulário com os dados do processo
        form.querySelector('#create-autor').value = card.autor || '';
        form.querySelector('#create-reu').value = card.reu || '';
        form.querySelector('#create-materia').value = card.materia || '';
        form.querySelector('#create-numero-processo').value = card.numeroProcesso || '';
        form.querySelector('#create-valor-causa').value = card.valorCausa || '';
        form.querySelector('#create-pedidos').value = card.pedidos || '';
        const typeSelect = selectors.createTypeSelect();
        if (typeSelect) {
            typeSelect.value = card.processType || '';
        }

        // Marca o formulário como edição
        form.dataset.editingId = processId;

        // Muda o título do modal
        const modalHeader = modal.querySelector('.kanban-modal-header h2');
        if (modalHeader) {
            modalHeader.innerHTML = '<i class="bi bi-pencil"></i> Editar Processo';
        }

        // Muda o texto do botão de submit
        const submitBtn = form.querySelector('button[type="submit"]');
        if (submitBtn) {
            submitBtn.innerHTML = '<i class="bi bi-save"></i> Salvar';
        }

        showCreateError('');
        modal.classList.add('active');
        document.body.classList.add('kanban-modal-open');

        console.log('[PROCESSOS-KANBAN] Modal de edição aberto');
    }

    async function deleteProcess(processId) {
        console.log('[PROCESSOS-KANBAN] Solicitando exclusão do processo:', processId);

        const card = state.cards.find(c => c.id === processId);
        if (!card) {
            console.error('[PROCESSOS-KANBAN] Processo não encontrado:', processId);
            return;
        }

        const confirmMsg = 'Tem certeza que deseja excluir o processo "' + card.numeroProcesso + '"?\n\nEsta ação não pode ser desfeita.';
        if (!confirm(confirmMsg)) {
            console.log('[PROCESSOS-KANBAN] Exclusão cancelada pelo usuário');
            return;
        }

        try {
            const response = await fetch(API_BASE + '/' + processId, {
                method: 'DELETE',
                headers: buildHeaders({
                    'Accept': 'application/json'
                })
            });

            if (!response.ok) {
                throw new Error('Erro ao deletar processo');
            }

            console.log('[PROCESSOS-KANBAN] Processo deletado com sucesso');

            // Remove do estado local
            state.cards = state.cards.filter(c => c.id !== processId);

            closeModal();
            render();

            alert('Processo excluído com sucesso!');

        } catch (error) {
            console.error('[PROCESSOS-KANBAN] Erro ao deletar processo:', error);
            alert('Não foi possível excluir o processo. Tente novamente.');
        }
    }

    function parseEventSnapshot(card) {
        if (!card || !card.sourceEventSnapshot) {
            return null;
        }

        if (typeof card.sourceEventSnapshot === 'object') {
            return card.sourceEventSnapshot;
        }

        try {
            return JSON.parse(card.sourceEventSnapshot);
        } catch (error) {
            console.warn('[PROCESSOS-KANBAN] Não foi possível interpretar o snapshot do evento:', error);
            return null;
        }
    }

    function buildEventDetailsSection(eventData, sourceEventId) {
        const fields = [
            { label: 'Título', value: eventData.titulo },
            { label: 'Descrição', value: eventData.descricao },
            { label: 'Status', value: eventData.statusLabel },
            { label: 'Prioridade', value: eventData.prioridadeLabel },
            { label: 'Associado', value: eventData.partnerName },
            { label: 'Placa', value: eventData.vehiclePlate || eventData.placaManual },
            { label: 'Motivo', value: eventData.motivoLabel },
            { label: 'Envolvimento', value: eventData.envolvimentoLabel },
            { label: 'Analista Responsável', value: eventData.analistaResponsavel },
            { label: 'Data do Ocorrido', value: formatDate(eventData.dataAconteceu) },
            { label: 'Hora do Ocorrido', value: formatTime(eventData.horaAconteceu) },
            { label: 'Data da Comunicação', value: formatDate(eventData.dataComunicacao) },
            { label: 'Hora da Comunicação', value: formatTime(eventData.horaComunicacao) },
            { label: 'Data de Vencimento', value: formatDate(eventData.dataVencimento) },
            { label: 'Observações', value: eventData.observacoes }
        ];

        const html = [];
        html.push('<div class="kanban-modal-section">');
        html.push('<h3 style="margin-bottom: 16px; font-size: 16px; font-weight: 600;">Dados do Evento enviado ao Jurídico</h3>');
        html.push('<div class="kanban-modal-grid">');

        fields
            .filter(field => field.value)
            .forEach(field => {
                html.push(`<div class="kanban-modal-row"><span>${field.label}:</span><strong>${escapeHtml(String(field.value))}</strong></div>`);
            });

        html.push('</div>');

        const documents = [
            { type: 'crlv', label: 'CRLV', has: eventData.hasCrlv },
            { type: 'cnh', label: 'CNH', has: eventData.hasCnh },
            { type: 'bo', label: 'B.O.', has: eventData.hasBo },
            { type: 'comprovante_residencia', label: 'Comprovante de Residência', has: eventData.hasComprovanteResidencia },
            { type: 'termo_abertura', label: 'Termo de Abertura', has: eventData.hasTermoAbertura }
        ];

        const availableDocs = documents.filter(doc => doc.has && sourceEventId);
        if (availableDocs.length > 0) {
            html.push('<div class="kanban-modal-section" style="margin-top: 20px; padding-top: 20px; border-top: 1px solid var(--border-color, #dee2e6);">');
            html.push('<h5 style="margin-bottom: 12px; font-size: 14px; font-weight: 600;">Documentos Anexados</h5>');
            html.push('<div style="display: flex; flex-direction: column; gap: 8px;">');
            availableDocs.forEach(doc => {
                const downloadUrl = `/events/${sourceEventId}/download/${doc.type}`;
                html.push(`<a href="${downloadUrl}" target="_blank" style="display: inline-flex; align-items: center; gap: 8px; padding: 8px 12px; background: var(--bg-secondary, #f8f9fa); border: 1px solid var(--border-color, #dee2e6); border-radius: 6px; color: var(--text-primary, #2c3e50); text-decoration: none; font-size: 13px;"><i class="bi bi-file-earmark-arrow-down" style="font-size: 16px;"></i><span>${escapeHtml(doc.label)}</span></a>`);
            });
            html.push('</div>');
            html.push('</div>');
        }

        html.push('<div id="legal-description-history-section" class="kanban-modal-section" style="margin-top: 20px; padding-top: 20px; border-top: 1px solid var(--border-color, #dee2e6); display: none;">');
        html.push('<h5 style="margin-bottom: 12px; font-size: 14px; font-weight: 600;">Histórico de Alterações da Descrição</h5>');
        html.push('<div id="legal-description-history-content" style="display: flex; flex-direction: column; gap: 12px;"></div>');
        html.push('</div>');

        html.push('<div id="legal-observation-history-section" class="kanban-modal-section" style="margin-top: 20px; padding-top: 20px; border-top: 1px solid var(--border-color, #dee2e6); display: none;">');
        html.push('<h5 style="margin-bottom: 12px; font-size: 14px; font-weight: 600;">Histórico de Alterações das Observações</h5>');
        html.push('<div id="legal-observation-history-content" style="display: flex; flex-direction: column; gap: 12px;"></div>');
        html.push('</div>');

        html.push('</div>');
        return html.join('');
    }

    function buildProcessDetailsSection(card) {
        const html = [];
        html.push('<div class="detail-section">');
        html.push('<h3>Informações do Processo</h3>');
        html.push('<p><strong>Número:</strong> ' + escapeHtml(card.numeroProcesso || 'N/A') + '</p>');
        html.push('<p><strong>Autor:</strong> ' + escapeHtml(card.autor || 'N/A') + '</p>');
        html.push('<p><strong>Réu:</strong> ' + escapeHtml(card.reu || 'N/A') + '</p>');
        html.push('<p><strong>Matéria:</strong> ' + escapeHtml(card.materia || 'N/A') + '</p>');
        html.push('<p><strong>Valor da Causa:</strong> R$ ' + formatCurrency(card.valorCausa || 0) + '</p>');
        html.push('<p><strong>Status:</strong> ' + escapeHtml(statusLabels[card.status] || card.status || 'N/A') + '</p>');
        html.push('</div>');

        html.push('<div class="detail-section"><h3>Pedidos</h3>');
        html.push('<p>' + escapeHtml(card.pedidos || 'Nenhum pedido registrado') + '</p></div>');

        html.push('<div class="modal-actions">');
        html.push('<button type="button" class="btn btn-primary" onclick="window.processosBoard.openEditModal(' + card.id + ')"><i class="bi bi-pencil"></i> Editar</button>');
        html.push('<button type="button" class="btn btn-danger" onclick="window.processosBoard.deleteProcess(' + card.id + ')"><i class="bi bi-trash"></i> Deletar</button>');
        html.push('</div>');

        return html.join('');
    }

    async function loadEventHistories(eventId) {
        await Promise.allSettled([
            loadEventDescriptionHistory(eventId),
            loadEventObservationHistory(eventId)
        ]);
    }

    async function loadEventDescriptionHistory(eventId) {
        const section = document.getElementById('legal-description-history-section');
        const content = document.getElementById('legal-description-history-content');
        if (!section || !content) {
            return;
        }

        try {
            const response = await fetch(`/events/${eventId}/description-history`, {
                headers: buildHeaders({ 'Accept': 'application/json' })
            });

            if (!response.ok) {
                return;
            }

            const history = await response.json();
            if (!Array.isArray(history) || history.length === 0) {
                return;
            }

            content.innerHTML = '';
            history.forEach(entry => {
                content.appendChild(createHistoryEntry({
                    previous: entry.previousDescription,
                    current: entry.newDescription,
                    modifiedBy: entry.modifiedBy,
                    modifiedAt: entry.modifiedAt
                }));
            });

            section.style.display = 'block';
        } catch (error) {
            console.error('[PROCESSOS-KANBAN] Erro ao carregar histórico de descrições do evento:', error);
        }
    }

    async function loadEventObservationHistory(eventId) {
        const section = document.getElementById('legal-observation-history-section');
        const content = document.getElementById('legal-observation-history-content');
        if (!section || !content) {
            return;
        }

        try {
            const response = await fetch(`/events/${eventId}/observation-history`, {
                headers: buildHeaders({ 'Accept': 'application/json' })
            });

            if (!response.ok) {
                return;
            }

            const history = await response.json();
            if (!Array.isArray(history) || history.length === 0) {
                return;
            }

            content.innerHTML = '';
            history.forEach(entry => {
                content.appendChild(createHistoryEntry({
                    previous: entry.previousObservation,
                    current: entry.newObservation,
                    modifiedBy: entry.modifiedBy,
                    modifiedAt: entry.modifiedAt
                }));
            });

            section.style.display = 'block';
        } catch (error) {
            console.error('[PROCESSOS-KANBAN] Erro ao carregar histórico de observações do evento:', error);
        }
    }

    function createHistoryEntry(entry) {
        const entryDiv = document.createElement('div');
        entryDiv.style.cssText = 'padding: 12px; background: var(--bg-secondary, #f8f9fa); border-left: 3px solid var(--success-color, #198754); border-radius: 4px;';

        const headerDiv = document.createElement('div');
        headerDiv.style.cssText = 'display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; font-size: 12px; color: var(--text-secondary, #6c757d);';

        const modifiedBySpan = document.createElement('span');
        modifiedBySpan.innerHTML = `<i class="bi bi-person"></i> <strong>${escapeHtml(entry.modifiedBy || 'Sistema')}</strong>`;

        const modifiedAtSpan = document.createElement('span');
        const modifiedDate = entry.modifiedAt ? new Date(entry.modifiedAt) : null;
        modifiedAtSpan.innerHTML = `<i class="bi bi-clock"></i> ${modifiedDate ? modifiedDate.toLocaleString('pt-BR') : 'Data não disponível'}`;

        headerDiv.appendChild(modifiedBySpan);
        headerDiv.appendChild(modifiedAtSpan);

        const changesDiv = document.createElement('div');
        changesDiv.style.cssText = 'display: grid; grid-template-columns: 1fr; gap: 8px; font-size: 13px;';

        if (entry.previous) {
            const previousDiv = document.createElement('div');
            previousDiv.innerHTML = `<strong style="color: var(--danger-color, #dc3545);">Anterior:</strong> <span style="color: var(--text-secondary, #6c757d); font-style: italic;">${escapeHtml(entry.previous)}</span>`;
            changesDiv.appendChild(previousDiv);
        }

        if (entry.current) {
            const currentDiv = document.createElement('div');
            currentDiv.innerHTML = `<strong style="color: var(--success-color, #198754);">Nova:</strong> <span style="color: var(--text-primary, #2c3e50);">${escapeHtml(entry.current)}</span>`;
            changesDiv.appendChild(currentDiv);
        }

        entryDiv.appendChild(headerDiv);
        entryDiv.appendChild(changesDiv);
        return entryDiv;
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

    function formatTime(value) {
        if (!value && value !== 0) {
            return '';
        }

        const numeric = Number(value);
        if (Number.isNaN(numeric)) {
            return '';
        }

        const padded = String(Math.trunc(numeric)).padStart(4, '0');
        return `${padded.substring(0, 2)}:${padded.substring(2, 4)}`;
    }

    function formatCurrency(value) {
        return new Intl.NumberFormat('pt-BR', {
            minimumFractionDigits: 2,
            maximumFractionDigits: 2
        }).format(value);
    }

    function escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    document.addEventListener('DOMContentLoaded', () => {
        console.log('[PROCESSOS-KANBAN] DOM carregado, inicializando...');

        window.processosBoard = {
            openCreateModal: (type) => openCreateModal(type ?? state.activeType),
            closeCreateModal: closeCreateModal,
            closeModal: closeModal,
            openEditModal: openEditModal,
            deleteProcess: deleteProcess
        };

        console.log('[PROCESSOS-KANBAN] window.processosBoard criado:', window.processosBoard);

        init();
    });
})();
