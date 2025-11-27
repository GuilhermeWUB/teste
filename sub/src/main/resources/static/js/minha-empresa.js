(function() {
    'use strict';

    // ========== CONSTANTES ==========
    const API_USUARIOS = '/crm/api/usuarios';
    const API_REGIONAIS = '/crm/api/regionais';

    // ========== STATE ==========
    const state = {
        users: [],
        filteredUsers: [],
        isEditingUser: false,
        editingUserId: null,

        regionais: [],
        filteredRegionais: [],
        isEditingRegional: false,
        editingRegionalId: null,

        currentSection: 'usuarios'
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
        console.log('Inicializando Minha Empresa');
        bindEvents();
        loadUsers();
        loadStats();
        loadRegionais();
        loadRegionaisStats();
    }

    function bindEvents() {
        // Navegação do menu
        document.querySelectorAll('.menu-item').forEach(item => {
            item.addEventListener('click', handleMenuClick);
        });

        // Forms
        const userForm = document.getElementById('userForm');
        if (userForm) {
            userForm.addEventListener('submit', handleUserFormSubmit);
        }

        const regionalForm = document.getElementById('regionalForm');
        if (regionalForm) {
            regionalForm.addEventListener('submit', handleRegionalFormSubmit);
        }

        // Fechar modais ao clicar fora
        window.addEventListener('click', (e) => {
            const userModal = document.getElementById('userModal');
            const confirmModal = document.getElementById('confirmModal');
            const regionalModal = document.getElementById('regionalModal');
            const confirmRegionalModal = document.getElementById('confirmRegionalModal');

            if (e.target === userModal) closeModal();
            if (e.target === confirmModal) closeConfirmModal();
            if (e.target === regionalModal) closeRegionalModal();
            if (e.target === confirmRegionalModal) closeConfirmRegionalModal();
        });
    }

    // ========== NAVEGAÇÃO ==========
    function handleMenuClick(e) {
        e.preventDefault();
        const section = this.getAttribute('data-section');
        console.log('Navegando para seção:', section);

        // Atualizar menu ativo
        document.querySelectorAll('.menu-item').forEach(item => {
            item.classList.remove('active');
        });
        this.classList.add('active');

        // Mostrar/ocultar seções
        document.querySelectorAll('.content-section').forEach(sec => {
            sec.style.display = 'none';
        });
        const targetSection = document.getElementById(`section-${section}`);
        if (targetSection) {
            targetSection.style.display = 'block';
            console.log('Seção exibida:', section);
        } else {
            console.error('Seção não encontrada:', `section-${section}`);
        }

        state.currentSection = section;
    }

    // ========================================
    // USUÁRIOS
    // ========================================

    async function loadUsers() {
        try {
            const response = await fetch(API_USUARIOS, {
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
            const response = await fetch(`${API_USUARIOS}/stats`, {
                headers: buildHeaders({ 'Accept': 'application/json' })
            });

            if (!response.ok) {
                throw new Error('Erro ao carregar estatísticas');
            }

            const data = await response.json();
            document.getElementById('activeCount').textContent = data.ativos || 0;
            document.getElementById('blockedCount').textContent = data.bloqueados || 0;
        } catch (error) {
            console.error('Erro ao carregar estatísticas:', error);
        }
    }

    function renderUsers() {
        const tbody = document.getElementById('usersTableBody');
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

    window.applyFilters = function() {
        const statusFilter = document.getElementById('filterStatus').value;
        const searchTerm = document.getElementById('searchInput').value.toLowerCase();

        state.filteredUsers = state.users.filter(user => {
            if (statusFilter !== '') {
                const isActive = statusFilter === 'true';
                if (user.active !== isActive) return false;
            }

            if (searchTerm) {
                const searchableText = `${user.fullName} ${user.email} ${user.username}`.toLowerCase();
                if (!searchableText.includes(searchTerm)) return false;
            }

            return true;
        });

        renderUsers();
    };

    window.openCreateModal = function() {
        state.isEditingUser = false;
        state.editingUserId = null;

        document.getElementById('modalTitle').textContent = 'Adicionar Usuário';
        document.getElementById('userForm').reset();
        document.getElementById('userId').value = '';
        document.getElementById('password').required = true;

        document.getElementById('userModal').style.display = 'block';
    };

    window.editUser = function(id) {
        const user = state.users.find(u => u.id === id);
        if (!user) return;

        state.isEditingUser = true;
        state.editingUserId = id;

        document.getElementById('modalTitle').textContent = 'Editar Usuário';
        document.getElementById('userId').value = user.id;
        document.getElementById('fullName').value = user.fullName;
        document.getElementById('username').value = user.username;
        document.getElementById('email').value = user.email;
        document.getElementById('password').value = '';
        document.getElementById('password').required = false;

        document.getElementById('userModal').style.display = 'block';
    };

    window.closeModal = function() {
        document.getElementById('userModal').style.display = 'none';
        document.getElementById('userForm').reset();
        state.isEditingUser = false;
        state.editingUserId = null;
    };

    window.closeConfirmModal = function() {
        document.getElementById('confirmModal').style.display = 'none';
    };

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

        document.getElementById('confirmModal').style.display = 'block';
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

        document.getElementById('confirmModal').style.display = 'block';
    };

    async function handleUserFormSubmit(e) {
        e.preventDefault();

        const userData = {
            fullName: document.getElementById('fullName').value,
            username: document.getElementById('username').value,
            email: document.getElementById('email').value,
            password: document.getElementById('password').value || null
        };

        try {
            if (state.isEditingUser) {
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

    async function createUser(userData) {
        const response = await fetch(API_USUARIOS, {
            method: 'POST',
            headers: buildHeaders({ 'Content-Type': 'application/json' }),
            body: JSON.stringify(userData)
        });

        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.error || 'Erro ao criar usuário');
        }

        return await response.json();
    }

    async function updateUser(id, userData) {
        const response = await fetch(`${API_USUARIOS}/${id}`, {
            method: 'PUT',
            headers: buildHeaders({ 'Content-Type': 'application/json' }),
            body: JSON.stringify(userData)
        });

        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.error || 'Erro ao atualizar usuário');
        }

        return await response.json();
    }

    async function blockUser(id) {
        const response = await fetch(`${API_USUARIOS}/${id}/bloquear`, {
            method: 'PUT',
            headers: buildHeaders({ 'Content-Type': 'application/json' })
        });

        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.error || 'Erro ao bloquear usuário');
        }

        return await response.json();
    }

    async function unblockUser(id) {
        const response = await fetch(`${API_USUARIOS}/${id}/desbloquear`, {
            method: 'PUT',
            headers: buildHeaders({ 'Content-Type': 'application/json' })
        });

        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.error || 'Erro ao desbloquear usuário');
        }

        return await response.json();
    }

    // ========================================
    // REGIONAIS
    // ========================================

    async function loadRegionais() {
        try {
            const response = await fetch(API_REGIONAIS, {
                headers: buildHeaders({ 'Accept': 'application/json' })
            });

            if (!response.ok) {
                throw new Error('Erro ao carregar regionais');
            }

            const data = await response.json();
            state.regionais = data;
            state.filteredRegionais = data;
            renderRegionais();
        } catch (error) {
            console.error('Erro ao carregar regionais:', error);
            showError('Erro ao carregar regionais. Tente novamente.');
        }
    }

    async function loadRegionaisStats() {
        try {
            const response = await fetch(`${API_REGIONAIS}/stats`, {
                headers: buildHeaders({ 'Accept': 'application/json' })
            });

            if (!response.ok) {
                throw new Error('Erro ao carregar estatísticas');
            }

            const data = await response.json();
            document.getElementById('regionaisActiveCount').textContent = data.ativas || 0;
            document.getElementById('regionaisInactiveCount').textContent = data.inativas || 0;
        } catch (error) {
            console.error('Erro ao carregar estatísticas de regionais:', error);
        }
    }

    function renderRegionais() {
        const tbody = document.getElementById('regionaisTableBody');
        if (!tbody) return;

        if (state.filteredRegionais.length === 0) {
            tbody.innerHTML = `
                <tr class="empty-row">
                    <td colspan="6" class="text-center">
                        <i class="bi bi-inbox"></i>
                        <p>Nenhuma regional encontrada</p>
                    </td>
                </tr>
            `;
            return;
        }

        tbody.innerHTML = state.filteredRegionais.map(regional => `
            <tr>
                <td>${escapeHtml(regional.name)}</td>
                <td><strong>${escapeHtml(regional.code)}</strong></td>
                <td>${escapeHtml(regional.description || '-')}</td>
                <td>${formatDate(regional.createdAt)}</td>
                <td>
                    <span class="badge ${regional.active ? 'badge-active' : 'badge-blocked'}">
                        ${regional.active ? 'Ativa' : 'Inativa'}
                    </span>
                </td>
                <td>
                    <div class="action-buttons">
                        <button class="btn-icon btn-edit" onclick="window.editRegional(${regional.id})" title="Editar">
                            <i class="bi bi-pencil"></i>
                        </button>
                        ${regional.active ?
                            `<button class="btn-icon btn-block" onclick="window.confirmDeactivate(${regional.id})" title="Desativar">
                                <i class="bi bi-x-circle"></i>
                            </button>` :
                            `<button class="btn-icon btn-unblock" onclick="window.confirmActivate(${regional.id})" title="Ativar">
                                <i class="bi bi-check-circle"></i>
                            </button>`
                        }
                    </div>
                </td>
            </tr>
        `).join('');
    }

    window.applyRegionalFilters = function() {
        const statusFilter = document.getElementById('filterRegionalStatus').value;
        const searchTerm = document.getElementById('searchRegionalInput').value.toLowerCase();

        state.filteredRegionais = state.regionais.filter(regional => {
            if (statusFilter !== '') {
                const isActive = statusFilter === 'true';
                if (regional.active !== isActive) return false;
            }

            if (searchTerm) {
                const searchableText = `${regional.name} ${regional.code} ${regional.description || ''}`.toLowerCase();
                if (!searchableText.includes(searchTerm)) return false;
            }

            return true;
        });

        renderRegionais();
    };

    window.openCreateRegionalModal = function() {
        state.isEditingRegional = false;
        state.editingRegionalId = null;

        document.getElementById('regionalModalTitle').textContent = 'Adicionar Regional';
        document.getElementById('regionalForm').reset();
        document.getElementById('regionalId').value = '';

        document.getElementById('regionalModal').style.display = 'block';
    };

    window.editRegional = function(id) {
        const regional = state.regionais.find(r => r.id === id);
        if (!regional) return;

        state.isEditingRegional = true;
        state.editingRegionalId = id;

        document.getElementById('regionalModalTitle').textContent = 'Editar Regional';
        document.getElementById('regionalId').value = regional.id;
        document.getElementById('regionalName').value = regional.name;
        document.getElementById('regionalCode').value = regional.code;
        document.getElementById('regionalDescription').value = regional.description || '';

        document.getElementById('regionalModal').style.display = 'block';
    };

    window.closeRegionalModal = function() {
        document.getElementById('regionalModal').style.display = 'none';
        document.getElementById('regionalForm').reset();
        state.isEditingRegional = false;
        state.editingRegionalId = null;
    };

    window.closeConfirmRegionalModal = function() {
        document.getElementById('confirmRegionalModal').style.display = 'none';
    };

    window.confirmActivate = function(id) {
        const regional = state.regionais.find(r => r.id === id);
        if (!regional) return;

        document.getElementById('confirmRegionalTitle').textContent = 'Ativar Regional';
        document.getElementById('confirmRegionalMessage').textContent =
            `Deseja realmente ativar a regional "${regional.name}"?`;

        const confirmBtn = document.getElementById('confirmRegionalButton');
        confirmBtn.onclick = async () => {
            try {
                await activateRegional(id);
                showSuccess('Regional ativada com sucesso!');
                closeConfirmRegionalModal();
                loadRegionais();
                loadRegionaisStats();
            } catch (error) {
                showError(error.message);
            }
        };

        document.getElementById('confirmRegionalModal').style.display = 'block';
    };

    window.confirmDeactivate = function(id) {
        const regional = state.regionais.find(r => r.id === id);
        if (!regional) return;

        document.getElementById('confirmRegionalTitle').textContent = 'Desativar Regional';
        document.getElementById('confirmRegionalMessage').textContent =
            `Deseja realmente desativar a regional "${regional.name}"?`;

        const confirmBtn = document.getElementById('confirmRegionalButton');
        confirmBtn.onclick = async () => {
            try {
                await deactivateRegional(id);
                showSuccess('Regional desativada com sucesso!');
                closeConfirmRegionalModal();
                loadRegionais();
                loadRegionaisStats();
            } catch (error) {
                showError(error.message);
            }
        };

        document.getElementById('confirmRegionalModal').style.display = 'block';
    };

    async function handleRegionalFormSubmit(e) {
        e.preventDefault();

        const regionalData = {
            name: document.getElementById('regionalName').value,
            code: document.getElementById('regionalCode').value,
            description: document.getElementById('regionalDescription').value || null
        };

        try {
            if (state.isEditingRegional) {
                await updateRegional(state.editingRegionalId, regionalData);
                showSuccess('Regional atualizada com sucesso!');
            } else {
                await createRegional(regionalData);
                showSuccess('Regional criada com sucesso!');
            }

            closeRegionalModal();
            loadRegionais();
            loadRegionaisStats();
        } catch (error) {
            showError(error.message);
        }
    }

    async function createRegional(regionalData) {
        const response = await fetch(API_REGIONAIS, {
            method: 'POST',
            headers: buildHeaders({ 'Content-Type': 'application/json' }),
            body: JSON.stringify(regionalData)
        });

        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.error || 'Erro ao criar regional');
        }

        return await response.json();
    }

    async function updateRegional(id, regionalData) {
        const response = await fetch(`${API_REGIONAIS}/${id}`, {
            method: 'PUT',
            headers: buildHeaders({ 'Content-Type': 'application/json' }),
            body: JSON.stringify(regionalData)
        });

        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.error || 'Erro ao atualizar regional');
        }

        return await response.json();
    }

    async function activateRegional(id) {
        const response = await fetch(`${API_REGIONAIS}/${id}/ativar`, {
            method: 'PUT',
            headers: buildHeaders({ 'Content-Type': 'application/json' })
        });

        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.error || 'Erro ao ativar regional');
        }

        return await response.json();
    }

    async function deactivateRegional(id) {
        const response = await fetch(`${API_REGIONAIS}/${id}/desativar`, {
            method: 'PUT',
            headers: buildHeaders({ 'Content-Type': 'application/json' })
        });

        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.error || 'Erro ao desativar regional');
        }

        return await response.json();
    }

    // ========== UTILITÁRIOS ==========
    function escapeHtml(text) {
        if (!text) return '';
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
