(function () {
    document.addEventListener('DOMContentLoaded', function () {
        const forms = document.querySelectorAll('form[data-demand-status-form]');
        if (!forms.length) {
            return;
        }

        const modalElement = document.getElementById('completionObservationModal');
        if (!modalElement || !window.bootstrap) {
            return;
        }

        const modalInstance = window.bootstrap.Modal.getOrCreateInstance(modalElement);
        const textarea = document.getElementById('completionObservationText');
        const errorEl = document.getElementById('completionObservationError');
        const confirmButton = document.getElementById('confirmCompletionObservation');
        let pendingForm = null;

        function resetModalState() {
            if (textarea) {
                textarea.value = '';
            }
            if (errorEl) {
                errorEl.textContent = '';
            }
        }

        forms.forEach(function (form) {
            form.addEventListener('submit', function (event) {
                const statusInput = form.querySelector('input[name="status"]');
                const observationInput = form.querySelector('input[name="completionObservation"]');
                if (!statusInput || !observationInput) {
                    return;
                }

                if (statusInput.value !== 'CONCLUIDA') {
                    return;
                }

                event.preventDefault();
                pendingForm = form;
                resetModalState();
                modalInstance.show();
            });
        });

        if (confirmButton) {
            confirmButton.addEventListener('click', function () {
                if (!pendingForm || !textarea) {
                    modalInstance.hide();
                    return;
                }

                const observation = textarea.value.trim();
                if (!observation.length) {
                    if (errorEl) {
                        errorEl.textContent = 'Descreva brevemente o que foi feito para concluir a demanda.';
                    }
                    textarea.focus();
                    return;
                }

                const hiddenInput = pendingForm.querySelector('input[name="completionObservation"]');
                if (hiddenInput) {
                    hiddenInput.value = observation;
                }

                modalInstance.hide();
                const formToSubmit = pendingForm;
                pendingForm = null;
                formToSubmit.submit();
            });
        }

        modalElement.addEventListener('hidden.bs.modal', function () {
            pendingForm = null;
            resetModalState();
        });
    });
})();
