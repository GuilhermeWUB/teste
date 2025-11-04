document.addEventListener('DOMContentLoaded', () => {
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
  
    const headers = {
      'Content-Type': 'application/json',
    };
    if (csrfToken && csrfHeader) {
      headers[csrfHeader] = csrfToken;
    }
  
    const columns = document.querySelectorAll('.kanban-column');
    let draggedCard = null;
  
    function fetchEventos() {
      const statuses = ['A_FAZER','EM_ANDAMENTO','AGUARDANDO','CONCLUIDO'];
      statuses.forEach(status => {
        const container = document.getElementById(`column-${status}`);
        if (container) container.innerHTML = '';
      });
      Promise.all(
        statuses.map(st => fetch(`/events/api/by-status/${st}`).then(r => r.json()).then(list => ({st, list})))
      ).then(results => {
        results.forEach(({st, list}) => {
          const container = document.getElementById(`column-${st}`);
          if (!container) return;
          list.forEach(evt => {
            const card = createTaskCard({ id: evt.id, titulo: evt.titulo, status: st });
            container.appendChild(card);
          });
        });
      });
    }
  
    function createTaskCard(evt) {
      const card = document.createElement('div');
      card.className = 'task-card';
      card.draggable = true;
      card.textContent = evt.titulo;
      card.dataset.id = evt.id;
      card.dataset.status = evt.status;
  
      card.addEventListener('dragstart', () => {
        draggedCard = card;
        setTimeout(() => card.classList.add('dragging'), 0);
      });
      card.addEventListener('dragend', () => {
        card.classList.remove('dragging');
        draggedCard = null;
      });
      card.addEventListener('click', () => {
        alert(`Detalhes do evento: ${evt.titulo}`);
      });
      return card;
    }
  
    columns.forEach(column => {
      const status = column.dataset.status;
      const container = column.querySelector('.tasks-container');
      const btnAdd = column.querySelector('.btn-add');
  
      column.addEventListener('dragover', e => e.preventDefault());
      column.addEventListener('drop', e => {
        e.preventDefault();
        if (draggedCard) {
          container.appendChild(draggedCard);
          const id = draggedCard.dataset.id;
          updateStatus(id, status).then(() => {
            draggedCard.dataset.status = status;
          });
        }
      });
  
      btnAdd.addEventListener('click', () => {
        window.location.href = '/events/new';
      });
    });
  
    // criação via formulário da aplicação (/events/new)
  
    function updateStatus(id, newStatus) {
      return fetch(`/events/api/${id}/status`, {
        method: 'PUT',
        headers,
        body: JSON.stringify({ status: newStatus })
      }).then(res => res.json());
    }
  
    fetchEventos();
  });
  