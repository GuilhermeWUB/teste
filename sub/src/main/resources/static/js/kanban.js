(function () {
    'use strict';

    document.addEventListener('DOMContentLoaded', () => {
        initCardMenus();
    });

    function initCardMenus() {
        const triggers = document.querySelectorAll('[data-menu-trigger]');
        if (!triggers.length) {
            return;
        }

        const closeAllMenus = () => {
            document.querySelectorAll('.task-card-menu-dropdown.is-open').forEach(dropdown => {
                dropdown.classList.remove('is-open');
                const trigger = dropdown.closest('.task-card-menu-wrapper')?.querySelector('[data-menu-trigger]');
                if (trigger) {
                    trigger.setAttribute('aria-expanded', 'false');
                }
            });
        };

        triggers.forEach(trigger => {
            const dropdown = trigger.closest('.task-card-menu-wrapper')?.querySelector('.task-card-menu-dropdown');
            if (!dropdown) {
                return;
            }

            trigger.addEventListener('click', event => {
                event.preventDefault();
                event.stopPropagation();

                const wasOpen = dropdown.classList.contains('is-open');
                closeAllMenus();

                if (!wasOpen) {
                    dropdown.classList.add('is-open');
                    trigger.setAttribute('aria-expanded', 'true');
                }
            });
        });

        document.addEventListener('click', event => {
            if (!event.target.closest('.task-card-menu-wrapper')) {
                closeAllMenus();
            }
        });

        document.addEventListener('keydown', event => {
            if (event.key === 'Escape') {
                closeAllMenus();
            }
        });

        const deleteButtons = document.querySelectorAll('.task-card-menu-item[data-action="delete"]');
        deleteButtons.forEach(button => {
            button.addEventListener('click', event => handleDeleteAction(event, closeAllMenus));
        });
    }

    async function handleDeleteAction(event, closeMenusFn) {
        event.preventDefault();
        event.stopPropagation();

        const button = event.currentTarget;
        const eventId = button.dataset.eventId;
        if (!eventId) {
            return;
        }

        closeMenusFn?.();

        const confirmationMessage = 'Tem certeza que deseja excluir este evento? Essa ação não pode ser desfeita.';
        if (!window.confirm(confirmationMessage)) {
            return;
        }

        const card = button.closest('.task-card');
        if (!card) {
            return;
        }

        setButtonBusy(button, true);

        try {
            await requestDelete(eventId);
            card.remove();
            updateColumnState(button.closest('.tasks-container'));
            showFeedback('success', 'Evento excluído com sucesso.');
        } catch (error) {
            console.error('Erro ao remover evento', error);
            showFeedback('error', error?.message || 'Não foi possível remover o evento. Tente novamente.');
        } finally {
            setButtonBusy(button, false);
        }
    }

    async function requestDelete(eventId) {
        const headers = buildFetchHeaders();
        const response = await fetch(`/events/api/${eventId}`, {
            method: 'DELETE',
            headers
        });

        let payload = null;
        try {
            payload = await response.json();
        } catch (ignored) {
            payload = null;
        }

        if (!response.ok) {
            const message = payload?.error || payload?.message || 'Falha ao remover evento.';
            throw new Error(message);
        }

        if (payload && payload.success === false) {
            throw new Error(payload.message || 'Falha ao remover evento.');
        }

        return payload;
    }

    function buildFetchHeaders() {
        const headers = new Headers({
            'Accept': 'application/json',
            'X-Requested-With': 'XMLHttpRequest'
        });

        const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
        const csrfHeaderName = document.querySelector('meta[name="_csrf_header"]')?.content;

        if (csrfToken && csrfHeaderName) {
            headers.append(csrfHeaderName, csrfToken);
        }

        return headers;
    }

    function updateColumnState(tasksContainer) {
        if (!tasksContainer) {
            return;
        }

        const cards = tasksContainer.querySelectorAll('.task-card');
        let emptyState = tasksContainer.querySelector('.empty-state');

        if (cards.length === 0) {
            if (!emptyState) {
                emptyState = document.createElement('div');
                emptyState.className = 'empty-state';
                emptyState.textContent = 'Nenhum evento neste status.';
                tasksContainer.appendChild(emptyState);
            }
        } else if (emptyState) {
            emptyState.remove();
        }

        const column = tasksContainer.closest('.kanban-column');
        const counter = column?.querySelector('.column-count');
        if (counter) {
            counter.textContent = String(cards.length);
        }
    }

    function setButtonBusy(button, busy) {
        if (!button) {
            return;
        }

        button.disabled = Boolean(busy);
        button.classList.toggle('is-loading', Boolean(busy));
    }

    function showFeedback(type, message) {
        const feedbackContainer = document.getElementById('kanban-feedback');
        if (!feedbackContainer) {
            return;
        }

        const alert = document.createElement('div');
        alert.className = `kanban-alert ${type === 'success' ? 'kanban-alert-success' : 'kanban-alert-error'}`;

        const icon = document.createElement('i');
        icon.className = type === 'success' ? 'bi bi-check-circle-fill' : 'bi bi-exclamation-triangle-fill';
        alert.appendChild(icon);

        const text = document.createElement('span');
        text.textContent = message;
        alert.appendChild(text);

        feedbackContainer.appendChild(alert);

        setTimeout(() => {
            alert.classList.add('is-fading');
            alert.style.transition = 'opacity 0.3s ease';
            alert.style.opacity = '0';
            setTimeout(() => alert.remove(), 300);
        }, 4000);
    }
})();
