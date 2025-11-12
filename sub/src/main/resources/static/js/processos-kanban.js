(function () {
    console.log('[PROCESSOS-KANBAN] Script carregado com sucesso');

    const API_BASE = '/juridico/api/processos';
    const ALL_STATUS = ['EM_ABERTO_7_0', 'EM_CONTATO_7_1', 'PROCESSO_JUDICIAL_7_2', 'ACORDO_ASSINADO_7_3'];

    const state = {
        cards: [],
        search: ''
    };

    const dragState = {
        cardId: null,
        originStatus: null
    };

    const statusLabels = {
        EM_ABERTO_7_0: 'Em Aberto 7.0',
        EM_CONTATO_7_1: 'Em Contato 7.1',
        PROCESSO_JUDICIAL_7_2: 'Processo Judicial 7.2',
        ACORDO_ASSINADO_7_3: 'Acordo Assinado 7.3'
    };

    const selectors = {
        board: () => document.querySelector('.kanban-board'),
        columns: () => Array.from(document.querySelectorAll('.kanban-column')),
        searchInput: () => document.getElementById('kanban-search'),
        modal: () => document.getElementById('kanban-modal'),
        modalBody: () => document.querySelector('#kanban-modal .kanban-modal-body'),
        createModal: () => document.getElementById('create-processo-modal'),
        createForm: () => document.getElementById('create-processo-form'),
        createError: () => document.getElementById('create-processo-error')
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
        console.log('[PROCESSOS-KANBAN] Inicializando...');

        if (!selectors.board()) {
            console.error('[PROCESSOS-KANBAN] Board não encontrado!');
            return;
        }

        console.log('[PROCESSOS-KANBAN] Board encontrado, carregando eventos...');
        bindEvents();
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

    function applyFilters(cards) {
        return cards.filter(card => {
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

    function groupByStatus(cards) {
        const grouped = {};
        ALL_STATUS.forEach(status => {
            grouped[status] = [];
        });

        cards.forEach(card => {
            const status = card.status || 'EM_ABERTO_7_0';
            if (grouped[status]) {
                grouped[status].push(card);
            }
        });

        return grouped;
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

        const card = state.cards.find(item => String(item.id) === String(cardId));
        if (!card) {
            return;
        }

        const previousStatus = card.status;
        if (previousStatus === targetStatus) {
            return;
        }

        // Atualização otimista
        card.status = targetStatus;
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

        modalBody.innerHTML = '<div class="detail-section"><h3>Informações do Processo</h3>' +
            '<p><strong>Número:</strong> ' + escapeHtml(card.numeroProcesso || 'N/A') + '</p>' +
            '<p><strong>Autor:</strong> ' + escapeHtml(card.autor || 'N/A') + '</p>' +
            '<p><strong>Réu:</strong> ' + escapeHtml(card.reu || 'N/A') + '</p>' +
            '<p><strong>Matéria:</strong> ' + escapeHtml(card.materia || 'N/A') + '</p>' +
            '<p><strong>Valor da Causa:</strong> R$ ' + formatCurrency(card.valorCausa || 0) + '</p>' +
            '<p><strong>Status:</strong> ' + (statusLabels[card.status] || card.status) + '</p>' +
            '</div><div class="detail-section"><h3>Pedidos</h3>' +
            '<p>' + escapeHtml(card.pedidos || 'Nenhum pedido registrado') + '</p></div>';

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

    function openCreateModal() {
        console.log('[PROCESSOS-KANBAN] Abrindo modal de criação');

        const modal = selectors.createModal();
        const form = selectors.createForm();

        if (!modal) {
            console.error('[PROCESSOS-KANBAN] Modal de criação não encontrado!');
            return;
        }

        showCreateError('');

        if (form) {
            form.reset();
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

        const data = new FormData(form);
        const autor = data.get('autor')?.toString().trim();
        const reu = data.get('reu')?.toString().trim();
        const materia = data.get('materia')?.toString().trim();
        const numeroProcesso = data.get('numeroProcesso')?.toString().trim();
        const pedidos = data.get('pedidos')?.toString().trim();
        const valorCausaStr = data.get('valorCausa')?.toString().replace(',', '.');
        const valorCausa = parseFloat(valorCausaStr);

        if (!autor || !reu || !materia || !numeroProcesso || !pedidos) {
            showCreateError('Todos os campos são obrigatórios.');
            return;
        }

        if (!valorCausa || valorCausa < 0) {
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

        console.log('[PROCESSOS-KANBAN] Enviando payload:', payload);

        try {
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
                let message = 'Não foi possível criar o processo.';
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

            const created = await response.json();
            console.log('[PROCESSOS-KANBAN] Processo criado:', created);

            state.cards.unshift(created);
            render();
            closeCreateModal();

        } catch (error) {
            console.error('[PROCESSOS-KANBAN] Erro ao criar processo:', error);
            showCreateError('Ocorreu um erro ao criar o processo. Tente novamente.');
        }
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
            openCreateModal: openCreateModal,
            closeCreateModal: closeCreateModal,
            closeModal: closeModal
        };

        console.log('[PROCESSOS-KANBAN] window.processosBoard criado:', window.processosBoard);

        init();
    });
})();
