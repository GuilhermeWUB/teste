// Board de Eventos - Drag & Drop com SortableJS

document.addEventListener('DOMContentLoaded', function() {
    console.log('Board de Eventos inicializado');

    // Inicializar SortableJS em cada coluna
    const columns = document.querySelectorAll('.column-content');

    columns.forEach(column => {
        new Sortable(column, {
            group: 'eventos', // Permite mover entre colunas
            animation: 200,
            ghostClass: 'sortable-ghost',
            dragClass: 'dragging',
            handle: '.event-card',

            // Quando o card é solto em uma nova coluna
            onEnd: function(evt) {
                const eventId = evt.item.dataset.eventId;
                const newStatus = evt.to.dataset.status;
                const oldStatus = evt.from.dataset.status;

                // Se mudou de coluna, atualizar no backend
                if (newStatus !== oldStatus) {
                    updateEventStatus(eventId, newStatus, evt.item, evt.from);
                }
            }
        });
    });

    console.log('SortableJS inicializado em', columns.length, 'colunas');
});

/**
 * Atualiza o status do evento no backend
 */
function updateEventStatus(eventId, newStatus, cardElement, oldColumn) {
    console.log(`Atualizando evento ${eventId} para status ${newStatus}`);

    // Adicionar classe de loading no card
    cardElement.classList.add('loading');

    fetch(`/events/api/${eventId}/status`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({ status: newStatus })
    })
    .then(response => {
        if (!response.ok) {
            throw new Error('Erro ao atualizar status');
        }
        return response.json();
    })
    .then(data => {
        console.log('Status atualizado com sucesso:', data);
        cardElement.classList.remove('loading');

        // Mostrar notificação de sucesso
        showToast('Status atualizado com sucesso!', 'success');

        // Atualizar contadores
        updateCounters();
    })
    .catch(error => {
        console.error('Erro ao atualizar status:', error);
        cardElement.classList.remove('loading');

        // Reverter o card para a coluna original em caso de erro
        oldColumn.appendChild(cardElement);

        // Mostrar notificação de erro
        showToast('Erro ao atualizar status. Tente novamente.', 'error');
    });
}

/**
 * Atualiza os contadores de cards em cada coluna
 */
function updateCounters() {
    const columns = document.querySelectorAll('.column-content');

    columns.forEach(column => {
        const status = column.dataset.status;
        const count = column.querySelectorAll('.event-card').length;

        // Encontrar o badge correspondente e atualizar
        const header = column.previousElementSibling;
        const badge = header.querySelector('.badge');
        if (badge) {
            badge.textContent = count;
        }
    });
}

/**
 * Mostra notificação toast
 */
function showToast(message, type = 'success') {
    // Criar elemento do toast
    const toast = document.createElement('div');
    toast.className = `toast-notification ${type}`;

    const icon = type === 'success' ?
        '<i class="bi bi-check-circle-fill text-success"></i>' :
        '<i class="bi bi-exclamation-circle-fill text-danger"></i>';

    toast.innerHTML = `
        ${icon}
        <span>${message}</span>
    `;

    // Adicionar ao body
    document.body.appendChild(toast);

    // Remover após 3 segundos
    setTimeout(() => {
        toast.style.opacity = '0';
        setTimeout(() => {
            document.body.removeChild(toast);
        }, 300);
    }, 3000);
}

/**
 * Função auxiliar para debug
 */
function logBoardState() {
    const columns = document.querySelectorAll('.column-content');

    columns.forEach(column => {
        const status = column.dataset.status;
        const cards = column.querySelectorAll('.event-card');
        console.log(`Coluna ${status}: ${cards.length} cards`);

        cards.forEach(card => {
            console.log(`  - Evento ID: ${card.dataset.eventId}`);
        });
    });
}

// Expor função para debug no console
window.logBoardState = logBoardState;