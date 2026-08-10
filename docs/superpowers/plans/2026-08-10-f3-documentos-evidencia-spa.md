# F3: Gestión de Documentos y Evidencias en SPA — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Exponer en la SPA React la generación de PDFs (etiqueta/lote/manifiesto), la descarga de evidencias y una vista de auditoría de emisiones, consumiendo los endpoints backend ya existentes sin tocar Java.

**Architecture:** Trabajo 100% frontend (`frontend-react`, Vite + React 19 + vitest). Tres frentes: (1) botón de descarga por evidencia en `EvidenciasGrid`; (2) botones PDF en `ShipmentDetailPage` (etiqueta, `window.open` inline) y en el card "Lote #N" de `ImportBatchPage` (etiquetas/manifiesto, descarga vía anchor); (3) página nueva `DocumentosPage` en `/dashboard/documentos` con filtro por tipo. Helpers en `services/api.js`: `getDocumentoUrl(tipo, ref)` y `descargarDocumento(url)`.

**Tech Stack:** React 19, react-router-dom 7, axios, vitest 3 + @testing-library/react + user-event (globals:true, jsdom).

## Global Constraints

- Cero cambios en Java (backend 317 tests intactos).
- TDD estricto: escribir el test, verlo fallar, implementar, verlo pasar, commit.
- Mocks de API devuelven `{ data: ... }` (forma real de axios).
- Fecha en URLs de documentos: `2026-08-10`.
- Ruta nueva protegida: `/dashboard/documentos`; enlace navbar etiquetado "Documentos".
- `descargarDocumento` crea un anchor en `document.body`, hace `click()` y lo elimina; `window.open` se usa solo para la etiqueta inline de `ShipmentDetailPage`.
- CSRF ignorado en `/api/**`; `/uploads/**` es `permitAll` (misma origin).
- Commits frecuentes en `main` (rama estable), sin push automático.

---

### Task 1: Helper de URLs y descarga de PDFs (`services/api.js` + test)

**Files:**
- Create: `frontend-react/src/services/api.test.js`
- Modify: `frontend-react/src/services/api.js`

**Interfaces:**
- Consumes: nada previo.
- Produces:
  - `getDocumentoUrl(tipo, referencia)` → string. `tipo` ∈ `'etiqueta' | 'etiquetas-lote' | 'manifiesto'`; `referencia` es `codigo` (string) para `etiqueta` o `batchId` (number) para `etiquetas-lote`/`manifiesto`.
  - `descargarDocumento(url)` → void (crea anchor, click, elimina).
  - `getAdminDocumentos(tipo)` → `Promise<AxiosResponse>` con `params: { tipo }` (tipo opcional).
  - `formatPesoBytes(bytes)` → string (KB con 1 decimal). Exportada para reutilizarla en `DocumentosPage`.

- [ ] **Step 1: Write the failing test**

`frontend-react/src/services/api.test.js`:

```jsx
import { getDocumentoUrl, descargarDocumento, formatPesoBytes } from './api'

describe('api helpers de documentos', () => {
  it('construye la URL de la etiqueta de un envío', () => {
    expect(getDocumentoUrl('etiqueta', 'MT-0001')).toBe('/admin/documentos/envios/MT-0001/etiqueta')
  })

  it('construye la URL de etiquetas de lote', () => {
    expect(getDocumentoUrl('etiquetas-lote', 10)).toBe('/admin/documentos/lotes/10/etiquetas')
  })

  it('construye la URL del manifiesto de lote', () => {
    expect(getDocumentoUrl('manifiesto', 10)).toBe('/admin/documentos/lotes/10/manifiesto')
  })

  it('descargarDocumento crea un anchor con href y download, hace click y lo elimina', () => {
    const appendSpy = vi.spyOn(document.body, 'appendChild')
    const removeSpy = vi.spyOn(document.body, 'removeChild')
    const clickSpy = vi.spyOn(HTMLAnchorElement.prototype, 'click')
    const url = '/admin/documentos/lotes/10/manifiesto'

    descargarDocumento(url)

    const anchor = appendSpy.mock.calls[0][0]
    expect(anchor.tagName).toBe('A')
    expect(anchor.href).toContain(url)
    expect(anchor.download).toBe('')
    expect(clickSpy).toHaveBeenCalledTimes(1)
    expect(removeSpy).toHaveBeenCalledTimes(1)
    expect(removeSpy.mock.calls[0][0]).toBe(anchor)
  })

  it('formatea bytes a KB con 1 decimal', () => {
    expect(formatPesoBytes(1536)).toBe('1.5 KB')
    expect(formatPesoBytes(0)).toBe('0.0 KB')
  })
})
```

- [ ] **Step 2: Run test to verify it fails**

Run (workdir `frontend-react`): `npx vitest run src/services/api.test.js`
Expected: FAIL — `getDocumentoUrl is not a function` (o import error).

- [ ] **Step 3: Write minimal implementation**

Append to `frontend-react/src/services/api.js` (after `checkSession`):

```js
export function getDocumentoUrl(tipo, referencia) {
  if (tipo === 'etiqueta') {
    return `/admin/documentos/envios/${referencia}/etiqueta`;
  }
  if (tipo === 'etiquetas-lote') {
    return `/admin/documentos/lotes/${referencia}/etiquetas`;
  }
  return `/admin/documentos/lotes/${referencia}/manifiesto`;
}

export function descargarDocumento(url) {
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = '';
  document.body.appendChild(anchor);
  anchor.click();
  document.body.removeChild(anchor);
}

export function getAdminDocumentos(tipo) {
  return api.get('/admin/documentos', { params: { tipo } });
}

export function formatPesoBytes(bytes) {
  if (!bytes || bytes <= 0) return '0.0 KB';
  return `${(bytes / 1024).toFixed(1)} KB`;
}
```

- [ ] **Step 4: Run test to verify it passes**

Run (workdir `frontend-react`): `npx vitest run src/services/api.test.js`
Expected: PASS (5 tests).

- [ ] **Step 5: Commit**

```bash
git add frontend-react/src/services/api.js frontend-react/src/services/api.test.js
git commit -m "feat(spa): helpers de URLs, descarga y formato de documentos PDF"
```

---

### Task 2: Botón de descarga en `EvidenciasGrid` + test

**Files:**
- Create: `frontend-react/src/components/EvidenciasGrid.test.jsx`
- Modify: `frontend-react/src/components/EvidenciasGrid.jsx`

**Interfaces:**
- Consumes: `descargarDocumento` de `../services/api` (Task 1).
- Produces: componente `EvidenciasGrid({ evidencias = [] })` con un botón de descarga ⬇ por tarjeta (clase `evidencia-download`). El modal de preview y el estado vacío se conservan.

- [ ] **Step 1: Write the failing test**

`frontend-react/src/components/EvidenciasGrid.test.jsx`:

```jsx
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import EvidenciasGrid from './EvidenciasGrid'

const mockDescargarDocumento = vi.fn()
vi.mock('../services/api', () => ({
  descargarDocumento: (...args) => mockDescargarDocumento(...args),
}))

const EVIDENCIAS = [
  { titulo: 'Guía de embarque', descripcion: 'Firmada por el capitán', tipo: 'DOCUMENTO', urlArchivo: '/uploads/evidencias/guia.pdf' },
  { titulo: 'Foto del envío', descripcion: 'Estado de la mercancía', tipo: 'FOTO', urlArchivo: '/uploads/evidencias/foto.jpg' },
]

describe('EvidenciasGrid', () => {
  beforeEach(() => vi.clearAllMocks())

  it('muestra el estado vacío sin evidencias', () => {
    render(<EvidenciasGrid evidencias={[]} />)
    expect(screen.getByText('No hay evidencias registradas para este envío')).toBeInTheDocument()
  })

  it('renderiza las tarjetas con título y descripción', () => {
    render(<EvidenciasGrid evidencias={EVIDENCIAS} />)
    expect(screen.getByText('Guía de embarque')).toBeInTheDocument()
    expect(screen.getByText('Foto del envío')).toBeInTheDocument()
  })

  it('descarga la evidencia al pulsar el botón de descarga', async () => {
    const user = userEvent.setup()
    render(<EvidenciasGrid evidencias={EVIDENCIAS} />)
    const botones = screen.getAllByRole('button', { name: /descargar/i })
    await user.click(botones[0])
    expect(mockDescargarDocumento).toHaveBeenCalledWith('/uploads/evidencias/guia.pdf')
  })

  it('abre el modal de preview al hacer clic en la imagen', async () => {
    const user = userEvent.setup()
    render(<EvidenciasGrid evidencias={EVIDENCIAS} />)
    await user.click(screen.getByAltText('Guía de embarque'))
    expect(screen.getByAltText('Guía de embarque')).toBeInTheDocument()
    expect(screen.getByText(/Firmada por el capitán/)).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: '✕' }))
    expect(screen.queryByText(/Firmada por el capitán/)).not.toBeInTheDocument()
  })
})
```

- [ ] **Step 2: Run test to verify it fails**

Run (workdir `frontend-react`): `npx vitest run src/components/EvidenciasGrid.test.jsx`
Expected: FAIL — los tests de descarga y de modal no encuentran el botón de descarga / hay violación de Rules of Hooks (el `useState` se llama tras un early return).

- [ ] **Step 3: Write minimal implementation**

Rewrite `frontend-react/src/components/EvidenciasGrid.jsx` (mueve `useState` arriba para cumplir Rules of Hooks y añade el botón):

```jsx
import { useState } from 'react';
import { descargarDocumento } from '../services/api';

export default function EvidenciasGrid({ evidencias = [] }) {
  const [preview, setPreview] = useState(null);

  if (!evidencias.length) {
    return (
      <div className="empty-state">
        <span className="empty-state-icon" style={{ fontSize: '2rem' }}>📷</span>
        <p className="empty-state-text">No hay evidencias registradas para este envío</p>
      </div>
    );
  }

  return (
    <div className="evidencias-section">
      <div className="evidencias-grid">
        {evidencias.map((ev, i) => (
          <div key={i} className="evidencia-card">
            <div className="evidencia-image-wrapper" onClick={() => setPreview(ev)}>
              <img
                src={ev.urlArchivo}
                alt={ev.titulo || 'Evidencia'}
                className="evidencia-image"
                loading="lazy"
              />
            </div>
            <div className="evidencia-info">
              <span className="evidencia-title">{ev.titulo || 'Sin título'}</span>
              {ev.descripcion && <span className="evidencia-desc">{ev.descripcion}</span>}
            </div>
            <div className="evidencia-actions">
              <button
                type="button"
                className="evidencia-download"
                aria-label={`Descargar ${ev.titulo || 'evidencia'}`}
                onClick={() => descargarDocumento(ev.urlArchivo)}
              >
                ⬇ Descargar
              </button>
            </div>
          </div>
        ))}
      </div>

      {preview && (
        <div className="evidencia-modal-overlay" onClick={() => setPreview(null)}>
          <div className="evidencia-modal" onClick={e => e.stopPropagation()}>
            <button className="evidencia-modal-close" onClick={() => setPreview(null)}>✕</button>
            <img src={preview.urlArchivo} alt={preview.titulo || 'Evidencia'} className="evidencia-modal-img" />
            <div className="evidencia-modal-info">
              <strong>{preview.titulo || 'Sin título'}</strong>
              {preview.descripcion && <p>{preview.descripcion}</p>}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
```

- [ ] **Step 4: Run test to verify it passes**

Run (workdir `frontend-react`): `npx vitest run src/components/EvidenciasGrid.test.jsx`
Expected: PASS (4 tests).

- [ ] **Step 5: Commit**

```bash
git add frontend-react/src/components/EvidenciasGrid.jsx frontend-react/src/components/EvidenciasGrid.test.jsx
git commit -m "feat(spa): descarga de evidencias en EvidenciasGrid"
```

---

### Task 3: Botón de etiqueta en `ShipmentDetailPage` + test

**Files:**
- Create: `frontend-react/src/pages/ShipmentDetailPage.test.jsx`
- Modify: `frontend-react/src/pages/ShipmentDetailPage.jsx`

**Interfaces:**
- Consumes: `getDocumentoUrl` de `../services/api` (Task 1); patrón existente de la página (mocks de `usePolling` y `NotificationContext` como en `ImportBatchPage.test.jsx`).
- Produces: botón "Etiqueta térmica PDF" (clase `btn-pdf`) en `.detail-topbar` que llama `window.open(getDocumentoUrl('etiqueta', codigo), '_blank')`.

- [ ] **Step 1: Write the failing test**

`frontend-react/src/pages/ShipmentDetailPage.test.jsx`:

```jsx
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import ShipmentDetailPage from './ShipmentDetailPage'

const mockGetAdminEnvioDetalle = vi.fn()
const mockGetDocumentoUrl = vi.fn()
const mockRefreshNow = vi.fn()
const mockShowSuccess = vi.fn()
const mockShowError = vi.fn()

vi.mock('react-router-dom', () => ({
  useParams: () => ({ codigo: 'MT-0001' }),
  useNavigate: () => vi.fn(),
}))

vi.mock('../services/api', () => ({
  getAdminEnvioDetalle: (...args) => mockGetAdminEnvioDetalle(...args),
  getDocumentoUrl: (...args) => mockGetDocumentoUrl(...args),
}))

vi.mock('../hooks/usePolling', () => ({
  default: () => ({ polling: false, lastUpdated: null, refreshNow: mockRefreshNow, refreshError: null }),
}))

vi.mock('../context/NotificationContext', () => ({
  useToast: () => ({ showSuccess: mockShowSuccess, showError: mockShowError }),
}))

vi.mock('../components/RefreshIndicator', () => ({ default: () => <div /> }))
vi.mock('../components/UpdateEstadoPanel', () => ({ default: () => <div /> }))
vi.mock('../components/Timeline', () => ({ default: () => <div /> }))
vi.mock('../components/EvidenciasGrid', () => ({ default: () => <div /> }))

const ENVIO = {
  codigoUnico: 'MT-0001',
  estado: 'EN_TRANSITO',
  origen: 'Asunción',
  destino: 'Madrid',
  destinatario: 'Ana López',
  peso: '2 kg',
  ultimaActualizacion: '2026-08-10T10:00:00',
  eventos: [],
  evidencias: [],
}

describe('ShipmentDetailPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockGetAdminEnvioDetalle.mockResolvedValue({ data: ENVIO })
    vi.stubGlobal('open', vi.fn())
  })

  it('abre la etiqueta térmica en pestaña nueva', async () => {
    const user = userEvent.setup()
    mockGetDocumentoUrl.mockReturnValue('/admin/documentos/envios/MT-0001/etiqueta')
    render(<ShipmentDetailPage />)
    const btn = await screen.findByRole('button', { name: /etiqueta térmica/i })
    await user.click(btn)
    expect(mockGetDocumentoUrl).toHaveBeenCalledWith('etiqueta', 'MT-0001')
    expect(window.open).toHaveBeenCalledWith('/admin/documentos/envios/MT-0001/etiqueta', '_blank')
  })
})
```

- [ ] **Step 2: Run test to verify it fails**

Run (workdir `frontend-react`): `npx vitest run src/pages/ShipmentDetailPage.test.jsx`
Expected: FAIL — no se encuentra el botón "Etiqueta térmica".

- [ ] **Step 3: Write minimal implementation**

In `frontend-react/src/pages/ShipmentDetailPage.jsx`:

1. Import `getDocumentoUrl`:
```jsx
import { getAdminEnvioDetalle, getDocumentoUrl } from '../services/api';
```

2. Replace the `.detail-topbar` block (lines ~142-145) with:

```jsx
      <div className="detail-topbar">
        <button className="btn-back" onClick={() => navigate('/')}>← Volver al dashboard</button>
        <div className="detail-topbar-actions">
          <button
            type="button"
            className="btn-pdf"
            onClick={() => window.open(getDocumentoUrl('etiqueta', codigo), '_blank')}
          >
            🏷 Etiqueta térmica PDF
          </button>
          <RefreshIndicator lastUpdated={lastUpdated} polling={polling} refreshError={refreshError} onRefresh={refreshNow} />
        </div>
      </div>
```

- [ ] **Step 4: Run test to verify it passes**

Run (workdir `frontend-react`): `npx vitest run src/pages/ShipmentDetailPage.test.jsx`
Expected: PASS (1 test).

- [ ] **Step 5: Commit**

```bash
git add frontend-react/src/pages/ShipmentDetailPage.jsx frontend-react/src/pages/ShipmentDetailPage.test.jsx
git commit -m "feat(spa): botón de etiqueta térmica en el detalle de envío"
```

---

### Task 4: Botones de etiquetas/manifiesto en `ImportBatchPage` + ampliar test

**Files:**
- Modify: `frontend-react/src/pages/ImportBatchPage.jsx`
- Modify: `frontend-react/src/pages/ImportBatchPage.test.jsx`

**Interfaces:**
- Consumes: `getDocumentoUrl` y `descargarDocumento` de `../services/api` (Task 1).
- Produces: en el card "Lote #N" activo, dos botones `btn-importar btn-importar--small`: "Etiquetas del lote" (→ `descargarDocumento(getDocumentoUrl('etiquetas-lote', loteActivo.id))`) y "Manifiesto" (→ `descargarDocumento(getDocumentoUrl('manifiesto', loteActivo.id))`).

- [ ] **Step 1: Write the failing test**

Append to `frontend-react/src/pages/ImportBatchPage.test.jsx`:

1. Add the mock functions next to the existing ones (after line 12):
```jsx
const mockGetDocumentoUrl = vi.fn()
const mockDescargarDocumento = vi.fn()
```

2. Add to the `vi.mock('../services/api', ...)` object (after line 19):
```jsx
  getDocumentoUrl: (...args) => mockGetDocumentoUrl(...args),
  descargarDocumento: (...args) => mockDescargarDocumento(...args),
```

3. Add a new test inside `describe('ImportBatchPage', ...)`:

```jsx
  it('descarga etiquetas y manifiesto del lote activo', async () => {
    const user = userEvent.setup()
    mockUploadImportCsv.mockResolvedValue({ data: { id: 99, estado: 'PROCESANDO' } })
    mockGetAdminImporte.mockResolvedValue({ data: {
      id: 99, estado: 'COMPLETADO', procesados: 10, exitosos: 10, fallidos: 0,
      totalRegistros: 10, nombreArchivo: 'envios.csv',
    } })

    render(<ImportBatchPage />)
    const file = new File(['codigo,estado\nMT-1,RECIBIDO'], 'envios.csv', { type: 'text/csv' })
    await user.upload(screen.getByLabelText('Fichero CSV'), file)
    await user.click(screen.getByRole('button', { name: /importar csv/i }))
    await screen.findByText('Lote #99')

    mockGetDocumentoUrl
      .mockReturnValueOnce('/admin/documentos/lotes/99/etiquetas')
      .mockReturnValueOnce('/admin/documentos/lotes/99/manifiesto')

    await user.click(screen.getByRole('button', { name: /etiquetas del lote/i }))
    expect(mockGetDocumentoUrl).toHaveBeenCalledWith('etiquetas-lote', 99)
    expect(mockDescargarDocumento).toHaveBeenCalledWith('/admin/documentos/lotes/99/etiquetas')

    await user.click(screen.getByRole('button', { name: /manifiesto/i }))
    expect(mockGetDocumentoUrl).toHaveBeenCalledWith('manifiesto', 99)
    expect(mockDescargarDocumento).toHaveBeenCalledWith('/admin/documentos/lotes/99/manifiesto')
  })
```

- [ ] **Step 2: Run test to verify it fails**

Run (workdir `frontend-react`): `npx vitest run src/pages/ImportBatchPage.test.jsx`
Expected: FAIL — no se encuentran los botones "Etiquetas del lote" / "Manifiesto".

- [ ] **Step 3: Write minimal implementation**

In `frontend-react/src/pages/ImportBatchPage.jsx`:

1. Import the helpers (lines 2-8):
```jsx
import {
  getAdminImports,
  getAdminClientes,
  getAdminImporte,
  getImportErrores,
  uploadImportCsv,
  getDocumentoUrl,
  descargarDocumento
} from '../services/api';
```

2. Replace the `{loteActivo && (` card block (after the `dashboard-subtitle` paragraph, before the closing `</section>`):

```jsx
          <div className="import-form-row">
            <button
              type="button"
              className="btn-importar btn-importar--small"
              onClick={() => descargarDocumento(getDocumentoUrl('etiquetas-lote', loteActivo.id))}
            >
              Etiquetas del lote
            </button>
            <button
              type="button"
              className="btn-importar btn-importar--small"
              onClick={() => descargarDocumento(getDocumentoUrl('manifiesto', loteActivo.id))}
            >
              Manifiesto
            </button>
          </div>
```

- [ ] **Step 4: Run test to verify it passes**

Run (workdir `frontend-react`): `npx vitest run src/pages/ImportBatchPage.test.jsx`
Expected: PASS (7 tests).

- [ ] **Step 5: Commit**

```bash
git add frontend-react/src/pages/ImportBatchPage.jsx frontend-react/src/pages/ImportBatchPage.test.jsx
git commit -m "feat(spa): descarga de etiquetas y manifiesto de lote en import batch"
```

---

### Task 5: Página de auditoría `DocumentosPage` + ruta + navbar + test

**Files:**
- Create: `frontend-react/src/pages/DocumentosPage.jsx`
- Create: `frontend-react/src/pages/DocumentosPage.test.jsx`
- Modify: `frontend-react/src/App.jsx`
- Modify: `frontend-react/src/layouts/MainLayout.jsx`

**Interfaces:**
- Consumes: `getAdminDocumentos`, `formatPesoBytes` de `../services/api` (Task 1); `parseLocalDateTime` de `../services/dateUtils`; `EmptyState` de `../components/EmptyState`.
- Produces: `DocumentosPage` — tabla de emisiones con filtro por tipo; ruta protegida `/dashboard/documentos`; enlace navbar "Documentos".

- [ ] **Step 1: Write the failing test**

`frontend-react/src/pages/DocumentosPage.test.jsx`:

```jsx
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import DocumentosPage from './DocumentosPage'

const mockGetAdminDocumentos = vi.fn()
const mockShowSuccess = vi.fn()
const mockShowError = vi.fn()

vi.mock('../services/api', () => ({
  getAdminDocumentos: (...args) => mockGetAdminDocumentos(...args),
  formatPesoBytes: (bytes) => `${(bytes / 1024).toFixed(1)} KB`,
}))

vi.mock('../context/NotificationContext', () => ({
  useToast: () => ({ showSuccess: mockShowSuccess, showError: mockShowError }),
}))

const EMISIONES = [
  {
    id: 1, tipo: 'ETIQUETA_TERMICA', referenciaId: 'MT-0001',
    nombreArchivo: 'etiqueta-MT-0001.pdf', pesoBytes: 20480,
    usuarioGeneracion: 'admin', fechaCreacion: '2026-08-10T09:00:00',
  },
  {
    id: 2, tipo: 'MANIFIESTO_CARGA', referenciaId: '10',
    nombreArchivo: 'manifiesto-lote-10.pdf', pesoBytes: 1536,
    usuarioGeneracion: 'admin', fechaCreacion: '2026-08-10T10:30:00',
  },
]

describe('DocumentosPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockGetAdminDocumentos.mockResolvedValue({ data: EMISIONES })
  })

  it('carga y muestra las emisiones en la tabla', async () => {
    render(<DocumentosPage />)
    expect(mockGetAdminDocumentos).toHaveBeenCalledWith(undefined)
    expect(await screen.findByText('etiqueta-MT-0001.pdf')).toBeInTheDocument()
    expect(screen.getByText('manifiesto-lote-10.pdf')).toBeInTheDocument()
    expect(screen.getByText('20.0 KB')).toBeInTheDocument()
    expect(screen.getByText('ETIQUETA_TERMICA')).toBeInTheDocument()
  })

  it('filtra por tipo al cambiar el select', async () => {
    const user = userEvent.setup()
    render(<DocumentosPage />)
    await screen.findByText('etiqueta-MT-0001.pdf')
    mockGetAdminDocumentos.mockClear()

    await user.selectOptions(screen.getByLabelText('Filtrar por tipo'), 'MANIFIESTO_CARGA')
    expect(mockGetAdminDocumentos).toHaveBeenCalledWith('MANIFIESTO_CARGA')
  })

  it('muestra el estado vacío sin emisiones', async () => {
    mockGetAdminDocumentos.mockResolvedValue({ data: [] })
    render(<DocumentosPage />)
    expect(await screen.findByText('No hay documentos generados todavía')).toBeInTheDocument()
  })

  it('muestra toast de error si falla la carga', async () => {
    mockGetAdminDocumentos.mockRejectedValue(new Error('Error de conexión'))
    render(<DocumentosPage />)
    await waitFor(() => expect(mockShowError).toHaveBeenCalled())
  })
})
```

Note: `waitFor` se importa de `@testing-library/react` — añadir a la primera línea del import: `import { render, screen, waitFor } from '@testing-library/react'`.

- [ ] **Step 2: Run test to verify it fails**

Run (workdir `frontend-react`): `npx vitest run src/pages/DocumentosPage.test.jsx`
Expected: FAIL — module not found `./DocumentosPage`.

- [ ] **Step 3: Write minimal implementation**

`frontend-react/src/pages/DocumentosPage.jsx`:

```jsx
import { useState, useEffect, useCallback } from 'react';
import { getAdminDocumentos, formatPesoBytes } from '../services/api';
import { parseLocalDateTime } from '../services/dateUtils';
import { useToast } from '../context/NotificationContext';
import EmptyState from '../components/EmptyState';

const TIPOS = [
  { value: '', label: 'Todos los tipos' },
  { value: 'ETIQUETA_TERMICA', label: 'Etiqueta térmica' },
  { value: 'ETIQUETAS_LOTE', label: 'Etiquetas de lote' },
  { value: 'MANIFIESTO_CARGA', label: 'Manifiesto de carga' },
];

const TIPO_BADGE = {
  ETIQUETA_TERMICA: 'lote-badge lote-badge--info',
  ETIQUETAS_LOTE: 'lote-badge lote-badge--warning',
  MANIFIESTO_CARGA: 'lote-badge lote-badge--success',
};

export default function DocumentosPage() {
  const { showError } = useToast();
  const [emisiones, setEmisiones] = useState([]);
  const [tipo, setTipo] = useState('');
  const [loading, setLoading] = useState(true);

  const cargar = useCallback(async (tipoFiltro) => {
    try {
      const res = await getAdminDocumentos(tipoFiltro || undefined);
      setEmisiones(res.data || []);
    } catch (err) {
      showError(err.message || 'Error al cargar las emisiones');
    } finally {
      setLoading(false);
    }
  }, [showError]);

  useEffect(() => {
    setLoading(true);
    cargar(tipo);
  }, [tipo, cargar]);

  return (
    <div className="dashboard">
      <header className="dashboard-header">
        <div>
          <h1>Auditoría de documentos</h1>
          <p className="dashboard-subtitle">Emisiones de PDFs generados por el equipo.</p>
        </div>
        <label className="import-label" htmlFor="tipoFiltro">Filtrar por tipo</label>
      </header>

      <div className="import-form-row">
        <select
          id="tipoFiltro"
          className="import-select"
          value={tipo}
          onChange={e => setTipo(e.target.value)}
          aria-label="Filtrar por tipo"
        >
          {TIPOS.map(t => (
            <option key={t.value} value={t.value}>{t.label}</option>
          ))}
        </select>
      </div>

      <section className="table-section">
        <div className="table-header">
          <h2>Emisiones</h2>
          <span className="table-count">{emisiones.length} documento{emisiones.length !== 1 ? 's' : ''}</span>
        </div>

        {loading ? (
          <EmptyState message="Cargando emisiones…" />
        ) : emisiones.length === 0 ? (
          <EmptyState message="No hay documentos generados todavía" />
        ) : (
          <table className="envios-table">
            <thead>
              <tr>
                <th>Tipo</th><th>Referencia</th><th>Archivo</th>
                <th>Peso</th><th>Usuario</th><th>Fecha</th>
              </tr>
            </thead>
            <tbody>
              {emisiones.map(e => (
                <tr key={e.id}>
                  <td><span className={TIPO_BADGE[e.tipo] || 'lote-badge'}>{e.tipo}</span></td>
                  <td className="cell-code">{e.referenciaId}</td>
                  <td>{e.nombreArchivo}</td>
                  <td className="cell-date">{formatPesoBytes(e.pesoBytes)}</td>
                  <td>{e.usuarioGeneracion}</td>
                  <td className="cell-date">
                    {e.fechaCreacion
                      ? parseLocalDateTime(e.fechaCreacion).toLocaleString('es-ES', {
                          day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit'
                        })
                      : '-'}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
    </div>
  );
}
```

In `frontend-react/src/App.jsx`:

1. Add import after `ImportBatchPage` import (line 8):
```jsx
import DocumentosPage from './pages/DocumentosPage';
```
2. Add route after the `/dashboard/imports` route (lines 27-29):
```jsx
            <Route path="/dashboard/documentos" element={
              <ProtectedRoute><DocumentosPage /></ProtectedRoute>
            } />
```

In `frontend-react/src/layouts/MainLayout.jsx`, add nav link after the "Importar envíos" button (lines 30-32):
```jsx
            <button className="btn-nav-link" onClick={() => navigate('/dashboard/documentos')}>
              Documentos
            </button>
```

- [ ] **Step 4: Run test to verify it passes**

Run (workdir `frontend-react`): `npx vitest run src/pages/DocumentosPage.test.jsx`
Expected: PASS (4 tests).

- [ ] **Step 5: Commit**

```bash
git add frontend-react/src/pages/DocumentosPage.jsx frontend-react/src/pages/DocumentosPage.test.jsx frontend-react/src/App.jsx frontend-react/src/layouts/MainLayout.jsx
git commit -m "feat(spa): vista de auditoría de documentos con filtro por tipo"
```

---

### Task 6: Estilos CSS para F3

**Files:**
- Modify: `frontend-react/src/index.css`

**Interfaces:**
- Consumes: clases usadas por Tasks 2-5: `.evidencia-download`, `.evidencia-actions`, `.detail-topbar-actions`, `.btn-pdf`.
- Produces: estilos dark consistentes (fondo `#1a1d27`, borde `#2a2d3a`, acento `#d4762a`).

- [ ] **Step 1: Write the failing test**

No hay test de CSS (el proyecto no tiene aserciones de estilos en vitest). Este paso se sustituye por **verificación visual** tras aplicar los estilos: el build y la suite deben seguir verdes. La verificación de este task es el `npm run build` + `npm test` del paso 3.

- [ ] **Step 2: Apply styles**

Append to `frontend-react/src/index.css`:

```css
/* === F3: Documentos y evidencias === */
.detail-topbar-actions {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.btn-pdf {
  background: #1a1d27;
  border: 1px solid #2a2d3a;
  color: #e1e5ee;
  padding: 0.45rem 1rem;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.82rem;
  transition: all 0.15s;
}

.btn-pdf:hover {
  border-color: #d4762a;
  color: #fff;
}

.evidencia-actions {
  padding: 0.5rem 0.8rem 0.7rem;
}

.evidencia-download {
  width: 100%;
  background: #0f1117;
  border: 1px solid #2a2d3a;
  color: #e1e5ee;
  padding: 0.4rem 0.75rem;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.78rem;
  font-weight: 500;
  transition: border-color 0.15s, color 0.15s;
}

.evidencia-download:hover {
  border-color: #d4762a;
  color: #fff;
}

.import-form-row .btn-importar--small {
  flex: 0 0 auto;
}
```

- [ ] **Step 3: Run full suite and build**

Run (workdir `frontend-react`):
```
npx vitest run
npm run build
```
Expected: all frontend tests PASS (21 previos + 14 nuevos de F3 = **35 tests**) y build OK.

- [ ] **Step 4: Commit**

```bash
git add frontend-react/src/index.css
git commit -m "style(spa): estilos para botones PDF, descarga de evidencias y filtro de auditoría"
```

---

### Task 7: Regresión backend + docs + cierre

**Files:**
- Modify: `docs/handoff.md`

**Interfaces:**
- Consumes: resultados de Tasks 1-6.

- [ ] **Step 1: Run backend regression**

Run (workdir raíz del repo, Maven local):
`C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd clean test`
Expected: BUILD SUCCESS — 317 tests (sin cambios en Java, sin regresiones).

- [ ] **Step 2: Update docs/handoff.md**

Añadir una entrada nueva (19) al historial, tras la entrada 18 (import-batch):

```markdown
19. **SPA React: Gestión de documentos y evidencias (F3)** (completado, 2026-08-10):
    - **Helpers** en `frontend-react/src/services/api.js`: `getDocumentoUrl(tipo, ref)` (etiqueta/etiquetas-lote/manifiesto), `descargarDocumento(url)` (anchor temporal) y `getAdminDocumentos(tipo)` consumiendo `GET /api/v1/admin/documentos` (backend ya existente, sin cambios en Java).
    - **EvidenciasGrid**: botón de descarga ⬇ por evidencia (misma origin, `/uploads/**` permitAll); el modal de preview y el estado vacío se conservan (hook `useState` movido arriba para cumplir Rules of Hooks).
    - **ShipmentDetailPage**: botón "Etiqueta térmica PDF" en el topbar (`window.open` inline).
    - **ImportBatchPage**: botones "Etiquetas del lote" y "Manifiesto" en el card Lote #N (descarga vía anchor, `attachment`).
    - **DocumentosPage** (nueva): ruta protegida `/dashboard/documentos`, enlace navbar "Documentos", tabla de emisiones con filtro por tipo (`ETIQUETA_TERMICA`/`ETIQUETAS_LOTE`/`MANIFIESTO_CARGA`), peso en KB y fecha localizada.
    - **Tests frontend (14 nuevos):** `api.test.js` (5), `EvidenciasGrid.test.jsx` (4), `ShipmentDetailPage.test.jsx` (1), ampliación `ImportBatchPage.test.jsx` (1), `DocumentosPage.test.jsx` (4). Suite completa frontend: **35 tests, 0 fallos** + `npm run build` OK. Regresión backend: **317 tests, BUILD SUCCESS**.
```

Actualizar además la sección "Estado Git Actual" (líneas ~173-177) para reflejar los commits de F3.

- [ ] **Step 3: Final verification**

Run (workdir `frontend-react`): `npx vitest run` → 35 tests PASS.
Run (workdir raíz): `mvn.cmd clean test` → BUILD SUCCESS.
Run: `git status --short` → solo `docs/handoff.md` modificado (más untracked de runtime `.claude-flow/`, `.swarm/`, `ruvector.db` que NO se commitean).

- [ ] **Step 4: Commit**

```bash
git add docs/handoff.md
git commit -m "docs: registra fase F3 (documentos y evidencias) en el handoff"
```

---

## Self-Review

**1. Spec coverage:**
- T1 → T6 de la spec (helpers, EvidenciasGrid, botón etiqueta, botones lote, DocumentosPage, estilos) → cubiertos por Tasks 1-6 del plan.
- Descarga de evidencias → Task 2. Generación PDF (etiqueta/lote/manifiesto) → Tasks 3 y 4. Auditoría con filtro → Task 5. Estilos → Task 6. Verificación/regresión → Task 7. ✅
- Fuera de alcance (POD) no se toca. ✅

**2. Placeholder scan:** No hay TBD/TODO; todo paso de código tiene código completo; comandos con salida esperada. ✅

**3. Type consistency:**
- `getDocumentoUrl(tipo, referencia)` firmas consistentes: Task 1 define; Tasks 3-4 consumen con `('etiqueta', codigo)`, `('etiquetas-lote', loteActivo.id)`, `('manifiesto', loteActivo.id)`. ✅
- `descargarDocumento(url)` definida en Task 1; consumida en Tasks 2 y 4. ✅
- `getAdminDocumentos(tipo)` / `formatPesoBytes(bytes)` definidas en Task 1; consumidas en Task 5. ✅
- Clases CSS `.detail-topbar-actions`, `.btn-pdf`, `.evidencia-actions`, `.evidencia-download` definidas en Task 6 y usadas en Tasks 3, 2. ✅
- `parseLocalDateTime` ya existe en `services/dateUtils.js`. ✅
- Conteo de tests: 21 previos + 5 + 4 + 1 + 1 + 4 = **35**. ✅
