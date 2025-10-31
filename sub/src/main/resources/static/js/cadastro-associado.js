/**
 * cadastro-associado.js
 * JavaScript para a página de cadastro de associados
 */

(function() {
    'use strict';

    // Aguardar o carregamento do DOM
    document.addEventListener('DOMContentLoaded', function() {
        initCadastroAssociado();
    });

    /**
     * Inicializa o módulo de cadastro de associados
     */
    function initCadastroAssociado() {
        console.log('Módulo de cadastro de associados inicializado');

        // Inicializar funcionalidades
        initFormValidation();
        initCPFMask();
        initTelefoneMask();
        initCEPLookup();
        initFieldEnhancements();
        initFormSubmit();
    }

    /**
     * Inicializa validação customizada do formulário
     */
    function initFormValidation() {
        const form = document.querySelector('form[action*="associado"]');
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
     * Aplica máscara de CPF
     */
    function initCPFMask() {
        const cpfInput = document.querySelector('input[name="cpf"]');
        if (!cpfInput) return;

        cpfInput.addEventListener('input', function(e) {
            let value = e.target.value.replace(/\D/g, '');

            if (value.length <= 11) {
                value = value.replace(/(\d{3})(\d)/, '$1.$2');
                value = value.replace(/(\d{3})(\d)/, '$1.$2');
                value = value.replace(/(\d{3})(\d{1,2})$/, '$1-$2');
                e.target.value = value;
            }
        });

        // Validação de CPF
        cpfInput.addEventListener('blur', function(e) {
            const cpf = e.target.value.replace(/\D/g, '');
            if (cpf && !isValidCPF(cpf)) {
                showFieldError(cpfInput, 'CPF inválido');
            } else {
                clearFieldError(cpfInput);
            }
        });
    }

    /**
     * Aplica máscara de telefone
     */
    function initTelefoneMask() {
        const telefoneInputs = document.querySelectorAll('input[name*="telefone"], input[name*="celular"]');

        telefoneInputs.forEach(input => {
            input.addEventListener('input', function(e) {
                let value = e.target.value.replace(/\D/g, '');

                if (value.length <= 11) {
                    if (value.length <= 10) {
                        // (11) 1234-5678
                        value = value.replace(/(\d{2})(\d)/, '($1) $2');
                        value = value.replace(/(\d{4})(\d)/, '$1-$2');
                    } else {
                        // (11) 91234-5678
                        value = value.replace(/(\d{2})(\d)/, '($1) $2');
                        value = value.replace(/(\d{5})(\d)/, '$1-$2');
                    }
                    e.target.value = value;
                }
            });
        });
    }

    /**
     * Busca CEP e preenche endereço automaticamente
     */
    function initCEPLookup() {
        const cepInput = document.querySelector('input[name="cep"]');
        if (!cepInput) return;

        cepInput.addEventListener('blur', function(e) {
            const cep = e.target.value.replace(/\D/g, '');

            if (cep.length === 8) {
                buscarCEP(cep);
            }
        });

        // Máscara de CEP
        cepInput.addEventListener('input', function(e) {
            let value = e.target.value.replace(/\D/g, '');
            if (value.length <= 8) {
                value = value.replace(/(\d{5})(\d)/, '$1-$2');
                e.target.value = value;
            }
        });
    }

    /**
     * Busca informações do CEP via API ViaCEP
     */
    function buscarCEP(cep) {
        const url = `https://viacep.com.br/ws/${cep}/json/`;

        fetch(url)
            .then(response => response.json())
            .then(data => {
                if (!data.erro) {
                    preencherEndereco(data);
                } else {
                    console.warn('CEP não encontrado');
                }
            })
            .catch(error => {
                console.error('Erro ao buscar CEP:', error);
            });
    }

    /**
     * Preenche campos de endereço com dados do CEP
     */
    function preencherEndereco(dados) {
        const campos = {
            'logradouro': dados.logradouro,
            'bairro': dados.bairro,
            'cidade': dados.localidade,
            'estado': dados.uf
        };

        Object.keys(campos).forEach(campo => {
            const input = document.querySelector(`input[name="${campo}"], select[name="${campo}"]`);
            if (input && !input.value) {
                input.value = campos[campo];
                input.dispatchEvent(new Event('change'));
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
        const form = document.querySelector('form[action*="associado"]');
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
                submitBtn.innerHTML = 'Cadastrar Associado';
            }, 3000);
        });
    }

    /**
     * Valida CPF
     */
    function isValidCPF(cpf) {
        if (cpf.length !== 11 || /^(\d)\1+$/.test(cpf)) {
            return false;
        }

        let soma = 0;
        let resto;

        for (let i = 1; i <= 9; i++) {
            soma += parseInt(cpf.substring(i - 1, i)) * (11 - i);
        }

        resto = (soma * 10) % 11;
        if (resto === 10 || resto === 11) resto = 0;
        if (resto !== parseInt(cpf.substring(9, 10))) return false;

        soma = 0;
        for (let i = 1; i <= 10; i++) {
            soma += parseInt(cpf.substring(i - 1, i)) * (12 - i);
        }

        resto = (soma * 10) % 11;
        if (resto === 10 || resto === 11) resto = 0;
        if (resto !== parseInt(cpf.substring(10, 11))) return false;

        return true;
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
    window.CadastroAssociado = {
        showFieldError: showFieldError,
        clearFieldError: clearFieldError,
        buscarCEP: buscarCEP
    };

})();
