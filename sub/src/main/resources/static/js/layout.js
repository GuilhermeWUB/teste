/**
 * layout.js
 * Script responsável por habilitar comportamentos globais do layout.
 *
 * O arquivo foi reescrito para priorizar legibilidade, modularidade e
 * isolamento de escopo. Cada funcionalidade é exposta através de funções
 * internas e inicializada apenas quando o DOM estiver pronto.
 */

(function layoutModule() {
    'use strict';

    document.addEventListener('DOMContentLoaded', () => {
        initialiseLayout();
    });

    /**
     * Expor utilidades imediatamente para evitar acessos antes do DOM ready.
     */
    window.LayoutUtils = {
        showNotification,
        confirmAction,
        loadingButton
    };

    /**
     * Inicializa todos os componentes do layout.
     */
    function initialiseLayout() {
        initialiseTooltips();
        initialiseAlerts();
        initialiseScrollBehaviour();
        initialiseNavigationHighlight();
        initialiseBackToTopButton();
        initialiseFormAnimations();
    }

    /**
     * Cria tooltips do Bootstrap quando a dependência estiver disponível.
     */
    function initialiseTooltips() {
        const tooltipSelector = '[data-bs-toggle="tooltip"]';
        const tooltipElements = document.querySelectorAll(tooltipSelector);

        if (!tooltipElements.length || typeof bootstrap === 'undefined' || !bootstrap.Tooltip) {
            return;
        }

        tooltipElements.forEach(element => new bootstrap.Tooltip(element));
    }

    /**
     * Configura alertas: fecha automaticamente os de sucesso e garante
     * a existência do botão de fechar.
     */
    function initialiseAlerts() {
        const alerts = document.querySelectorAll('.alert:not(.alert-permanent)');

        alerts.forEach(alert => {
            ensureCloseButton(alert);

            if (alert.classList.contains('alert-success')) {
                window.setTimeout(() => fadeOutAlert(alert), 5000);
            }
        });
    }

    function ensureCloseButton(alert) {
        if (alert.querySelector('.btn-close')) {
            return;
        }

        const button = document.createElement('button');
        button.type = 'button';
        button.className = 'btn-close';
        button.setAttribute('data-bs-dismiss', 'alert');
        button.setAttribute('aria-label', 'Fechar');
        alert.appendChild(button);
    }

    function fadeOutAlert(alert) {
        alert.style.transition = 'opacity 0.3s ease';
        alert.style.opacity = '0';

        window.setTimeout(() => {
            alert.remove();
        }, 300);
    }

    /**
     * Implementa scroll suave para links âncora do documento.
     */
    function initialiseScrollBehaviour() {
        const anchors = document.querySelectorAll('a[href^="#"]');

        anchors.forEach(anchor => {
            anchor.addEventListener('click', event => {
                const href = anchor.getAttribute('href');

                if (!href || href === '#') {
                    return;
                }

                const target = document.querySelector(href);

                if (!target) {
                    return;
                }

                event.preventDefault();
                target.scrollIntoView({ behavior: 'smooth', block: 'start' });
            });
        });
    }

    /**
     * Sinaliza o item de navegação que corresponde à página atual.
     */
    function initialiseNavigationHighlight() {
        const currentPath = normalisePath(window.location.pathname);
        const navLinks = document.querySelectorAll('.nav-link, .navbar-nav .nav-link');

        navLinks.forEach(link => {
            const href = link.getAttribute('href');
            if (!href) {
                return;
            }

            const normalisedHref = normalisePath(href);
            if (!normalisedHref || normalisedHref === '/') {
                return;
            }

            if (currentPath.startsWith(normalisedHref)) {
                markAsActive(link);
            }
        });
    }

    function normalisePath(pathname) {
        if (!pathname) {
            return '';
        }

        try {
            const url = new URL(pathname, window.location.origin);
            return url.pathname.replace(/\/+$/, '') || '/';
        } catch (error) {
            return pathname;
        }
    }

    function markAsActive(link) {
        link.classList.add('active');
        const dropdown = link.closest('.dropdown');

        if (!dropdown) {
            return;
        }

        const toggle = dropdown.querySelector('.dropdown-toggle');
        if (toggle) {
            toggle.classList.add('active');
        }
    }

    /**
     * Cria e gerencia o botão "Voltar ao topo".
     */
    function initialiseBackToTopButton() {
        const button = getOrCreateBackToTopButton();

        if (!button) {
            return;
        }

        const updateVisibility = () => {
            if (window.scrollY > 300) {
                showBackToTop(button);
            } else {
                hideBackToTop(button);
            }
        };

        window.addEventListener('scroll', updateVisibility, { passive: true });
        updateVisibility();

        button.addEventListener('click', event => {
            event.preventDefault();
            window.scrollTo({ top: 0, behavior: 'smooth' });
        });
    }

    function getOrCreateBackToTopButton() {
        let button = document.querySelector('.back-to-top, #back-to-top');

        if (!button) {
            button = document.createElement('button');
            button.id = 'back-to-top';
            button.type = 'button';
            button.className = 'back-to-top';
            button.innerHTML = `
                <span class="back-to-top__icon" aria-hidden="true">⬆</span>
                <span class="back-to-top__text">Voltar ao topo</span>
            `;
            document.body.appendChild(button);
        } else {
            button.id = 'back-to-top';
            button.type = 'button';
            button.classList.add('back-to-top');
            button.removeAttribute('style');
            button.innerHTML = button.innerHTML.trim() || `
                <span class="back-to-top__icon" aria-hidden="true">⬆</span>
                <span class="back-to-top__text">Voltar ao topo</span>
            `;
        }

        button.classList.remove('is-visible');
        button.setAttribute('aria-hidden', 'true');
        button.setAttribute('tabindex', '-1');

        return button;
    }

    function showBackToTop(button) {
        button.classList.add('is-visible');
        button.removeAttribute('aria-hidden');
        button.removeAttribute('tabindex');
    }

    function hideBackToTop(button) {
        button.classList.remove('is-visible');
        button.setAttribute('aria-hidden', 'true');
        button.setAttribute('tabindex', '-1');
    }

    /**
     * Aplica animações simples aos elementos de formulário.
     */
    function initialiseFormAnimations() {
        const selectors = '.form-control, .form-select';
        const controls = document.querySelectorAll(selectors);

        controls.forEach(control => {
            control.addEventListener('focus', () => {
                control.parentElement?.classList.add('focused');
            });

            control.addEventListener('blur', () => {
                if (!control.value) {
                    control.parentElement?.classList.remove('focused');
                }
            });
        });
    }

    function showNotification(message, type = 'info') {
        const toast = document.createElement('div');
        toast.className = `alert alert-${type} alert-dismissible fade show position-fixed top-0 start-50 translate-middle-x mt-3`;
        toast.style.cssText = 'z-index: 9999; min-width: 300px;';
        toast.innerHTML = `
            ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Fechar"></button>
        `;

        document.body.appendChild(toast);
        window.setTimeout(() => fadeOutAlert(toast), 5000);
    }

    function confirmAction(message, callback) {
        if (window.confirm(message) && typeof callback === 'function') {
            callback();
        }
    }

    function loadingButton(button, loading = true) {
        if (!button) {
            return;
        }

        if (loading) {
            button.disabled = true;
            if (!button.dataset.originalText) {
                button.dataset.originalText = button.innerHTML;
            }
            button.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Carregando...';
        } else {
            button.disabled = false;
            if (button.dataset.originalText) {
                button.innerHTML = button.dataset.originalText;
                delete button.dataset.originalText;
            }
        }
    }
})();
