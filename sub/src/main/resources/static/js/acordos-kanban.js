(function () {
    console.log('[ACORDOS-KANBAN] Script carregado com sucesso');

    const API_BASE = '/juridico/api/acordos';
    const STATUSES = ['PENDENTE', 'EM_NEGOCIACAO', 'APROVADO', 'PAGO', 'CANCELADO', 'VENCIDO'];

    const state = {
        cards: [],
        search: ''
    };

    const dragState = {
        cardId: null,
        originStatus: null
    };

    const statusLabels = {
        PENDENTE: 'Pendente',
        EM_NEGOCIACAO: 'Em Negociacao',
        APROVADO: 'Aprovado',
        PAGO: 'Pago',
        CANCELADO: 'Cancelado',
        VENCIDO: 'Vencido'
    };

    const statusColors = {
        PENDENTE: '#f59e0b',
        EM_NEGOCIACAO: '#3b82f6',
        APROVADO: '#10b981',
        PAGO: '#22c55e',
        CANCELADO: '#ef4444',
        VENCIDO: '#dc2626'
    };

    const selectors = {
        board: () => document.querySelector('.kanban-board'),
        searchInput: () => document.getElementById('kanban-search'),
        modal: () => document.getElementById('kanban-modal'),
        modalBody: () => document.querySelector('#kanban-modal .kanban-modal-body'),
        createModal: () => document.getElementById('create-acordo-modal'),
        createForm: () => document.getElementById('create-acordo-form'),
        createError: () => document.getElementById('create-acordo-error')
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
        console.log('[ACORDOS-KANBAN] Inicializando...');

        if (!selectors.board()) {
            console.error('[ACORDOS-KANBAN] Board nao encontrado!');
            return;
        }

        console.log('[ACORDOS-KANBAN] Board encontrado, carregando acordos...');
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
        console.log('[ACORDOS-KANBAN] Carregando acordos...');
        try {
            const response = await fetch(API_BASE, {
                headers: buildHeaders({ 'Accept': 'application/json' })
            });

            if (!response.ok) {
                throw new Error('Erro ao carregar acordos');
            }

            const data = await response.json();
            console.log('[ACORDOS-KANBAN] Acordos carregados:', data);

            if (Array.isArray(data)) {
                state.cards = data;
            } else {
                state.cards = [];
            }

            render();
        } catch (error) {
            console.error('[ACORDOS-KANBAN] Erro ao carregar acordos:', error);
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
                const titulo = (card.titulo || '').toLowerCase();
                const parteEnvolvida = (card.parteEnvolvida || '').toLowerCase();
                const numeroProcesso = (card.numeroProcesso || '').toLowerCase();
                const descricao = (card.descricao || '').toLowerCase();

                if (!titulo.includes(searchLower) &&
                    !parteEnvolvida.includes(searchLower) &&
                    !numeroProcesso.includes(searchLower) &&
                    !descricao.includes(searchLower)) {
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
        title.textContent = card.titulo || 'Sem titulo';
        header.appendChild(title);

        article.appendChild(header);

        const body = document.createElement('div');
        body.className = 'task-card-body';

        const parteP = document.createElement('p');
        parteP.innerHTML = '<strong>Parte:</strong> ' + escapeHtml(card.parteEnvolvida || 'N/A');
        body.appendChild(parteP);

        if (card.valor) {
            const valorP = document.createElement('p');
            valorP.innerHTML = '<strong>Valor:</strong> R$ ' + formatCurrency(card.valor);
            body.appendChild(valorP);
        }

        if (card.dataVencimento) {
            const vencimentoP = document.createElement('p');
            vencimentoP.innerHTML = '<strong>Vencimento:</strong> ' + formatDate(card.dataVencimento);
            body.appendChild(vencimentoP);
        }

        if (card.numeroParcelas && card.parcelaAtual) {
            const parcelasP = document.createElement('p');
            parcelasP.innerHTML = '<strong>Parcela:</strong> ' + card.parcelaAtual + '/' + card.numeroParcelas;
            body.appendChild(parcelasP);
        }

        if (card.numeroProcesso) {
            const processoP = document.createElement('p');
            processoP.innerHTML = '<strong>Processo:</strong> ' + escapeHtml(card.numeroProcesso);
            body.appendChild(processoP);
        }

        article.appendChild(body);

        article.addEventListener('click', () => openModal(card));

        return article;
    }

    function createEmptyState() {
        const div = document.createElement('div');
        div.className = 'empty-state';
        div.innerHTML = '<i class="bi bi-inbox"></i> Nenhum acordo aqui ainda';
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

            console.log('[ACORDOS-KANBAN] Status atualizado com sucesso');
        } catch (error) {
            console.error('[ACORDOS-KANBAN] Erro ao atualizar status:', error);
            // Reverte em caso de erro
            card.status = previousStatus;
            render();
            alert('Nao foi possivel atualizar o status do acordo.');
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

        const html = buildAcordoDetailsSection(card);
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
        console.log('[ACORDOS-KANBAN] Abrindo modal de criacao');

        const modal = selectors.createModal();
        const form = selectors.createForm();

        if (!modal) {
            console.error('[ACORDOS-KANBAN] Modal de criacao nao encontrado!');
            return;
        }

        showCreateError('');

        if (form) {
            form.reset();
            delete form.dataset.editingId;

            // Restaura o titulo do modal
            const modalHeader = modal.querySelector('.kanban-modal-header h2');
            if (modalHeader) {
                modalHeader.innerHTML = '<i class="bi bi-plus-circle"></i> Novo Acordo';
            }

            // Restaura o texto do botao de submit
            const submitBtn = form.querySelector('button[type="submit"]');
            if (submitBtn) {
                submitBtn.innerHTML = '<i class="bi bi-send-fill"></i> Registrar';
            }
        }

        modal.classList.add('active');
        document.body.classList.add('kanban-modal-open');

        console.log('[ACORDOS-KANBAN] Modal aberto');
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
        console.log('[ACORDOS-KANBAN] Formulario submetido');

        const form = event.currentTarget;
        if (!form) {
            return;
        }

        const isEditing = !!form.dataset.editingId;
        const editingId = form.dataset.editingId;

        const data = new FormData(form);
        const titulo = data.get('titulo')?.toString().trim();
        const descricao = data.get('descricao')?.toString().trim();
        const parteEnvolvida = data.get('parteEnvolvida')?.toString().trim();
        const valorStr = data.get('valor')?.toString().replace(',', '.');
        const valor = parseFloat(valorStr);
        const dataVencimento = data.get('dataVencimento')?.toString() || null;
        const dataPagamento = data.get('dataPagamento')?.toString() || null;
        const observacoes = data.get('observacoes')?.toString().trim() || null;
        const numeroParcelasStr = data.get('numeroParcelas')?.toString();
        const numeroParcelas = numeroParcelasStr ? parseInt(numeroParcelasStr, 10) : null;
        const parcelaAtualStr = data.get('parcelaAtual')?.toString();
        const parcelaAtual = parcelaAtualStr ? parseInt(parcelaAtualStr, 10) : null;
        const numeroProcesso = data.get('numeroProcesso')?.toString().trim() || null;

        if (!titulo || !descricao || !parteEnvolvida) {
            showCreateError('Titulo, descricao e parte envolvida sao obrigatorios.');
            return;
        }

        if (!valor || valor <= 0) {
            showCreateError('Informe um valor valido.');
            return;
        }

        const payload = {
            titulo: titulo,
            descricao: descricao,
            parteEnvolvida: parteEnvolvida,
            valor: valor,
            dataVencimento: dataVencimento,
            dataPagamento: dataPagamento,
            observacoes: observacoes,
            numeroParcelas: numeroParcelas,
            parcelaAtual: parcelaAtual,
            numeroProcesso: numeroProcesso
        };

        console.log('[ACORDOS-KANBAN] Enviando payload:', payload, 'Edicao:', isEditing);

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

            console.log('[ACORDOS-KANBAN] Response status:', response.status);

            if (!response.ok) {
                let message = isEditing ? 'Nao foi possivel atualizar o acordo.' : 'Nao foi possivel criar o acordo.';
                try {
                    const errorData = await response.json();
                    message = errorData.message || errorData.error || message;
                } catch (e) {
                    const textError = await response.text();
                    console.error('[ACORDOS-KANBAN] Erro:', textError);
                }
                showCreateError(message);
                return;
            }

            const savedAcordo = await response.json();
            console.log('[ACORDOS-KANBAN] Acordo salvo:', savedAcordo);

            if (isEditing) {
                // Atualiza o acordo no estado
                const index = state.cards.findIndex(c => c.id === parseInt(editingId, 10));
                if (index !== -1) {
                    state.cards[index] = savedAcordo;
                }
            } else {
                // Adiciona novo acordo ao inicio
                state.cards.unshift(savedAcordo);
            }

            render();
            closeCreateModal();

            if (isEditing) {
                alert('Acordo atualizado com sucesso!');
            }

        } catch (error) {
            console.error('[ACORDOS-KANBAN] Erro ao salvar acordo:', error);
            showCreateError('Ocorreu um erro ao salvar o acordo. Tente novamente.');
        }
    }

    function openEditModal(acordoId) {
        console.log('[ACORDOS-KANBAN] Abrindo modal de edicao para acordo:', acordoId);

        const card = state.cards.find(c => c.id === acordoId);
        if (!card) {
            console.error('[ACORDOS-KANBAN] Acordo nao encontrado:', acordoId);
            return;
        }

        closeModal();

        const modal = selectors.createModal();
        const form = selectors.createForm();

        if (!modal || !form) {
            console.error('[ACORDOS-KANBAN] Modal ou form nao encontrado!');
            return;
        }

        // Preenche o formulario com os dados do acordo
        form.querySelector('#create-titulo').value = card.titulo || '';
        form.querySelector('#create-descricao').value = card.descricao || '';
        form.querySelector('#create-parte-envolvida').value = card.parteEnvolvida || '';
        form.querySelector('#create-valor').value = card.valor || '';
        form.querySelector('#create-data-vencimento').value = card.dataVencimento || '';
        form.querySelector('#create-data-pagamento').value = card.dataPagamento || '';
        form.querySelector('#create-observacoes').value = card.observacoes || '';
        form.querySelector('#create-numero-parcelas').value = card.numeroParcelas || '';
        form.querySelector('#create-parcela-atual').value = card.parcelaAtual || '';
        form.querySelector('#create-numero-processo').value = card.numeroProcesso || '';

        // Marca o formulario como edicao
        form.dataset.editingId = acordoId;

        // Muda o titulo do modal
        const modalHeader = modal.querySelector('.kanban-modal-header h2');
        if (modalHeader) {
            modalHeader.innerHTML = '<i class="bi bi-pencil"></i> Editar Acordo';
        }

        // Muda o texto do botao de submit
        const submitBtn = form.querySelector('button[type="submit"]');
        if (submitBtn) {
            submitBtn.innerHTML = '<i class="bi bi-save"></i> Salvar';
        }

        showCreateError('');
        modal.classList.add('active');
        document.body.classList.add('kanban-modal-open');

        console.log('[ACORDOS-KANBAN] Modal de edicao aberto');
    }

    async function deleteAcordo(acordoId) {
        console.log('[ACORDOS-KANBAN] Solicitando exclusao do acordo:', acordoId);

        const card = state.cards.find(c => c.id === acordoId);
        if (!card) {
            console.error('[ACORDOS-KANBAN] Acordo nao encontrado:', acordoId);
            return;
        }

        const confirmMsg = 'Tem certeza que deseja excluir o acordo "' + card.titulo + '"?\n\nEsta acao nao pode ser desfeita.';
        if (!confirm(confirmMsg)) {
            console.log('[ACORDOS-KANBAN] Exclusao cancelada pelo usuario');
            return;
        }

        try {
            const response = await fetch(API_BASE + '/' + acordoId, {
                method: 'DELETE',
                headers: buildHeaders({
                    'Accept': 'application/json'
                })
            });

            if (!response.ok) {
                throw new Error('Erro ao deletar acordo');
            }

            console.log('[ACORDOS-KANBAN] Acordo deletado com sucesso');

            // Remove do estado local
            state.cards = state.cards.filter(c => c.id !== acordoId);

            closeModal();
            render();

            alert('Acordo excluido com sucesso!');

        } catch (error) {
            console.error('[ACORDOS-KANBAN] Erro ao deletar acordo:', error);
            alert('Nao foi possivel excluir o acordo. Tente novamente.');
        }
    }

    function buildAcordoDetailsSection(card) {
        const html = [];
        html.push('<div class="detail-section">');
        html.push('<h3>Informacoes do Acordo</h3>');
        html.push('<p><strong>Titulo:</strong> ' + escapeHtml(card.titulo || 'N/A') + '</p>');
        html.push('<p><strong>Descricao:</strong> ' + escapeHtml(card.descricao || 'N/A') + '</p>');
        html.push('<p><strong>Parte Envolvida:</strong> ' + escapeHtml(card.parteEnvolvida || 'N/A') + '</p>');
        html.push('<p><strong>Valor:</strong> R$ ' + formatCurrency(card.valor || 0) + '</p>');
        html.push('<p><strong>Status:</strong> ' + escapeHtml(statusLabels[card.status] || card.status || 'N/A') + '</p>');

        if (card.dataVencimento) {
            html.push('<p><strong>Data de Vencimento:</strong> ' + formatDate(card.dataVencimento) + '</p>');
        }

        if (card.dataPagamento) {
            html.push('<p><strong>Data de Pagamento:</strong> ' + formatDate(card.dataPagamento) + '</p>');
        }

        if (card.numeroParcelas) {
            html.push('<p><strong>Parcelas:</strong> ' + (card.parcelaAtual || 1) + '/' + card.numeroParcelas + '</p>');
        }

        if (card.numeroProcesso) {
            html.push('<p><strong>Numero do Processo:</strong> ' + escapeHtml(card.numeroProcesso) + '</p>');
        }

        html.push('</div>');

        if (card.observacoes) {
            html.push('<div class="detail-section"><h3>Observacoes</h3>');
            html.push('<p>' + escapeHtml(card.observacoes) + '</p></div>');
        }

        html.push('<div class="modal-actions">');
        html.push('<button type="button" class="btn btn-primary" onclick="window.acordosBoard.openEditModal(' + card.id + ')"><i class="bi bi-pencil"></i> Editar</button>');
        html.push('<button type="button" class="btn btn-danger" onclick="window.acordosBoard.deleteAcordo(' + card.id + ')"><i class="bi bi-trash"></i> Deletar</button>');
        html.push('</div>');

        return html.join('');
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
        console.log('[ACORDOS-KANBAN] DOM carregado, inicializando...');

        window.acordosBoard = {
            openCreateModal: openCreateModal,
            closeCreateModal: closeCreateModal,
            closeModal: closeModal,
            openEditModal: openEditModal,
            deleteAcordo: deleteAcordo
        };

        console.log('[ACORDOS-KANBAN] window.acordosBoard criado:', window.acordosBoard);

        init();
    });
})();
