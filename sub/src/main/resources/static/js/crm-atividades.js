(function () {
    const today = new Date();
    let activities = [];

    const statusLabels = {
        'atrasada': 'Atrasada',
        'para-hoje': 'Para hoje',
        'planejada': 'Planejada',
        'concluida': 'Concluída'
    };

    const listContainer = document.getElementById('activities-list');
    const emptyState = document.getElementById('activities-empty');
    const loadingState = document.getElementById('activities-loading');
    const errorState = document.getElementById('activities-error');
    const searchInput = document.getElementById('search-input');
    const responsibleSelect = document.getElementById('responsavel-select');
    const periodButtons = document.querySelectorAll('.period-button');
    const applyPeriodButton = document.getElementById('apply-period');
    const applyFiltersButton = document.getElementById('apply-filters');
    const typeCheckboxes = document.querySelectorAll('.checkbox-list input[type="checkbox"]');
    const tabs = document.querySelectorAll('[data-status-filter]');
    const form = document.getElementById('activity-form');
    const feedback = document.getElementById('activity-feedback');
    const newActivityButton = document.getElementById('open-new-activity');
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

    const state = {
        status: 'atrasada',
        search: '',
        responsible: '',
        period: 'hoje',
        startDate: null,
        endDate: null,
        types: new Set(['calculo', 'ligacao', 'falecomigoloja', 'falecomigoportal', 'lead'])
    };

    init();

    function init() {
        hydrateFormDefaults();
        bindFilters();
        bindForm();
        bindHeaderButton();
        loadActivities();
    }

    function hydrateFormDefaults() {
        const dateInput = document.getElementById('activity-date');
        const timeInput = document.getElementById('activity-time');
        if (dateInput) {
            dateInput.valueAsDate = today;
        }
        if (timeInput) {
            const now = new Date();
            timeInput.value = `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`;
        }
    }

    function bindHeaderButton() {
        if (!newActivityButton || !form) return;
        newActivityButton.addEventListener('click', () => {
            form.scrollIntoView({ behavior: 'smooth', block: 'start' });
            const title = document.getElementById('activity-title');
            title?.focus();
        });
    }

    function bindFilters() {
        tabs.forEach(tab => {
            tab.addEventListener('click', () => {
                tabs.forEach(item => {
                    item.classList.remove('active');
                    item.setAttribute('aria-selected', 'false');
                });
                tab.classList.add('active');
                tab.setAttribute('aria-selected', 'true');
                state.status = tab.dataset.statusFilter;
                renderActivities();
            });
        });

        searchInput?.addEventListener('input', event => {
            state.search = event.target.value.toLowerCase();
            renderActivities();
        });

        responsibleSelect?.addEventListener('change', event => {
            state.responsible = event.target.value;
            renderActivities();
        });

        periodButtons.forEach(button => {
            button.addEventListener('click', () => {
                periodButtons.forEach(item => item.classList.remove('active'));
                button.classList.add('active');
                state.period = button.dataset.period;
                renderActivities();
            });
        });

        applyPeriodButton?.addEventListener('click', () => {
            const start = document.getElementById('period-start').value;
            const end = document.getElementById('period-end').value;
            state.startDate = start ? new Date(start) : null;
            state.endDate = end ? new Date(end) : null;
            state.period = 'custom';
            periodButtons.forEach(item => item.classList.remove('active'));
            renderActivities();
        });

        applyFiltersButton?.addEventListener('click', () => {
            const selected = Array.from(typeCheckboxes)
                .filter(input => input.checked)
                .map(input => input.value);
            state.types = new Set(selected);
            renderActivities();
        });
    }

    function bindForm() {
        if (!form) return;
        form.addEventListener('submit', async (event) => {
            event.preventDefault();
            clearFeedback();
            const payload = buildPayloadFromForm();
            if (!payload) {
                return;
            }

            setFormSubmitting(true);
            try {
                const created = await submitActivity(payload);
                activities.push(normalizeActivity(created));
                updateSummary();
                updateTabs();
                renderActivities();
                form.reset();
                hydrateFormDefaults();
                showFeedback('Atividade criada e listada com sucesso.', 'success');
            } catch (error) {
                showFeedback(error.message || 'Não foi possível salvar a atividade.', 'error');
            } finally {
                setFormSubmitting(false);
            }
        });
    }

    function buildPayloadFromForm() {
        const title = document.getElementById('activity-title')?.value.trim();
        const responsible = document.getElementById('activity-responsible')?.value.trim();
        const leadSource = document.getElementById('activity-lead')?.value.trim();
        const status = document.getElementById('activity-status')?.value;
        const type = document.getElementById('activity-type')?.value;
        const dueDate = document.getElementById('activity-date')?.value;
        const dueTime = document.getElementById('activity-time')?.value;

        if (!title || !responsible || !leadSource || !status || !type || !dueDate || !dueTime) {
            showFeedback('Preencha os campos obrigatórios: título, responsável, origem, status, tipo, data e hora.', 'error');
            return null;
        }

        return {
            title,
            responsible,
            leadSource,
            status,
            type,
            city: document.getElementById('activity-city')?.value.trim() || null,
            state: document.getElementById('activity-state')?.value.trim() || null,
            description: document.getElementById('activity-description')?.value.trim() || null,
            dueDate,
            dueTime
        };
    }

    async function submitActivity(payload) {
        const headers = { 'Content-Type': 'application/json' };
        if (csrfHeader && csrfToken) {
            headers[csrfHeader] = csrfToken;
        }

        const response = await fetch('/api/crm/atividades', {
            method: 'POST',
            headers,
            body: JSON.stringify(payload)
        });

        if (!response.ok) {
            const errorMessage = await response.text();
            throw new Error(errorMessage || 'Erro ao salvar atividade.');
        }

        return response.json();
    }

    async function loadActivities() {
        if (loadingState) loadingState.hidden = false;
        if (errorState) errorState.hidden = true;

        try {
            const response = await fetch('/api/crm/atividades');
            if (!response.ok) {
                throw new Error('Não foi possível carregar as atividades.');
            }

            const data = await response.json();
            activities = data.map(normalizeActivity);
            updateSummary();
            updateTabs();
            renderActivities();
        } catch (error) {
            console.error(error);
            if (errorState) errorState.hidden = false;
        } finally {
            if (loadingState) loadingState.hidden = true;
        }
    }

    function normalizeActivity(activity) {
        const due = new Date(activity.dueAt);
        return {
            ...activity,
            dueAt: due,
            time: formatTime(due)
        };
    }

    function renderActivities() {
        listContainer.querySelectorAll('.activity-card').forEach(el => el.remove());

        if (loadingState && !loadingState.hidden) return;

        const filtered = activities.filter(activity =>
            matchesStatus(activity) &&
            matchesSearch(activity) &&
            matchesResponsible(activity) &&
            matchesType(activity) &&
            matchesPeriod(activity)
        );

        if (!filtered.length) {
            emptyState.hidden = false;
            return;
        }

        emptyState.hidden = true;
        const fragment = document.createDocumentFragment();
        filtered
            .sort((a, b) => new Date(a.dueAt) - new Date(b.dueAt))
            .forEach(activity => fragment.appendChild(buildCard(activity)));

        listContainer.appendChild(fragment);
    }

    function buildCard(activity) {
        const card = document.createElement('article');
        card.className = 'activity-card';

        const time = document.createElement('div');
        time.className = 'activity-time';
        time.innerHTML = `<span class="time-label">${activity.time}</span><span class="time-day">${formatDay(activity.dueAt)}</span>`;

        const content = document.createElement('div');
        content.className = 'activity-content';
        content.innerHTML = `
            <h3>${activity.title}</h3>
            <p class="activity-meta">${activity.description || 'Sem descrição adicional.'}</p>
        `;

        const badges = document.createElement('div');
        badges.className = 'activity-badges';
        if (activity.leadSource) {
            badges.appendChild(createBadge('info', activity.leadSource));
        }
        if (activity.city || activity.state) {
            const location = `${activity.city || ''}${activity.city && activity.state ? ' - ' : ''}${activity.state || ''}`;
            badges.appendChild(createBadge('warning', location || 'Local indefinido'));
        }
        if (activity.responsible) {
            badges.appendChild(createBadge('danger', `Responsável: ${formatResponsible(activity.responsible)}`));
        }
        content.appendChild(badges);

        const actions = document.createElement('div');
        actions.className = 'activity-actions';
        actions.innerHTML = `
            <span class="activity-badge-status ${statusClass(activity.status)}">${statusLabels[activity.status]}</span>
            <span class="secondary"><i class="bi bi-check2-square"></i> Marcar como concluída</span>
        `;

        card.appendChild(time);
        card.appendChild(content);
        card.appendChild(actions);
        return card;
    }

    function createBadge(type, label) {
        const badge = document.createElement('span');
        badge.className = `badge ${type}`;
        badge.textContent = label;
        return badge;
    }

    function matchesStatus(activity) {
        return activity.status === state.status;
    }

    function matchesSearch(activity) {
        if (!state.search) return true;
        const target = `${activity.title} ${activity.description || ''} ${activity.leadSource || ''}`.toLowerCase();
        return target.includes(state.search);
    }

    function matchesResponsible(activity) {
        if (!state.responsible) return true;
        return activity.responsible === state.responsible;
    }

    function matchesType(activity) {
        return state.types.has(activity.type);
    }

    function matchesPeriod(activity) {
        const activityDate = new Date(activity.dueAt);
        const startOfToday = startOfDay(today);
        const diffDays = Math.round((activityDate - startOfToday) / (1000 * 60 * 60 * 24));

        if (state.period === 'custom') {
            if (state.startDate && activityDate < startOfDay(state.startDate)) return false;
            if (state.endDate && activityDate > endOfDay(state.endDate)) return false;
            return true;
        }

        switch (state.period) {
            case 'hoje':
                return isSameDay(activityDate, today);
            case '3d':
                return diffDays >= 0 && diffDays <= 3;
            case '7d':
                return diffDays >= 0 && diffDays <= 7;
            case '15d':
                return diffDays >= 0 && diffDays <= 15;
            case 'mes':
                return activityDate.getMonth() === today.getMonth() && activityDate.getFullYear() === today.getFullYear();
            case 'tudo':
            default:
                return true;
        }
    }

    function updateSummary() {
        const counters = countByStatus(activities);
        document.getElementById('summary-delayed').textContent = counters['atrasada'] || 0;
        document.getElementById('summary-today').textContent = counters['para-hoje'] || 0;
        document.getElementById('summary-planned').textContent = counters['planejada'] || 0;
        document.getElementById('summary-done').textContent = counters['concluida'] || 0;
    }

    function updateTabs() {
        const counters = countByStatus(activities);
        document.getElementById('tab-delayed').textContent = counters['atrasada'] || 0;
        document.getElementById('tab-today').textContent = counters['para-hoje'] || 0;
        document.getElementById('tab-planned').textContent = counters['planejada'] || 0;
        document.getElementById('tab-done').textContent = counters['concluida'] || 0;
    }

    function countByStatus(data) {
        return data.reduce((acc, current) => {
            acc[current.status] = (acc[current.status] || 0) + 1;
            return acc;
        }, {});
    }

    function formatDay(date) {
        const d = new Date(date);
        const todayLabel = new Intl.DateTimeFormat('pt-BR', { weekday: 'short' }).format(d);
        const dateLabel = `${String(d.getDate()).padStart(2, '0')}/${String(d.getMonth() + 1).padStart(2, '0')}`;
        return `${todayLabel} | ${dateLabel}`;
    }

    function formatTime(date) {
        return new Intl.DateTimeFormat('pt-BR', { hour: '2-digit', minute: '2-digit' }).format(date);
    }

    function isSameDay(a, b) {
        return a.getFullYear() === b.getFullYear() &&
            a.getMonth() === b.getMonth() &&
            a.getDate() === b.getDate();
    }

    function startOfDay(date) {
        const d = new Date(date);
        d.setHours(0, 0, 0, 0);
        return d;
    }

    function endOfDay(date) {
        const d = new Date(date);
        d.setHours(23, 59, 59, 999);
        return d;
    }

    function statusClass(status) {
        switch (status) {
            case 'atrasada':
                return 'delayed';
            case 'para-hoje':
                return 'today';
            case 'planejada':
                return 'planned';
            case 'concluida':
            default:
                return 'done';
        }
    }

    function formatResponsible(value) {
        const map = {
            admin: 'Admin',
            jovani: 'Jovani Faussat',
            henrique: 'Henrique',
            gabriel: 'Gabriel'
        };
        return map[value] || value || 'Equipe';
    }

    function showFeedback(message, type) {
        if (!feedback) return;
        feedback.textContent = message;
        feedback.className = `form-feedback ${type}`;
    }

    function clearFeedback() {
        if (!feedback) return;
        feedback.textContent = '';
        feedback.className = 'form-feedback';
    }

    function setFormSubmitting(isSubmitting) {
        const submitButton = document.getElementById('submit-activity');
        if (!submitButton) return;
        submitButton.disabled = isSubmitting;
        submitButton.textContent = isSubmitting ? 'Salvando...' : 'Salvar atividade';
    }
})();
