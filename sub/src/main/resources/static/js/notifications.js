document.addEventListener('DOMContentLoaded', function() {
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

    const headers = {
        'Content-Type': 'application/json'
    };
    if (csrfToken && csrfHeader) {
        headers[csrfHeader] = csrfToken;
    }

    // Função auxiliar para chamadas fetch
    async function performAction(url, method, successCallback) {
        try {
            const response = await fetch(url, {
                method: method,
                headers: headers
            });
            if (!response.ok) {
                throw new Error('Erro na requisição: ' + response.statusText);
            }
            const data = await response.json();
            if (data.status === 'success') {
                successCallback(data);
            } else {
                console.error('Erro na ação:', data.message);
            }
        } catch (error) {
            console.error('Erro:', error);
        }
    }

    // Manipuladores de eventos
    document.body.addEventListener('click', function(event) {
        const target = event.target.closest('button');
        if (!target) return;

        const id = target.dataset.id;
        const action = target.dataset.action;

        if (!action) return;

        const notificationItem = target.closest('.notification-item');

        switch (action) {
            case 'mark-read':
                performAction(`/notifications/api/${id}/mark-read`, 'POST', () => {
                    notificationItem.classList.remove('notification-unread');
                    notificationItem.classList.add('notification-read');
                    target.style.display = 'none';
                    const unreadBtn = notificationItem.querySelector('[data-action="mark-unread"]');
                    if (unreadBtn) unreadBtn.style.display = 'inline-block';
                });
                break;
            case 'mark-unread':
                performAction(`/notifications/api/${id}/mark-unread`, 'POST', () => {
                    notificationItem.classList.add('notification-unread');
                    notificationItem.classList.remove('notification-read');
                    target.style.display = 'none';
                    const readBtn = notificationItem.querySelector('[data-action="mark-read"]');
                    if (readBtn) readBtn.style.display = 'inline-block';
                });
                break;
            case 'archive':
                performAction(`/notifications/api/${id}/archive`, 'POST', () => {
                    notificationItem.style.transition = 'opacity 0.5s';
                    notificationItem.style.opacity = '0';
                    setTimeout(() => notificationItem.remove(), 500);
                });
                break;
            case 'delete':
                if (confirm('Tem certeza que deseja deletar esta notificação?')) {
                    performAction(`/notifications/api/${id}`, 'DELETE', () => {
                        notificationItem.style.transition = 'opacity 0.5s';
                        notificationItem.style.opacity = '0';
                        setTimeout(() => notificationItem.remove(), 500);
                    });
                }
                break;
        }
    });

    const markAllBtn = document.getElementById('markAllReadBtn');
    if (markAllBtn) {
        markAllBtn.addEventListener('click', function() {
            performAction('/notifications/api/mark-all-read', 'POST', (data) => {
                document.querySelectorAll('.notification-unread').forEach(item => {
                    item.classList.remove('notification-unread');
                    item.classList.add('notification-read');
                    const readBtn = item.querySelector('[data-action="mark-read"]');
                    if(readBtn) readBtn.style.display = 'none';

                    const unreadBtn = item.querySelector('[data-action="mark-unread"]');
                    if (unreadBtn) unreadBtn.style.display = 'inline-block';
                });
                markAllBtn.style.display = 'none';
            });
        });
    }

});
