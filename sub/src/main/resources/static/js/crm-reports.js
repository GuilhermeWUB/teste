// CRM Reports - Data Loading and Rendering

console.log('✅ CRM Reports script loaded');

// Load reports data on page load
document.addEventListener('DOMContentLoaded', () => {
    loadReportsData();
});

// Format number with thousands separator
function formatNumber(value) {
    return new Intl.NumberFormat('pt-BR').format(value || 0);
}

// Load all reports data
async function loadReportsData() {
    const loadingState = document.getElementById('loadingState');
    const reportsContent = document.getElementById('reportsContent');

    try {
        // Show loading state
        loadingState.style.display = 'block';
        reportsContent.style.display = 'none';

        // Fetch general report from API
        const response = await fetch('/crm/api/relatorios/geral', {
            headers: {
                'X-Requested-With': 'XMLHttpRequest',
                'Accept': 'application/json'
            }
        });

        if (!response.ok) {
            throw new Error('Failed to load reports data');
        }

        const data = await response.json();
        console.log('Reports data loaded:', data);

        // Render all metrics
        renderMetrics(data);
        renderSalesDetails(data);
        renderWithdrawalsDetails(data);
        renderActivitiesDetails(data);

        // Hide loading, show content
        loadingState.style.display = 'none';
        reportsContent.style.display = 'block';

    } catch (error) {
        console.error('Error loading reports:', error);

        // Show error state
        loadingState.innerHTML = `
            <div style="color: #ef4444;">
                <i class="bi bi-exclamation-triangle" style="font-size: 3rem; margin-bottom: 1rem;"></i>
                <p>Erro ao carregar relatórios</p>
                <button onclick="loadReportsData()" class="btn-refresh" style="margin-top: 1rem;">
                    <i class="bi bi-arrow-clockwise"></i> Tentar novamente
                </button>
            </div>
        `;
    }
}

// Render main metrics
function renderMetrics(data) {
    // Total de Ligações
    const totalLigacoes = document.getElementById('totalLigacoes');
    if (totalLigacoes) {
        totalLigacoes.textContent = formatNumber(data.totalLigacoes);
    }

    // Total de Vendas
    const totalVendas = document.getElementById('totalVendas');
    if (totalVendas) {
        totalVendas.textContent = formatNumber(data.totalVendas);
    }

    // Vendas Concluídas (subtitle)
    const vendasConcluidas = document.getElementById('vendasConcluidas');
    if (vendasConcluidas) {
        vendasConcluidas.textContent = formatNumber(data.vendasConcluidas);
    }

    // Total de Saques
    const totalSaques = document.getElementById('totalSaques');
    if (totalSaques) {
        totalSaques.textContent = formatNumber(data.totalSaques);
    }

    // Saques Concluídos (subtitle)
    const saquesConcluidos = document.getElementById('saquesConcluidos');
    if (saquesConcluidos) {
        saquesConcluidos.textContent = formatNumber(data.saquesConcluidos);
    }

    // Taxa de Conversão
    const taxaConversao = document.getElementById('taxaConversao');
    if (taxaConversao) {
        taxaConversao.textContent = data.taxaConversao + '%';
    }
}

// Render sales details
function renderSalesDetails(data) {
    // Vendas Pendentes
    const vendasPendentes = document.getElementById('vendasPendentes');
    if (vendasPendentes) {
        vendasPendentes.textContent = formatNumber(data.vendasPendentes);
    }

    // Vendas Concluídas (detail)
    const vendasConcluidasDetail = document.getElementById('vendasConcluidasDetail');
    if (vendasConcluidasDetail) {
        vendasConcluidasDetail.textContent = formatNumber(data.vendasConcluidas);
    }
}

// Render withdrawals details
function renderWithdrawalsDetails(data) {
    // Saques Pendentes
    const saquesPendentes = document.getElementById('saquesPendentes');
    if (saquesPendentes) {
        saquesPendentes.textContent = formatNumber(data.saquesPendentes);
    }

    // Saques Aprovados
    const saquesAprovados = document.getElementById('saquesAprovados');
    if (saquesAprovados) {
        saquesAprovados.textContent = formatNumber(data.saquesAprovados);
    }

    // Saques Concluídos (detail)
    const saquesConcluidosDetail = document.getElementById('saquesConcluidosDetail');
    if (saquesConcluidosDetail) {
        saquesConcluidosDetail.textContent = formatNumber(data.saquesConcluidos);
    }

    // Saques Rejeitados
    const saquesRejeitados = document.getElementById('saquesRejeitados');
    if (saquesRejeitados) {
        saquesRejeitados.textContent = formatNumber(data.saquesRejeitados);
    }
}

// Render activities details
function renderActivitiesDetails(data) {
    // E-mails
    const totalEmails = document.getElementById('totalEmails');
    if (totalEmails) {
        totalEmails.textContent = formatNumber(data.totalEmails);
    }

    // Reuniões
    const totalReunioes = document.getElementById('totalReunioes');
    if (totalReunioes) {
        totalReunioes.textContent = formatNumber(data.totalReunioes);
    }

    // Visitas
    const totalVisitas = document.getElementById('totalVisitas');
    if (totalVisitas) {
        totalVisitas.textContent = formatNumber(data.totalVisitas);
    }

    // Total de Atividades
    const totalAtividades = document.getElementById('totalAtividades');
    if (totalAtividades) {
        totalAtividades.textContent = formatNumber(data.totalAtividades);
    }
}

// Export for inline onclick handlers
window.loadReportsData = loadReportsData;

console.log('✅ CRM Reports ready');
