/**
 * layout.js
 * JavaScript global para o layout da aplicação
 */

(function() {
    'use strict';

    // Aguardar o carregamento do DOM
    document.addEventListener('DOMContentLoaded', function() {
        initLayout();
    });

    /**
     * Inicializa o módulo de layout
     */
    function initLayout() {
        console.log('Módulo de layout inicializado');

        // Inicializar funcionalidades globais
        initTooltips();
        initAlerts();
        initScrollBehavior();
        initNavigationHighlight();
        // initBackToTop(); // Desativado para remover o botão "Voltar ao Topo" duplicado
        initFormAnimations();
    }

    /**
     * Inicializa tooltips do Bootstrap (se houver)
     */
    function initTooltips() {
        const tooltipTriggerList = document.querySelectorAll('[data-bs-toggle="tooltip"]');
        if (tooltipTriggerList.length > 0 && typeof bootstrap !== 'undefined') {
            [...tooltipTriggerList].map(tooltipTriggerEl =>
                new bootstrap.Tooltip(tooltipTriggerEl)
            );
        }
    }

    /**
     * Auto-fechar alertas após alguns segundos
     */
    function initAlerts() {
        const alerts = document.querySelectorAll('.alert:not(.alert-permanent)');

        alerts.forEach(alert => {
            // Alertas de sucesso desaparecem automaticamente
            if (alert.classList.contains('alert-success')) {
                setTimeout(() => {
                    fadeOutAlert(alert);
                }, 5000);
            }

            // Adicionar botão de fechar se não existir
            if (!alert.querySelector('.btn-close')) {
                const closeBtn = document.createElement('button');
                closeBtn.type = 'button';
                closeBtn.className = 'btn-close';
                closeBtn.setAttribute('data-bs-dismiss', 'alert');
                closeBtn.setAttribute('aria-label', 'Fechar');
                alert.appendChild(closeBtn);
            }
        });
    }

    /**
     * Faz fade out de um alerta
     */
    function fadeOutAlert(alert) {
        alert.style.transition = 'opacity 0.3s ease';
        alert.style.opacity = '0';

        setTimeout(() => {
            alert.remove();
        }, 300);
    }

    /**
     * Melhora o comportamento de scroll
     */
    function initScrollBehavior() {
        // Smooth scroll para links âncora
        document.querySelectorAll('a[href^="#"]').forEach(anchor => {
            anchor.addEventListener('click', function(e) {
                const href = this.getAttribute('href');
                if (href === '#') return;

                const target = document.querySelector(href);
                if (target) {
                    e.preventDefault();
                    target.scrollIntoView({
                        behavior: 'smooth',
                        block: 'start'
                    });
                }
            });
        });
    }

    /**
     * Destaca o item de navegação ativo
     */
    function initNavigationHighlight() {
        const currentPath = window.location.pathname;
        const navLinks = document.querySelectorAll('.nav-link, .navbar-nav .nav-link');

        navLinks.forEach(link => {
            const href = link.getAttribute('href');
            if (href && currentPath.includes(href) && href !== '/') {
                link.classList.add('active');

                // Se estiver dentro de um dropdown
                const dropdown = link.closest('.dropdown');
                if (dropdown) {
                    const dropdownToggle = dropdown.querySelector('.dropdown-toggle');
                    if (dropdownToggle) {
                        dropdownToggle.classList.add('active');
                    }
                }
            }
        });
    }

    /**
     * Adiciona botão "Voltar ao topo"
     */
    function initBackToTop() {
        // Criar botão se não existir
        let backToTopBtn = document.getElementById('back-to-top');

        if (!backToTopBtn) {
            backToTopBtn = document.createElement('button');
            backToTopBtn.id = 'back-to-top';
            backToTopBtn.className = 'btn btn-primary position-fixed bottom-0 end-0 m-4';
            backToTopBtn.style.cssText = `
                opacity: 0;
                visibility: hidden;
                transition: all 0.3s ease;
                z-index: 1000;
                border-radius: 50%;
                width: 50px;
                height: 50px;
                padding: 0;
                box-shadow: 0 4px 12px rgba(0,0,0,0.15);
            `;
            backToTopBtn.innerHTML = '↑';
            backToTopBtn.setAttribute('aria-label', 'Voltar ao topo');
            document.body.appendChild(backToTopBtn);
        }

        // Mostrar/ocultar baseado no scroll
        window.addEventListener('scroll', () => {
            if (window.scrollY > 300) {
                backToTopBtn.style.opacity = '1';
                backToTopBtn.style.visibility = 'visible';
            } else {
                backToTopBtn.style.opacity = '0';
                backToTopBtn.style.visibility = 'hidden';
            }
        });

        // Ação de click
        backToTopBtn.addEventListener('click', () => {
            window.scrollTo({
                top: 0,
                behavior: 'smooth'
            });
        });
    }

    /**
     * Adiciona animações aos formulários
     */
    function initFormAnimations() {
        // Animação de label flutuante
        const formControls = document.querySelectorAll('.form-control, .form-select');

        formControls.forEach(control => {
            // Adicionar efeito de ripple nos inputs
            control.addEventListener('focus', function() {
                this.parentElement?.classList.add('focused');
            });

            control.addEventListener('blur', function() {
                if (!this.value) {
                    this.parentElement?.classList.remove('focused');
                }
            });
        });
    }

    /**
     * Utilitários globais
     */
    window.LayoutUtils = {
        showNotification: showNotification,
        confirmAction: confirmAction,
        loadingButton: loadingButton
    };

    /**
     * Exibe notificação toast
     */
    function showNotification(message, type = 'info') {
        const alertDiv = document.createElement('div');
        alertDiv.className = `alert alert-${type} alert-dismissible fade show position-fixed top-0 start-50 translate-middle-x mt-3`;
        alertDiv.style.cssText = 'z-index: 9999; min-width: 300px;';
        alertDiv.innerHTML = `
            ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        `;

        document.body.appendChild(alertDiv);

        setTimeout(() => {
            fadeOutAlert(alertDiv);
        }, 5000);
    }

    /**
     * Diálogo de confirmação customizado
     */
    function confirmAction(message, callback) {
        if (confirm(message)) {
            callback();
        }
    }

    /**
     * Transforma botão em estado de loading
     */
    function loadingButton(button, loading = true) {
        if (loading) {
            button.disabled = true;
            button.dataset.originalText = button.innerHTML;
            button.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Carregando...';
        } else {
            button.disabled = false;
            button.innerHTML = button.dataset.originalText || button.innerHTML;
        }
    }

})();
