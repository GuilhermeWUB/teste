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
        initBackToTop();
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
        let backToTopBtn = document.querySelector('.back-to-top') || document.getElementById('back-to-top');

        if (!backToTopBtn) {
            backToTopBtn = document.createElement('button');
            backToTopBtn.id = 'back-to-top';
            backToTopBtn.className = 'back-to-top';
            backToTopBtn.type = 'button';
            backToTopBtn.setAttribute('aria-hidden', 'true');
            backToTopBtn.setAttribute('tabindex', '-1');
            backToTopBtn.innerHTML = `
                <span class="back-to-top__icon" aria-hidden="true">⬆</span>
                <span class="back-to-top__text">Voltar ao topo</span>
            `;
            backToTopBtn.classList.remove('is-visible');
            document.body.appendChild(backToTopBtn);
        } else {
            backToTopBtn.id = 'back-to-top';
            backToTopBtn.classList.add('back-to-top');
            backToTopBtn.removeAttribute('style');
            backToTopBtn.type = 'button';
            backToTopBtn.setAttribute('aria-hidden', 'true');
            backToTopBtn.setAttribute('tabindex', '-1');
            backToTopBtn.classList.remove('is-visible');

            ['btn', 'btn-primary', 'position-fixed', 'bottom-0', 'end-0', 'm-4'].forEach(cls => {
                backToTopBtn.classList.remove(cls);
            });

            if (!backToTopBtn.querySelector('.back-to-top__icon')) {
                backToTopBtn.innerHTML = `
                    <span class="back-to-top__icon" aria-hidden="true">⬆</span>
                    <span class="back-to-top__text">Voltar ao topo</span>
                `;
            }

            if (!document.body.contains(backToTopBtn)) {
                document.body.appendChild(backToTopBtn);
            }
        const backToTopBtn = document.querySelector('.back-to-top');

        if (!backToTopBtn) {
            return;
        }

        const hideButton = () => {
            backToTopBtn.classList.remove('is-visible');
            backToTopBtn.setAttribute('aria-hidden', 'true');
            backToTopBtn.setAttribute('tabindex', '-1');
        };

        const showButton = () => {
            backToTopBtn.classList.add('is-visible');
            backToTopBtn.removeAttribute('aria-hidden');
            backToTopBtn.removeAttribute('tabindex');
        };

        const updateVisibility = () => {
            if (window.scrollY > 300) {
                showButton();
            } else {
                hideButton();
            }
        };

        window.addEventListener('scroll', updateVisibility, {passive: true});
        updateVisibility();

        backToTopBtn.addEventListener('click', event => {
            event.preventDefault();
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
