(function () {
    'use strict';

    const contacts = [
        { id: 1, name: 'Jeniffer Gonçalves Bueno', phone: '(43) 99998-9999', email: 'jennifer.goncalves@example.com', negotiations: 1, owner: 'Jeniffer Gonçalves Bueno', duplicateEmail: true, duplicatePhone: false },
        { id: 2, name: 'Eduardo Fernando Camargo', phone: '(11) 95530-5533', email: 'eduardo.camargo@example.com', negotiations: 1, owner: 'Eduardo Fernando Camargo', duplicateEmail: true, duplicatePhone: false },
        { id: 3, name: 'LUIZ CARLOS SUHRE', phone: '(46) 00000-0000', email: 'luiz.suhre@example.com', negotiations: 1, owner: 'Jeniffer Gonçalves Bueno', duplicateEmail: false, duplicatePhone: true },
        { id: 4, name: 'Jorge', phone: '(11) 00000-0000', email: 'jorge@example.com', negotiations: 1, owner: 'Jeniffer Gonçalves Bueno', duplicateEmail: false, duplicatePhone: true },
        { id: 5, name: 'ANA ROSE JORDAO BARBOSA', phone: '(13) 98189-3452', email: 'ana.rose@example.com', negotiations: 1, owner: 'Jeniffer Gonçalves Bueno', duplicateEmail: false, duplicatePhone: false },
        { id: 6, name: 'CARLOS AUGUSTO OLIVEIRA FERNANDES', phone: '(28) 98811-1005', email: 'carlos.fernandes@example.com', negotiations: 1, owner: 'Jeniffer Gonçalves Bueno', duplicateEmail: false, duplicatePhone: false },
        { id: 7, name: 'JOSIELTON DA SILVA DE ARAUJO', phone: '(27) 99825-5459', email: 'josielton.araujo@example.com', negotiations: 1, owner: 'Jeniffer Gonçalves Bueno', duplicateEmail: false, duplicatePhone: false },
        { id: 8, name: 'ANA CRISTINA CRUZ', phone: '(11) 99971-4064', email: 'ana.cruz@example.com', negotiations: 1, owner: 'Jeniffer Gonçalves Bueno', duplicateEmail: false, duplicatePhone: true },
        { id: 9, name: 'CARLOS ALBERTO MARTINS', phone: '(41) 99932-4322', email: 'carlos.martins@example.com', negotiations: 2, owner: 'Jeniffer Gonçalves Bueno', duplicateEmail: false, duplicatePhone: false },
        { id: 10, name: 'Matheus Knapp', phone: '(11) 94040-2068', email: 'matheus.knapp@example.com', negotiations: 1, owner: 'Jeniffer Gonçalves Bueno', duplicateEmail: true, duplicatePhone: false },
        { id: 11, name: 'GILSON LUIS DE OLIVEIRA', phone: '(47) 98828-2653', email: 'gilson.oliveira@example.com', negotiations: 0, owner: 'Jeniffer Gonçalves Bueno', duplicateEmail: false, duplicatePhone: false },
        { id: 12, name: 'EDUARDO HEITEHELROITT TERRAS', phone: '(12) 98131-2566', email: 'eduardo.terras@example.com', negotiations: 0, owner: 'Jeniffer Gonçalves Bueno', duplicateEmail: false, duplicatePhone: false }
    ];

    const contactsBody = document.getElementById('contactsBody');
    const contactsCount = document.getElementById('contactsCount');
    const searchInput = document.getElementById('searchInput');
    const responsibleFilter = document.getElementById('responsibleFilter');
    const filterDuplicateEmail = document.getElementById('filterDuplicateEmail');
    const filterDuplicatePhone = document.getElementById('filterDuplicatePhone');
    const filterShowUnique = document.getElementById('filterShowUnique');
    const selectAll = document.getElementById('selectAll');
    const selectionInfo = document.getElementById('selectionInfo');
    const selectedCountLabel = document.getElementById('selectedCount');
    const resultLabel = document.getElementById('resultLabel');
    const statTotal = document.getElementById('statTotal');
    const statDuplicates = document.getElementById('statDuplicates');
    const statNegotiations = document.getElementById('statNegotiations');
    const statOwners = document.getElementById('statOwners');
    const emptyState = document.getElementById('emptyState');
    const pendingDuplicates = document.getElementById('pendingDuplicates');
    const duplicateTags = document.getElementById('duplicateTags');
    const formName = document.getElementById('formName');
    const formEmail = document.getElementById('formEmail');
    const formPhone = document.getElementById('formPhone');

    document.addEventListener('DOMContentLoaded', () => {
        fillResponsibleOptions();
        renderStats();
        renderContacts();
        bindEvents();
        updateDuplicatesSidebar();
    });

    function fillResponsibleOptions() {
        const owners = Array.from(new Set(contacts.map(c => c.owner))).sort();
        owners.forEach(owner => {
            const option = document.createElement('option');
            option.value = owner;
            option.textContent = owner;
            responsibleFilter.appendChild(option);
        });
    }

    function renderStats(filtered = contacts) {
        const duplicateCount = filtered.filter(c => c.duplicateEmail || c.duplicatePhone).length;
        const negotiations = filtered.reduce((total, c) => total + (c.negotiations > 0 ? 1 : 0), 0);
        const owners = new Set(filtered.map(c => c.owner));

        statTotal.textContent = filtered.length.toLocaleString('pt-BR');
        statDuplicates.textContent = duplicateCount.toLocaleString('pt-BR');
        statNegotiations.textContent = negotiations.toLocaleString('pt-BR');
        statOwners.textContent = owners.size.toString();
    }

    function renderContacts() {
        const filters = collectFilters();
        const filtered = applyFilters(filters);

        contactsBody.innerHTML = '';
        filtered.forEach(contact => {
            const row = document.createElement('tr');
            row.innerHTML = `
                <td class="checkbox-col">
                    <label class="checkbox">
                        <input type="checkbox" data-contact-id="${contact.id}">
                        <span></span>
                    </label>
                </td>
                <td>
                    <div class="contact-line">
                        <div class="avatar">${getInitials(contact.name)}</div>
                        <div class="meta">
                            <p class="contact-name">${contact.name}</p>
                            <p class="contact-sub">${contact.email}</p>
                        </div>
                    </div>
                </td>
                <td>${contact.phone}</td>
                <td>${contact.email}</td>
                <td><span class="negotiations-count">${contact.negotiations}</span></td>
                <td>
                    <div class="contact-sub">${contact.owner}</div>
                    ${renderStatus(contact)}
                </td>
            `;
            contactsBody.appendChild(row);
        });

        contactsCount.textContent = `${filtered.length} contato(s) listados`;
        resultLabel.textContent = `${filtered.length} resultado(s)`;
        emptyState.hidden = filtered.length > 0;
        renderStats(filtered);
        resetSelection();
    }

    function collectFilters() {
        return {
            term: searchInput.value.trim().toLowerCase(),
            owner: responsibleFilter.value,
            duplicateEmail: filterDuplicateEmail.checked,
            duplicatePhone: filterDuplicatePhone.checked,
            showUnique: filterShowUnique.checked,
            formName: formName.value.trim().toLowerCase(),
            formEmail: formEmail.value.trim().toLowerCase(),
            formPhone: formPhone.value.trim()
        };
    }

    function applyFilters(filters) {
        return contacts.filter(contact => {
            const matchesSearch = !filters.term ||
                contact.name.toLowerCase().includes(filters.term) ||
                contact.email.toLowerCase().includes(filters.term) ||
                contact.phone.toLowerCase().includes(filters.term);

            const matchesOwner = !filters.owner || contact.owner === filters.owner;
            const matchesForm = (!filters.formName || contact.name.toLowerCase().includes(filters.formName)) &&
                (!filters.formEmail || contact.email.toLowerCase().includes(filters.formEmail)) &&
                (!filters.formPhone || contact.phone.includes(filters.formPhone));

            const isDuplicate = (filters.duplicateEmail && contact.duplicateEmail) ||
                (filters.duplicatePhone && contact.duplicatePhone);

            const passesDuplicateFilter = (isDuplicate) || (filters.showUnique && !isDuplicate);

            return matchesSearch && matchesOwner && matchesForm && passesDuplicateFilter;
        });
    }

    function renderStatus(contact) {
        const badges = [];
        if (contact.duplicateEmail) {
            badges.push('<span class="status-pill duplicate"><i class="bi bi-exclamation-triangle"></i> E-mail duplicado</span>');
        }
        if (contact.duplicatePhone) {
            badges.push('<span class="status-pill duplicate"><i class="bi bi-telephone-x"></i> Telefone repetido</span>');
        }
        if (!contact.duplicateEmail && !contact.duplicatePhone) {
            badges.push('<span class="status-pill unique"><i class="bi bi-shield-check"></i> Único</span>');
        }
        return badges.join('');
    }

    function bindEvents() {
        [searchInput, responsibleFilter, filterDuplicateEmail, filterDuplicatePhone, filterShowUnique].forEach(el => {
            el.addEventListener('input', renderContacts);
            el.addEventListener('change', renderContacts);
        });

        document.getElementById('searchForm').addEventListener('submit', (event) => {
            event.preventDefault();
            renderContacts();
        });

        document.getElementById('refreshButton').addEventListener('click', () => {
            renderContacts();
        });

        document.getElementById('applyFilters').addEventListener('click', () => {
            renderContacts();
        });

        selectAll.addEventListener('change', toggleSelectAll);
        contactsBody.addEventListener('change', handleRowSelection);

        document.getElementById('processDuplicates').addEventListener('click', () => {
            alert('A rotina de processamento consolidará contatos com e-mails ou telefones duplicados.');
        });

        document.getElementById('explainDuplicates').addEventListener('click', () => {
            alert('Duplicados são identificados comparando e-mail e telefone. Você pode resolvê-los manualmente ou aceitar a sugestão automática.');
        });
    }

    function toggleSelectAll(event) {
        const isChecked = event.target.checked;
        contactsBody.querySelectorAll('input[type="checkbox"]').forEach(cb => cb.checked = isChecked);
        updateSelectionInfo();
    }

    function handleRowSelection() {
        const allRowsSelected = Array.from(contactsBody.querySelectorAll('input[type="checkbox"]')).every(cb => cb.checked);
        selectAll.checked = allRowsSelected;
        updateSelectionInfo();
    }

    function updateSelectionInfo() {
        const selected = contactsBody.querySelectorAll('input[type="checkbox"]:checked').length;
        selectedCountLabel.textContent = selected.toString();
        selectionInfo.hidden = selected === 0;
    }

    function resetSelection() {
        selectAll.checked = false;
        selectionInfo.hidden = true;
        selectedCountLabel.textContent = '0';
    }

    function getInitials(name) {
        return name.split(' ').filter(Boolean).slice(0, 2).map(n => n[0].toUpperCase()).join('');
    }

    function updateDuplicatesSidebar() {
        const duplicates = contacts.filter(c => c.duplicateEmail || c.duplicatePhone);
        pendingDuplicates.textContent = duplicates.length.toString();
        duplicateTags.innerHTML = '';

        const tags = new Map();
        contacts.forEach(contact => {
            if (contact.duplicateEmail) {
                tags.set(contact.email, 'E-mail duplicado');
            }
            if (contact.duplicatePhone) {
                tags.set(contact.phone, 'Telefone duplicado');
            }
        });

        Array.from(tags.keys()).slice(0, 6).forEach(tag => {
            const badge = document.createElement('span');
            badge.className = 'chip';
            badge.textContent = tag;
            duplicateTags.appendChild(badge);
        });
    }
})();
