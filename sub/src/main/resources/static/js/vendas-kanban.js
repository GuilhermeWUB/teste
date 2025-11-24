(function () {
    console.log('[VENDAS-KANBAN] Script carregado com sucesso');

    const API_BASE = '/crm/api/vendas';
    const STATUSES = ['NOVO_LEAD', 'CONTATO_INICIAL', 'PROPOSTA_ENVIADA', 'NEGOCIACAO', 'FECHADO', 'PERDIDO'];

    const state = {
        cards: [],
        search: ''
    };

    const dragState = {
        cardId: null,
        originStatus: null
    };

    const statusLabels = {
        NOVO_LEAD: 'Novo Lead',
        CONTATO_INICIAL: 'Contato Inicial',
        PROPOSTA_ENVIADA: 'Proposta Enviada',
        NEGOCIACAO: 'Negociação',
        FECHADO: 'Fechado',
        PERDIDO: 'Perdido'
    };

    const statusColors = {
        NOVO_LEAD: '#6366f1',
        CONTATO_INICIAL: '#3b82f6',
        PROPOSTA_ENVIADA: '#f59e0b',
        NEGOCIACAO: '#8b5cf6',
        FECHADO: '#10b981',
        PERDIDO: '#ef4444'
    };

    const selectors = {
        board: () => document.querySelector('.kanban-board'),
        searchInput: () => document.getElementById('kanban-search'),
        modal: () => document.getElementById('kanban-modal'),
        modalBody: () => document.querySelector('#kanban-modal .kanban-modal-body'),
        createModal: () => document.getElementById('create-venda-modal'),
        createForm: () => document.getElementById('create-venda-form'),
        createError: () => document.getElementById('create-venda-error')
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
        console.log('[VENDAS-KANBAN] Inicializando...');

        if (!selectors.board()) {
            console.error('[VENDAS-KANBAN] Board nao encontrado!');
            return;
        }

        console.log('[VENDAS-KANBAN] Board encontrado, carregando vendas...');
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
        console.log('[VENDAS-KANBAN] Carregando vendas...');
        try {
            const response = await fetch(API_BASE, {
                headers: buildHeaders({ 'Accept': 'application/json' })
            });

            if (!response.ok) {
                throw new Error('Erro ao carregar vendas');
            }

            const data = await response.json();
            console.log('[VENDAS-KANBAN] Vendas carregadas:', data);

            if (Array.isArray(data)) {
                state.cards = data;
            } else {
                state.cards = [];
            }

            render();
        } catch (error) {
            console.error('[VENDAS-KANBAN] Erro ao carregar vendas:', error);
            state.cards = [];
            render();
        }
    }

    function render() {
        const board = selectors.board();
        if (!board) {
            return;
        }

        const filteredCards = applyFilters(state.cards);
        const grouped = groupByStatus(filteredCards);

        board.innerHTML = '';

        STATUSES.forEach(status => {
            const column = createColumn(status, grouped[status] || []);
            board.appendChild(column);
        });

        enableDragAndDrop();
    }

    function applyFilters(cards) {
        return cards.filter(card => {
            if (state.search) {
                const searchLower = state.search;
                const nomeContato = (card.nomeContato || '').toLowerCase();
                const email = (card.email || '').toLowerCase();
                const placa = (card.placa || '').toLowerCase();
                const celular = (card.celular || '').toLowerCase();

                if (!nomeContato.includes(searchLower) &&
                    !email.includes(searchLower) &&
                    !placa.includes(searchLower) &&
                    !celular.includes(searchLower)) {
                    return false;
                }
            }

            return true;
        });
    }

    function groupByStatus(cards) {
        const grouped = {};
        STATUSES.forEach(status => {
            grouped[status] = [];
        });

        cards.forEach(card => {
            const status = card.status || STATUSES[0];
            if (!grouped[status]) {
                grouped[status] = [];
            }
            grouped[status].push(card);
        });

        return grouped;
    }

    function createColumn(status, items) {
        const column = document.createElement('div');
        column.className = 'kanban-column';
        column.dataset.status = status;
        column.style.borderTopColor = statusColors[status] || '#6366f1';

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
        addButton.innerHTML = '<i class="bi bi-plus"></i> Novo';
        addButton.addEventListener('click', () => openCreateModal());

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
        title.textContent = card.nomeContato || 'Sem nome';
        header.appendChild(title);

        article.appendChild(header);

        const body = document.createElement('div');
        body.className = 'task-card-body';

        if (card.email) {
            const emailP = document.createElement('p');
            emailP.innerHTML = '<strong>Email:</strong> ' + escapeHtml(card.email);
            body.appendChild(emailP);
        }

        if (card.celular) {
            const celularP = document.createElement('p');
            celularP.innerHTML = '<strong>Celular:</strong> ' + escapeHtml(card.celular);
            body.appendChild(celularP);
        }

        if (card.placa) {
            const placaP = document.createElement('p');
            placaP.innerHTML = '<strong>Placa:</strong> ' + escapeHtml(card.placa);
            body.appendChild(placaP);
        }

        if (card.tipoVeiculo) {
            const tipoP = document.createElement('p');
            tipoP.innerHTML = '<strong>Tipo:</strong> ' + escapeHtml(card.tipoVeiculo);
            body.appendChild(tipoP);
        }

        if (card.origemLead) {
            const origemP = document.createElement('p');
            origemP.innerHTML = '<strong>Origem:</strong> ' + escapeHtml(card.origemLead);
            body.appendChild(origemP);
        }

        article.appendChild(body);

        article.addEventListener('click', () => openModal(card));

        return article;
    }

    function createEmptyState() {
        const div = document.createElement('div');
        div.className = 'empty-state';
        div.innerHTML = '<i class="bi bi-inbox"></i> Nenhuma negociação aqui ainda';
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

        if (!STATUSES.includes(targetStatus)) {
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

        // Atualizacao otimista
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

            console.log('[VENDAS-KANBAN] Status atualizado com sucesso');
        } catch (error) {
            console.error('[VENDAS-KANBAN] Erro ao atualizar status:', error);
            // Reverte em caso de erro
            card.status = previousStatus;
            render();
            alert('Nao foi possivel atualizar o status da venda.');
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

        const html = buildVendaDetailsSection(card);
        modalBody.innerHTML = html;

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
        console.log('[VENDAS-KANBAN] Abrindo modal de criacao');

        const modal = selectors.createModal();
        const form = selectors.createForm();

        if (!modal) {
            console.error('[VENDAS-KANBAN] Modal de criacao nao encontrado!');
            return;
        }

        showCreateError('');

        if (form) {
            form.reset();
            delete form.dataset.editingId;

            // Restaura o titulo do modal
            const modalHeader = modal.querySelector('.kanban-modal-header h2');
            if (modalHeader) {
                modalHeader.innerHTML = '<i class="bi bi-plus-circle"></i> Adicione uma nova negociação';
            }

            // Restaura o texto do botao de submit
            const submitBtn = form.querySelector('button[type="submit"]');
            if (submitBtn) {
                submitBtn.innerHTML = '<i class="bi bi-send-fill"></i> Adicionar negociação';
            }
        }

        modal.classList.add('active');
        document.body.classList.add('kanban-modal-open');

        console.log('[VENDAS-KANBAN] Modal aberto');
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
        console.log('[VENDAS-KANBAN] Formulario submetido');

        const form = event.currentTarget;
        if (!form) {
            return;
        }

        const isEditing = !!form.dataset.editingId;
        const editingId = form.dataset.editingId;

        const data = new FormData(form);

        const payload = {
            cooperativa: data.get('cooperativa')?.toString().trim() || null,
            tipoVeiculo: data.get('tipoVeiculo')?.toString().trim() || null,
            placa: data.get('placa')?.toString().trim() || null,
            marca: data.get('marca')?.toString().trim() || null,
            anoModelo: data.get('anoModelo')?.toString().trim() || null,
            modelo: data.get('modelo')?.toString().trim() || null,
            nomeContato: data.get('nomeContato')?.toString().trim() || null,
            email: data.get('email')?.toString().trim() || null,
            celular: data.get('celular')?.toString().trim() || null,
            estado: data.get('estado')?.toString().trim() || null,
            cidade: data.get('cidade')?.toString().trim() || null,
            origemLead: data.get('origemLead')?.toString().trim() || null,
            veiculoTrabalho: data.get('veiculoTrabalho') === 'on',
            enviarCotacao: data.get('enviarCotacao') === 'on',
            observacoes: data.get('observacoes')?.toString().trim() || null
        };

        console.log('[VENDAS-KANBAN] Enviando payload:', payload, 'Edicao:', isEditing);

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

            console.log('[VENDAS-KANBAN] Response status:', response.status);

            if (!response.ok) {
                let message = isEditing ? 'Nao foi possivel atualizar a venda.' : 'Nao foi possivel criar a venda.';
                try {
                    const errorData = await response.json();
                    message = errorData.message || errorData.error || message;
                } catch (e) {
                    const textError = await response.text();
                    console.error('[VENDAS-KANBAN] Erro:', textError);
                }
                showCreateError(message);
                return;
            }

            const savedVenda = await response.json();
            console.log('[VENDAS-KANBAN] Venda salva:', savedVenda);

            if (isEditing) {
                // Atualiza a venda no estado
                const index = state.cards.findIndex(c => c.id === parseInt(editingId, 10));
                if (index !== -1) {
                    state.cards[index] = savedVenda;
                }
            } else {
                // Adiciona nova venda ao inicio
                state.cards.unshift(savedVenda);
            }

            render();
            closeCreateModal();

            if (isEditing) {
                alert('Venda atualizada com sucesso!');
            }

        } catch (error) {
            console.error('[VENDAS-KANBAN] Erro ao salvar venda:', error);
            showCreateError('Ocorreu um erro ao salvar a venda. Tente novamente.');
        }
    }

    function openEditModal(vendaId) {
        console.log('[VENDAS-KANBAN] Abrindo modal de edicao para venda:', vendaId);

        const card = state.cards.find(c => c.id === vendaId);
        if (!card) {
            console.error('[VENDAS-KANBAN] Venda nao encontrada:', vendaId);
            return;
        }

        closeModal();

        const modal = selectors.createModal();
        const form = selectors.createForm();

        if (!modal || !form) {
            console.error('[VENDAS-KANBAN] Modal ou form nao encontrado!');
            return;
        }

        // Preenche o formulario com os dados da venda
        form.querySelector('#create-cooperativa').value = card.cooperativa || '';
        form.querySelector('#create-tipo-veiculo').value = card.tipoVeiculo || '';
        form.querySelector('#create-placa').value = card.placa || '';
        form.querySelector('#create-marca').value = card.marca || '';
        form.querySelector('#create-ano-modelo').value = card.anoModelo || '';
        form.querySelector('#create-modelo').value = card.modelo || '';
        form.querySelector('#create-nome-contato').value = card.nomeContato || '';
        form.querySelector('#create-email').value = card.email || '';
        form.querySelector('#create-celular').value = card.celular || '';
        form.querySelector('#create-estado').value = card.estado || '';
        form.querySelector('#create-cidade').value = card.cidade || '';
        form.querySelector('#create-origem-lead').value = card.origemLead || '';
        form.querySelector('#create-veiculo-trabalho').checked = card.veiculoTrabalho || false;
        form.querySelector('#create-enviar-cotacao').checked = card.enviarCotacao || false;
        form.querySelector('#create-observacoes').value = card.observacoes || '';

        // Marca o formulario como edicao
        form.dataset.editingId = vendaId;

        // Muda o titulo do modal
        const modalHeader = modal.querySelector('.kanban-modal-header h2');
        if (modalHeader) {
            modalHeader.innerHTML = '<i class="bi bi-pencil"></i> Editar Negociação';
        }

        // Muda o texto do botao de submit
        const submitBtn = form.querySelector('button[type="submit"]');
        if (submitBtn) {
            submitBtn.innerHTML = '<i class="bi bi-save"></i> Salvar';
        }

        showCreateError('');
        modal.classList.add('active');
        document.body.classList.add('kanban-modal-open');

        console.log('[VENDAS-KANBAN] Modal de edicao aberto');
    }

    async function deleteVenda(vendaId) {
        console.log('[VENDAS-KANBAN] Solicitando exclusao da venda:', vendaId);

        const card = state.cards.find(c => c.id === vendaId);
        if (!card) {
            console.error('[VENDAS-KANBAN] Venda nao encontrada:', vendaId);
            return;
        }

        const confirmMsg = 'Tem certeza que deseja excluir a negociação de "' + (card.nomeContato || 'sem nome') + '"?\n\nEsta acao nao pode ser desfeita.';
        if (!confirm(confirmMsg)) {
            console.log('[VENDAS-KANBAN] Exclusao cancelada pelo usuario');
            return;
        }

        try {
            const response = await fetch(API_BASE + '/' + vendaId, {
                method: 'DELETE',
                headers: buildHeaders({
                    'Accept': 'application/json'
                })
            });

            if (!response.ok) {
                throw new Error('Erro ao deletar venda');
            }

            console.log('[VENDAS-KANBAN] Venda deletada com sucesso');

            // Remove do estado local
            state.cards = state.cards.filter(c => c.id !== vendaId);

            closeModal();
            render();

            alert('Venda excluida com sucesso!');

        } catch (error) {
            console.error('[VENDAS-KANBAN] Erro ao deletar venda:', error);
            alert('Nao foi possivel excluir a venda. Tente novamente.');
        }
    }

    function buildVendaDetailsSection(card) {
        const html = [];
        html.push('<div class="detail-section">');
        html.push('<h3>Informações da Negociação</h3>');
        html.push('<p><strong>Nome para contato:</strong> ' + escapeHtml(card.nomeContato || 'N/A') + '</p>');
        html.push('<p><strong>Email:</strong> ' + escapeHtml(card.email || 'N/A') + '</p>');
        html.push('<p><strong>Celular:</strong> ' + escapeHtml(card.celular || 'N/A') + '</p>');
        html.push('<p><strong>Status:</strong> ' + escapeHtml(statusLabels[card.status] || card.status || 'N/A') + '</p>');
        html.push('</div>');

        html.push('<div class="detail-section">');
        html.push('<h3>Dados do Veículo</h3>');
        html.push('<p><strong>Tipo:</strong> ' + escapeHtml(card.tipoVeiculo || 'N/A') + '</p>');
        html.push('<p><strong>Placa:</strong> ' + escapeHtml(card.placa || 'N/A') + '</p>');
        html.push('<p><strong>Marca:</strong> ' + escapeHtml(card.marca || 'N/A') + '</p>');
        html.push('<p><strong>Modelo:</strong> ' + escapeHtml(card.modelo || 'N/A') + '</p>');
        html.push('<p><strong>Ano:</strong> ' + escapeHtml(card.anoModelo || 'N/A') + '</p>');
        html.push('<p><strong>Veículo de trabalho:</strong> ' + (card.veiculoTrabalho ? 'Sim' : 'Não') + '</p>');
        html.push('</div>');

        html.push('<div class="detail-section">');
        html.push('<h3>Outras Informações</h3>');
        html.push('<p><strong>Cooperativa:</strong> ' + escapeHtml(card.cooperativa || 'N/A') + '</p>');
        html.push('<p><strong>Estado:</strong> ' + escapeHtml(card.estado || 'N/A') + '</p>');
        html.push('<p><strong>Cidade:</strong> ' + escapeHtml(card.cidade || 'N/A') + '</p>');
        html.push('<p><strong>Origem do Lead:</strong> ' + escapeHtml(card.origemLead || 'N/A') + '</p>');
        html.push('<p><strong>Enviar cotação:</strong> ' + (card.enviarCotacao ? 'Sim' : 'Não') + '</p>');
        html.push('</div>');

        if (card.observacoes) {
            html.push('<div class="detail-section"><h3>Observações</h3>');
            html.push('<p>' + escapeHtml(card.observacoes) + '</p></div>');
        }

        html.push('<div class="modal-actions">');
        html.push('<button type="button" class="btn btn-primary" onclick="window.vendasBoard.openEditModal(' + card.id + ')"><i class="bi bi-pencil"></i> Editar</button>');
        html.push('<button type="button" class="btn btn-danger" onclick="window.vendasBoard.deleteVenda(' + card.id + ')"><i class="bi bi-trash"></i> Deletar</button>');
        html.push('</div>');

        return html.join('');
    }

    function escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    document.addEventListener('DOMContentLoaded', () => {
        console.log('[VENDAS-KANBAN] DOM carregado, inicializando...');

        window.vendasBoard = {
            openCreateModal: openCreateModal,
            closeCreateModal: closeCreateModal,
            closeModal: closeModal,
            openEditModal: openEditModal,
            deleteVenda: deleteVenda
        };

        console.log('[VENDAS-KANBAN] window.vendasBoard criado:', window.vendasBoard);

        init();
    });
})();
