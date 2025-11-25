(function () {
    console.log('[VENDAS-KANBAN] Script carregado com sucesso');

    const API_BASE = '/crm/api/vendas';
    const STATUSES = ['COTACOES_RECEBIDAS', 'EM_NEGOCIACAO', 'VISTORIAS', 'LIBERADAS_PARA_CADASTRO', 'FILIACAO_CONCRETIZADAS'];

    const state = {
        cards: [],
        search: ''
    };

    const dragState = {
        cardId: null,
        originStatus: null
    };

    const statusLabels = {
        COTACOES_RECEBIDAS: 'Cotações recebidas',
        EM_NEGOCIACAO: 'Em negociação',
        VISTORIAS: 'Vistorias',
        LIBERADAS_PARA_CADASTRO: 'Liberadas para cadastro',
        FILIACAO_CONCRETIZADAS: 'Filiação concretizadas'
    };

    const statusColors = {
        COTACOES_RECEBIDAS: '#6366f1',
        EM_NEGOCIACAO: '#f59e0b',
        VISTORIAS: '#3b82f6',
        LIBERADAS_PARA_CADASTRO: '#8b5cf6',
        FILIACAO_CONCRETIZADAS: '#10b981'
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
            const rawStatus = card.status || STATUSES[0];
            const status = STATUSES.includes(rawStatus) ? rawStatus : STATUSES[0];
            grouped[status].push(card);
        });

        return grouped;
    }

    function createColumn(status, items) {
        const column = document.createElement('div');
        column.className = 'vendas-kanban-column kanban-column';
        column.dataset.status = status;

        // Barra colorida no topo da coluna
        const bar = document.createElement('div');
        bar.className = 'vendas-kanban-column-bar';
        column.appendChild(bar);

        const header = document.createElement('div');
        header.className = 'vendas-kanban-column-header';

        const title = document.createElement('div');
        title.className = 'vendas-kanban-column-title';

        const heading = document.createElement('h3');
        heading.textContent = statusLabels[status] || status;

        const counter = document.createElement('span');
        counter.className = 'vendas-kanban-column-count';
        counter.textContent = items.length;

        title.appendChild(heading);
        title.appendChild(counter);

        header.appendChild(title);

        const helper = document.createElement('p');
        helper.className = 'vendas-kanban-column-helper';
        helper.textContent = getStatusHelper(status);
        header.appendChild(helper);

        const container = document.createElement('div');
        container.className = 'vendas-kanban-tasks tasks-container';

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

    function getStatusHelper(status) {
        const helpers = {
            COTACOES_RECEBIDAS: 'Cotações aguardando análise',
            EM_NEGOCIACAO: 'Negociações em andamento',
            VISTORIAS: 'Vistorias agendadas ou em execução',
            LIBERADAS_PARA_CADASTRO: 'Aprovadas para cadastro',
            FILIACAO_CONCRETIZADAS: 'Filiações concluídas com sucesso'
        };
        return helpers[status] || 'Acompanhe o progresso';
    }

    function createCard(card) {
        const article = document.createElement('article');
        article.className = 'vendas-kanban-card';
        article.dataset.id = card.id;
        article.setAttribute('draggable', 'true');

        // Topo do card
        const top = document.createElement('div');
        top.className = 'vendas-kanban-card-top';

        const topLeft = document.createElement('div');

        const origin = document.createElement('span');
        origin.className = 'vendas-kanban-card-origin';
        origin.textContent = card.origemLead || 'Origem não definida';
        topLeft.appendChild(origin);

        const label = document.createElement('p');
        label.className = 'vendas-kanban-card-label';
        label.textContent = `${card.nomeContato || 'Não definido'} • ${formatDateTimeShort(card.createdAt) || 'Sem data'}`;
        topLeft.appendChild(label);

        const title = document.createElement('h4');
        title.className = 'vendas-kanban-card-title';
        title.textContent = card.responsavel || statusLabels[card.status] || 'Não definido';
        topLeft.appendChild(title);

        top.appendChild(topLeft);

        const topRight = document.createElement('div');
        topRight.className = 'vendas-kanban-card-top-right';

        const statusRow = document.createElement('div');
        statusRow.className = 'vendas-kanban-card-status-row';

        const statusBadge = document.createElement('span');
        statusBadge.className = 'vendas-kanban-card-status';
        statusBadge.textContent = statusLabels[card.status] || 'Em andamento';
        statusRow.appendChild(statusBadge);

        const amount = document.createElement('div');
        amount.className = 'vendas-kanban-card-amount';
        amount.innerHTML = `<i class="bi bi-currency-dollar"></i><span>${formatCurrency(card.valor)}</span>`;
        statusRow.appendChild(amount);

        topRight.appendChild(statusRow);

        top.appendChild(topRight);

        article.appendChild(top);

    function formatCurrency(value) {
        const numeric = typeof value === 'number' ? value : Number(value);
        if (Number.isNaN(numeric)) {
            return 'R$ 0,00';
        }

        return numeric.toLocaleString('pt-BR', {
            style: 'currency',
            currency: 'BRL',
            minimumFractionDigits: 2,
            maximumFractionDigits: 2
        });
    }

    function formatDateTimeShort(dateValue) {
        if (!dateValue) return '';
        const date = new Date(dateValue);
        if (Number.isNaN(date.getTime())) return '';

        const day = String(date.getDate()).padStart(2, '0');
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const year = date.getFullYear();
        const hours = String(date.getHours()).padStart(2, '0');
        const minutes = String(date.getMinutes()).padStart(2, '0');

        return `${day}/${month}/${year} - ${hours}:${minutes}`;
    }

    function formatCurrency(value) {
        const numeric = typeof value === 'number' ? value : Number(value);
        if (Number.isNaN(numeric)) {
            return 'R$ 0,00';
        }

        return numeric.toLocaleString('pt-BR', {
            style: 'currency',
            currency: 'BRL',
            minimumFractionDigits: 2,
            maximumFractionDigits: 2
        });
    }

    function createEmptyState() {
        const div = document.createElement('div');
        div.className = 'vendas-kanban-empty';
        div.innerHTML = '<i class="bi bi-inbox"></i><p>Nenhuma negociação aqui ainda</p>';
        return div;
    }

    function enableDragAndDrop() {
        document.querySelectorAll('.vendas-kanban-card').forEach(card => {
            card.setAttribute('draggable', 'true');
            card.addEventListener('dragstart', handleDragStart);
            card.addEventListener('dragend', handleDragEnd);
        });
    }

    function bindColumnDropZone(column) {
        const container = column.querySelector('.vendas-kanban-tasks, .tasks-container');
        if (!container || container.dataset.dndBound) {
            return;
        }

        container.addEventListener('dragover', handleDragOver);
        container.addEventListener('drop', handleDrop);
        container.addEventListener('dragenter', handleDragEnter);
        container.addEventListener('dragleave', handleDragLeave);
        container.dataset.dndBound = 'true';
    }

    function handleDragStart(event) {
        const card = event.currentTarget;
        dragState.cardId = card.dataset.id || null;
        dragState.originStatus = card.closest('.vendas-kanban-column, .kanban-column')?.dataset.status || null;

        if (event.dataTransfer) {
            event.dataTransfer.effectAllowed = 'move';
            event.dataTransfer.setData('text/plain', dragState.cardId || '');
        }

        card.classList.add('dragging');
    }

    function handleDragEnd(event) {
        event.currentTarget.classList.remove('dragging');
        // Remove drag-over de todas as colunas
        document.querySelectorAll('.vendas-kanban-column').forEach(col => {
            col.classList.remove('drag-over');
        });
    }

    function handleDragEnter(event) {
        const column = event.currentTarget.closest('.vendas-kanban-column');
        if (column) {
            column.classList.add('drag-over');
        }
    }

    function handleDragLeave(event) {
        const container = event.currentTarget;
        const column = container.closest('.vendas-kanban-column');

        // Verifica se realmente saiu da coluna
        if (column && !column.contains(event.relatedTarget)) {
            column.classList.remove('drag-over');
        }
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
        const column = container.closest('.vendas-kanban-column, .kanban-column');
        const targetStatus = column?.dataset.status;
        const cardId = dragState.cardId || event.dataTransfer?.getData('text/plain');

        // Remove drag-over visual
        if (column) {
            column.classList.remove('drag-over');
        }

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
        const statusText = statusLabels[card.status] || 'Status não definido';
        const createdAt = formatDateTime(card.createdAt) || '24/11/2025 12:06';
        const updatedAt = formatDateTime(card.updatedAt);
        const responsavel = card.responsavel || 'Nenhum';
        const observacoes = card.observacoes ? escapeHtml(card.observacoes) : 'Nenhuma observação registrada.';
        const vehicleTitle = [card.marca, card.modelo, card.anoModelo].filter(Boolean).join(' • ') || 'Negociação em aberto';
        const statusClass = (card.status || 'default').toLowerCase().replace(/[^a-z0-9]+/g, '-');
        const contactName = card.nomeContato || 'Contato não informado';
        const contactInitials = (contactName.trim().charAt(0) || '?').toUpperCase();

        return `
            <div class="crm-dialog">
                <header class="crm-hero">
                        <div class="crm-hero-left">
                            <div class="crm-kicker">Olá</div>
                            <div class="crm-hero-title">${escapeHtml(vehicleTitle)}</div>
                            <div class="crm-hero-chips">
                                <span class="crm-chip"><i class="bi bi-credit-card-2-front"></i>${escapeHtml(card.placa || 'Placa não informada')}</span>
                                <span class="crm-chip status ${statusClass}">${escapeHtml(statusText)}</span>
                            </div>
                        <div class="crm-hero-contact">
                            <div class="crm-avatar">${escapeHtml(contactInitials)}</div>
                            <div>
                                <p class="crm-contact-name">${escapeHtml(contactName)}</p>
                                <div class="crm-contact-tags">${buildCommunicationTags(card)}</div>
                            </div>
                        </div>
                    </div>
                    <div class="crm-hero-right">
                        <div class="crm-hero-summary">
                            <span class="label">Responsável</span>
                            <strong>${escapeHtml(responsavel)}</strong>
                            <p class="crm-muted">Defina alguém para assumir esta negociação.</p>
                        </div>
                        <div class="crm-hero-actions">
                            <button class="crm-btn crm-btn-success" type="button">Atender cotação</button>
                            <button class="crm-btn crm-btn-ghost" type="button" onclick="window.vendasBoard.closeModal()">Fechar</button>
                        </div>
                    </div>
                </header>

                <div class="crm-tabs primary">
                    <button class="active" type="button">Atividades</button>
                    <button type="button">Cotações</button>
                    <button type="button">Pós-vendas</button>
                    <button type="button">Frota</button>
                    <button type="button">Riscos</button>
                </div>

                <div class="crm-grid">
                    <div class="crm-main">
                        <section class="crm-card highlight">
                            <div class="crm-card-header">
                                <div>
                                    <p class="crm-card-eyebrow">Aceite de cotação</p>
                                    <h3>Envie o documento ao cliente</h3>
                                    <p class="crm-muted">Revise as condições e confirme com o associado.</p>
                                </div>
                                <button class="crm-btn crm-btn-primary" type="button">Aceitar</button>
                            </div>
                        </section>

                        <section class="crm-card">
                            <div class="crm-card-header">
                                <h3>Informações da venda</h3>
                                <span class="crm-pill">${escapeHtml(statusText)}</span>
                            </div>
                            <div class="crm-info-grid">
                                ${buildInfoRow('Cooperativa', card.cooperativa)}
                                ${buildInfoRow('Placa', card.placa)}
                                ${buildInfoRow('Marca', card.marca)}
                                ${buildInfoRow('Modelo', card.modelo)}
                                ${buildInfoRow('Ano modelo', card.anoModelo)}
                                ${buildInfoRow('Estado', card.estado)}
                                ${buildInfoRow('Cidade', card.cidade)}
                                ${buildInfoRow('Veículo de trabalho', card.veiculoTrabalho ? 'Sim' : 'Não')}
                            </div>
                            <div class="crm-history">
                                <div class="crm-history-item">
                                    <div class="crm-history-dot"></div>
                                    <div>
                                        <strong>Negociação criada</strong>
                                        <p class="crm-muted">${createdAt}</p>
                                    </div>
                                </div>
                                ${updatedAt ? `<div class="crm-history-item"><div class="crm-history-dot"></div><div><strong>Última atualização</strong><p class="crm-muted">${updatedAt}</p></div></div>` : ''}
                            </div>
                        </section>

                        <section class="crm-card">
                            <div class="crm-card-header">
                                <h3>Atividades</h3>
                                <span class="crm-pill">Agenda</span>
                            </div>
                            <div class="crm-form-grid">
                                <label class="crm-field">
                                    <span>Atividade*</span>
                                    <select>
                                        <option>Ligar</option>
                                        <option>Email</option>
                                        <option>Whatsapp</option>
                                        <option>Reunião</option>
                                    </select>
                                </label>
                                <label class="crm-field">
                                    <span>Responsável*</span>
                                    <select>
                                        <option>${escapeHtml(responsavel)}</option>
                                        <option>Equipe Comercial</option>
                                        <option>Time de Vendas</option>
                                    </select>
                                </label>
                                <label class="crm-field">
                                    <span>Quando*</span>
                                    <input type="datetime-local" value="${formatDateTimeInput(card.createdAt)}">
                                </label>
                                <label class="crm-field">
                                    <span>Cliente*</span>
                                    <select>
                                        <option>${escapeHtml(contactName)}</option>
                                    </select>
                                </label>
                                <label class="crm-field full">
                                    <span>Observação</span>
                                    <textarea rows="2" placeholder="Adicione observações">${card.observacoes ? escapeHtml(card.observacoes) : ''}</textarea>
                                </label>
                            </div>
                            <div class="crm-card-footer">
                                <div class="crm-card-footer-actions">
                                    <button class="crm-btn crm-btn-primary" type="button">Salvar atividade</button>
                                    <button class="crm-btn crm-btn-secondary" type="button">Comunicação</button>
                                    <button class="crm-btn crm-btn-secondary" type="button">Histórico</button>
                                    <button class="crm-btn crm-btn-secondary" type="button">PowerSign</button>
                                </div>
                                <p class="crm-muted">Sem atividades nesta negociação.</p>
                            </div>
                        </section>
                    </div>

                    <div class="crm-side">
                        <section class="crm-card ghost">
                            <div class="crm-card-header">
                                <h3>Responsável</h3>
                                <div class="crm-status">${escapeHtml(responsavel)}</div>
                            </div>
                            <p class="crm-muted">Defina um responsável para avançar com esta oportunidade.</p>
                            <button class="crm-btn crm-btn-success full" type="button">Atender essa cotação</button>
                        </section>

                        <section class="crm-card">
                            <div class="crm-card-header">
                                <h3>Contrato</h3>
                                <span class="crm-pill light">Detalhes</span>
                            </div>
                            <div class="crm-info-grid compact">
                                ${buildInfoRow('Envio de cotação', card.enviarCotacao ? 'Sim' : 'Não')}
                                ${buildInfoRow('Cidade', card.cidade)}
                                ${buildInfoRow('Estado', card.estado)}
                            </div>
                        </section>

                        <section class="crm-card">
                            <div class="crm-card-header">
                                <h3>Observações</h3>
                                <span class="crm-pill light">Notas</span>
                            </div>
                            <p class="crm-muted">${observacoes}</p>
                        </section>
                    </div>
                </div>

                <div class="crm-actions">
                    <button type="button" class="crm-btn crm-btn-primary" onclick="window.vendasBoard.openEditModal(${card.id})"><i class="bi bi-pencil"></i> Editar</button>
                    <button type="button" class="crm-btn crm-btn-danger" onclick="window.vendasBoard.deleteVenda(${card.id})"><i class="bi bi-trash"></i> Deletar</button>
                </div>
            </div>
        `;
    }

    function buildInfoRow(label, value) {
        return `
            <div class="crm-info-row">
                <span>${escapeHtml(label)}</span>
                <strong>${escapeHtml(value || 'Não informado')}</strong>
            </div>
        `;
    }

    function buildCommunicationTags(card) {
        const tags = [];

        if (card.email) {
            tags.push({ icon: 'bi-envelope', label: card.email });
        }

        if (card.celular) {
            tags.push({ icon: 'bi-whatsapp', label: card.celular });
        }

        if (card.placa) {
            tags.push({ icon: 'bi-car-front', label: card.placa });
        }

        if (card.cidade || card.estado) {
            tags.push({ icon: 'bi-geo-alt', label: `${card.cidade || ''} ${card.estado ? '/ ' + card.estado : ''}`.trim() });
        }

        if (!tags.length) {
            return '<span class="muted">Nenhum contato informado.</span>';
        }

        return tags.map(tag => `<span class="tag-chip"><i class="bi ${tag.icon}"></i> ${escapeHtml(tag.label)}</span>`).join('');
    }

    function formatDateTime(dateValue) {
        if (!dateValue) {
            return '';
        }

        const date = new Date(dateValue);
        if (Number.isNaN(date.getTime())) {
            return '';
        }

        return date.toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' });
    }

    function formatDateTimeInput(dateValue) {
        const date = new Date(dateValue || Date.now());

        if (Number.isNaN(date.getTime())) {
            return '';
        }

        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        const hours = String(date.getHours()).padStart(2, '0');
        const minutes = String(date.getMinutes()).padStart(2, '0');

        return `${year}-${month}-${day}T${hours}:${minutes}`;
    }

    function escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    // Função para aguardar o board estar disponível
    function waitForBoard(callback, maxAttempts = 50) {
        let attempts = 0;

        const checkBoard = () => {
            attempts++;
            const board = selectors.board();

            if (board) {
                console.log('[VENDAS-KANBAN] Board encontrado após', attempts, 'tentativa(s)');
                callback();
            } else if (attempts < maxAttempts) {
                console.log('[VENDAS-KANBAN] Aguardando board... tentativa', attempts);
                setTimeout(checkBoard, 100);
            } else {
                console.error('[VENDAS-KANBAN] Board não encontrado após', maxAttempts, 'tentativas');
            }
        };

        checkBoard();
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

        // Aguarda o board estar disponível antes de inicializar
        waitForBoard(init);
    });
})();
