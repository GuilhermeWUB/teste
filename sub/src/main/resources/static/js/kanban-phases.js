/**
 * Kanban Phases JavaScript
 * Funcionalidades específicas para gerenciamento das 5 fases do Kanban
 * com suporte a collapse/expand e contadores por fase
 */

(function () {
    'use strict';

    // Estado das fases (todas expandidas por padrão)
    const phaseState = {
        1: true, // Comunicação
        2: true, // Análise
        3: true, // Negociação
        4: true, // Execução
        5: true  // Garantia
    };

    /**
     * Inicializa as funcionalidades das fases
     */
    function initPhases() {
        // Restaurar estado das fases do localStorage
        loadPhaseState();

        // Aplicar estado inicial
        applyPhaseStates();

        // Atualizar contadores das fases
        updatePhaseCounts();

        // Observar mudanças nos cards para atualizar contadores
        observeCardChanges();
    }

    /**
     * Toggle da visibilidade de uma fase
     */
    window.kanbanBoard = window.kanbanBoard || {};
    window.kanbanBoard.togglePhase = function(phaseNumber) {
        const phase = document.querySelector(`.kanban-phase[data-phase="${phaseNumber}"]`);
        if (!phase) return;

        // Inverter estado
        phaseState[phaseNumber] = !phaseState[phaseNumber];

        // Aplicar classe collapsed
        if (phaseState[phaseNumber]) {
            phase.classList.remove('collapsed');
        } else {
            phase.classList.add('collapsed');
        }

        // Salvar estado no localStorage
        savePhaseState();
    };

    /**
     * Atualiza os contadores de eventos de cada fase
     */
    function updatePhaseCounts() {
        // Para cada fase (1 a 5)
        for (let phaseNum = 1; phaseNum <= 5; phaseNum++) {
            const phase = document.querySelector(`.kanban-phase[data-phase="${phaseNum}"]`);
            if (!phase) continue;

            // Contar todos os cards nas colunas desta fase
            const columns = phase.querySelectorAll('.kanban-column');
            let totalCards = 0;

            columns.forEach(column => {
                const cards = column.querySelectorAll('.task-card');
                const columnCount = column.querySelector('.column-count');

                // Atualizar contador da coluna
                if (columnCount) {
                    columnCount.textContent = cards.length;
                }

                totalCards += cards.length;
            });

            // Atualizar contador da fase
            const phaseCount = phase.querySelector('.phase-count');
            if (phaseCount) {
                phaseCount.textContent = totalCards;
            }
        }
    }

    /**
     * Aplica o estado de collapse/expand de todas as fases
     */
    function applyPhaseStates() {
        Object.keys(phaseState).forEach(phaseNum => {
            const phase = document.querySelector(`.kanban-phase[data-phase="${phaseNum}"]`);
            if (!phase) return;

            if (phaseState[phaseNum]) {
                phase.classList.remove('collapsed');
            } else {
                phase.classList.add('collapsed');
            }
        });
    }

    /**
     * Salva o estado das fases no localStorage
     */
    function savePhaseState() {
        try {
            localStorage.setItem('kanban-phase-state', JSON.stringify(phaseState));
        } catch (e) {
            console.warn('[KANBAN PHASES] Não foi possível salvar estado:', e);
        }
    }

    /**
     * Carrega o estado das fases do localStorage
     */
    function loadPhaseState() {
        try {
            const saved = localStorage.getItem('kanban-phase-state');
            if (saved) {
                const parsed = JSON.parse(saved);
                Object.assign(phaseState, parsed);
            }
        } catch (e) {
            console.warn('[KANBAN PHASES] Não foi possível carregar estado:', e);
        }
    }

    /**
     * Observa mudanças nos cards para atualizar contadores automaticamente
     */
    function observeCardChanges() {
        const kanbanBoard = document.querySelector('.kanban-board');
        if (!kanbanBoard) return;

        // Criar observer para detectar mudanças no DOM
        const observer = new MutationObserver(mutations => {
            let needsUpdate = false;

            mutations.forEach(mutation => {
                // Verificar se houve adição/remoção de cards
                if (mutation.type === 'childList') {
                    const target = mutation.target;
                    if (target.classList && target.classList.contains('tasks-container')) {
                        needsUpdate = true;
                    }
                }
            });

            if (needsUpdate) {
                // Debounce: aguardar um pouco antes de atualizar
                clearTimeout(observer.updateTimer);
                observer.updateTimer = setTimeout(() => {
                    updatePhaseCounts();
                }, 100);
            }
        });

        // Observar todas as mudanças dentro do kanban board
        observer.observe(kanbanBoard, {
            childList: true,
            subtree: true
        });
    }

    /**
     * Expande todas as fases
     */
    window.kanbanBoard.expandAllPhases = function() {
        for (let phaseNum = 1; phaseNum <= 5; phaseNum++) {
            phaseState[phaseNum] = true;
            const phase = document.querySelector(`.kanban-phase[data-phase="${phaseNum}"]`);
            if (phase) {
                phase.classList.remove('collapsed');
            }
        }
        savePhaseState();
    };

    /**
     * Colapsa todas as fases
     */
    window.kanbanBoard.collapseAllPhases = function() {
        for (let phaseNum = 1; phaseNum <= 5; phaseNum++) {
            phaseState[phaseNum] = false;
            const phase = document.querySelector(`.kanban-phase[data-phase="${phaseNum}"]`);
            if (phase) {
                phase.classList.add('collapsed');
            }
        }
        savePhaseState();
    };

    /**
     * Atualiza os novos status no objeto global do kanban
     */
    function updateStatusMappings() {
        if (window.kanbanBoard) {
            // Atualizar mapeamento de status labels
            window.kanbanBoard.statusLabels = {
                // Fase 1 - Comunicação
                COMUNICADO: '1.0 Comunicado',
                ABERTO: '1.1 Aberto',

                // Fase 2 - Análise
                VISTORIA: '2.0 Vistoria',
                ANALISE: '2.1 Análise',
                SINDICANCIA: '2.2 Sindicância',
                DESISTENCIA: '2.8 Desistência',

                // Fase 3 - Negociação
                ORCAMENTO: '3.0 Orçamento',
                COTA_PARTICIPACAO: '3.1 Cota de Participação',
                ACORDO_ANDAMENTO: '3.2 Acordo em Andamento',

                // Fase 4 - Execução
                COMPRA: '4.0 Compra',
                AGENDADO: '4.1 Agendado',
                REPAROS_LIBERADOS: '4.2 Reparos Liberados',
                COMPLEMENTOS: '4.3 Complementos',
                ENTREGUES: '4.7 Entregues',
                PESQUISA_SATISFACAO: '4.8 Pesquisa de Satisfação',

                // Fase 5 - Garantia
                ABERTURA_GARANTIA: '5.0 Abertura de Garantia',
                VISTORIA_GARANTIA: '5.1 Vistoria de Garantia',
                GARANTIA_AUTORIZADA: '5.2 Garantia Autorizada',
                GARANTIA_ENTREGUE: '5.7 Garantia Entregue'
            };

            // Atualizar lista de status válidos
            window.kanbanBoard.validStatuses = Object.keys(window.kanbanBoard.statusLabels);
        }
    }

    /**
     * Inicialização quando o DOM estiver pronto
     */
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', function() {
            initPhases();
            updateStatusMappings();
        });
    } else {
        initPhases();
        updateStatusMappings();
    }

    // Atualizar contadores quando o kanban principal terminar de carregar
    const originalRender = window.kanbanBoard?.render;
    if (originalRender) {
        window.kanbanBoard.render = function(...args) {
            const result = originalRender.apply(this, args);
            updatePhaseCounts();
            return result;
        };
    }

    // Expor função de atualização de contadores para uso externo
    window.kanbanBoard = window.kanbanBoard || {};
    window.kanbanBoard.updatePhaseCounts = updatePhaseCounts;

})();
