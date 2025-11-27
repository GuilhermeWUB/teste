// CRM Reports - Data Loading and Rendering

console.log('✅ CRM Reports script loaded');

// Format number with thousands separator
function formatNumber(value) {
    return new Intl.NumberFormat('pt-BR').format(value || 0);
}

// Generate report based on selected checkboxes
async function generateReport() {
    const checkLigacoes = document.getElementById('checkLigacoes').checked;
    const checkVendas = document.getElementById('checkVendas').checked;
    const checkSaques = document.getElementById('checkSaques').checked;
    const checkAtividades = document.getElementById('checkAtividades').checked;

    // Validate at least one option is selected
    if (!checkLigacoes && !checkVendas && !checkSaques && !checkAtividades) {
        alert('Selecione pelo menos um relatório para gerar!');
        return;
    }

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

        // Render selected sections
        if (checkLigacoes) {
            renderLigacoes(data);
            document.getElementById('sectionLigacoes').style.display = 'block';
        } else {
            document.getElementById('sectionLigacoes').style.display = 'none';
        }

        if (checkVendas) {
            renderVendas(data);
            document.getElementById('sectionVendas').style.display = 'block';
        } else {
            document.getElementById('sectionVendas').style.display = 'none';
        }

        if (checkSaques) {
            renderSaques(data);
            document.getElementById('sectionSaques').style.display = 'block';
        } else {
            document.getElementById('sectionSaques').style.display = 'none';
        }

        if (checkAtividades) {
            renderAtividades(data);
            document.getElementById('sectionAtividades').style.display = 'block';
        } else {
            document.getElementById('sectionAtividades').style.display = 'none';
        }

        // Hide loading, show content
        loadingState.style.display = 'none';
        reportsContent.style.display = 'block';

        // Scroll to results
        reportsContent.scrollIntoView({ behavior: 'smooth', block: 'start' });

    } catch (error) {
        console.error('Error loading reports:', error);

        // Show error state
        loadingState.innerHTML = `
            <div style="color: #ef4444;">
                <i class="bi bi-exclamation-triangle" style="font-size: 3rem; margin-bottom: 1rem;"></i>
                <p>Erro ao gerar relatório</p>
                <button onclick="generateReport()" class="btn-refresh" style="margin-top: 1rem;">
                    <i class="bi bi-arrow-clockwise"></i> Tentar novamente
                </button>
            </div>
        `;
        loadingState.style.display = 'block';
    }
}

// Clear report and hide results
function clearReport() {
    const reportsContent = document.getElementById('reportsContent');
    reportsContent.style.display = 'none';

    // Hide all sections
    document.getElementById('sectionLigacoes').style.display = 'none';
    document.getElementById('sectionVendas').style.display = 'none';
    document.getElementById('sectionSaques').style.display = 'none';
    document.getElementById('sectionAtividades').style.display = 'none';
}

// Render Ligações section
function renderLigacoes(data) {
    const totalLigacoes = document.getElementById('totalLigacoes');
    if (totalLigacoes) {
        totalLigacoes.textContent = formatNumber(data.totalLigacoes);
    }
}

// Render Vendas section
function renderVendas(data) {
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

    // Taxa de Conversão
    const taxaConversao = document.getElementById('taxaConversao');
    if (taxaConversao) {
        taxaConversao.textContent = data.taxaConversao + '%';
    }

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

// Render Saques section
function renderSaques(data) {
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

// Render Atividades section
function renderAtividades(data) {
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
window.generateReport = generateReport;
window.clearReport = clearReport;

console.log('✅ CRM Reports ready');
