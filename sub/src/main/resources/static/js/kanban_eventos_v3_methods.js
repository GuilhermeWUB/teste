/**
 * KANBAN BOARD V3 - MÉTODOS COMPLEMENTARES
 * Parte 2: Edição Inline, Export, Notificações, Temas, Histórico
 */

// Adiciona métodos ao protótipo da classe KanbanBoard
Object.assign(KanbanBoard.prototype, {

    /**
     * EDIÇÃO INLINE DE EVENTOS
     */
    async editEventInline(eventId) {
        const event = this.events.find(e => e.id == eventId);
        if (!event) return;

        console.log('[KANBAN V3] ✏️ Editando inline evento:', eventId);

        const modal = document.getElementById('kanban-modal');
        if (!modal) return;

        const modalBody = modal.querySelector('.kanban-modal-body');
        const modalTitle = modal.querySelector('.kanban-modal-header h2');

        if (modalTitle) modalTitle.textContent = 'Editar Evento';

        modalBody.innerHTML = `
            <form id="inline-edit-form" class="inline-edit-form">
                <div class="row g-3">
                    <div class="col-md-12">
                        <label class="form-label fw-bold">Título</label>
                        <input type="text" class="form-control" name="titulo" value="${this.escapeHtml(event.titulo)}" required>
                    </div>

                    <div class="col-md-12">
                        <label class="form-label fw-bold">Descrição</label>
                        <textarea class="form-control" name="descricao" rows="3">${this.escapeHtml(event.descricao || '')}</textarea>
                    </div>

                    <div class="col-md-6">
                        <label class="form-label fw-bold">Status</label>
                        <select class="form-select" name="status" required>
                            ${this.statuses.map(st => `
                                <option value="${st}" ${event.status === st ? 'selected' : ''}>
                                    ${this.statusLabels[st]}
                                </option>
                            `).join('')}
                        </select>
                    </div>

                    <div class="col-md-6">
                        <label class="form-label fw-bold">Prioridade</label>
                        <select class="form-select" name="prioridade">
                            <option value="">Selecione...</option>
                            ${Object.keys(this.priorityLabels).map(p => `
                                <option value="${p}" ${event.prioridade === p ? 'selected' : ''}>
                                    ${this.priorityLabels[p]}
                                </option>
                            `).join('')}
                        </select>
                    </div>

                    <div class="col-md-6">
                        <label class="form-label fw-bold">Data de Vencimento</label>
                        <input type="date" class="form-control" name="dataVencimento" value="${event.dataVencimento || ''}">
                    </div>

                    <div class="col-md-6">
                        <label class="form-label fw-bold">Analista Responsável</label>
                        <input type="text" class="form-control" name="analistaResponsavel" value="${this.escapeHtml(event.analistaResponsavel || '')}">
                    </div>

                    <div class="col-md-12">
                        <label class="form-label fw-bold">Observações</label>
                        <textarea class="form-control" name="observacoes" rows="2">${this.escapeHtml(event.observacoes || '')}</textarea>
                    </div>

                    <div class="col-12 d-flex gap-2 justify-content-end mt-4">
                        <button type="button" class="btn btn-secondary" onclick="kanbanBoard.closeModal()">
                            <i class="bi bi-x-circle"></i> Cancelar
                        </button>
                        <button type="submit" class="btn btn-primary">
                            <i class="bi bi-check-circle"></i> Salvar Alterações
                        </button>
                    </div>
                </div>
            </form>
        `;

        modal.classList.add('active');

        // Form submit
        const form = document.getElementById('inline-edit-form');
        form.addEventListener('submit', async (e) => {
            e.preventDefault();
            await this.saveInlineEdit(eventId, new FormData(form));
        });
    },

    async saveInlineEdit(eventId, formData) {
        const updates = {};
        formData.forEach((value, key) => {
            updates[key] = value;
        });

        console.log('[KANBAN V3] 💾 Salvando edição inline:', updates);

        try {
            const headers = {
                'Content-Type': 'application/json'
            };

            if (this.csrfToken && this.csrfHeader) {
                headers[this.csrfHeader] = this.csrfToken;
            }

            const response = await fetch(`/events/api/${eventId}/update`, {
                method: 'PUT',
                headers: headers,
                body: JSON.stringify(updates)
            });

            if (!response.ok) {
                throw new Error(`HTTP ${response.status}`);
            }

            const result = await response.json();

            // Atualiza evento local
            const event = this.events.find(e => e.id == eventId);
            if (event) {
                Object.assign(event, updates);
            }

            // Log change
            await this.logChange(eventId, 'update', 'Vários campos atualizados', JSON.stringify(updates));

            this.closeModal();
            this.filterAndRender();
            this.showToast('✅ Evento atualizado com sucesso!', 'success');
        } catch (error) {
            console.error('[KANBAN V3] ❌ Erro ao salvar:', error);
            this.showToast('Erro ao atualizar evento', 'error');
        }
    },

    /**
     * FILTROS AVANÇADOS
     */
    showAdvancedFiltersModal() {
        console.log('[KANBAN V3] 🔍 Abrindo filtros avançados...');

        const modal = document.getElementById('kanban-modal');
        if (!modal) return;

        const modalBody = modal.querySelector('.kanban-modal-body');
        const modalTitle = modal.querySelector('.kanban-modal-header h2');

        if (modalTitle) modalTitle.textContent = 'Filtros Avançados';

        modalBody.innerHTML = `
            <form id="advanced-filters-form" class="advanced-filters-form">
                <div class="row g-3">
                    <div class="col-md-6">
                        <label class="form-label fw-bold">Data Inicial</label>
                        <input type="date" class="form-control" name="dateFrom" value="${this.advancedFilters.dateFrom || ''}">
                    </div>

                    <div class="col-md-6">
                        <label class="form-label fw-bold">Data Final</label>
                        <input type="date" class="form-control" name="dateTo" value="${this.advancedFilters.dateTo || ''}">
                    </div>

                    <div class="col-md-6">
                        <label class="form-label fw-bold">Prioridade</label>
                        <select class="form-select" name="prioridade">
                            <option value="">Todas</option>
                            ${Object.keys(this.priorityLabels).map(p => `
                                <option value="${p}" ${this.advancedFilters.prioridade === p ? 'selected' : ''}>
                                    ${this.priorityLabels[p]}
                                </option>
                            `).join('')}
                        </select>
                    </div>

                    <div class="col-md-6">
                        <label class="form-label fw-bold">Motivo</label>
                        <select class="form-select" name="motivo">
                            <option value="">Todos</option>
                            <option value="COLISAO" ${this.advancedFilters.motivo === 'COLISAO' ? 'selected' : ''}>Colisão</option>
                            <option value="ROUBO" ${this.advancedFilters.motivo === 'ROUBO' ? 'selected' : ''}>Roubo</option>
                            <option value="FURTO" ${this.advancedFilters.motivo === 'FURTO' ? 'selected' : ''}>Furto</option>
                        </select>
                    </div>

                    <div class="col-12 d-flex gap-2 justify-content-end mt-4">
                        <button type="button" class="btn btn-secondary" onclick="kanbanBoard.clearAdvancedFilters()">
                            <i class="bi bi-x-circle"></i> Limpar Filtros
                        </button>
                        <button type="submit" class="btn btn-primary">
                            <i class="bi bi-funnel-fill"></i> Aplicar Filtros
                        </button>
                    </div>
                </div>
            </form>
        `;

        modal.classList.add('active');

        const form = document.getElementById('advanced-filters-form');
        form.addEventListener('submit', (e) => {
            e.preventDefault();
            this.applyAdvancedFilters(new FormData(form));
        });
    },

    applyAdvancedFilters(formData) {
        formData.forEach((value, key) => {
            this.advancedFilters[key] = value || null;
        });

        console.log('[KANBAN V3] ✅ Filtros aplicados:', this.advancedFilters);

        this.closeModal();
        this.filterAndRender();
        this.showToast('Filtros aplicados!', 'info');
    },

    clearAdvancedFilters() {
        this.advancedFilters = {
            dateFrom: null,
            dateTo: null,
            partnerId: null,
            vehicleId: null,
            prioridade: null,
            motivo: null
        };

        console.log('[KANBAN V3] 🧹 Filtros limpos');

        this.closeModal();
        this.filterAndRender();
        this.showToast('Filtros removidos!', 'info');
    },

    /**
     * EXPORTAÇÃO PDF/EXCEL
     */
    showExportModal() {
        console.log('[KANBAN V3] 📥 Abrindo opções de exportação...');

        const modal = document.getElementById('kanban-modal');
        if (!modal) return;

        const modalBody = modal.querySelector('.kanban-modal-body');
        const modalTitle = modal.querySelector('.kanban-modal-header h2');

        if (modalTitle) modalTitle.textContent = 'Exportar Eventos';

        modalBody.innerHTML = `
            <div class="export-options">
                <div class="row g-3">
                    <div class="col-12">
                        <h5>Selecione o formato de exportação:</h5>
                    </div>

                    <div class="col-md-6">
                        <button class="btn btn-lg btn-outline-danger w-100 export-btn" onclick="kanbanBoard.exportToPDF()">
                            <i class="bi bi-file-earmark-pdf display-4 d-block mb-2"></i>
                            <strong>Exportar para PDF</strong>
                            <p class="small text-muted mb-0">Documento formatado para impressão</p>
                        </button>
                    </div>

                    <div class="col-md-6">
                        <button class="btn btn-lg btn-outline-success w-100 export-btn" onclick="kanbanBoard.exportToExcel()">
                            <i class="bi bi-file-earmark-excel display-4 d-block mb-2"></i>
                            <strong>Exportar para Excel</strong>
                            <p class="small text-muted mb-0">Planilha editável (.xlsx)</p>
                        </button>
                    </div>

                    <div class="col-12 mt-4">
                        <div class="alert alert-info">
                            <i class="bi bi-info-circle"></i>
                            <strong>Nota:</strong> Serão exportados ${this.filteredEvents.length} eventos com base nos filtros atuais.
                        </div>
                    </div>

                    <div class="col-12 d-flex justify-content-end">
                        <button class="btn btn-secondary" onclick="kanbanBoard.closeModal()">
                            <i class="bi bi-x-circle"></i> Cancelar
                        </button>
                    </div>
                </div>
            </div>
        `;

        modal.classList.add('active');
    },

    async exportToPDF() {
        console.log('[KANBAN V3] 📄 Exportando para PDF...');
        this.showToast('Gerando PDF...', 'info');

        try {
            const headers = {};
            if (this.csrfToken && this.csrfHeader) {
                headers[this.csrfHeader] = this.csrfToken;
            }

            const eventIds = this.filteredEvents.map(e => e.id).join(',');
            const response = await fetch(`/events/api/export/pdf?ids=${eventIds}`, {
                headers: headers
            });

            if (!response.ok) {
                throw new Error(`HTTP ${response.status}`);
            }

            const blob = await response.blob();
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = `eventos_${new Date().toISOString().split('T')[0]}.pdf`;
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
            window.URL.revokeObjectURL(url);

            this.closeModal();
            this.showToast('✅ PDF gerado com sucesso!', 'success');
        } catch (error) {
            console.error('[KANBAN V3] ❌ Erro ao exportar PDF:', error);
            this.showToast('Erro ao gerar PDF', 'error');
        }
    },

    async exportToExcel() {
        console.log('[KANBAN V3] 📊 Exportando para Excel...');
        this.showToast('Gerando Excel...', 'info');

        try {
            // Gera CSV (compatível com Excel)
            const headers = ['ID', 'Título', 'Descrição', 'Status', 'Prioridade', 'Motivo', 'Associado', 'Veículo', 'Vencimento', 'Responsável'];
            const rows = this.filteredEvents.map(e => [
                e.id,
                e.titulo,
                e.descricao || '',
                this.statusLabels[e.status],
                this.priorityLabels[e.prioridade] || '',
                e.motivo || '',
                e.partner?.name || '',
                e.vehicle?.plaque || '',
                e.dataVencimento || '',
                e.analistaResponsavel || ''
            ]);

            let csvContent = headers.join(',') + '\n';
            rows.forEach(row => {
                csvContent += row.map(cell => `"${String(cell).replace(/"/g, '""')}"`).join(',') + '\n';
            });

            const blob = new Blob(['\ufeff' + csvContent], { type: 'text/csv;charset=utf-8;' });
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = `eventos_${new Date().toISOString().split('T')[0]}.csv`;
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
            window.URL.revokeObjectURL(url);

            this.closeModal();
            this.showToast('✅ Excel gerado com sucesso!', 'success');
        } catch (error) {
            console.error('[KANBAN V3] ❌ Erro ao exportar Excel:', error);
            this.showToast('Erro ao gerar Excel', 'error');
        }
    },

    /**
     * NOTIFICAÇÕES DE PRAZO
     */
    async checkDeadlines() {
        console.log('[KANBAN V3] ⏰ Verificando prazos...');

        const today = new Date();
        today.setHours(0, 0, 0, 0);

        const overdueEvents = [];
        const todayEvents = [];
        const soonEvents = [];

        this.events.forEach(event => {
            if (!event.dataVencimento || event.status === 'CONCLUIDO') return;

            const deadline = new Date(event.dataVencimento + 'T00:00:00');
            const diffTime = deadline - today;
            const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));

            if (diffDays < 0) {
                overdueEvents.push(event);
            } else if (diffDays === 0) {
                todayEvents.push(event);
            } else if (diffDays <= 3) {
                soonEvents.push(event);
            }
        });

        if (overdueEvents.length > 0) {
            this.showDeadlineNotification('overdue', overdueEvents.length);
        }

        if (todayEvents.length > 0) {
            this.showDeadlineNotification('today', todayEvents.length);
        }

        if (soonEvents.length > 0) {
            this.showDeadlineNotification('soon', soonEvents.length);
        }
    },

    showDeadlineNotification(type, count) {
        const messages = {
            'overdue': `⚠️ ${count} evento(s) atrasado(s)!`,
            'today': `📅 ${count} evento(s) vence(m) hoje!`,
            'soon': `⏰ ${count} evento(s) vence(m) em breve!`
        };

        const types = {
            'overdue': 'error',
            'today': 'warning',
            'soon': 'info'
        };

        this.showToast(messages[type], types[type]);
    },

    startDeadlineChecker() {
        // Verifica prazos a cada 5 minutos
        setInterval(() => {
            this.checkDeadlines();
        }, 5 * 60 * 1000);
    },

    /**
     * TEMAS CUSTOMIZÁVEIS
     */
    showThemeSelector() {
        console.log('[KANBAN V3] 🎨 Abrindo seletor de temas...');

        const modal = document.getElementById('kanban-modal');
        if (!modal) return;

        const modalBody = modal.querySelector('.kanban-modal-body');
        const modalTitle = modal.querySelector('.kanban-modal-header h2');

        if (modalTitle) modalTitle.textContent = 'Selecionar Tema';

        const themes = [
            { id: 'default', name: 'Padrão', color: '#2383e2' },
            { id: 'purple', name: 'Roxo', color: '#9333ea' },
            { id: 'green', name: 'Verde', color: '#16a34a' },
            { id: 'orange', name: 'Laranja', color: '#f59e0b' },
            { id: 'pink', name: 'Rosa', color: '#ec4899' },
            { id: 'teal', name: 'Azul Petróleo', color: '#14b8a6' }
        ];

        modalBody.innerHTML = `
            <div class="theme-selector">
                <div class="row g-3">
                    ${themes.map(theme => `
                        <div class="col-md-4">
                            <button class="btn btn-outline-primary w-100 theme-btn ${this.theme === theme.id ? 'active' : ''}"
                                    onclick="kanbanBoard.applyCustomTheme('${theme.id}')"
                                    style="border-color: ${theme.color};">
                                <div class="theme-preview" style="background: ${theme.color}; width: 100%; height: 60px; border-radius: 8px; margin-bottom: 8px;"></div>
                                <strong>${theme.name}</strong>
                            </button>
                        </div>
                    `).join('')}
                </div>

                <div class="mt-4 d-flex justify-content-end">
                    <button class="btn btn-secondary" onclick="kanbanBoard.closeModal()">
                        <i class="bi bi-x-circle"></i> Fechar
                    </button>
                </div>
            </div>
        `;

        modal.classList.add('active');
    },

    applyCustomTheme(themeId) {
        console.log('[KANBAN V3] 🎨 Aplicando tema:', themeId);

        this.theme = themeId;
        localStorage.setItem('kanban-custom-theme', themeId);

        const themes = {
            'default': '#2383e2',
            'purple': '#9333ea',
            'green': '#16a34a',
            'orange': '#f59e0b',
            'pink': '#ec4899',
            'teal': '#14b8a6'
        };

        document.documentElement.style.setProperty('--accent-blue', themes[themeId]);

        this.closeModal();
        this.showToast(`Tema ${themeId} aplicado!`, 'success');
    },

    /**
     * HISTÓRICO DE MUDANÇAS
     */
    async showHistory(eventId) {
        console.log('[KANBAN V3] 📜 Mostrando histórico do evento:', eventId);

        const modal = document.getElementById('kanban-modal');
        if (!modal) return;

        const modalBody = modal.querySelector('.kanban-modal-body');
        const modalTitle = modal.querySelector('.kanban-modal-header h2');

        if (modalTitle) modalTitle.textContent = 'Histórico de Alterações';

        modalBody.innerHTML = '<div class="text-center py-5"><div class="spinner-border"></div><p class="mt-3">Carregando histórico...</p></div>';
        modal.classList.add('active');

        try {
            const headers = {};
            if (this.csrfToken && this.csrfHeader) {
                headers[this.csrfHeader] = this.csrfToken;
            }

            const response = await fetch(`/events/api/${eventId}/history`, {
                headers: headers
            });

            if (!response.ok) {
                throw new Error(`HTTP ${response.status}`);
            }

            const history = await response.json();

            if (!history || history.length === 0) {
                modalBody.innerHTML = `
                    <div class="alert alert-info">
                        <i class="bi bi-info-circle"></i> Nenhuma alteração registrada para este evento.
                    </div>
                `;
                return;
            }

            modalBody.innerHTML = `
                <div class="history-timeline">
                    ${history.map(entry => `
                        <div class="history-entry">
                            <div class="history-icon">
                                <i class="bi bi-clock-history"></i>
                            </div>
                            <div class="history-content">
                                <div class="history-header">
                                    <strong>${entry.action}</strong>
                                    <span class="text-muted small">${new Date(entry.timestamp).toLocaleString('pt-BR')}</span>
                                </div>
                                <div class="history-details">
                                    ${entry.oldValue ? `<span class="badge bg-danger">De: ${entry.oldValue}</span>` : ''}
                                    ${entry.newValue ? `<span class="badge bg-success">Para: ${entry.newValue}</span>` : ''}
                                </div>
                            </div>
                        </div>
                    `).join('')}
                </div>

                <div class="mt-4 d-flex justify-content-end">
                    <button class="btn btn-secondary" onclick="kanbanBoard.closeModal()">
                        <i class="bi bi-x-circle"></i> Fechar
                    </button>
                </div>
            `;
        } catch (error) {
            console.error('[KANBAN V3] ❌ Erro ao buscar histórico:', error);
            modalBody.innerHTML = `
                <div class="alert alert-danger">
                    <i class="bi bi-exclamation-triangle"></i> Erro ao carregar histórico
                </div>
            `;
        }
    },

    async logChange(eventId, field, oldValue, newValue) {
        console.log('[KANBAN V3] 📝 Registrando mudança:', { eventId, field, oldValue, newValue });

        try {
            const headers = {
                'Content-Type': 'application/json'
            };

            if (this.csrfToken && this.csrfHeader) {
                headers[this.csrfHeader] = this.csrfToken;
            }

            await fetch(`/events/api/${eventId}/history`, {
                method: 'POST',
                headers: headers,
                body: JSON.stringify({
                    field,
                    oldValue,
                    newValue,
                    timestamp: new Date().toISOString()
                })
            });
        } catch (error) {
            console.error('[KANBAN V3] ❌ Erro ao registrar mudança:', error);
        }
    },

    /**
     * HELPER METHODS
     */
    toggleView() {
        this.currentView = this.currentView === 'board' ? 'list' : 'board';
        localStorage.setItem('kanban-view', this.currentView);

        console.log('[KANBAN V3] 👁️ Mudando visualização para:', this.currentView);

        const viewBtn = document.getElementById('kanban-view-toggle');
        if (viewBtn) {
            const icon = viewBtn.querySelector('i');
            if (icon) {
                icon.className = this.currentView === 'list' ? 'bi bi-kanban' : 'bi bi-list-ul';
            }
        }

        this.renderCurrentView();
        this.showToast(`Visualização: ${this.currentView === 'list' ? 'Lista' : 'Board'}`, 'info');
    },

    showEventDetails(event) {
        if (typeof event === 'number') {
            event = this.events.find(e => e.id === event);
        }
        if (!event) return;

        // Implementação já existente no arquivo principal
        console.log('[KANBAN V3] 👁️ Mostrando detalhes:', event.id);
    },

    updateColumnCount(status, count) {
        const column = document.querySelector(`.kanban-column[data-status="${status}"]`);
        if (!column) return;

        const countBadge = column.querySelector('.column-count');
        if (countBadge) {
            countBadge.textContent = count;
        }
    },

    updateColumnCounts() {
        this.statuses.forEach(status => {
            const count = this.events.filter(e => e.status === status).length;
            this.updateColumnCount(status, count);
        });
    },

    async updateEventStatus(eventId, newStatus) {
        console.log(`[KANBAN V3] 🔄 Atualizando status do evento ${eventId} para ${newStatus}`);

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

            return true;
        } catch (error) {
            console.error('[KANBAN V3] ❌ Erro ao atualizar status:', error);
            return false;
        }
    },

    closeModal() {
        const modal = document.getElementById('kanban-modal');
        if (modal) {
            modal.classList.remove('active');
        }
    },

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
    },

    hideLoading() {
        const loading = document.querySelector('.kanban-loading');
        if (loading) {
            loading.remove();
        }
    },

    showToast(message, type = 'info') {
        const toast = document.createElement('div');
        toast.className = `kanban-toast kanban-toast-${type}`;
        toast.textContent = message;
        toast.style.cssText = `
            position: fixed;
            top: 20px;
            right: 20px;
            padding: 16px 24px;
            background: ${type === 'success' ? '#10b981' : type === 'error' ? '#ef4444' : type === 'warning' ? '#f59e0b' : '#3b82f6'};
            color: white;
            border-radius: 8px;
            box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
            z-index: 10000;
            animation: slideInRight 0.3s ease;
            max-width: 300px;
        `;

        document.body.appendChild(toast);

        setTimeout(() => {
            toast.style.animation = 'slideOutRight 0.3s ease';
            setTimeout(() => toast.remove(), 300);
        }, 3000);
    },

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
    },

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
    },

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
    },

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
    },

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
    },

    showCardMenu(event, eventId) {
        event.stopPropagation();
        console.log('[KANBAN V3] 📋 Menu do card:', eventId);
        // Implementar menu contextual aqui
    },

    escapeHtml(text) {
        if (!text) return '';
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
});
