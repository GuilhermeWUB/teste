// Automatically submits the vehicles listing form when the page size changes
// to update the results without requiring a manual search click.
document.addEventListener('DOMContentLoaded', () => {
    const pageSizeSelect = document.getElementById('pageSize');
    if (!pageSizeSelect) return;

    const form = pageSizeSelect.closest('form');
    if (!form) return;

    pageSizeSelect.addEventListener('change', () => {
        form.submit();
    });
});
