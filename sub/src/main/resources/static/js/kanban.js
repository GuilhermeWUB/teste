(function () {
    const state = {
        cards: [],
        search: "",
        quickFilter: "all"
    };

    const selectors = {
        columns: () => Array.from(document.querySelectorAll('.kanban-column')),
        searchInput: () => document.getElementById('kanban-search'),
        filterButtons: () => Array.from(document.querySelectorAll('.kanban-filter-btn')),
        modal: () => document.getElementById('kanban-modal'),
        modalBody: () => document.querySelector('#kanban-modal .kanban-modal-body')
    };

    function init() {
        if (!document.querySelector('.kanban-board')) {
            return;
        }
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

        selectors.filterButtons().forEach(button => {
            button.addEventListener('click', () => {
                selectors.filterButtons().forEach(btn => btn.classList.remove('active'));
                button.classList.add('active');
                state.quickFilter = button.dataset.filter || 'all';
                render();
            });
        });

        const modal = selectors.modal();
        if (modal) {
            modal.addEventListener('click', event => {
                if (event.target === modal) {
                    closeModal();
                }
            });

            document.addEventListener('keydown', event => {
                if (event.key === 'Escape' && modal.classList.contains('active')) {
                    closeModal();
                }
            });
        }
    }

    async function loadBoard() {
        try {
            const response = await fetch('/events/api/board', {
                headers: { 'Accept': 'application/json' }
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
        } catch (error) {
            console.error('[KANBAN] Falha ao carregar eventos:', error);
            renderError();
        }
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

            container.innerHTML = '';

            const items = grouped[status] || [];
            if (items.length === 0) {
                container.appendChild(createEmptyState());
                return;
            }

            items.forEach(card => container.appendChild(createCard(card)));
        });
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
        const quick = state.quickFilter;

        return cards.filter(card => {
            const matchesFilter = quick === 'all' || (card.prioridade && card.prioridade === quick);
            if (!matchesFilter) {
                return false;
            }

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
            const status = card.status || 'A_FAZER';
            acc[status] = acc[status] || [];
            acc[status].push(card);
            return acc;
        }, {});
    }

    function createCard(card) {
        const article = document.createElement('article');
        article.className = 'task-card';
        article.dataset.id = card.id;

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

    function buildModalContent(card) {
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
            { label: 'Data de vencimento', value: card.dataVencimento ? formatDate(card.dataVencimento) : null }
        ];

        const html = [
            '<div class="kanban-modal-grid">',
            ...fields
                .filter(field => Boolean(field.value))
                .map(field => `  <div class="kanban-modal-row"><span>${field.label}:</span><strong>${escapeHtml(field.value)}</strong></div>`),
            '</div>'
        ];
        return html.join('\n');
    }

    function escapeHtml(value) {
        return String(value)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
    }

    window.kanbanBoard = {
        init,
        closeModal
    };

    document.addEventListener('DOMContentLoaded', init);
})();
