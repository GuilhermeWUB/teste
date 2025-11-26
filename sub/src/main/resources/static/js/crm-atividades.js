// CRM Atividades - Management and Display

let allActivities = [];
let filteredActivities = [];

// Load activities on page load
document.addEventListener('DOMContentLoaded', () => {
    loadActivities();
    loadStats();
});

// Format date/time
function formatDateTime(dateTimeStr) {
    if (!dateTimeStr) return '-';
    const date = new Date(dateTimeStr);
    return date.toLocaleString('pt-BR', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
}

// Load activities from API
async function loadActivities() {
    const loadingState = document.getElementById('loadingState');
    const emptyState = document.getElementById('emptyState');
    const grid = document.getElementById('activitiesGrid');

    try {
        loadingState.style.display = 'block';
        emptyState.style.display = 'none';
        grid.style.display = 'none';

        const response = await fetch('/crm/api/atividades', {
            headers: { 'X-Requested-With': 'XMLHttpRequest' }
        });

        if (!response.ok) throw new Error('Failed to load activities');

        allActivities = await response.json();
        filteredActivities = allActivities;

        renderActivities();
        loadingState.style.display = 'none';

    } catch (error) {
        console.error('Error loading activities:', error);
        loadingState.innerHTML = `
            <div style="color: #ef4444;">
                <i class="bi bi-exclamation-triangle" style="font-size: 3rem; margin-bottom: 1rem;"></i>
                <p>Erro ao carregar atividades</p>
                <button onclick="loadActivities()" class="btn-primary">
                    <i class="bi bi-arrow-clockwise"></i> Tentar novamente
                </button>
            </div>
        `;
    }
}

// Load stats
async function loadStats() {
    try {
        const response = await fetch('/crm/api/dashboard/atividades', {
            headers: { 'X-Requested-With': 'XMLHttpRequest' }
        });

        if (!response.ok) return;

        const stats = await response.json();

        document.getElementById('totalCount').textContent = stats.totalAtividades || 0;
        document.getElementById('agendadasCount').textContent = stats.atividadesAgendadas || 0;
        document.getElementById('concluidasCount').textContent = stats.atividadesConcluidas || 0;
        document.getElementById('hojeCount').textContent = stats.atividadesHoje || 0;

    } catch (error) {
        console.error('Error loading stats:', error);
    }
}

// Render activities
function renderActivities() {
    const emptyState = document.getElementById('emptyState');
    const grid = document.getElementById('activitiesGrid');

    if (filteredActivities.length === 0) {
        emptyState.style.display = 'block';
        grid.style.display = 'none';
        return;
    }

    emptyState.style.display = 'none';
    grid.style.display = 'grid';
    grid.innerHTML = '';

    filteredActivities.forEach(activity => {
        const card = createActivityCard(activity);
        grid.appendChild(card);
    });
}

// Create activity card
function createActivityCard(activity) {
    const card = document.createElement('div');
    card.className = 'activity-card';

    const statusClass = `status-${activity.status}`;
    const tipoLabel = getTipoLabel(activity.tipo);
    const statusLabel = getStatusLabel(activity.status);
    const prioridadeLabel = activity.prioridade ? getPrioridadeLabel(activity.prioridade) : '';

    card.innerHTML = `
        <div class="activity-card-header">
            <div style="flex: 1;">
                <div class="activity-title">${activity.titulo}</div>
                <div class="activity-badges">
                    <span class="badge ${statusClass}">${statusLabel}</span>
                    <span class="badge" style="background: #f1f5f9; color: #334155;">${tipoLabel}</span>
                    ${prioridadeLabel ? `<span class="badge" style="background: #fef3c7; color: #92400e;">${prioridadeLabel}</span>` : ''}
                </div>
            </div>
        </div>

        ${activity.descricao ? `<div class="activity-desc">${activity.descricao}</div>` : ''}

        <div class="activity-meta">
            ${activity.contatoNome ? `
                <div class="activity-meta-item">
                    <i class="bi bi-person"></i>
                    <span>${activity.contatoNome}</span>
                </div>
            ` : ''}
            ${activity.dataAgendada ? `
                <div class="activity-meta-item">
                    <i class="bi bi-calendar"></i>
                    <span>${formatDateTime(activity.dataAgendada)}</span>
                </div>
            ` : ''}
            ${activity.responsavel ? `
                <div class="activity-meta-item">
                    <i class="bi bi-person-badge"></i>
                    <span>${activity.responsavel}</span>
                </div>
            ` : ''}
            ${activity.contatoTelefone ? `
                <div class="activity-meta-item">
                    <i class="bi bi-telephone"></i>
                    <span>${activity.contatoTelefone}</span>
                </div>
            ` : ''}
        </div>

        <div class="activity-footer">
            <div class="activity-actions">
                <button class="btn-icon" onclick="editActivity(${activity.id})" title="Editar">
                    <i class="bi bi-pencil"></i>
                </button>
                <button class="btn-icon" onclick="deleteActivity(${activity.id})" title="Excluir">
                    <i class="bi bi-trash"></i>
                </button>
            </div>
            ${activity.status !== 'CONCLUIDA' ? `
                <button class="btn-primary" style="font-size: 0.875rem; padding: 0.5rem 1rem;" onclick="markAsConcluida(${activity.id})">
                    <i class="bi bi-check-circle"></i> Concluir
                </button>
            ` : ''}
        </div>
    `;

    return card;
}

// Apply filters
function applyFilters() {
    const statusFilter = document.getElementById('filterStatus').value;
    const tipoFilter = document.getElementById('filterTipo').value;
    const searchTerm = document.getElementById('searchInput').value.toLowerCase();

    filteredActivities = allActivities.filter(activity => {
        const matchesStatus = !statusFilter || activity.status === statusFilter;
        const matchesTipo = !tipoFilter || activity.tipo === tipoFilter;
        const matchesSearch = !searchTerm ||
            activity.titulo.toLowerCase().includes(searchTerm) ||
            (activity.descricao && activity.descricao.toLowerCase().includes(searchTerm)) ||
            (activity.contatoNome && activity.contatoNome.toLowerCase().includes(searchTerm));

        return matchesStatus && matchesTipo && matchesSearch;
    });

    renderActivities();
}

// Open create modal
function openCreateModal() {
    document.getElementById('modalTitle').innerHTML = '<i class="bi bi-plus-circle"></i> Nova Atividade';
    document.getElementById('activityForm').reset();
    document.getElementById('activityId').value = '';
    document.getElementById('activityModal').style.display = 'flex';
}

// Close modal
function closeModal() {
    document.getElementById('activityModal').style.display = 'none';
}

// Save activity
async function saveActivity(event) {
    event.preventDefault();

    const id = document.getElementById('activityId').value;
    const data = {
        titulo: document.getElementById('titulo').value,
        descricao: document.getElementById('descricao').value || null,
        tipo: document.getElementById('tipo').value,
        prioridade: document.getElementById('prioridade').value || null,
        contatoNome: document.getElementById('contatoNome').value || null,
        contatoEmail: document.getElementById('contatoEmail').value || null,
        contatoTelefone: document.getElementById('contatoTelefone').value || null,
        responsavel: document.getElementById('responsavel').value || null,
        dataAgendada: document.getElementById('dataAgendada').value || null,
        saleId: document.getElementById('saleId').value || null,
        resultado: document.getElementById('resultado').value || null
    };

    try {
        const url = id ? `/crm/api/atividades/${id}` : '/crm/api/atividades';
        const method = id ? 'PUT' : 'POST';

        const response = await fetch(url, {
            method,
            headers: {
                'Content-Type': 'application/json',
                'X-Requested-With': 'XMLHttpRequest'
            },
            body: JSON.stringify(data)
        });

        if (!response.ok) throw new Error('Failed to save activity');

        closeModal();
        loadActivities();
        loadStats();

    } catch (error) {
        console.error('Error saving activity:', error);
        alert('Erro ao salvar atividade');
    }
}

// Edit activity
async function editActivity(id) {
    try {
        const response = await fetch(`/crm/api/atividades/${id}`, {
            headers: { 'X-Requested-With': 'XMLHttpRequest' }
        });

        if (!response.ok) throw new Error('Failed to load activity');

        const activity = await response.json();

        document.getElementById('modalTitle').innerHTML = '<i class="bi bi-pencil"></i> Editar Atividade';
        document.getElementById('activityId').value = activity.id;
        document.getElementById('titulo').value = activity.titulo || '';
        document.getElementById('descricao').value = activity.descricao || '';
        document.getElementById('tipo').value = activity.tipo || '';
        document.getElementById('prioridade').value = activity.prioridade || '';
        document.getElementById('contatoNome').value = activity.contatoNome || '';
        document.getElementById('contatoEmail').value = activity.contatoEmail || '';
        document.getElementById('contatoTelefone').value = activity.contatoTelefone || '';
        document.getElementById('responsavel').value = activity.responsavel || '';
        document.getElementById('saleId').value = activity.sale?.id || '';
        document.getElementById('resultado').value = activity.resultado || '';

        if (activity.dataAgendada) {
            const date = new Date(activity.dataAgendada);
            const formatted = date.toISOString().slice(0, 16);
            document.getElementById('dataAgendada').value = formatted;
        }

        document.getElementById('activityModal').style.display = 'flex';

    } catch (error) {
        console.error('Error loading activity:', error);
        alert('Erro ao carregar atividade');
    }
}

// Delete activity
async function deleteActivity(id) {
    if (!confirm('Tem certeza que deseja excluir esta atividade?')) return;

    try {
        const response = await fetch(`/crm/api/atividades/${id}`, {
            method: 'DELETE',
            headers: { 'X-Requested-With': 'XMLHttpRequest' }
        });

        if (!response.ok) throw new Error('Failed to delete activity');

        loadActivities();
        loadStats();

    } catch (error) {
        console.error('Error deleting activity:', error);
        alert('Erro ao excluir atividade');
    }
}

// Mark as concluded
async function markAsConcluida(id) {
    try {
        const response = await fetch(`/crm/api/atividades/${id}/status`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'X-Requested-With': 'XMLHttpRequest'
            },
            body: JSON.stringify({ status: 'CONCLUIDA' })
        });

        if (!response.ok) throw new Error('Failed to update status');

        loadActivities();
        loadStats();

    } catch (error) {
        console.error('Error updating status:', error);
        alert('Erro ao concluir atividade');
    }
}

// Helper functions
function getTipoLabel(tipo) {
    const labels = {
        'LIGACAO': 'Ligação',
        'EMAIL': 'E-mail',
        'REUNIAO': 'Reunião',
        'VISITA': 'Visita',
        'FOLLOW_UP': 'Follow-up',
        'APRESENTACAO': 'Apresentação',
        'NEGOCIACAO': 'Negociação',
        'VISTORIA': 'Vistoria',
        'OUTRO': 'Outro'
    };
    return labels[tipo] || tipo;
}

function getStatusLabel(status) {
    const labels = {
        'AGENDADA': 'Agendada',
        'EM_ANDAMENTO': 'Em andamento',
        'CONCLUIDA': 'Concluída',
        'CANCELADA': 'Cancelada',
        'REAGENDADA': 'Reagendada'
    };
    return labels[status] || status;
}

function getPrioridadeLabel(prioridade) {
    const labels = {
        'BAIXA': 'Baixa',
        'MEDIA': 'Média',
        'ALTA': 'Alta',
        'URGENTE': 'Urgente'
    };
    return labels[prioridade] || prioridade;
}

console.log('✅ CRM Atividades ready');
