/**
 * ===============================================
 * BOARD DE EVENTOS - KANBAN DRAG & DROP
 * Sistema avançado com SortableJS
 * ===============================================
 */

// Estado global da aplicação
const BoardState = {
    sortableInstances: [],
    searchActive: false,
    currentFilter: '',
    isDragging: false
};

/**
 * Inicialização do Board ao carregar o DOM
 */
document.addEventListener('DOMContentLoaded', function() {
    console.log('🎯 Board de Eventos inicializando...');

    initializeSortable();
    initializeSearch();
    initializeEventListeners();
    updateAllCounters();
    animateCards();

    console.log('✅ Board de Eventos inicializado com sucesso!');
});

/**
 * Inicializa SortableJS em todas as colunas
 */
function initializeSortable() {
    const columns = document.querySelectorAll('.column-content');

    columns.forEach(column => {
        const sortable = new Sortable(column, {
            group: 'eventos',
            animation: 250,
            easing: 'cubic-bezier(0.4, 0, 0.2, 1)',
            ghostClass: 'sortable-ghost',
            dragClass: 'sortable-drag',
            chosenClass: 'sortable-chosen',
            handle: '.event-card',
            draggable: '.event-card',
            forceFallback: true,
            fallbackClass: 'sortable-fallback',
            fallbackOnBody: true,
            swapThreshold: 0.65,

            // Evento ao começar a arrastar
            onStart: function(evt) {
                BoardState.isDragging = true;
                evt.item.style.cursor = 'grabbing';
                addDragFeedback();
                console.log('🎯 Arrastando evento:', evt.item.dataset.eventId);
            },

            // Evento ao soltar o card
            onEnd: function(evt) {
                BoardState.isDragging = false;
                evt.item.style.cursor = 'grab';
                removeDragFeedback();

                const eventId = evt.item.dataset.eventId;
                const newStatus = evt.to.dataset.status;
                const oldStatus = evt.from.dataset.status;

                // Se mudou de coluna, atualizar no backend
                if (newStatus !== oldStatus) {
                    // Confirmar se está movendo para concluído
                    if (newStatus === 'CONCLUIDO') {
                        confirmMoveToCompleted(eventId, evt.item, evt.from, evt.to);
                    } else {
                        updateEventStatus(eventId, newStatus, evt.item, evt.from);
                    }
                } else {
                    console.log('ℹ️ Evento movido na mesma coluna');
                }
            },

            // Evento ao mover entre colunas
            onChange: function(evt) {
                updateColumnIndicators();
            }
        });

        BoardState.sortableInstances.push(sortable);
    });

    console.log(`📋 SortableJS inicializado em ${columns.length} colunas`);
}

/**
 * Inicializa o sistema de busca
 */
function initializeSearch() {
    const btnSearch = document.getElementById('btnSearch');
    const btnClearSearch = document.getElementById('btnClearSearch');
    const searchBar = document.getElementById('searchBar');
    const searchInput = document.getElementById('searchInput');

    if (btnSearch) {
        btnSearch.addEventListener('click', function() {
            searchBar.style.display = searchBar.style.display === 'none' ? 'block' : 'none';
            if (searchBar.style.display === 'block') {
                searchInput.focus();
            } else {
                clearSearch();
            }
        });
    }

    if (btnClearSearch) {
        btnClearSearch.addEventListener('click', function() {
            clearSearch();
        });
    }

    if (searchInput) {
        searchInput.addEventListener('input', function(e) {
            filterCards(e.target.value);
        });

        // Buscar ao pressionar Enter
        searchInput.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                filterCards(e.target.value);
            }
        });
    }
}

/**
 * Inicializa event listeners adicionais
 */
function initializeEventListeners() {
    // Adicionar hover effects nos cards
    document.querySelectorAll('.event-card').forEach(card => {
        card.addEventListener('mouseenter', function() {
            if (!BoardState.isDragging) {
                this.style.transform = 'translateY(-3px) scale(1.01)';
            }
        });

        card.addEventListener('mouseleave', function() {
            if (!BoardState.isDragging) {
                this.style.transform = '';
            }
        });
    });

    // Detectar teclas de atalho
    document.addEventListener('keydown', function(e) {
        // Ctrl/Cmd + F para buscar
        if ((e.ctrlKey || e.metaKey) && e.key === 'f') {
            e.preventDefault();
            const searchBar = document.getElementById('searchBar');
            if (searchBar) {
                searchBar.style.display = 'block';
                document.getElementById('searchInput').focus();
            }
        }

        // Esc para fechar busca
        if (e.key === 'Escape') {
            clearSearch();
        }
    });
}

/**
 * Confirma movimento para coluna de concluídos
 */
function confirmMoveToCompleted(eventId, cardElement, oldColumn, newColumn) {
    const eventTitle = cardElement.dataset.title || 'este evento';

    // Criar modal de confirmação customizado
    showConfirmationModal(
        'Concluir Evento?',
        `Tem certeza que deseja marcar "${eventTitle}" como concluído?`,
        function() {
            // Confirmado
            updateEventStatus(eventId, 'CONCLUIDO', cardElement, oldColumn);
            cardElement.classList.add('event-card-completed');
        },
        function() {
            // Cancelado - reverter
            oldColumn.appendChild(cardElement);
            showToast('Ação cancelada', 'info');
        }
    );
}

/**
 * Atualiza o status do evento no backend
 */
function updateEventStatus(eventId, newStatus, cardElement, oldColumn) {
    console.log(`📝 Atualizando evento ${eventId} para status ${newStatus}`);

    // Adicionar classe de loading
    cardElement.classList.add('loading');

    // Desabilitar drag temporariamente
    cardElement.style.pointerEvents = 'none';

    fetch(`/events/api/${eventId}/status`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({ status: newStatus })
    })
    .then(response => {
        if (!response.ok) {
            throw new Error(`Erro HTTP: ${response.status}`);
        }
        return response.json();
    })
    .then(data => {
        console.log('✅ Status atualizado com sucesso:', data);

        // Remover loading
        cardElement.classList.remove('loading');
        cardElement.style.pointerEvents = '';

        // Adicionar animação de sucesso
        cardElement.classList.add('scale-up');
        setTimeout(() => cardElement.classList.remove('scale-up'), 300);

        // Mostrar notificação de sucesso
        showToast(`Evento movido para "${formatStatus(newStatus)}" com sucesso!`, 'success');

        // Atualizar contadores e estatísticas
        updateAllCounters();
        updateColumnProgress();
    })
    .catch(error => {
        console.error('❌ Erro ao atualizar status:', error);

        // Remover loading
        cardElement.classList.remove('loading');
        cardElement.style.pointerEvents = '';

        // Reverter o card para a coluna original
        oldColumn.appendChild(cardElement);

        // Mostrar notificação de erro
        showToast('Erro ao atualizar status. Tente novamente.', 'error');

        // Log detalhado do erro
        console.error('Detalhes do erro:', {
            eventId,
            newStatus,
            error: error.message
        });
    });
}

/**
 * Filtra cards baseado no termo de busca
 */
function filterCards(searchTerm) {
    BoardState.currentFilter = searchTerm.toLowerCase().trim();
    BoardState.searchActive = BoardState.currentFilter.length > 0;

    const allCards = document.querySelectorAll('.event-card');
    let visibleCount = 0;

    allCards.forEach(card => {
        const title = (card.dataset.title || '').toLowerCase();
        const partner = (card.dataset.partner || '').toLowerCase();
        const plate = (card.dataset.plate || '').toLowerCase();

        const matches = title.includes(BoardState.currentFilter) ||
                       partner.includes(BoardState.currentFilter) ||
                       plate.includes(BoardState.currentFilter);

        if (BoardState.searchActive) {
            if (matches) {
                card.style.display = '';
                card.classList.add('fade-in');
                visibleCount++;
            } else {
                card.style.display = 'none';
            }
        } else {
            card.style.display = '';
            card.classList.remove('fade-in');
            visibleCount++;
        }
    });

    // Atualizar mensagens de coluna vazia
    updateEmptyColumnMessages();

    // Mostrar resultado da busca
    if (BoardState.searchActive) {
        console.log(`🔍 Busca por "${BoardState.currentFilter}": ${visibleCount} resultados`);
    }
}

/**
 * Limpa a busca
 */
function clearSearch() {
    const searchBar = document.getElementById('searchBar');
    const searchInput = document.getElementById('searchInput');

    if (searchInput) {
        searchInput.value = '';
    }

    if (searchBar) {
        searchBar.style.display = 'none';
    }

    filterCards('');
}

/**
 * Atualiza todos os contadores de cards
 */
function updateAllCounters() {
    const columns = document.querySelectorAll('.column-content');

    columns.forEach(column => {
        const status = column.dataset.status;
        const cards = column.querySelectorAll('.event-card');
        const visibleCards = Array.from(cards).filter(card => card.style.display !== 'none');
        const count = BoardState.searchActive ? visibleCards.length : cards.length;

        // Atualizar badge da coluna
        const header = column.previousElementSibling;
        const badge = header.querySelector('.column-badge');
        if (badge) {
            badge.textContent = count;
            animateCounter(badge);
        }
    });

    // Atualizar estatísticas globais
    updateGlobalStats();
}

/**
 * Atualiza estatísticas globais
 */
function updateGlobalStats() {
    const statCards = document.querySelectorAll('.stat-card');

    statCards.forEach(statCard => {
        const statValue = statCard.querySelector('.stat-value');
        const statLabel = statCard.querySelector('.stat-label');

        if (statLabel) {
            const label = statLabel.textContent.trim();
            let count = 0;

            if (label === 'Total de Eventos') {
                count = document.querySelectorAll('.event-card').length;
            } else {
                const statusMap = {
                    'A Fazer': 'A_FAZER',
                    'Em Andamento': 'EM_ANDAMENTO',
                    'Aguardando': 'AGUARDANDO',
                    'Concluído': 'CONCLUIDO'
                };

                const status = statusMap[label];
                if (status) {
                    const column = document.querySelector(`[data-status="${status}"]`);
                    if (column) {
                        count = column.querySelectorAll('.event-card').length;
                    }
                }
            }

            if (statValue) {
                animateValue(statValue, parseInt(statValue.textContent) || 0, count, 500);
            }
        }
    });
}

/**
 * Atualiza o progresso visual das colunas
 */
function updateColumnProgress() {
    const totalEvents = document.querySelectorAll('.event-card').length;

    if (totalEvents === 0) return;

    document.querySelectorAll('.column-content').forEach(column => {
        const count = column.querySelectorAll('.event-card').length;
        const percentage = (count / totalEvents) * 100;

        const header = column.previousElementSibling;
        const progressBar = header.querySelector('.progress-bar');

        if (progressBar) {
            progressBar.style.width = `${percentage}%`;
        }
    });
}

/**
 * Atualiza mensagens de coluna vazia
 */
function updateEmptyColumnMessages() {
    document.querySelectorAll('.column-content').forEach(column => {
        const cards = column.querySelectorAll('.event-card');
        const visibleCards = Array.from(cards).filter(card => card.style.display !== 'none');
        const emptyMessage = column.querySelector('.empty-column');

        if (emptyMessage) {
            emptyMessage.style.display = visibleCards.length === 0 ? 'flex' : 'none';
        }
    });
}

/**
 * Adiciona feedback visual durante drag
 */
function addDragFeedback() {
    document.querySelectorAll('.column-content').forEach(column => {
        column.style.backgroundColor = '#f8f9fa';
        column.style.border = '2px dashed #dee2e6';
    });
}

/**
 * Remove feedback visual de drag
 */
function removeDragFeedback() {
    document.querySelectorAll('.column-content').forEach(column => {
        column.style.backgroundColor = '';
        column.style.border = '';
    });
}

/**
 * Atualiza indicadores visuais das colunas
 */
function updateColumnIndicators() {
    updateAllCounters();
}

/**
 * Anima os cards na entrada
 */
function animateCards() {
    const cards = document.querySelectorAll('.event-card');
    cards.forEach((card, index) => {
        card.style.animationDelay = `${index * 0.05}s`;
    });
}

/**
 * Mostra notificação toast
 */
function showToast(message, type = 'success') {
    // Remover toasts anteriores
    document.querySelectorAll('.toast-notification').forEach(toast => {
        toast.remove();
    });

    // Criar elemento do toast
    const toast = document.createElement('div');
    toast.className = `toast-notification ${type}`;

    // Definir ícone baseado no tipo
    const icons = {
        success: 'bi-check-circle-fill text-success',
        error: 'bi-exclamation-circle-fill text-danger',
        warning: 'bi-exclamation-triangle-fill text-warning',
        info: 'bi-info-circle-fill text-info'
    };

    const iconClass = icons[type] || icons.info;

    toast.innerHTML = `
        <i class="bi ${iconClass}"></i>
        <span>${message}</span>
    `;

    // Adicionar ao body
    document.body.appendChild(toast);

    // Remover após 3 segundos com animação
    setTimeout(() => {
        toast.style.animation = 'slideOutRight 0.3s ease';
        setTimeout(() => {
            if (toast.parentNode) {
                document.body.removeChild(toast);
            }
        }, 300);
    }, 3000);
}

/**
 * Mostra modal de confirmação customizado
 */
function showConfirmationModal(title, message, onConfirm, onCancel) {
    // Criar overlay
    const overlay = document.createElement('div');
    overlay.style.cssText = `
        position: fixed;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        background: rgba(0, 0, 0, 0.5);
        display: flex;
        align-items: center;
        justify-content: center;
        z-index: 10000;
        animation: fadeIn 0.2s ease;
    `;

    // Criar modal
    const modal = document.createElement('div');
    modal.style.cssText = `
        background: white;
        padding: 2rem;
        border-radius: 16px;
        box-shadow: 0 12px 32px rgba(0, 0, 0, 0.2);
        max-width: 400px;
        width: 90%;
        animation: scaleUp 0.2s ease;
    `;

    modal.innerHTML = `
        <h4 style="margin: 0 0 1rem 0; color: #212529; font-weight: 700;">${title}</h4>
        <p style="margin: 0 0 1.5rem 0; color: #6c757d; line-height: 1.6;">${message}</p>
        <div style="display: flex; gap: 0.75rem; justify-content: flex-end;">
            <button class="btn btn-outline-secondary" id="modalCancel">Cancelar</button>
            <button class="btn btn-primary" id="modalConfirm">Confirmar</button>
        </div>
    `;

    overlay.appendChild(modal);
    document.body.appendChild(overlay);

    // Event listeners
    document.getElementById('modalConfirm').addEventListener('click', function() {
        document.body.removeChild(overlay);
        if (onConfirm) onConfirm();
    });

    document.getElementById('modalCancel').addEventListener('click', function() {
        document.body.removeChild(overlay);
        if (onCancel) onCancel();
    });

    // Fechar ao clicar no overlay
    overlay.addEventListener('click', function(e) {
        if (e.target === overlay) {
            document.body.removeChild(overlay);
            if (onCancel) onCancel();
        }
    });

    // Fechar com ESC
    const escHandler = function(e) {
        if (e.key === 'Escape') {
            if (overlay.parentNode) {
                document.body.removeChild(overlay);
                if (onCancel) onCancel();
            }
            document.removeEventListener('keydown', escHandler);
        }
    };
    document.addEventListener('keydown', escHandler);
}

/**
 * Anima contador com efeito de contagem
 */
function animateCounter(element) {
    element.style.transform = 'scale(1.2)';
    element.style.transition = 'transform 0.2s ease';

    setTimeout(() => {
        element.style.transform = 'scale(1)';
    }, 200);
}

/**
 * Anima valor numérico com efeito de contagem
 */
function animateValue(element, start, end, duration) {
    const range = end - start;
    const increment = range / (duration / 16);
    let current = start;

    const timer = setInterval(() => {
        current += increment;
        if ((increment > 0 && current >= end) || (increment < 0 && current <= end)) {
            element.textContent = end;
            clearInterval(timer);
        } else {
            element.textContent = Math.round(current);
        }
    }, 16);
}

/**
 * Formata nome do status
 */
function formatStatus(status) {
    const statusMap = {
        'A_FAZER': 'A Fazer',
        'EM_ANDAMENTO': 'Em Andamento',
        'AGUARDANDO': 'Aguardando',
        'CONCLUIDO': 'Concluído'
    };

    return statusMap[status] || status;
}

/**
 * Função auxiliar para debug
 */
function logBoardState() {
    console.log('📊 Estado do Board:');
    console.log('==================');

    const columns = document.querySelectorAll('.column-content');

    columns.forEach(column => {
        const status = column.dataset.status;
        const cards = column.querySelectorAll('.event-card');
        console.log(`\n${formatStatus(status)}: ${cards.length} cards`);

        cards.forEach((card, index) => {
            console.log(`  ${index + 1}. Evento ID: ${card.dataset.eventId} - ${card.dataset.title}`);
        });
    });

    console.log('\n==================');
    console.log(`Total de eventos: ${document.querySelectorAll('.event-card').length}`);
    console.log(`Busca ativa: ${BoardState.searchActive ? 'Sim' : 'Não'}`);
    if (BoardState.searchActive) {
        console.log(`Filtro: "${BoardState.currentFilter}"`);
    }
}

// Expor funções úteis para o console
window.BoardUtils = {
    logBoardState,
    updateAllCounters,
    updateColumnProgress,
    clearSearch,
    filterCards
};

// CSS adicional para animações (injetado via JS)
const styleSheet = document.createElement('style');
styleSheet.textContent = `
    @keyframes slideOutRight {
        from {
            opacity: 1;
            transform: translateX(0);
        }
        to {
            opacity: 0;
            transform: translateX(100px);
        }
    }
`;
document.head.appendChild(styleSheet);

console.log('🎨 Board Utils carregados. Use window.BoardUtils para debug.');
