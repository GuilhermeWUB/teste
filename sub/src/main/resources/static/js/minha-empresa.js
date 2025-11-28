(function() {
    'use strict';

    console.log('[Minha Empresa] Iniciando módulo...');

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

    console.log('[Minha Empresa] CSRF Token:', csrfToken ? 'Presente' : 'Ausente');

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
        console.log('[Minha Empresa] Executando init()...');

        try {
            bindEvents();

            const initialSection = getInitialSection();
            console.log('[Minha Empresa] Seção inicial:', initialSection);

            navigateToSection(initialSection);

            // Carregar dados baseado na seção inicial
            if (initialSection === 'regionais') {
                console.log('[Minha Empresa] Carregando dados de Regionais...');
                loadRegionais();
                loadRegionaisStats();
            } else {
                console.log('[Minha Empresa] Carregando dados de Usuários...');
                loadUsers();
                loadStats();
            }
        } catch (error) {
            console.error('[Minha Empresa] Erro durante inicialização:', error);
            showError('Erro ao inicializar a página. Verifique o console para mais detalhes.');
        }
    }

    function bindEvents() {
        console.log('[Minha Empresa] Vinculando eventos...');

        // Forms
        const userForm = document.getElementById('userForm');
        if (userForm) {
            userForm.addEventListener('submit', handleUserFormSubmit);
            console.log('[Minha Empresa] Form de usuário vinculado');
        } else {
            console.warn('[Minha Empresa] Form de usuário não encontrado');
        }

        const regionalForm = document.getElementById('regionalForm');
        if (regionalForm) {
            regionalForm.addEventListener('submit', handleRegionalFormSubmit);
            console.log('[Minha Empresa] Form de regional vinculado');
        } else {
            console.warn('[Minha Empresa] Form de regional não encontrado');
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

        console.log('[Minha Empresa] Eventos vinculados com sucesso');
    }

    // ========== NAVEGAÇÃO ==========
    function getInitialSection() {
        const container = document.querySelector('.empresa-content');
        const defaultSection = container?.dataset?.defaultSection;

        console.log('[Minha Empresa] defaultSection do container:', defaultSection);

        return ['usuarios', 'regionais'].includes(defaultSection)
            ? defaultSection
            : 'usuarios';
    }

    function navigateToSection(section) {
        console.log('[Minha Empresa] Navegando para seção:', section);

        const targetSection = document.getElementById(`section-${section}`);
        if (!targetSection) {
            console.error('[Minha Empresa] Seção não encontrada:', `section-${section}`);
            return;
        }

        // Atualizar menu
        document.querySelectorAll('.menu-item').forEach(item => {
            const isActive = item.getAttribute('data-section') === section;
            item.classList.toggle('active', isActive);
        });

        // Mostrar/esconder seções
        document.querySelectorAll('.content-section').forEach(sec => {
            sec.style.display = sec === targetSection ? 'block' : 'none';
        });

        state.currentSection = section;
        console.log('[Minha Empresa] Navegação concluída. Seção atual:', state.currentSection);
    }

    // ========================================
    // USUÁRIOS
    // ========================================

    async function loadUsers() {
        console.log('[Usuários] Carregando usuários...');

        try {
            const response = await fetch(API_USUARIOS, {
                headers: buildHeaders({ 'Accept': 'application/json' })
            });

            console.log('[Usuários] Response status:', response.status);

            if (!response.ok) {
                throw new Error(`Erro HTTP: ${response.status}`);
            }

            const data = await response.json();
            console.log('[Usuários] Dados recebidos:', data.length, 'usuários');

            state.users = data;
            state.filteredUsers = data;
            renderUsers();
        } catch (error) {
            console.error('[Usuários] Erro ao carregar:', error);
            showError('Erro ao carregar usuários. Verifique sua conexão.');

            // Mostrar mensagem na tabela
            const tbody = document.getElementById('usersTableBody');
            if (tbody) {
                tbody.innerHTML = `
                    <tr class="empty-row">
                        <td colspan="6" class="text-center">
                            <i class="bi bi-exclamation-circle"></i>
                            <p style="color: #ff6b6b;">Erro ao carregar usuários</p>
                        </td>
                    </tr>
                `;
            }
        }
    }

    async function loadStats() {
        console.log('[Usuários] Carregando estatísticas...');

        try {
            const response = await fetch(`${API_USUARIOS}/stats`, {
                headers: buildHeaders({ 'Accept': 'application/json' })
            });

            if (!response.ok) {
                throw new Error(`Erro HTTP: ${response.status}`);
            }

            const data = await response.json();
            console.log('[Usuários] Estatísticas:', data);

            const activeEl = document.getElementById('activeCount');
            const blockedEl = document.getElementById('blockedCount');

            if (activeEl) activeEl.textContent = data.ativos || 0;
            if (blockedEl) blockedEl.textContent = data.bloqueados || 0;
        } catch (error) {
            console.error('[Usuários] Erro ao carregar estatísticas:', error);
        }
    }

    function renderUsers() {
        console.log('[Usuários] Renderizando', state.filteredUsers.length, 'usuários');

        const tbody = document.getElementById('usersTableBody');
        if (!tbody) {
            console.error('[Usuários] Elemento usersTableBody não encontrado');
            return;
        }

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

        console.log('[Usuários] Renderização concluída');
    }

    window.applyFilters = function() {
        console.log('[Usuários] Aplicando filtros...');

        const statusFilter = document.getElementById('filterStatus')?.value || '';
        const searchTerm = document.getElementById('searchInput')?.value?.toLowerCase() || '';

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

        console.log('[Usuários] Filtros aplicados. Resultados:', state.filteredUsers.length);
        renderUsers();
    };

    window.openCreateModal = function() {
        console.log('[Usuários] Abrindo modal de criação');

        state.isEditingUser = false;
        state.editingUserId = null;

        document.getElementById('modalTitle').textContent = 'Adicionar Usuário';
        document.getElementById('userForm').reset();
        document.getElementById('userId').value = '';
        document.getElementById('password').required = true;

        document.getElementById('userModal').style.display = 'block';
    };

    window.editUser = function(id) {
        console.log('[Usuários] Editando usuário:', id);

        const user = state.users.find(u => u.id === id);
        if (!user) {
            console.error('[Usuários] Usuário não encontrado:', id);
            return;
        }

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
        console.log('[Usuários] Fechando modal');

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
                await loadUsers();
                await loadStats();
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
                await loadUsers();
                await loadStats();
            } catch (error) {
                showError(error.message);
            }
        };

        document.getElementById('confirmModal').style.display = 'block';
    };

    async function handleUserFormSubmit(e) {
        e.preventDefault();
        console.log('[Usuários] Submetendo formulário...');

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
            await loadUsers();
            await loadStats();
        } catch (error) {
            console.error('[Usuários] Erro ao salvar:', error);
            showError(error.message);
        }
    }

    async function createUser(userData) {
        console.log('[Usuários] Criando usuário...');

        const response = await fetch(API_USUARIOS, {
            method: 'POST',
            headers: buildHeaders({ 'Content-Type': 'application/json' }),
            body: JSON.stringify(userData)
        });

        if (!response.ok) {
            const errorData = await response.json().catch(() => ({}));
            throw new Error(errorData.error || 'Erro ao criar usuário');
        }

        return await response.json();
    }

    async function updateUser(id, userData) {
        console.log('[Usuários] Atualizando usuário:', id);

        const response = await fetch(`${API_USUARIOS}/${id}`, {
            method: 'PUT',
            headers: buildHeaders({ 'Content-Type': 'application/json' }),
            body: JSON.stringify(userData)
        });

        if (!response.ok) {
            const errorData = await response.json().catch(() => ({}));
            throw new Error(errorData.error || 'Erro ao atualizar usuário');
        }

        return await response.json();
    }

    async function blockUser(id) {
        console.log('[Usuários] Bloqueando usuário:', id);

        const response = await fetch(`${API_USUARIOS}/${id}/bloquear`, {
            method: 'PUT',
            headers: buildHeaders({ 'Content-Type': 'application/json' })
        });

        if (!response.ok) {
            const errorData = await response.json().catch(() => ({}));
            throw new Error(errorData.error || 'Erro ao bloquear usuário');
        }

        return await response.json();
    }

    async function unblockUser(id) {
        console.log('[Usuários] Desbloqueando usuário:', id);

        const response = await fetch(`${API_USUARIOS}/${id}/desbloquear`, {
            method: 'PUT',
            headers: buildHeaders({ 'Content-Type': 'application/json' })
        });

        if (!response.ok) {
            const errorData = await response.json().catch(() => ({}));
            throw new Error(errorData.error || 'Erro ao desbloquear usuário');
        }

        return await response.json();
    }

    // ========================================
    // REGIONAIS
    // ========================================

    async function loadRegionais() {
        console.log('[Regionais] Carregando regionais...');

        try {
            const response = await fetch(API_REGIONAIS, {
                headers: buildHeaders({ 'Accept': 'application/json' })
            });

            console.log('[Regionais] Response status:', response.status);

            if (!response.ok) {
                throw new Error(`Erro HTTP: ${response.status}`);
            }

            const data = await response.json();
            console.log('[Regionais] Dados recebidos:', data.length, 'regionais');

            state.regionais = data;
            state.filteredRegionais = data;
            renderRegionais();
        } catch (error) {
            console.error('[Regionais] Erro ao carregar:', error);
            showError('Erro ao carregar regionais. Verifique sua conexão.');

            // Mostrar mensagem na tabela
            const tbody = document.getElementById('regionaisTableBody');
            if (tbody) {
                tbody.innerHTML = `
                    <tr class="empty-row">
                        <td colspan="6" class="text-center">
                            <i class="bi bi-exclamation-circle"></i>
                            <p style="color: #ff6b6b;">Erro ao carregar regionais</p>
                        </td>
                    </tr>
                `;
            }
        }
    }

    async function loadRegionaisStats() {
        console.log('[Regionais] Carregando estatísticas...');

        try {
            const response = await fetch(`${API_REGIONAIS}/stats`, {
                headers: buildHeaders({ 'Accept': 'application/json' })
            });

            if (!response.ok) {
                throw new Error(`Erro HTTP: ${response.status}`);
            }

            const data = await response.json();
            console.log('[Regionais] Estatísticas:', data);

            const activeEl = document.getElementById('regionaisActiveCount');
            const inactiveEl = document.getElementById('regionaisInactiveCount');

            if (activeEl) activeEl.textContent = data.ativas || 0;
            if (inactiveEl) inactiveEl.textContent = data.inativas || 0;
        } catch (error) {
            console.error('[Regionais] Erro ao carregar estatísticas:', error);
        }
    }

    function renderRegionais() {
        console.log('[Regionais] Renderizando', state.filteredRegionais.length, 'regionais');

        const tbody = document.getElementById('regionaisTableBody');
        if (!tbody) {
            console.error('[Regionais] Elemento regionaisTableBody não encontrado');
            return;
        }

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
                        <button class="btn-icon btn-delete" onclick="window.confirmDeleteRegional(${regional.id})" title="Excluir">
                            <i class="bi bi-trash"></i>
                        </button>
                    </div>
                </td>
            </tr>
        `).join('');

        console.log('[Regionais] Renderização concluída');
    }

    window.applyRegionalFilters = function() {
        console.log('[Regionais] Aplicando filtros...');

        const statusFilter = document.getElementById('filterRegionalStatus')?.value || '';
        const searchTerm = document.getElementById('searchRegionalInput')?.value?.toLowerCase() || '';

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

        console.log('[Regionais] Filtros aplicados. Resultados:', state.filteredRegionais.length);
        renderRegionais();
    };

    window.openCreateRegionalModal = function() {
        console.log('[Regionais] Abrindo modal de criação');

        state.isEditingRegional = false;
        state.editingRegionalId = null;

        document.getElementById('regionalModalTitle').textContent = 'Adicionar Regional';
        document.getElementById('regionalForm').reset();
        document.getElementById('regionalId').value = '';

        document.getElementById('regionalModal').style.display = 'block';
    };

    window.editRegional = function(id) {
        console.log('[Regionais] Editando regional:', id);

        const regional = state.regionais.find(r => r.id === id);
        if (!regional) {
            console.error('[Regionais] Regional não encontrada:', id);
            return;
        }

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
        console.log('[Regionais] Fechando modal');

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
                await loadRegionais();
                await loadRegionaisStats();
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
                await loadRegionais();
                await loadRegionaisStats();
            } catch (error) {
                showError(error.message);
            }
        };

        document.getElementById('confirmRegionalModal').style.display = 'block';
    };

    window.confirmDeleteRegional = function(id) {
        const regional = state.regionais.find(r => r.id === id);
        if (!regional) return;

        document.getElementById('confirmRegionalTitle').textContent = 'Excluir Regional';
        document.getElementById('confirmRegionalMessage').textContent =
            `Deseja realmente excluir a regional "${regional.name}"? Esta ação não poderá ser desfeita.`;

        const confirmBtn = document.getElementById('confirmRegionalButton');
        confirmBtn.onclick = async () => {
            try {
                await deleteRegional(id);
                showSuccess('Regional excluída com sucesso!');
                closeConfirmRegionalModal();
                await loadRegionais();
                await loadRegionaisStats();
            } catch (error) {
                showError(error.message);
            }
        };

        document.getElementById('confirmRegionalModal').style.display = 'block';
    };

    async function handleRegionalFormSubmit(e) {
        e.preventDefault();
        console.log('[Regionais] Submetendo formulário...');

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
            await loadRegionais();
            await loadRegionaisStats();
        } catch (error) {
            console.error('[Regionais] Erro ao salvar:', error);
            showError(error.message);
        }
    }

    async function createRegional(regionalData) {
        console.log('[Regionais] Criando regional...');

        const response = await fetch(API_REGIONAIS, {
            method: 'POST',
            headers: buildHeaders({ 'Content-Type': 'application/json' }),
            body: JSON.stringify(regionalData)
        });

        if (!response.ok) {
            const errorData = await response.json().catch(() => ({}));
            throw new Error(errorData.error || 'Erro ao criar regional');
        }

        return await response.json();
    }

    async function updateRegional(id, regionalData) {
        console.log('[Regionais] Atualizando regional:', id);

        const response = await fetch(`${API_REGIONAIS}/${id}`, {
            method: 'PUT',
            headers: buildHeaders({ 'Content-Type': 'application/json' }),
            body: JSON.stringify(regionalData)
        });

        if (!response.ok) {
            const errorData = await response.json().catch(() => ({}));
            throw new Error(errorData.error || 'Erro ao atualizar regional');
        }

        return await response.json();
    }

    async function activateRegional(id) {
        console.log('[Regionais] Ativando regional:', id);

        const response = await fetch(`${API_REGIONAIS}/${id}/ativar`, {
            method: 'PUT',
            headers: buildHeaders({ 'Content-Type': 'application/json' })
        });

        if (!response.ok) {
            const errorData = await response.json().catch(() => ({}));
            throw new Error(errorData.error || 'Erro ao ativar regional');
        }

        return await response.json();
    }

    async function deactivateRegional(id) {
        console.log('[Regionais] Desativando regional:', id);

        const response = await fetch(`${API_REGIONAIS}/${id}/desativar`, {
            method: 'PUT',
            headers: buildHeaders({ 'Content-Type': 'application/json' })
        });

        if (!response.ok) {
            const errorData = await response.json().catch(() => ({}));
            throw new Error(errorData.error || 'Erro ao desativar regional');
        }

        return await response.json();
    }

    async function deleteRegional(id) {
        console.log('[Regionais] Excluindo regional:', id);

        const response = await fetch(`${API_REGIONAIS}/${id}`, {
            method: 'DELETE',
            headers: buildHeaders({ 'Content-Type': 'application/json' })
        });

        if (!response.ok) {
            let errorMessage = 'Erro ao excluir regional';
            try {
                const errorData = await response.json();
                errorMessage = errorData.error || errorMessage;
            } catch (e) {
                // ignore json parsing errors
            }
            throw new Error(errorMessage);
        }
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
        return text.toString().replace(/[&<>"']/g, m => map[m]);
    }

    function formatDate(dateString) {
        if (!dateString) return '-';
        try {
            const date = new Date(dateString);
            return date.toLocaleDateString('pt-BR', {
                day: '2-digit',
                month: '2-digit',
                year: 'numeric'
            });
        } catch (e) {
            console.error('[Utils] Erro ao formatar data:', e);
            return '-';
        }
    }

    function showSuccess(message) {
        console.log('[Success]', message);
        alert('✓ ' + message);
    }

    function showError(message) {
        console.error('[Error]', message);
        alert('✗ Erro: ' + message);
    }

    // ========== EXECUÇÃO INICIAL ==========
    if (document.readyState === 'loading') {
        console.log('[Minha Empresa] Aguardando DOMContentLoaded...');
        document.addEventListener('DOMContentLoaded', init);
    } else {
        console.log('[Minha Empresa] DOM já carregado, iniciando imediatamente');
        init();
    }

    console.log('[Minha Empresa] Módulo configurado');
})();
