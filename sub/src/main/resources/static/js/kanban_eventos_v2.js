/**
 * KANBAN BOARD - NOTION-INSPIRED
 * Enhanced version with modern UX features
 */

class KanbanBoard {
    constructor() {
        this.events = [];
        this.filteredEvents = [];
        this.draggedCard = null;
        this.currentFilter = 'all';
        this.searchQuery = '';
        this.csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
        this.csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

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
        console.log('[KANBAN] Inicializando Kanban Board v2...');
        this.syncWithSystemTheme(); // Sincroniza com tema global
        this.observeThemeChanges(); // Observa mudanças no tema
        this.showLoading();
        this.setupEventListeners();
        await this.fetchAllEvents();
        this.hideLoading();
        this.renderBoard();
        console.log('[KANBAN] Kanban Board inicializado com sucesso!');
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

        // Filter buttons
        const filterButtons = document.querySelectorAll('[data-filter]');
        filterButtons.forEach(btn => {
            btn.addEventListener('click', () => {
                filterButtons.forEach(b => b.classList.remove('active'));
                btn.classList.add('active');
                this.currentFilter = btn.dataset.filter;
                this.filterAndRender();
            });
        });

        // View toggle
        const viewBtn = document.getElementById('kanban-view-toggle');
        if (viewBtn) {
            viewBtn.addEventListener('click', () => {
                this.toggleView();
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

        // Setup drag and drop for columns
        this.setupDragAndDrop();
    }

    setupDragAndDrop() {
        const columns = document.querySelectorAll('.kanban-column');

        columns.forEach(column => {
            column.addEventListener('dragover', (e) => {
                e.preventDefault();
                column.classList.add('dragover');
            });

            column.addEventListener('dragleave', () => {
                column.classList.remove('dragover');
            });

            column.addEventListener('drop', async (e) => {
                e.preventDefault();
                column.classList.remove('dragover');

                if (this.draggedCard) {
                    const newStatus = column.dataset.status;
                    const eventId = this.draggedCard.dataset.id;
                    const container = column.querySelector('.tasks-container');

                    // Move card visually first (optimistic update)
                    container.appendChild(this.draggedCard);
                    this.draggedCard.classList.add('slide-in');

                    // Update on backend
                    const success = await this.updateEventStatus(eventId, newStatus);

                    if (success) {
                        // Update local data
                        const event = this.events.find(e => e.id == eventId);
                        if (event) {
                            event.status = newStatus;
                        }
                        this.updateColumnCounts();
                        this.showToast('Status atualizado com sucesso!', 'success');
                    } else {
                        // Revert on failure
                        this.showToast('Erro ao atualizar status', 'error');
                        this.renderBoard();
                    }
                }
            });
        });

        // Setup add buttons
        const addButtons = document.querySelectorAll('.column-add-btn');
        addButtons.forEach(btn => {
            btn.addEventListener('click', () => {
                window.location.href = '/events/new';
            });
        });
    }

    async fetchAllEvents() {
        console.log('[KANBAN] Buscando todos os eventos...');

        try {
            const promises = this.statuses.map(status =>
                fetch(`/events/api/by-status/${status}`)
                    .then(res => {
                        if (!res.ok) throw new Error(`HTTP ${res.status}`);
                        return res.json();
                    })
                    .then(events => {
                        console.log(`[KANBAN] Status ${status}: ${events.length} eventos`);
                        return events;
                    })
            );

            const results = await Promise.all(promises);
            this.events = results.flat();
            this.filteredEvents = [...this.events];

            console.log(`[KANBAN] Total de eventos carregados: ${this.events.length}`);
        } catch (error) {
            console.error('[KANBAN] Erro ao buscar eventos:', error);
            this.showToast('Erro ao carregar eventos', 'error');
        }
    }

    filterAndRender() {
        this.filteredEvents = this.events.filter(event => {
            // Filter by priority
            if (this.currentFilter !== 'all' && event.prioridade !== this.currentFilter) {
                return false;
            }

            // Filter by search query
            if (this.searchQuery) {
                const searchableText = [
                    event.titulo,
                    event.descricao,
                    event.partner?.name,
                    event.vehicle?.plaque,
                    event.analistaResponsavel
                ].join(' ').toLowerCase();

                if (!searchableText.includes(this.searchQuery)) {
                    return false;
                }
            }

            return true;
        });

        this.renderBoard();
    }

    renderBoard() {
        console.log('[KANBAN] Renderizando board...');

        this.statuses.forEach(status => {
            const container = document.getElementById(`column-${status}`);
            if (!container) return;

            // Clear container
            container.innerHTML = '';

            // Get events for this status
            const statusEvents = this.filteredEvents.filter(e => e.status === status);

            // Update count
            this.updateColumnCount(status, statusEvents.length);

            // Render cards
            statusEvents.forEach(event => {
                const card = this.createTaskCard(event);
                container.appendChild(card);
            });

            // Add empty state if needed
            if (statusEvents.length === 0) {
                container.classList.add('empty');
            } else {
                container.classList.remove('empty');
            }
        });
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

        // Click to view details
        card.addEventListener('click', (e) => {
            if (!e.target.closest('.task-card-menu-btn')) {
                this.showEventDetails(event);
            }
        });

        // Build card HTML
        card.innerHTML = `
            <div class="task-card-header">
                <h4 class="task-card-title">${this.escapeHtml(event.titulo)}</h4>
                <div class="task-card-menu">
                    <button class="task-card-menu-btn" onclick="window.location.href='/events/edit/${event.id}'">
                        <i class="bi bi-pencil"></i>
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
                ${event.partner ? `
                    <div class="task-info-item">
                        <i class="bi bi-person-fill"></i>
                        <span><strong>${this.escapeHtml(event.partner.name)}</strong></span>
                    </div>
                ` : ''}
                ${event.vehicle ? `
                    <div class="task-info-item">
                        <i class="bi bi-car-front-fill"></i>
                        <span><strong>${this.escapeHtml(event.vehicle.plaque)}</strong> - ${this.escapeHtml(event.vehicle.maker || '')} ${this.escapeHtml(event.vehicle.model || '')}</span>
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

    renderPriorityBadge(prioridade) {
        if (!prioridade) return '';

        const icon = this.priorityIcons[prioridade] || 'bi-dash';
        const label = this.priorityLabels[prioridade] || prioridade;

        return `
            <span class="task-badge priority-${prioridade}">
                <i class="bi ${icon}"></i>
                ${label}
            </span>
        `;
    }

    renderMotivoBadge(motivo) {
        if (!motivo) return '';

        const motivoLabels = {
            'COLISAO': 'Colisão',
            'ROUBO': 'Roubo',
            'FURTO': 'Furto',
            'NAO_INFORMADO': 'Não Informado',
            'VENTO_ALAGAMENTO_GRANIZO_ETC': 'Fenômenos Naturais',
            'VIDROS_E_PARA_BRISA': 'Vidros',
            'FAROIS_E_LANTERNAS': 'Faróis',
            'RETROVISORES': 'Retrovisores',
            'COBRANCA_FIDELIDADE': 'Cobrança'
        };

        return `
            <span class="task-badge motivo">
                <i class="bi bi-exclamation-circle"></i>
                ${motivoLabels[motivo] || motivo}
            </span>
        `;
    }

    renderEnvolvimentoBadge(envolvimento) {
        if (!envolvimento) return '';

        const envolvimentoLabels = {
            'CAUSADOR': 'Causador',
            'VITIMA': 'Vítima',
            'NAO_INFORMADO': 'Não Informado'
        };

        return `
            <span class="task-badge envolvimento">
                ${envolvimentoLabels[envolvimento] || envolvimento}
            </span>
        `;
    }

    renderDeadline(dataVencimento) {
        if (!dataVencimento) return '<span></span>';

        const deadline = new Date(dataVencimento + 'T00:00:00');
        const today = new Date();
        today.setHours(0, 0, 0, 0);

        const diffTime = deadline - today;
        const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));

        let className = 'task-deadline';
        let icon = 'bi-calendar';

        if (diffDays < 0) {
            className += ' overdue';
            icon = 'bi-exclamation-triangle-fill';
        } else if (diffDays === 0) {
            className += ' today';
            icon = 'bi-alarm-fill';
        } else if (diffDays <= 3) {
            className += ' soon';
            icon = 'bi-clock-fill';
        }

        const formattedDate = deadline.toLocaleDateString('pt-BR', {
            day: '2-digit',
            month: '2-digit',
            year: 'numeric'
        });

        return `
            <span class="${className}">
                <i class="bi ${icon}"></i>
                ${formattedDate}
            </span>
        `;
    }

    renderAssignee(analista) {
        if (!analista) return '';

        const initials = analista
            .split(' ')
            .map(word => word[0])
            .join('')
            .substring(0, 2)
            .toUpperCase();

        return `
            <div class="task-assignee">
                <div class="task-assignee-avatar">${initials}</div>
                <span>${this.escapeHtml(analista)}</span>
            </div>
        `;
    }

    updateColumnCount(status, count) {
        const column = document.querySelector(`.kanban-column[data-status="${status}"]`);
        if (!column) return;

        const countBadge = column.querySelector('.column-count');
        if (countBadge) {
            countBadge.textContent = count;
        }
    }

    updateColumnCounts() {
        this.statuses.forEach(status => {
            const count = this.events.filter(e => e.status === status).length;
            this.updateColumnCount(status, count);
        });
    }

    async updateEventStatus(eventId, newStatus) {
        console.log(`[KANBAN] Atualizando evento ${eventId} para status ${newStatus}`);

        try {
            const headers = {
                'Content-Type': 'application/json'
            };

            if (this.csrfToken && this.csrfHeader) {
                headers[this.csrfHeader] = this.csrfToken;
            }

            const response = await fetch(`/events/api/${eventId}/status`, {
                method: 'PUT',
                headers: headers,
                body: JSON.stringify({ status: newStatus })
            });

            if (!response.ok) {
                throw new Error(`HTTP ${response.status}`);
            }

            const result = await response.json();
            console.log('[KANBAN] Status atualizado com sucesso:', result);
            return true;
        } catch (error) {
            console.error('[KANBAN] Erro ao atualizar status:', error);
            return false;
        }
    }

    showEventDetails(event) {
        const modal = document.getElementById('kanban-modal');
        if (!modal) return;

        const modalBody = modal.querySelector('.kanban-modal-body');
        if (!modalBody) return;

        modalBody.innerHTML = `
            <div style="display: flex; flex-direction: column; gap: 24px;">
                <div>
                    <h3 style="margin: 0 0 8px 0; font-size: 24px;">${this.escapeHtml(event.titulo)}</h3>
                    <div style="display: flex; gap: 8px; flex-wrap: wrap;">
                        ${this.renderPriorityBadge(event.prioridade)}
                        ${this.renderMotivoBadge(event.motivo)}
                        ${event.envolvimento ? this.renderEnvolvimentoBadge(event.envolvimento) : ''}
                    </div>
                </div>

                ${event.descricao ? `
                    <div>
                        <h4 style="margin: 0 0 8px 0; color: var(--notion-text-light); font-size: 14px; font-weight: 600;">DESCRIÇÃO</h4>
                        <p style="margin: 0; line-height: 1.6; color: var(--notion-text);">${this.escapeHtml(event.descricao)}</p>
                    </div>
                ` : ''}

                <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 16px;">
                    ${event.partner ? `
                        <div>
                            <h4 style="margin: 0 0 8px 0; color: var(--notion-text-light); font-size: 14px; font-weight: 600;">ASSOCIADO</h4>
                            <p style="margin: 0; color: var(--notion-text);"><strong>${this.escapeHtml(event.partner.name)}</strong></p>
                            ${event.partner.cpf ? `<p style="margin: 4px 0 0 0; font-size: 13px; color: var(--notion-text-light);">CPF: ${this.escapeHtml(event.partner.cpf)}</p>` : ''}
                        </div>
                    ` : ''}

                    ${event.vehicle ? `
                        <div>
                            <h4 style="margin: 0 0 8px 0; color: var(--notion-text-light); font-size: 14px; font-weight: 600;">VEÍCULO</h4>
                            <p style="margin: 0; color: var(--notion-text);"><strong>${this.escapeHtml(event.vehicle.plaque)}</strong></p>
                            <p style="margin: 4px 0 0 0; font-size: 13px; color: var(--notion-text-light);">${this.escapeHtml(event.vehicle.maker || '')} ${this.escapeHtml(event.vehicle.model || '')}</p>
                        </div>
                    ` : ''}

                    ${event.dataVencimento ? `
                        <div>
                            <h4 style="margin: 0 0 8px 0; color: var(--notion-text-light); font-size: 14px; font-weight: 600;">VENCIMENTO</h4>
                            ${this.renderDeadline(event.dataVencimento)}
                        </div>
                    ` : ''}

                    ${event.analistaResponsavel ? `
                        <div>
                            <h4 style="margin: 0 0 8px 0; color: var(--notion-text-light); font-size: 14px; font-weight: 600;">RESPONSÁVEL</h4>
                            <p style="margin: 0; color: var(--notion-text);">${this.escapeHtml(event.analistaResponsavel)}</p>
                        </div>
                    ` : ''}
                </div>

                ${event.observacoes ? `
                    <div>
                        <h4 style="margin: 0 0 8px 0; color: var(--notion-text-light); font-size: 14px; font-weight: 600;">OBSERVAÇÕES</h4>
                        <p style="margin: 0; line-height: 1.6; color: var(--notion-text);">${this.escapeHtml(event.observacoes)}</p>
                    </div>
                ` : ''}

                <div style="display: flex; gap: 12px; padding-top: 16px; border-top: 1px solid var(--notion-border);">
                    <button onclick="window.location.href='/events/edit/${event.id}'" style="flex: 1; padding: 12px; background: var(--accent-blue); color: white; border: none; border-radius: 8px; cursor: pointer; font-weight: 600;">
                        <i class="bi bi-pencil"></i> Editar Evento
                    </button>
                    <button onclick="kanbanBoard.closeModal()" style="flex: 1; padding: 12px; background: var(--notion-hover); color: var(--notion-text); border: 1px solid var(--notion-border); border-radius: 8px; cursor: pointer; font-weight: 600;">
                        Fechar
                    </button>
                </div>
            </div>
        `;

        modal.classList.add('active');
    }

    closeModal() {
        const modal = document.getElementById('kanban-modal');
        if (modal) {
            modal.classList.remove('active');
        }
    }

    toggleView() {
        // Future feature: toggle between board and list view
        this.showToast('Visualização alternativa em breve!', 'info');
    }

    /**
     * Sincroniza o Kanban com o tema global do sistema
     * Lê o atributo data-theme do elemento <html>
     */
    syncWithSystemTheme() {
        const htmlElement = document.documentElement;
        const currentTheme = htmlElement.getAttribute('data-theme');
        const isDark = currentTheme === 'dark';

        console.log('[KANBAN] 🎨 Sincronizando com tema global:', currentTheme);

        const wrapper = document.querySelector('.kanban-wrapper');

        if (isDark) {
            wrapper?.classList.add('dark-mode');
        } else {
            wrapper?.classList.remove('dark-mode');
        }

        if (wrapper) {
            wrapper.setAttribute('data-color-mode', isDark ? 'dark' : 'light');
        }
    }

    /**
     * Observa mudanças no tema global e sincroniza automaticamente
     * Usa MutationObserver para detectar mudanças no atributo data-theme
     */
    observeThemeChanges() {
        const htmlElement = document.documentElement;

        // Cria observer para detectar mudanças no atributo data-theme
        const observer = new MutationObserver((mutations) => {
            mutations.forEach((mutation) => {
                if (mutation.type === 'attributes' && mutation.attributeName === 'data-theme') {
                    const newTheme = htmlElement.getAttribute('data-theme');
                    console.log('[KANBAN] 🔄 Tema global mudou para:', newTheme);
                    this.syncWithSystemTheme();
                }
            });
        });

        // Configura o observer
        observer.observe(htmlElement, {
            attributes: true,
            attributeFilter: ['data-theme']
        });

        console.log('[KANBAN] 👁️ Observer de tema ativado');
    }

    showLoading() {
        const board = document.querySelector('.kanban-board');
        if (board) {
            board.innerHTML = `
                <div class="kanban-loading">
                    <div class="kanban-loading-spinner"></div>
                    <p>Carregando eventos...</p>
                </div>
            `;
        }
    }

    hideLoading() {
        const loading = document.querySelector('.kanban-loading');
        if (loading) {
            loading.remove();
        }
    }

    showToast(message, type = 'info') {
        // Simple toast notification
        const toast = document.createElement('div');
        toast.className = `kanban-toast kanban-toast-${type}`;
        toast.textContent = message;
        toast.style.cssText = `
            position: fixed;
            top: 20px;
            right: 20px;
            padding: 16px 24px;
            background: ${type === 'success' ? '#10b981' : type === 'error' ? '#ef4444' : '#3b82f6'};
            color: white;
            border-radius: 8px;
            box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
            z-index: 10000;
            animation: slideInRight 0.3s ease;
        `;

        document.body.appendChild(toast);

        setTimeout(() => {
            toast.style.animation = 'slideOutRight 0.3s ease';
            setTimeout(() => toast.remove(), 300);
        }, 3000);
    }

    escapeHtml(text) {
        if (!text) return '';
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
}

// Initialize when DOM is ready
let kanbanBoard;
document.addEventListener('DOMContentLoaded', () => {
    console.log('[KANBAN] DOM carregado, inicializando...');
    kanbanBoard = new KanbanBoard();
});

// Add animations CSS
const style = document.createElement('style');
style.textContent = `
    @keyframes slideInRight {
        from {
            transform: translateX(100%);
            opacity: 0;
        }
        to {
            transform: translateX(0);
            opacity: 1;
        }
    }

    @keyframes slideOutRight {
        from {
            transform: translateX(0);
            opacity: 1;
        }
        to {
            transform: translateX(100%);
            opacity: 0;
        }
    }
`;
document.head.appendChild(style);
