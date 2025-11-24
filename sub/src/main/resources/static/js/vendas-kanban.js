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
        const statusText = statusLabels[card.status] || 'Status não definido';
        const statusClass = (card.status || 'desconhecido').toLowerCase();
        const createdAt = formatDateTime(card.createdAt) || 'Data não informada';
        const updatedAt = formatDateTime(card.updatedAt);
        const observacoes = card.observacoes ? escapeHtml(card.observacoes) : 'Nenhuma observação registrada.';

        const infoRows = [
            ['Cooperativa', card.cooperativa],
            ['Tipo de veículo', card.tipoVeiculo],
            ['Placa', card.placa],
            ['Marca', card.marca],
            ['Modelo', card.modelo],
            ['Ano modelo', card.anoModelo],
            ['Estado', card.estado],
            ['Cidade', card.cidade],
            ['Origem do Lead', card.origemLead],
            ['Veículo de trabalho', card.veiculoTrabalho ? 'Sim' : 'Não'],
            ['Enviar cotação', card.enviarCotacao ? 'Sim' : 'Não']
        ];

        return `
            <div class="venda-detail">
                <div class="venda-detail-header">
                    <div class="venda-detail-title">
                        <span class="badge-pill">${escapeHtml(card.cooperativa || 'Negociação')}</span>
                        <h3>${escapeHtml(card.nomeContato || 'Não definido')}</h3>
                        <p class="venda-subtitle">${escapeHtml(card.placa || 'Placa não informada')}</p>
                    </div>
                    <div class="venda-detail-header-actions">
                        <span class="badge-id">ID: ${escapeHtml(String(card.id || '-'))}</span>
                        <span class="status-chip status-${statusClass}">${escapeHtml(statusText)}</span>
                    </div>
                </div>

                <div class="venda-detail-grid">
                    <div class="venda-main">
                        <section class="venda-panel">
                            <header class="venda-panel-header">
                                <div>
                                    <h4>Atividades</h4>
                                    <p class="muted">Organize o próximo passo desta negociação</p>
                                </div>
                                <div class="venda-toolbar">
                                    <button class="btn btn-ghost" type="button"><i class="bi bi-download"></i> Receber cotação</button>
                                    <button class="btn btn-primary" type="button"><i class="bi bi-telephone-fill"></i> Atender essa cotação</button>
                                </div>
                            </header>

                            <div class="venda-form-grid">
                                <label class="venda-field">
                                    <span>Atividade</span>
                                    <div class="inline-options">
                                        <label class="pill-option"><input type="radio" name="atividade-${card.id}" checked> Ligar</label>
                                        <label class="pill-option"><input type="radio" name="atividade-${card.id}"> Email</label>
                                    </div>
                                </label>

                                <label class="venda-field">
                                    <span>Quando?</span>
                                    <input type="datetime-local" value="${formatDateTimeInput(card.createdAt)}">
                                </label>

                                <label class="venda-field">
                                    <span>Responsável</span>
                                    <select>
                                        <option>Guilherme Wolff (Você)</option>
                                        <option>Equipe Comercial</option>
                                        <option>Time de Vendas</option>
                                    </select>
                                </label>
                            </div>

                            <button class="btn btn-primary btn-full" type="button"><i class="bi bi-plus-circle"></i> Atividades</button>
                            <p class="muted centered">Sem atividades nesta negociação.</p>
                        </section>

                        <section class="venda-panel">
                            <header class="venda-panel-header">
                                <h4>Geral</h4>
                                <span class="badge-pill soft">Status atual: ${escapeHtml(statusText)}</span>
                            </header>
                            <div class="venda-info-grid">
                                ${infoRows.map(row => buildInfoRow(row[0], row[1])).join('')}
                            </div>
                            <div class="venda-timeline">
                                <div class="venda-timeline-item">
                                    <div class="venda-timeline-dot"></div>
                                    <div>
                                        <strong>Negociação criada pelo site</strong>
                                        <p class="muted">${createdAt}</p>
                                    </div>
                                </div>
                                ${updatedAt ? `<div class="venda-timeline-item"><div class="venda-timeline-dot"></div><div><strong>Última atualização</strong><p class="muted">${updatedAt}</p></div></div>` : ''}
                            </div>
                        </section>

                        <section class="venda-panel">
                            <header class="venda-panel-header">
                                <h4>Observações</h4>
                                <span class="badge-pill soft">Histórico</span>
                            </header>
                            <p>${observacoes}</p>
                        </section>
                    </div>

                    <div class="venda-side">
                        <section class="venda-panel responsavel-panel">
                            <header class="venda-panel-header">
                                <h4>Responsável</h4>
                                <span class="muted">${escapeHtml(card.responsavel || 'Nenhum')}</span>
                            </header>
                            <p class="muted">Atenção: esta negociação ainda não possui responsável definido.</p>
                            <button class="btn btn-success btn-full" type="button"><i class="bi bi-headset"></i> Atender essa cotação</button>
                        </section>

                        <section class="venda-panel contato-panel">
                            <div class="contato-heading">
                                <div class="contato-avatar">${escapeHtml((card.nomeContato || 'N')[0])}</div>
                                <div>
                                    <strong>${escapeHtml(card.nomeContato || 'Contato não informado')}</strong>
                                    <p class="muted">Lead via ${escapeHtml(card.origemLead || 'formulário')}</p>
                                </div>
                            </div>
                            <div class="venda-info-grid compact">
                                ${buildInfoRow('Email', card.email || 'Não informado')}
                                ${buildInfoRow('Celular', card.celular || 'Não informado')}
                                ${buildInfoRow('Cidade/Estado', (card.cidade && card.estado) ? card.cidade + ' / ' + card.estado : 'Não informado')}
                            </div>
                        </section>

                        <section class="venda-panel status-panel">
                            <header class="venda-panel-header">
                                <h4>Contratação online</h4>
                                <span class="status-chip warning">Dados incorretos</span>
                            </header>
                            <p class="muted">Revise os dados antes de liberar para cadastro.</p>
                            <div class="venda-toolbar">
                                <button class="btn btn-danger" type="button">Solicitar correção</button>
                                <button class="btn btn-secondary" type="button">Editar</button>
                            </div>
                        </section>

                        <section class="venda-panel lead-panel">
                            <div class="venda-info-row">
                                <span>Cooperativa</span>
                                <strong>${escapeHtml(card.cooperativa || 'Não informado')}</strong>
                            </div>
                            <div class="venda-info-row">
                                <span>Origem do lead</span>
                                <select class="pill-select">
                                    <option ${card.origemLead === 'Marketing ON' ? 'selected' : ''}>Marketing ON</option>
                                    <option ${card.origemLead === 'Site' ? 'selected' : ''}>Site</option>
                                    <option ${card.origemLead === 'Redes Sociais' ? 'selected' : ''}>Redes Sociais</option>
                                    <option ${card.origemLead === 'Indicação' ? 'selected' : ''}>Indicação</option>
                                </select>
                            </div>
                        </section>

                        <section class="venda-panel comunicacao-panel">
                            <header class="venda-panel-header">
                                <h4>Comunicação</h4>
                                <span class="badge-pill soft">Canais</span>
                            </header>
                            <div class="tag-list">${buildCommunicationTags(card)}</div>
                        </section>

                        <section class="venda-panel tags-panel">
                            <header class="venda-panel-header">
                                <h4>Tags</h4>
                                <span class="badge-pill soft">Categoria</span>
                            </header>
                            <div class="tag-list">
                                <span class="tag-chip">Validado</span>
                                <span class="tag-chip">Elegível</span>
                                <span class="tag-chip">Venda</span>
                            </div>
                        </section>

                        <section class="venda-panel apps-panel">
                            <header class="venda-panel-header">
                                <h4>Apps</h4>
                                <span class="badge-pill soft">Integrações</span>
                            </header>
                            <div class="venda-toolbar vertical">
                                <button class="btn btn-outline" type="button">Formulário de Website</button>
                                <button class="btn btn-outline" type="button">Formulário integrado do Facebook</button>
                            </div>
                        </section>
                    </div>
                </div>

                <div class="modal-actions">
                    <button type="button" class="btn btn-primary" onclick="window.vendasBoard.openEditModal(${card.id})"><i class="bi bi-pencil"></i> Editar</button>
                    <button type="button" class="btn btn-danger" onclick="window.vendasBoard.deleteVenda(${card.id})"><i class="bi bi-trash"></i> Deletar</button>
                </div>
            </div>
        `;
    }

    function buildInfoRow(label, value) {
        return `
            <div class="venda-info-row">
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
