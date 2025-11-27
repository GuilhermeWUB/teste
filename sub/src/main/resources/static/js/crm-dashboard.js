// CRM Dashboard - Data Loading and Rendering

console.log('✅ CRM Dashboard script loaded');

// Load dashboard data on page load
document.addEventListener('DOMContentLoaded', () => {
    loadDashboardData();
});

// Format currency in Brazilian Real
function formatCurrency(value) {
    return new Intl.NumberFormat('pt-BR', {
        style: 'currency',
        currency: 'BRL'
    }).format(value || 0);
}

// Format number with thousands separator
function formatNumber(value) {
    return new Intl.NumberFormat('pt-BR').format(value || 0);
}

// Load user balance
async function loadUserBalance() {
    try {
        const response = await fetch('/crm/api/usuarios/meu-saldo', {
            headers: {
                'X-Requested-With': 'XMLHttpRequest',
                'Accept': 'application/json'
            }
        });

        if (!response.ok) {
            console.error('Failed to load user balance');
            return;
        }

        const data = await response.json();
        const saldoElement = document.getElementById('meuSaldo');
        if (saldoElement) {
            saldoElement.textContent = formatCurrency(data.saldo);
        }
    } catch (error) {
        console.error('Error loading user balance:', error);
    }
}

// Load all dashboard metrics
async function loadDashboardData() {
    const loadingState = document.getElementById('loadingState');
    const dashboardContent = document.getElementById('dashboardContent');

    try {
        // Show loading state
        loadingState.style.display = 'block';
        dashboardContent.style.display = 'none';

        // Fetch metrics from API
        const response = await fetch('/crm/api/dashboard/metrics', {
            headers: {
                'X-Requested-With': 'XMLHttpRequest'
            }
        });

        if (!response.ok) {
            throw new Error('Failed to load dashboard metrics');
        }

        const metrics = await response.json();
        console.log('Dashboard metrics loaded:', metrics);

        // Render all sections
        renderVendasMetrics(metrics);
        renderFunnelMetrics(metrics);
        renderAtividadesMetrics(metrics);
        renderAtividadesTipo(metrics);

        // Load user balance
        await loadUserBalance();

        // Hide loading, show content
        loadingState.style.display = 'none';
        dashboardContent.style.display = 'block';

    } catch (error) {
        console.error('Error loading dashboard:', error);

        // Show error state
        loadingState.innerHTML = `
            <div style="color: #ef4444;">
                <i class="bi bi-exclamation-triangle" style="font-size: 3rem; margin-bottom: 1rem;"></i>
                <p>Erro ao carregar métricas do dashboard</p>
                <button onclick="loadDashboardData()" class="btn-refresh" style="margin-top: 1rem;">
                    <i class="bi bi-arrow-clockwise"></i> Tentar novamente
                </button>
            </div>
        `;
    }
}

// Render vendas metrics
function renderVendasMetrics(metrics) {
    // Total vendas
    document.getElementById('totalVendas').textContent = formatNumber(metrics.totalVendas);

    // Vendas concluídas
    document.getElementById('vendasConcluidas').textContent = formatNumber(metrics.vendasConcluidas);

    // Progress bar
    const progressPercentage = metrics.totalVendas > 0
        ? (metrics.vendasConcluidas / metrics.totalVendas) * 100
        : 0;
    document.getElementById('progressConcluidas').style.width = `${progressPercentage}%`;

    // Receita total
    document.getElementById('receitaTotal').textContent = formatCurrency(metrics.totalReceita);

    // Receita do mês
    document.getElementById('receitaMes').textContent = `Este mês: ${formatCurrency(metrics.receitaMesAtual)}`;

    // Taxa de conversão
    const taxaFormatted = metrics.taxaConversao.toFixed(1);
    document.getElementById('taxaConversao').textContent = `${taxaFormatted}%`;
    document.getElementById('conversionFill').style.width = `${Math.min(metrics.taxaConversao, 100)}%`;
}

// Render funnel metrics (vendas por status)
function renderFunnelMetrics(metrics) {
    const funnelGrid = document.getElementById('funnelGrid');
    funnelGrid.innerHTML = '';

    // Render each status
    for (const [status, count] of Object.entries(metrics.vendasPorStatus)) {
        const funnelItem = document.createElement('div');
        funnelItem.className = 'funnel-item';
        funnelItem.innerHTML = `
            <div class="funnel-label">${status}</div>
            <div class="funnel-count">${formatNumber(count)}</div>
        `;
        funnelGrid.appendChild(funnelItem);
    }
}

// Render atividades metrics
function renderAtividadesMetrics(metrics) {
    document.getElementById('totalAtividades').textContent = formatNumber(metrics.totalAtividades);
    document.getElementById('atividadesAgendadas').textContent = formatNumber(metrics.atividadesAgendadas);
    document.getElementById('atividadesConcluidas').textContent = formatNumber(metrics.atividadesConcluidas);
    document.getElementById('atividadesHoje').textContent = formatNumber(metrics.atividadesHoje);
}

// Render atividades por tipo
function renderAtividadesTipo(metrics) {
    const grid = document.getElementById('activitiesTypeGrid');
    grid.innerHTML = '';

    // Icon mapping for activity types
    const iconMap = {
        'Ligação': 'bi-telephone',
        'E-mail': 'bi-envelope',
        'Reunião': 'bi-people',
        'Visita': 'bi-geo-alt',
        'Follow-up': 'bi-arrow-repeat',
        'Apresentação': 'bi-presentation',
        'Negociação': 'bi-handshake',
        'Vistoria': 'bi-clipboard-check',
        'Outro': 'bi-three-dots'
    };

    // Render each type
    for (const [tipo, count] of Object.entries(metrics.atividadesPorTipo)) {
        if (count === 0) continue; // Skip types with 0 count

        const icon = iconMap[tipo] || 'bi-circle';

        const card = document.createElement('div');
        card.className = 'activity-type-card';
        card.innerHTML = `
            <div class="activity-type-icon">
                <i class="bi ${icon}"></i>
            </div>
            <div class="activity-type-name">${tipo}</div>
            <div class="activity-type-count">${formatNumber(count)}</div>
        `;
        grid.appendChild(card);
    }

    // If no activities, show message
    if (Object.values(metrics.atividadesPorTipo).every(count => count === 0)) {
        grid.innerHTML = `
            <div style="grid-column: 1 / -1; text-align: center; padding: 2rem; color: var(--text-secondary);">
                <i class="bi bi-inbox" style="font-size: 2rem; margin-bottom: 0.5rem;"></i>
                <p>Nenhuma atividade registrada ainda</p>
            </div>
        `;
    }
}

// Export for inline onclick handlers
window.loadDashboardData = loadDashboardData;

console.log('✅ CRM Dashboard ready');
