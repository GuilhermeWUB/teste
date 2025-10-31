/**
 * cadastro-veiculo.js
 * JavaScript para a página de cadastro de veículos
 */

(function() {
    'use strict';

    // Aguardar o carregamento do DOM
    document.addEventListener('DOMContentLoaded', function() {
        initCadastroVeiculo();
    });

    /**
     * Inicializa o módulo de cadastro de veículos
     */
    function initCadastroVeiculo() {
        console.log('Módulo de cadastro de veículos inicializado');

        // Inicializar funcionalidades
        initFormValidation();
        initPlacaMask();
        initFieldEnhancements();
        initFormSubmit();
    }

    /**
     * Inicializa validação customizada do formulário
     */
    function initFormValidation() {
        const form = document.querySelector('form[action*="veiculo"]');
        if (!form) return;

        form.addEventListener('submit', function(e) {
            if (!form.checkValidity()) {
                e.preventDefault();
                e.stopPropagation();
            }
            form.classList.add('was-validated');
        });
    }

    /**
     * Aplica máscara na placa do veículo (formato brasileiro)
     */
    function initPlacaMask() {
        const placaInput = document.querySelector('input[name="placa"]');
        if (!placaInput) return;

        placaInput.addEventListener('input', function(e) {
            let value = e.target.value.replace(/[^A-Za-z0-9]/g, '').toUpperCase();

            // Formato ABC-1234 ou ABC1D23 (Mercosul)
            if (value.length <= 7) {
                if (value.length > 3) {
                    value = value.slice(0, 3) + '-' + value.slice(3);
                }
                e.target.value = value;
            } else {
                e.target.value = value.slice(0, 7);
            }
        });

        // Validação adicional
        placaInput.addEventListener('blur', function(e) {
            const value = e.target.value.replace('-', '');
            const placaRegex = /^[A-Z]{3}[0-9]{4}$|^[A-Z]{3}[0-9][A-Z][0-9]{2}$/;

            if (value && !placaRegex.test(value)) {
                showFieldError(placaInput, 'Placa inválida. Use o formato ABC-1234 ou ABC1D23');
            } else {
                clearFieldError(placaInput);
            }
        });
    }

    /**
     * Melhorias nos campos do formulário
     */
    function initFieldEnhancements() {
        // Auto-foco no próximo campo ao pressionar Enter
        const inputs = document.querySelectorAll('input, select, textarea');
        inputs.forEach((input, index) => {
            input.addEventListener('keydown', function(e) {
                if (e.key === 'Enter' && input.type !== 'textarea' && input.type !== 'submit') {
                    e.preventDefault();
                    const nextInput = inputs[index + 1];
                    if (nextInput) {
                        nextInput.focus();
                    }
                }
            });
        });

        // Highlight de campos obrigatórios vazios ao perder foco
        const requiredFields = document.querySelectorAll('[required]');
        requiredFields.forEach(field => {
            field.addEventListener('blur', function() {
                if (!this.value.trim()) {
                    this.classList.add('is-invalid');
                } else {
                    this.classList.remove('is-invalid');
                    this.classList.add('is-valid');
                }
            });
        });
    }

    /**
     * Gerenciar submissão do formulário
     */
    function initFormSubmit() {
        const form = document.querySelector('form[action*="veiculo"]');
        if (!form) return;

        const submitBtn = form.querySelector('button[type="submit"]');
        if (!submitBtn) return;

        form.addEventListener('submit', function() {
            // Desabilitar botão durante submissão
            submitBtn.disabled = true;
            submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Salvando...';

            // Re-habilitar após 3 segundos (caso o form não redirecione)
            setTimeout(() => {
                submitBtn.disabled = false;
                submitBtn.innerHTML = 'Cadastrar Veículo';
            }, 3000);
        });
    }

    /**
     * Exibe erro customizado em um campo
     */
    function showFieldError(field, message) {
        field.classList.add('is-invalid');

        let feedback = field.nextElementSibling;
        if (!feedback || !feedback.classList.contains('invalid-feedback')) {
            feedback = document.createElement('div');
            feedback.className = 'invalid-feedback';
            field.parentNode.insertBefore(feedback, field.nextSibling);
        }
        feedback.textContent = message;
    }

    /**
     * Remove erro de um campo
     */
    function clearFieldError(field) {
        field.classList.remove('is-invalid');
        const feedback = field.nextElementSibling;
        if (feedback && feedback.classList.contains('invalid-feedback')) {
            feedback.textContent = '';
        }
    }

    // Exportar funções globalmente se necessário
    window.CadastroVeiculo = {
        showFieldError: showFieldError,
        clearFieldError: clearFieldError
    };

})();
