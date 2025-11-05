/**
 * KANBAN BOARD V3 - SISTEMA COMPLETO PROFISSIONAL
 * Features: Lista/Board, Edição Inline, Filtros Avançados, Export, Notificações, Reordenação, Temas, Histórico
 */

class KanbanBoard {
    constructor() {
        this.events = [];
        this.filteredEvents = [];
        this.draggedCard = null;
        this.currentView = localStorage.getItem('kanban-view') || 'board'; // board ou list
        this.currentFilter = 'all';
        this.searchQuery = '';
        this.advancedFilters = {
            dateFrom: null,
            dateTo: null,
            partnerId: null,
            vehicleId: null,
            prioridade: null,
            motivo: null
        };
        this.csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
        this.csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
        this.sortBy = localStorage.getItem('kanban-sort') || 'dataVencimento';
        this.sortOrder = localStorage.getItem('kanban-sort-order') || 'asc';
        this.theme = localStorage.getItem('kanban-custom-theme') || 'default';

        this.statuses = ['A_FAZER', 'EM_ANDAMENTO', 'AGUARDANDO', 'CONCLUIDO'];
        this.statusLabels = {
            'A_FAZER': 'A Fazer',
            'EM_ANDAMENTO': 'Em Andamento',
            'AGUARDANDO': 'Aguardando',
            'CONCLUIDO': 'Concluído'
        };

        this.priorityLabels = {
            'BAIXA': 'Baixa',
            'MEDIA': 'Média',
            'ALTA': 'Alta',
            'URGENTE': 'Urgente'
        };

        this.priorityIcons = {
            'BAIXA': 'bi-arrow-down',
            'MEDIA': 'bi-dash',
            'ALTA': 'bi-arrow-up',
            'URGENTE': 'bi-exclamation-triangle-fill'
        };

        this.init();
    }

    async init() {
        console.log('[KANBAN V3] 🚀 Inicializando Kanban Board Professional...');
        this.syncWithSystemTheme();
        this.observeThemeChanges();
        this.applyCustomTheme();
        this.showLoading();
        this.setupEventListeners();
        await this.fetchAllEvents();
        await this.checkDeadlines(); // Notificações de prazo
        this.hideLoading();
        this.renderCurrentView();
        this.startDeadlineChecker(); // Verifica prazos a cada minuto
        console.log('[KANBAN V3] ✅ Sistema inicializado com sucesso!');
    }

    setupEventListeners() {
        // Search
        const searchInput = document.getElementById('kanban-search');
        if (searchInput) {
            searchInput.addEventListener('input', (e) => {
                this.searchQuery = e.target.value.toLowerCase();
                this.filterAndRender();
            });
        }

        // Quick Filter buttons
        const filterButtons = document.querySelectorAll('[data-filter]');
        filterButtons.forEach(btn => {
            btn.addEventListener('click', () => {
                filterButtons.forEach(b => b.classList.remove('active'));
                btn.classList.add('active');
                this.currentFilter = btn.dataset.filter;
                this.filterAndRender();
            });
        });

        // View toggle (Board/List)
        const viewBtn = document.getElementById('kanban-view-toggle');
        if (viewBtn) {
            viewBtn.addEventListener('click', () => {
                this.toggleView();
            });
        }

        // Advanced filters button
        const advFiltersBtn = document.getElementById('kanban-advanced-filters');
        if (advFiltersBtn) {
            advFiltersBtn.addEventListener('click', () => {
                this.showAdvancedFiltersModal();
            });
        }

        // Export button
        const exportBtn = document.getElementById('kanban-export');
        if (exportBtn) {
            exportBtn.addEventListener('click', () => {
                this.showExportModal();
            });
        }

        // Theme selector
        const themeBtn = document.getElementById('kanban-theme-selector');
        if (themeBtn) {
            themeBtn.addEventListener('click', () => {
                this.showThemeSelector();
            });
        }

        // Sort controls
        const sortSelect = document.getElementById('kanban-sort');
        if (sortSelect) {
            sortSelect.addEventListener('change', (e) => {
                this.sortBy = e.target.value;
                localStorage.setItem('kanban-sort', this.sortBy);
                this.filterAndRender();
            });
        }

        const sortOrderBtn = document.getElementById('kanban-sort-order');
        if (sortOrderBtn) {
            sortOrderBtn.addEventListener('click', () => {
                this.sortOrder = this.sortOrder === 'asc' ? 'desc' : 'asc';
                localStorage.setItem('kanban-sort-order', this.sortOrder);
                sortOrderBtn.innerHTML = this.sortOrder === 'asc'
                    ? '<i class="bi bi-sort-up"></i>'
                    : '<i class="bi bi-sort-down"></i>';
                this.filterAndRender();
            });
        }

        // Modal close
        const modal = document.getElementById('kanban-modal');
        if (modal) {
            modal.addEventListener('click', (e) => {
                if (e.target === modal) {
                    this.closeModal();
                }
            });
        }

        // Setup drag and drop
        this.setupDragAndDrop();
    }

    setupDragAndDrop() {
        const columns = document.querySelectorAll('.kanban-column');

        columns.forEach(column => {
            const container = column.querySelector('.tasks-container');

            // Drag over column
            column.addEventListener('dragover', (e) => {
                e.preventDefault();
                column.classList.add('dragover');
            });

            column.addEventListener('dragleave', () => {
                column.classList.remove('dragover');
            });

            // Drop on column (muda status)
            column.addEventListener('drop', async (e) => {
                e.preventDefault();
                column.classList.remove('dragover');

                if (this.draggedCard) {
                    const newStatus = column.dataset.status;
                    const eventId = this.draggedCard.dataset.id;
                    const oldStatus = this.draggedCard.dataset.status;

                    if (oldStatus !== newStatus) {
                        // Move to new column
                        container.appendChild(this.draggedCard);
                        this.draggedCard.classList.add('slide-in');

                        // Update on backend
                        const success = await this.updateEventStatus(eventId, newStatus);

                        if (success) {
                            const event = this.events.find(e => e.id == eventId);
                            if (event) {
                                event.status = newStatus;
                                await this.logChange(eventId, 'status', oldStatus, newStatus);
                            }
                            this.updateColumnCounts();
                            this.showToast('Status atualizado com sucesso!', 'success');
                        } else {
                            this.showToast('Erro ao atualizar status', 'error');
                            this.renderCurrentView();
                        }
                    }
                }
            });

            // Sortable dentro da mesma coluna (reordenação)
            if (container) {
                this.makeSortable(container);
            }
        });

        // Setup add buttons
        const addButtons = document.querySelectorAll('.column-add-btn');
        addButtons.forEach(btn => {
            btn.addEventListener('click', () => {
                window.location.href = '/events/new';
            });
        });
    }

    /**
     * Torna um container sortable (arrastar para reordenar)
     */
    makeSortable(container) {
        container.addEventListener('dragover', (e) => {
            e.preventDefault();
            const afterElement = this.getDragAfterElement(container, e.clientY);
            const draggable = this.draggedCard;

            if (afterElement == null) {
                container.appendChild(draggable);
            } else {
                container.insertBefore(draggable, afterElement);
            }
        });
    }

    getDragAfterElement(container, y) {
        const draggableElements = [...container.querySelectorAll('.task-card:not(.dragging)')];

        return draggableElements.reduce((closest, child) => {
            const box = child.getBoundingClientRect();
            const offset = y - box.top - box.height / 2;

            if (offset < 0 && offset > closest.offset) {
                return { offset: offset, element: child };
            } else {
                return closest;
            }
        }, { offset: Number.NEGATIVE_INFINITY }).element;
    }

    async fetchAllEvents() {
        console.log('[KANBAN V3] 📡 Buscando todos os eventos...');

        try {
            const promises = this.statuses.map(status =>
                fetch(`/events/api/by-status/${status}`)
                    .then(res => {
                        if (!res.ok) throw new Error(`HTTP ${res.status}`);
                        return res.json();
                    })
                    .then(events => {
                        console.log(`[KANBAN V3] Status ${status}: ${events.length} eventos`);
                        return events;
                    })
            );

            const results = await Promise.all(promises);
            this.events = results.flat();
            this.filteredEvents = [...this.events];

            console.log(`[KANBAN V3] ✅ Total: ${this.events.length} eventos carregados`);
        } catch (error) {
            console.error('[KANBAN V3] ❌ Erro ao buscar eventos:', error);
            this.showToast('Erro ao carregar eventos', 'error');
        }
    }

    filterAndRender() {
        this.filteredEvents = this.events.filter(event => {
            // Quick filter (prioridade)
            if (this.currentFilter !== 'all' && event.prioridade !== this.currentFilter) {
                return false;
            }

            // Search query
            if (this.searchQuery) {
                const searchableText = [
                    event.titulo,
                    event.descricao,
                    event.partnerName,
                    event.vehiclePlate,
                    event.placaManual,
                    event.analistaResponsavel
                ].join(' ').toLowerCase();

                if (!searchableText.includes(this.searchQuery)) {
                    return false;
                }
            }

            // Advanced filters
            if (this.advancedFilters.dateFrom && event.dataVencimento) {
                const eventDate = new Date(event.dataVencimento);
                const fromDate = new Date(this.advancedFilters.dateFrom);
                if (eventDate < fromDate) return false;
            }

            if (this.advancedFilters.dateTo && event.dataVencimento) {
                const eventDate = new Date(event.dataVencimento);
                const toDate = new Date(this.advancedFilters.dateTo);
                if (eventDate > toDate) return false;
            }

            if (this.advancedFilters.partnerId && event.partnerId != this.advancedFilters.partnerId) {
                return false;
            }

            if (this.advancedFilters.vehicleId && event.vehicleId != this.advancedFilters.vehicleId) {
                return false;
            }

            if (this.advancedFilters.prioridade && event.prioridade !== this.advancedFilters.prioridade) {
                return false;
            }

            if (this.advancedFilters.motivo && event.motivo !== this.advancedFilters.motivo) {
                return false;
            }

            return true;
        });

        // Sort
        this.sortEvents();

        // Render
        this.renderCurrentView();
    }

    sortEvents() {
        this.filteredEvents.sort((a, b) => {
            let aVal = a[this.sortBy];
            let bVal = b[this.sortBy];

            // Handle nested objects
            if (this.sortBy === 'partner') {
                aVal = a.partnerName || '';
                bVal = b.partnerName || '';
            } else if (this.sortBy === 'vehicle') {
                aVal = a.vehiclePlate || a.placaManual || '';
                bVal = b.vehiclePlate || b.placaManual || '';
            }

            // Handle dates
            if (this.sortBy === 'dataVencimento' || this.sortBy === 'dataAconteceu') {
                aVal = aVal ? new Date(aVal) : new Date(0);
                bVal = bVal ? new Date(bVal) : new Date(0);
            }

            // Handle nulls
            if (!aVal && !bVal) return 0;
            if (!aVal) return 1;
            if (!bVal) return -1;

            // Compare
            let comparison = 0;
            if (aVal > bVal) comparison = 1;
            if (aVal < bVal) comparison = -1;

            return this.sortOrder === 'asc' ? comparison : -comparison;
        });
    }

    renderCurrentView() {
        if (this.currentView === 'list') {
            this.renderListView();
        } else {
            this.renderBoardView();
        }
    }

    renderBoardView() {
        console.log('[KANBAN V3] 🎨 Renderizando Board View...');
        console.log('[KANBAN V3] 📊 Eventos a renderizar:', this.filteredEvents);

        // Hide list, show board
        const listView = document.getElementById('kanban-list-view');
        const boardView = document.querySelector('.kanban-board');

        console.log('[KANBAN V3] 📋 Board element:', boardView);

        if (listView) listView.style.display = 'none';
        if (boardView) boardView.style.display = 'flex';

        this.statuses.forEach(status => {
            const container = document.getElementById(`column-${status}`);
            console.log(`[KANBAN V3] 📦 Container ${status}:`, container);

            if (!container) {
                console.error(`[KANBAN V3] ❌ Container não encontrado: column-${status}`);
                return;
            }

            container.innerHTML = '';

            const statusEvents = this.filteredEvents.filter(e => e.status === status);
            console.log(`[KANBAN V3] 🎯 Status ${status}: ${statusEvents.length} eventos`, statusEvents);

            this.updateColumnCount(status, statusEvents.length);

            statusEvents.forEach((event, index) => {
                console.log(`[KANBAN V3] 🎴 Criando card ${index + 1}/${statusEvents.length} para evento:`, event);
                try {
                    const card = this.createTaskCard(event);
                    console.log(`[KANBAN V3] ✅ Card criado:`, card);
                    container.appendChild(card);
                    console.log(`[KANBAN V3] ✅ Card adicionado ao container`);
                } catch (error) {
                    console.error(`[KANBAN V3] ❌ Erro ao criar card:`, error);
                }
            });

            if (statusEvents.length === 0) {
                container.classList.add('empty');
            } else {
                container.classList.remove('empty');
            }
        });

        console.log('[KANBAN V3] 🏁 Renderização concluída');
    }

    renderListView() {
        console.log('[KANBAN V3] 📋 Renderizando List View...');

        // Hide board, show list
        const listView = document.getElementById('kanban-list-view');
        const boardView = document.querySelector('.kanban-board');

        if (boardView) boardView.style.display = 'none';
        if (!listView) {
            this.createListView();
            return;
        }

        listView.style.display = 'block';
        const tbody = listView.querySelector('tbody');
        if (!tbody) return;

        tbody.innerHTML = '';

        this.filteredEvents.forEach(event => {
            const row = this.createListRow(event);
            tbody.appendChild(row);
        });
    }

    createListView() {
        const boardView = document.querySelector('.kanban-board');
        if (!boardView) return;

        const listView = document.createElement('div');
        listView.id = 'kanban-list-view';
        listView.className = 'kanban-list-view';
        listView.innerHTML = `
            <div class="list-view-container">
                <table class="table table-hover">
                    <thead>
                        <tr>
                            <th>Título</th>
                            <th>Status</th>
                            <th>Prioridade</th>
                            <th>Associado</th>
                            <th>Veículo</th>
                            <th>Vencimento</th>
                            <th>Ações</th>
                        </tr>
                    </thead>
                    <tbody></tbody>
                </table>
            </div>
        `;

        boardView.parentNode.insertBefore(listView, boardView.nextSibling);
        this.renderListView();
    }

    createListRow(event) {
        const row = document.createElement('tr');
        row.dataset.id = event.id;
        row.className = 'list-row fade-in';

        const statusBadge = this.getStatusBadge(event.status);
        const priorityBadge = this.renderPriorityBadge(event.prioridade);
        const deadline = this.renderDeadline(event.dataVencimento);

        row.innerHTML = `
            <td>
                <div class="fw-semibold">${this.escapeHtml(event.titulo)}</div>
                <small class="text-muted">${this.escapeHtml(event.descricao || '').substring(0, 50)}...</small>
            </td>
            <td>${statusBadge}</td>
            <td>${priorityBadge}</td>
            <td>${this.escapeHtml(event.partnerName || '-')}</td>
            <td>${this.escapeHtml(event.vehiclePlate || event.placaManual || '-')}</td>
            <td>${deadline}</td>
            <td>
                <div class="btn-group btn-group-sm">
                    <button class="btn btn-outline-primary btn-sm" onclick="kanbanBoard.editEventInline(${event.id})" title="Editar">
                        <i class="bi bi-pencil"></i>
                    </button>
                    <button class="btn btn-outline-info btn-sm" onclick="kanbanBoard.showEventDetails(${event.id})" title="Ver">
                        <i class="bi bi-eye"></i>
                    </button>
                    <button class="btn btn-outline-secondary btn-sm" onclick="kanbanBoard.showHistory(${event.id})" title="Histórico">
                        <i class="bi bi-clock-history"></i>
                    </button>
                </div>
            </td>
        `;

        return row;
    }

    getStatusBadge(status) {
        const colors = {
            'A_FAZER': 'secondary',
            'EM_ANDAMENTO': 'primary',
            'AGUARDANDO': 'warning',
            'CONCLUIDO': 'success'
        };

        return `<span class="badge bg-${colors[status]}">${this.statusLabels[status]}</span>`;
    }

    createTaskCard(event) {
        const card = document.createElement('div');
        card.className = 'task-card fade-in';
        card.draggable = true;
        card.dataset.id = event.id;
        card.dataset.status = event.status;

        // Drag events
        card.addEventListener('dragstart', () => {
            this.draggedCard = card;
            setTimeout(() => card.classList.add('dragging'), 0);
        });

        card.addEventListener('dragend', () => {
            card.classList.remove('dragging');
            this.draggedCard = null;
        });

        // Double click for inline edit
        card.addEventListener('dblclick', (e) => {
            if (!e.target.closest('.task-card-menu-btn')) {
                this.editEventInline(event.id);
            }
        });

        // Click to view details
        card.addEventListener('click', (e) => {
            if (!e.target.closest('.task-card-menu-btn') && e.detail === 1) {
                setTimeout(() => {
                    if (e.detail === 1) {
                        this.showEventDetails(event);
                    }
                }, 200);
            }
        });

        card.innerHTML = `
            <div class="task-card-header">
                <h4 class="task-card-title">${this.escapeHtml(event.titulo)}</h4>
                <div class="task-card-menu">
                    <button class="task-card-menu-btn" onclick="kanbanBoard.showCardMenu(event, ${event.id})">
                        <i class="bi bi-three-dots-vertical"></i>
                    </button>
                </div>
            </div>

            ${event.descricao ? `<p class="task-card-description">${this.escapeHtml(event.descricao)}</p>` : ''}

            <div class="task-card-meta">
                <div class="task-card-badges">
                    ${this.renderPriorityBadge(event.prioridade)}
                    ${this.renderMotivoBadge(event.motivo)}
                    ${event.envolvimento ? this.renderEnvolvimentoBadge(event.envolvimento) : ''}
                </div>
            </div>

            <div class="task-card-info">
                ${event.partnerName ? `
                    <div class="task-info-item">
                        <i class="bi bi-person-fill"></i>
                        <span><strong>${this.escapeHtml(event.partnerName)}</strong></span>
                    </div>
                ` : ''}
                ${event.vehiclePlate || event.placaManual ? `
                    <div class="task-info-item">
                        <i class="bi bi-car-front-fill"></i>
                        <span><strong>${this.escapeHtml(event.vehiclePlate || event.placaManual)}</strong></span>
                    </div>
                ` : ''}
            </div>

            <div class="task-card-footer">
                ${this.renderDeadline(event.dataVencimento)}
                ${event.analistaResponsavel ? this.renderAssignee(event.analistaResponsavel) : ''}
            </div>
        `;

        return card;
    }

    // ... (continuação nos próximos blocos - arquivo muito grande)

    syncWithSystemTheme() {
        const htmlElement = document.documentElement;
        const currentTheme = htmlElement.getAttribute('data-theme');
        const isDark = currentTheme === 'dark';

        console.log('[KANBAN V3] 🎨 Sincronizando com tema global:', currentTheme);

        const wrapper = document.querySelector('.kanban-wrapper');

        if (isDark) {
            wrapper?.classList.add('dark-mode');
        } else {
            wrapper?.classList.remove('dark-mode');
        }
    }

    observeThemeChanges() {
        const htmlElement = document.documentElement;

        const observer = new MutationObserver((mutations) => {
            mutations.forEach((mutation) => {
                if (mutation.type === 'attributes' && mutation.attributeName === 'data-theme') {
                    const newTheme = htmlElement.getAttribute('data-theme');
                    console.log('[KANBAN V3] 🔄 Tema global mudou para:', newTheme);
                    this.syncWithSystemTheme();
                }
            });
        });

        observer.observe(htmlElement, {
            attributes: true,
            attributeFilter: ['data-theme']
        });

        console.log('[KANBAN V3] 👁️ Observer de tema ativado');
    }

    // Continua no próximo arquivo...
}

// Initialize
let kanbanBoard;

// Função de inicialização que funciona independente do timing
function initKanbanBoard() {
    console.log('[KANBAN V3] 🎬 Inicializando sistema kanban...');

    // Verificar se já foi inicializado
    if (kanbanBoard) {
        console.log('[KANBAN V3] ⚠️ Sistema já inicializado, pulando...');
        return;
    }

    // Verificar se containers existem
    const containers = document.querySelectorAll('.tasks-container');
    if (containers.length === 0) {
        console.warn('[KANBAN V3] ⚠️ Containers não encontrados ainda, aguardando...');
        setTimeout(initKanbanBoard, 100);
        return;
    }

    console.log('[KANBAN V3] ✅ Containers encontrados:', containers.length);
    kanbanBoard = new KanbanBoard();
}

// Tentar inicializar de várias formas para garantir compatibilidade
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initKanbanBoard);
} else {
    // DOM já está pronto, inicializar imediatamente
    initKanbanBoard();
}
