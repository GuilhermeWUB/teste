# Tela de Listagem de Contatos — Versão Moderna

Este guia descreve a estrutura visual de uma tela de contatos com foco em produtividade e UX. Inclui os principais componentes e um exemplo de implementação em React com Tailwind CSS (podendo ser adaptado para HTML/CSS puro).

## Estrutura geral
- **Layout base:** grid com **sidebar** fixa à esquerda e **main** com header rico + tabela em cartão com sombra suave.
- **Tipografia:** fonte sans-serif (ex.: `Inter`, `Roboto`), tamanhos em `rem` e pesos moderados para manter hierarquia.
- **Espaçamento e profundidade:** uso generoso de `padding`/`gap`, cantos arredondados (`12px`) e sombras leves (`shadow-lg/20`).

## Componentes e comportamento
1. **Sidebar de navegação**
   - Logo reduzida no topo, seguida de itens de menu com ícone + label.
   - Item ativo destacado com cor primária e leve fundo.

2. **Header rico**
   - **Breadcrumbs:** indicam o contexto (p. ex., CRM › Contatos).
   - **Título + resumo:** "Contatos" com contagem opcional.
   - **Barra de busca global:** ocupa toda a largura disponível, com ícone à esquerda e atalho de teclado (⌘/Ctrl + K) indicado no placeholder.
   - **Botão de filtros avançados:** ícone + label, estado de ativo quando filtros aplicados.
   - **CTA "Novo Contato":** cor primária, destaque visual e rótulo claro.

3. **Tabela melhorada**
   - Container tipo **card** com cabeçalho de ações (ex.: resumo de seleção, exportar, ordenar).
   - **Coluna Contato:** avatar circular (placeholder com iniciais) + nome em destaque e email/empresa em subtítulo.
   - **Status/Negociações:** badges coloridas ("Novo Lead" → verde, "Em Negociação" → amarelo, "Fechado" → cinza/azul) com cores semânticas e contraste suficiente.
   - **Responsável:** miniatura do responsável + nome.
   - **Ações:** menu de três pontos (kebab) que abre opções "Editar", "Enviar Email", "Arquivar".
   - Linhas com hover, seleção por checkbox e espaçamento alto para facilitar leitura.

4. **Feedback e acessibilidade**
   - Foco visível (`outline-offset: 2px`), estados `:hover`/`:active` consistentes.
   - Atalhos de teclado nos elementos-chave (busca, novo contato) e `aria-label` para botões icônicos.

## Exemplo em React + Tailwind
> Estrutura pode ser usada em HTML puro mantendo as classes utilitárias como referência visual.

```tsx
import { useState } from "react";

const statusMap = {
  novo: "bg-emerald-100 text-emerald-700",
  negociando: "bg-amber-100 text-amber-700",
  fechado: "bg-slate-200 text-slate-700",
} as const;

type StatusKey = keyof typeof statusMap;

type Contact = {
  id: number;
  name: string;
  company: string;
  email: string;
  status: StatusKey;
  owner: { name: string; avatar?: string };
};

const contacts: Contact[] = [
  {
    id: 1,
    name: "Ana Rodrigues",
    company: "DeltaTech",
    email: "ana@deltatech.com",
    status: "novo",
    owner: { name: "João Lima" },
  },
  {
    id: 2,
    name: "Carlos Nunes",
    company: "Bright Co.",
    email: "carlos@bright.co",
    status: "negociando",
    owner: { name: "Marina Luz" },
  },
];

function Avatar({ name, src }: { name: string; src?: string }) {
  if (src) {
    return <img src={src} alt={name} className="h-9 w-9 rounded-full object-cover" />;
  }
  const initials = name
    .split(" ")
    .map((n) => n[0])
    .join("")
    .slice(0, 2)
    .toUpperCase();
  return (
    <div className="flex h-9 w-9 items-center justify-center rounded-full bg-slate-200 text-xs font-semibold text-slate-700">
      {initials}
    </div>
  );
}

export default function ContactListPage() {
  const [query, setQuery] = useState("");
  const filtered = contacts.filter((c) =>
    c.name.toLowerCase().includes(query.toLowerCase()) ||
    c.email.toLowerCase().includes(query.toLowerCase())
  );

  return (
    <div className="min-h-screen bg-slate-50 text-slate-900">
      <div className="grid min-h-screen grid-cols-[260px_1fr]">
        {/* Sidebar */}
        <aside className="border-r border-slate-200 bg-white px-6 py-8 shadow-sm">
          <div className="mb-8 text-xl font-semibold">CRM</div>
          <nav className="space-y-2">
            {[
              { label: "Visão Geral", icon: "🏠" },
              { label: "Contatos", icon: "👥", active: true },
              { label: "Negociações", icon: "📊" },
              { label: "Relatórios", icon: "📑" },
            ].map((item) => (
              <button
                key={item.label}
                className={`flex w-full items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition ${
                  item.active
                    ? "bg-slate-100 text-slate-900"
                    : "text-slate-600 hover:bg-slate-50"
                }`}
                aria-current={item.active ? "page" : undefined}
              >
                <span>{item.icon}</span>
                {item.label}
              </button>
            ))}
          </nav>
        </aside>

        {/* Conteúdo principal */}
        <main className="px-10 py-8">
          {/* Header rico */}
          <header className="mb-6 flex flex-col gap-4">
            <div className="text-sm text-slate-500">CRM / Contatos</div>
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div>
                <h1 className="text-2xl font-semibold text-slate-900">Contatos</h1>
                <p className="text-sm text-slate-500">Lista atualizada de leads e clientes</p>
              </div>
              <div className="flex items-center gap-2">
                <button className="rounded-lg border border-slate-200 px-4 py-2 text-sm font-medium text-slate-700 shadow-sm transition hover:border-slate-300 hover:bg-white">
                  Filtros avançados
                </button>
                <button className="rounded-lg bg-indigo-600 px-4 py-2 text-sm font-semibold text-white shadow-lg shadow-indigo-500/30 transition hover:bg-indigo-700">
                  Novo Contato
                </button>
              </div>
            </div>
            <div className="flex items-center gap-3">
              <div className="relative w-full">
                <span className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-slate-400">🔍</span>
                <input
                  type="search"
                  value={query}
                  onChange={(e) => setQuery(e.target.value)}
                  placeholder="Buscar contatos (Ctrl/⌘ + K)"
                  className="w-full rounded-lg border border-slate-200 bg-white py-3 pl-10 pr-3 text-sm shadow-sm outline-none transition focus:border-indigo-400 focus:ring-2 focus:ring-indigo-100"
                />
              </div>
            </div>
          </header>

          {/* Card da tabela */}
          <section className="rounded-xl border border-slate-200 bg-white shadow-lg shadow-slate-900/5">
            <div className="flex items-center justify-between border-b border-slate-100 px-6 py-4 text-sm text-slate-600">
              <span>{filtered.length} contatos</span>
              <button className="text-indigo-600 hover:text-indigo-700">Exportar CSV</button>
            </div>
            <div className="overflow-x-auto">
              <table className="min-w-full text-left text-sm">
                <thead className="bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
                  <tr>
                    <th className="w-12 px-6 py-3">
                      <input type="checkbox" aria-label="Selecionar todos" className="h-4 w-4 rounded border-slate-300 text-indigo-600" />
                    </th>
                    <th className="px-6 py-3">Contato</th>
                    <th className="px-6 py-3">Status / Negociações</th>
                    <th className="px-6 py-3">Responsável</th>
                    <th className="px-6 py-3 text-right">Ações</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {filtered.map((contact) => (
                    <tr key={contact.id} className="hover:bg-slate-50">
                      <td className="px-6 py-4">
                        <input type="checkbox" aria-label={`Selecionar ${contact.name}`} className="h-4 w-4 rounded border-slate-300 text-indigo-600" />
                      </td>
                      <td className="px-6 py-4">
                        <div className="flex items-center gap-3">
                          <Avatar name={contact.name} />
                          <div>
                            <div className="text-sm font-semibold text-slate-900">{contact.name}</div>
                            <div className="text-xs text-slate-500">{contact.company} • {contact.email}</div>
                          </div>
                        </div>
                      </td>
                      <td className="px-6 py-4">
                        <span
                          className={`inline-flex rounded-full px-3 py-1 text-xs font-semibold ${statusMap[contact.status]}`}
                        >
                          {contact.status === "novo"
                            ? "Novo Lead"
                            : contact.status === "negociando"
                              ? "Em Negociação"
                              : "Fechado"}
                        </span>
                      </td>
                      <td className="px-6 py-4">
                        <div className="flex items-center gap-3">
                          <Avatar name={contact.owner.name} />
                          <span className="text-sm font-medium text-slate-700">{contact.owner.name}</span>
                        </div>
                      </td>
                      <td className="px-6 py-4 text-right">
                        <button
                          className="rounded-full p-2 text-slate-500 transition hover:bg-slate-100 hover:text-slate-700"
                          aria-label="Abrir menu"
                        >
                          ⋮
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>
        </main>
      </div>
    </div>
  );
}
```

## Ajustes rápidos de CSS (para uso sem Tailwind)
```css
:root {
  --primary: #4f46e5;
  --bg: #f8fafc;
  --surface: #ffffff;
  --text: #0f172a;
  --muted: #64748b;
  --radius: 12px;
  font-family: "Inter", "Roboto", system-ui, -apple-system, sans-serif;
}

body { background: var(--bg); color: var(--text); }
.card { background: var(--surface); border: 1px solid #e2e8f0; border-radius: var(--radius); box-shadow: 0 10px 30px rgba(15, 23, 42, 0.08); }
.badge { display: inline-flex; padding: 6px 10px; border-radius: 999px; font-weight: 600; font-size: 12px; }
.badge.novo { background: #d1fae5; color: #065f46; }
.badge.negociando { background: #fef9c3; color: #854d0e; }
.badge.fechado { background: #e2e8f0; color: #334155; }
.avatar { width: 36px; height: 36px; border-radius: 50%; background: #e2e8f0; display: grid; place-items: center; font-weight: 700; }
input:focus, button:focus { outline: 2px solid #c7d2fe; outline-offset: 2px; }
```

Use este blueprint para prototipar rapidamente a nova interface com ênfase em clareza, hierarquia visual e ações prioritárias evidentes.
