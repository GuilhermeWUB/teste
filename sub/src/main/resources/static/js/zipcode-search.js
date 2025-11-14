document.addEventListener('DOMContentLoaded', function () {
    const zipcodeField = document.getElementById('zipcode');

    if (!zipcodeField) {
        return; // Campo de CEP não existe na página
    }

    zipcodeField.addEventListener('blur', function () {
        const zipcode = this.value.replace(/\D/g, '');

        if (zipcode.length === 8) {
            fetch(`https://brasilapi.com.br/api/cep/v2/${zipcode}`)
                .then(response => {
                    if (!response.ok) {
                        throw new Error('CEP não encontrado');
                    }
                    return response.json();
                })
                .then(data => {
                    if (data) {
                        const addressField = document.getElementById('address');
                        const neighborhoodField = document.getElementById('neighborhood');
                        const cityField = document.getElementById('city');
                        const statesField = document.getElementById('states');

                        if (addressField) addressField.value = data.street || '';
                        if (neighborhoodField) neighborhoodField.value = data.neighborhood || '';
                        if (cityField) cityField.value = data.city || '';
                        if (statesField) statesField.value = data.state || '';
                        clearZipcodeError();
                    }
                })
                .catch(error => {
                    console.error('Erro ao buscar CEP:', error);
                    showZipcodeError('CEP não encontrado ou inválido.');
                    clearAddressFields();
                });
        } else {
            showZipcodeError('CEP deve conter 8 dígitos.');
            clearAddressFields();
        }
    });

    function showZipcodeError(message) {
        let errorDiv = document.getElementById('zipcode-error');
        if (!errorDiv) {
            errorDiv = document.createElement('div');
            errorDiv.id = 'zipcode-error';
            errorDiv.className = 'text-danger';
            zipcodeField.parentNode.insertBefore(errorDiv, zipcodeField.nextSibling);
        }
        errorDiv.textContent = message;
    }

    function clearZipcodeError() {
        const errorDiv = document.getElementById('zipcode-error');
        if (errorDiv) {
            errorDiv.textContent = '';
        }
    }

    function clearAddressFields() {
        const addressField = document.getElementById('address');
        const neighborhoodField = document.getElementById('neighborhood');
        const cityField = document.getElementById('city');
        const statesField = document.getElementById('states');

        if (addressField) addressField.value = '';
        if (neighborhoodField) neighborhoodField.value = '';
        if (cityField) cityField.value = '';
        if (statesField) statesField.value = '';
    }
});
