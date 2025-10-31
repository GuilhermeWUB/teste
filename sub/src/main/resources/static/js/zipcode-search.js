document.addEventListener('DOMContentLoaded', function () {
    const zipcodeField = document.getElementById('zipcode');

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
                        document.getElementById('address').value = data.street || '';
                        document.getElementById('neighborhood').value = data.neighborhood || '';
                        document.getElementById('city').value = data.city || '';
                        document.getElementById('states').value = data.state || '';
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
        document.getElementById('address').value = '';
        document.getElementById('neighborhood').value = '';
        document.getElementById('city').value = '';
        document.getElementById('states').value = '';
    }
});
