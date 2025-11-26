(function () {
    const today = new Date();

    const statusLabels = {
        'atrasada': 'Atrasada',
        'para-hoje': 'Para hoje',
        'planejada': 'Planejada',
        'concluida': 'Concluída'
    };

    const activities = [
        {
            id: 1,
            title: 'manda todos os valores e expliquei tudo que ajuda, está bem interessado',
            description: 'Solicitar ligação para avançar na proposta e revisar cálculo.',
            time: '15:30',
            date: shiftDays(today, 0),
            status: 'atrasada',
            type: 'calculo',
            leadSource: 'Fale Comigo',
            responsible: 'admin',
            city: 'Fortaleza',
            state: 'CE'
        },
        {
            id: 2,
            title: 'Marcos, Ligar 11horas',
            description: 'Lead vindo do chat, aguardando retorno do cálculo.',
            time: '15:30',
            date: shiftDays(today, 0),
            status: 'atrasada',
            type: 'ligacao',
            leadSource: 'Lead do chat',
            responsible: 'jovani',
            city: 'Goiânia',
            state: 'GO'
        },
        {
            id: 3,
            title: 'Ele não tem email nem sabe usar',
            description: 'Solicitar ligação e enviar explicação detalhada.',
            time: '16:30',
            date: shiftDays(today, 0),
            status: 'atrasada',
            type: 'falecomigoloja',
            leadSource: 'Loja física',
            responsible: 'admin',
            city: 'Fortaleza',
            state: 'CE'
        },
        {
            id: 4,
            title: 'Diz ligar depois',
            description: 'Solicitar ligação para confirmar interesse.',
            time: '15:30',
            date: shiftDays(today, 0),
            status: 'para-hoje',
            type: 'falecomigoportal',
            leadSource: 'Portal',
            responsible: 'henrique',
            city: 'Brasília',
            state: 'DF'
        },
        {
            id: 5,
            title: 'Atualizar base de Abril',
            description: 'Revisar cálculos e compartilhar atualização com o cliente.',
            time: '15:30',
            date: shiftDays(today, 0),
            status: 'para-hoje',
            type: 'calculo',
            leadSource: 'Site',
            responsible: 'gabriel',
            city: 'São Paulo',
            state: 'SP'
        },
        {
            id: 6,
            title: 'não consegue trocar a "franja", expliquei que chamamos de franquia e expliquei como funciona',
            description: 'Solicitar ligação e reforçar condições para o lead.',
            time: '12:30',
            date: shiftDays(today, 2),
            status: 'planejada',
            type: 'lead',
            leadSource: 'Chat',
            responsible: 'jovani',
            city: 'Fortaleza',
            state: 'CE'
        },
        {
            id: 7,
            title: 'Aguardando contato do setor responsável',
            description: 'Lead de site, acompanhar resposta de Henrique.',
            time: '14:10',
            date: shiftDays(today, -1),
            status: 'concluida',
            type: 'calculo',
            leadSource: 'Site',
            responsible: 'henrique',
            city: 'Recife',
            state: 'PE'
        }
    ];

    const state = {
        status: 'atrasada',
        search: '',
        responsible: '',
        period: 'hoje',
        startDate: null,
        endDate: null,
        types: new Set(['calculo', 'ligacao', 'falecomigoloja', 'falecomigoportal', 'lead'])
    };

    const listContainer = document.getElementById('activities-list');
    const emptyState = document.getElementById('activities-empty');
    const searchInput = document.getElementById('search-input');
    const responsibleSelect = document.getElementById('responsavel-select');
    const periodButtons = document.querySelectorAll('.period-button');
    const applyPeriodButton = document.getElementById('apply-period');
    const applyFiltersButton = document.getElementById('apply-filters');
    const typeCheckboxes = document.querySelectorAll('.checkbox-list input[type="checkbox"]');
    const tabs = document.querySelectorAll('[data-status-filter]');

    init();

    function init() {
        attachEvents();
        updateSummary();
        updateTabs();
        renderActivities();
    }

    function attachEvents() {
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

    function renderActivities() {
        const filtered = activities.filter(activity =>
            matchesStatus(activity) &&
            matchesSearch(activity) &&
            matchesResponsible(activity) &&
            matchesType(activity) &&
            matchesPeriod(activity)
        );

        listContainer.querySelectorAll('.activity-card').forEach(el => el.remove());

        if (!filtered.length) {
            emptyState.hidden = false;
            return;
        }

        emptyState.hidden = true;
        const fragment = document.createDocumentFragment();
        filtered
            .sort((a, b) => new Date(a.date) - new Date(b.date))
            .forEach(activity => fragment.appendChild(buildCard(activity)));

        listContainer.appendChild(fragment);
    }

    function buildCard(activity) {
        const card = document.createElement('article');
        card.className = 'activity-card';

        const time = document.createElement('div');
        time.className = 'activity-time';
        time.innerHTML = `<span class="time-label">${activity.time}</span><span class="time-day">${formatDay(activity.date)}</span>`;

        const content = document.createElement('div');
        content.className = 'activity-content';
        content.innerHTML = `
            <h3>${activity.title}</h3>
            <p class="activity-meta">${activity.description}</p>
        `;

        const badges = document.createElement('div');
        badges.className = 'activity-badges';
        badges.appendChild(createBadge('info', activity.leadSource));
        badges.appendChild(createBadge('warning', `${activity.city} - ${activity.state}`));
        badges.appendChild(createBadge('danger', `Responsável: ${formatResponsible(activity.responsible)}`));
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
        const target = `${activity.title} ${activity.description} ${activity.leadSource}`.toLowerCase();
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
        const activityDate = new Date(activity.date);
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

    function shiftDays(base, days) {
        const clone = new Date(base);
        clone.setDate(clone.getDate() + days);
        return clone;
    }

    function formatDay(date) {
        const d = new Date(date);
        const todayLabel = new Intl.DateTimeFormat('pt-BR', { weekday: 'short' }).format(d);
        const dateLabel = `${String(d.getDate()).padStart(2, '0')}/${String(d.getMonth() + 1).padStart(2, '0')}`;
        return `${todayLabel} | ${dateLabel}`;
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
        return map[value] || 'Equipe';
    }
})();
