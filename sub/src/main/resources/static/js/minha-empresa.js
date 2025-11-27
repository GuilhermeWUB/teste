(function() {
    'use strict';

    // ========== CONSTANTES ==========
    const API_BASE = '/crm/api/usuarios';

    // ========== STATE ==========
    const state = {
        users: [],
        filteredUsers: [],
        isEditing: false,
        editingUserId: null
    };

    // ========== SELETORES DOM ==========
    const selectors = {
        usersTableBody: () => document.getElementById('usersTableBody'),
        activeCount: () => document.getElementById('activeCount'),
        blockedCount: () => document.getElementById('blockedCount'),
        filterStatus: () => document.getElementById('filterStatus'),
        searchInput: () => document.getElementById('searchInput'),
        userModal: () => document.getElementById('userModal'),
        confirmModal: () => document.getElementById('confirmModal'),
        userForm: () => document.getElementById('userForm'),
        modalTitle: () => document.getElementById('modalTitle')
    };

    // ========== CSRF TOKEN ==========
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

    function buildHeaders(extra = {}) {
        const headers = {
            'X-Requested-With': 'XMLHttpRequest',
            ...extra
        };
        if (csrfToken && csrfHeader) {
            headers[csrfHeader] = csrfToken;
        }
        return headers;
    }

    // ========== INICIALIZAÇÃO ==========
    function init() {
        console.log('Inicializando Minha Empresa - Usuários');
        bindEvents();
        loadUsers();
        loadStats();
    }

    function bindEvents() {
        const form = selectors.userForm();
        if (form) {
            form.addEventListener('submit', handleFormSubmit);
        }

        // Fechar modal ao clicar fora
        window.addEventListener('click', (e) => {
            const userModal = selectors.userModal();
            const confirmModal = selectors.confirmModal();

            if (e.target === userModal) {
                closeModal();
            }
            if (e.target === confirmModal) {
                closeConfirmModal();
            }
        });
    }

    // ========== REQUISIÇÕES API ==========
    async function loadUsers() {
        try {
            const response = await fetch(API_BASE, {
                headers: buildHeaders({ 'Accept': 'application/json' })
            });

            if (!response.ok) {
                throw new Error('Erro ao carregar usuários');
            }

            const data = await response.json();
            state.users = data;
            state.filteredUsers = data;
            renderUsers();
        } catch (error) {
            console.error('Erro ao carregar usuários:', error);
            showError('Erro ao carregar usuários. Tente novamente.');
        }
    }

    async function loadStats() {
        try {
            const response = await fetch(`${API_BASE}/stats`, {
                headers: buildHeaders({ 'Accept': 'application/json' })
            });

            if (!response.ok) {
                throw new Error('Erro ao carregar estatísticas');
            }

            const data = await response.json();
            selectors.activeCount().textContent = data.ativos || 0;
            selectors.blockedCount().textContent = data.bloqueados || 0;
        } catch (error) {
            console.error('Erro ao carregar estatísticas:', error);
        }
    }

    async function createUser(userData) {
        const response = await fetch(API_BASE, {
            method: 'POST',
            headers: buildHeaders({
                'Content-Type': 'application/json'
            }),
            body: JSON.stringify(userData)
        });

        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.error || 'Erro ao criar usuário');
        }

        return await response.json();
    }

    async function updateUser(id, userData) {
        const response = await fetch(`${API_BASE}/${id}`, {
            method: 'PUT',
            headers: buildHeaders({
                'Content-Type': 'application/json'
            }),
            body: JSON.stringify(userData)
        });

        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.error || 'Erro ao atualizar usuário');
        }

        return await response.json();
    }

    async function blockUser(id) {
        const response = await fetch(`${API_BASE}/${id}/bloquear`, {
            method: 'PUT',
            headers: buildHeaders({
                'Content-Type': 'application/json'
            })
        });

        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.error || 'Erro ao bloquear usuário');
        }

        return await response.json();
    }

    async function unblockUser(id) {
        const response = await fetch(`${API_BASE}/${id}/desbloquear`, {
            method: 'PUT',
            headers: buildHeaders({
                'Content-Type': 'application/json'
            })
        });

        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.error || 'Erro ao desbloquear usuário');
        }

        return await response.json();
    }

    // ========== RENDERIZAÇÃO ==========
    function renderUsers() {
        const tbody = selectors.usersTableBody();
        if (!tbody) return;

        if (state.filteredUsers.length === 0) {
            tbody.innerHTML = `
                <tr class="empty-row">
                    <td colspan="6" class="text-center">
                        <i class="bi bi-inbox"></i>
                        <p>Nenhum usuário encontrado</p>
                    </td>
                </tr>
            `;
            return;
        }

        tbody.innerHTML = state.filteredUsers.map(user => `
            <tr>
                <td>${escapeHtml(user.fullName)}</td>
                <td>${escapeHtml(user.email)}</td>
                <td>${escapeHtml(user.username)}</td>
                <td>${formatDate(user.createdAt)}</td>
                <td>
                    <span class="badge ${user.active ? 'badge-active' : 'badge-blocked'}">
                        ${user.active ? 'Ativo' : 'Bloqueado'}
                    </span>
                </td>
                <td>
                    <div class="action-buttons">
                        <button class="btn-icon btn-edit" onclick="window.editUser(${user.id})" title="Editar">
                            <i class="bi bi-pencil"></i>
                        </button>
                        ${user.active ?
                            `<button class="btn-icon btn-block" onclick="window.confirmBlock(${user.id})" title="Bloquear">
                                <i class="bi bi-x-circle"></i>
                            </button>` :
                            `<button class="btn-icon btn-unblock" onclick="window.confirmUnblock(${user.id})" title="Desbloquear">
                                <i class="bi bi-check-circle"></i>
                            </button>`
                        }
                    </div>
                </td>
            </tr>
        `).join('');
    }

    // ========== FILTROS ==========
    window.applyFilters = function() {
        const statusFilter = selectors.filterStatus().value;
        const searchTerm = selectors.searchInput().value.toLowerCase();

        state.filteredUsers = state.users.filter(user => {
            // Filtro de status
            if (statusFilter !== '') {
                const isActive = statusFilter === 'true';
                if (user.active !== isActive) {
                    return false;
                }
            }

            // Filtro de busca
            if (searchTerm) {
                const searchableText = `${user.fullName} ${user.email} ${user.username}`.toLowerCase();
                if (!searchableText.includes(searchTerm)) {
                    return false;
                }
            }

            return true;
        });

        renderUsers();
    };

    // ========== MODAIS ==========
    window.openCreateModal = function() {
        state.isEditing = false;
        state.editingUserId = null;

        selectors.modalTitle().textContent = 'Adicionar Usuário';
        selectors.userForm().reset();
        document.getElementById('userId').value = '';
        document.getElementById('password').required = true;

        selectors.userModal().style.display = 'block';
    };

    window.editUser = function(id) {
        const user = state.users.find(u => u.id === id);
        if (!user) return;

        state.isEditing = true;
        state.editingUserId = id;

        selectors.modalTitle().textContent = 'Editar Usuário';
        document.getElementById('userId').value = user.id;
        document.getElementById('fullName').value = user.fullName;
        document.getElementById('username').value = user.username;
        document.getElementById('email').value = user.email;
        document.getElementById('password').value = '';
        document.getElementById('password').required = false;

        selectors.userModal().style.display = 'block';
    };

    window.closeModal = function() {
        selectors.userModal().style.display = 'none';
        selectors.userForm().reset();
        state.isEditing = false;
        state.editingUserId = null;
    };

    window.closeConfirmModal = function() {
        selectors.confirmModal().style.display = 'none';
    };

    // ========== CONFIRMAÇÕES ==========
    window.confirmBlock = function(id) {
        const user = state.users.find(u => u.id === id);
        if (!user) return;

        document.getElementById('confirmTitle').textContent = 'Bloquear Usuário';
        document.getElementById('confirmMessage').textContent =
            `Deseja realmente bloquear o usuário "${user.fullName}"?`;

        const confirmBtn = document.getElementById('confirmButton');
        confirmBtn.onclick = async () => {
            try {
                await blockUser(id);
                showSuccess('Usuário bloqueado com sucesso!');
                closeConfirmModal();
                loadUsers();
                loadStats();
            } catch (error) {
                showError(error.message);
            }
        };

        selectors.confirmModal().style.display = 'block';
    };

    window.confirmUnblock = function(id) {
        const user = state.users.find(u => u.id === id);
        if (!user) return;

        document.getElementById('confirmTitle').textContent = 'Desbloquear Usuário';
        document.getElementById('confirmMessage').textContent =
            `Deseja realmente desbloquear o usuário "${user.fullName}"?`;

        const confirmBtn = document.getElementById('confirmButton');
        confirmBtn.onclick = async () => {
            try {
                await unblockUser(id);
                showSuccess('Usuário desbloqueado com sucesso!');
                closeConfirmModal();
                loadUsers();
                loadStats();
            } catch (error) {
                showError(error.message);
            }
        };

        selectors.confirmModal().style.display = 'block';
    };

    // ========== FORM SUBMIT ==========
    async function handleFormSubmit(e) {
        e.preventDefault();

        const userData = {
            fullName: document.getElementById('fullName').value,
            username: document.getElementById('username').value,
            email: document.getElementById('email').value,
            password: document.getElementById('password').value || null
        };

        try {
            if (state.isEditing) {
                await updateUser(state.editingUserId, userData);
                showSuccess('Usuário atualizado com sucesso!');
            } else {
                if (!userData.password) {
                    showError('A senha é obrigatória para novos usuários.');
                    return;
                }
                await createUser(userData);
                showSuccess('Usuário criado com sucesso!');
            }

            closeModal();
            loadUsers();
            loadStats();
        } catch (error) {
            showError(error.message);
        }
    }

    // ========== UTILITÁRIOS ==========
    function escapeHtml(text) {
        const map = {
            '&': '&amp;',
            '<': '&lt;',
            '>': '&gt;',
            '"': '&quot;',
            "'": '&#039;'
        };
        return text.replace(/[&<>"']/g, m => map[m]);
    }

    function formatDate(dateString) {
        if (!dateString) return '-';
        const date = new Date(dateString);
        return date.toLocaleDateString('pt-BR', {
            day: '2-digit',
            month: '2-digit',
            year: 'numeric'
        });
    }

    function showSuccess(message) {
        alert(message);
    }

    function showError(message) {
        alert('Erro: ' + message);
    }

    // ========== EXECUÇÃO INICIAL ==========
    document.addEventListener('DOMContentLoaded', init);
})();
