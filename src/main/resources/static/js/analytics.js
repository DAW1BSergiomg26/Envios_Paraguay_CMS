(function () {
    'use strict';

    var URL_RESUMEN = '/api/v1/admin/analytics/resumen';
    var URL_REFRESH = '/api/v1/admin/analytics/refresh';

    var PALETA = {
        ENTREGADO: '#4ADE80',
        EN_TRANSITO: '#E67E22',
        RECIBIDO: '#1B4D3B',
        EN_ADUANA_ORIGEN: '#2D6A4F',
        EN_ADUANA_DESTINO: '#153C2D',
        EN_REPARTO: '#F09A54'
    };
    var FALLBACK_COLOR = '#5B8C7A';

    var graficos = [];
    var cargando = false;

    function $(id) { return document.getElementById(id); }

    function colorDeEstado(estado) { return PALETA[estado] || FALLBACK_COLOR; }

    function coloresTema() {
        var dark = document.documentElement.getAttribute('data-theme') === 'dark';
        return {
            grid: dark ? 'rgba(255,255,255,0.08)' : 'rgba(0,0,0,0.08)',
            ticks: dark ? 'rgba(255,255,255,0.72)' : 'rgba(0,0,0,0.72)',
            label: dark ? 'rgba(255,255,255,0.9)' : 'rgba(0,0,0,0.9)'
        };
    }

    function formatoNumero(v) {
        var n = Math.round(v * 100) / 100;
        return n.toLocaleString('es');
    }

    function destruirGraficos() {
        graficos.forEach(function (g) { try { g.destroy(); } catch (e) {} });
        graficos = [];
    }

    function limpiarVacios() {
        var nodos = document.querySelectorAll('.bi-vacio');
        for (var i = 0; i < nodos.length; i++) {
            nodos[i].parentNode.removeChild(nodos[i]);
        }
    }

    function renderKpis(kpis) {
        kpis.forEach(function (kpi, i) {
            var v = $('bi-kpi-value-' + i);
            var l = $('bi-kpi-label-' + i);
            var d = $('bi-kpi-dot-' + i);
            if (v) v.textContent = formatoNumero(kpi.value);
            if (l) l.textContent = kpi.label;
            if (d) d.style.background = kpi.color;
        });
    }

    function estadoVacio(id, mensaje) {
        var c = $(id);
        if (!c) return;
        var div = document.createElement('div');
        div.className = 'bi-vacio';
        div.textContent = mensaje;
        c.parentNode.appendChild(div);
    }

    function graficoDona(id, datos) {
        var canvas = $(id);
        if (!canvas) return;
        if (!datos.length) { estadoVacio(id, 'Sin envíos registrados'); return; }
        var tc = coloresTema();
        graficos.push(new Chart(canvas.getContext('2d'), {
            type: 'doughnut',
            data: {
                labels: datos.map(function (d) { return d.estado; }),
                datasets: [{
                    data: datos.map(function (d) { return d.cantidad; }),
                    backgroundColor: datos.map(function (d) { return colorDeEstado(d.estado); }),
                    borderWidth: 2
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { position: 'bottom', labels: { color: tc.label } } }
            }
        }));
    }

    function graficoLinea(id, etiquetas, valores, color) {
        var canvas = $(id);
        if (!canvas) return;
        if (!valores.length) { estadoVacio(id, 'Sin datos en el periodo'); return; }
        var tc = coloresTema();
        graficos.push(new Chart(canvas.getContext('2d'), {
            type: 'line',
            data: {
                labels: etiquetas,
                datasets: [{
                    label: 'Envíos',
                    data: valores,
                    borderColor: color,
                    backgroundColor: color + '33',
                    fill: true,
                    tension: 0.35,
                    pointBackgroundColor: color,
                    pointRadius: 3
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                scales: {
                    x: { ticks: { color: tc.ticks }, grid: { color: tc.grid } },
                    y: { beginAtZero: true, ticks: { color: tc.ticks, precision: 0 }, grid: { color: tc.grid } }
                },
                plugins: { legend: { display: false } }
            }
        }));
    }

    function graficoBarras(id, etiquetas, valores) {
        var canvas = $(id);
        if (!canvas) return;
        if (!valores.length) { estadoVacio(id, 'Sin rutas registradas'); return; }
        var tc = coloresTema();
        graficos.push(new Chart(canvas.getContext('2d'), {
            type: 'bar',
            data: {
                labels: etiquetas,
                datasets: [{
                    label: 'Envíos',
                    data: valores,
                    backgroundColor: ['#1B4D3B', '#2D6A4F', '#153C2D', '#E67E22', '#4ADE80']
                }]
            },
            options: {
                indexAxis: 'y',
                responsive: true,
                maintainAspectRatio: false,
                scales: {
                    x: { beginAtZero: true, ticks: { color: tc.ticks, precision: 0 }, grid: { color: tc.grid } },
                    y: { ticks: { color: tc.ticks }, grid: { display: false } }
                },
                plugins: { legend: { display: false } }
            }
        }));
    }

    function renderCharts(data) {
        limpiarVacios();
        destruirGraficos();

        renderKpis(data.kpis || []);

        graficoDona('bi-chart-estado', data.enviosPorEstado || []);

        var tend = data.tendencia || [];
        graficoLinea('bi-chart-tendencia',
            tend.map(function (p) { return p.fecha.slice(5); }),
            tend.map(function (p) { return p.total; }),
            '#E67E22');

        var rutas = data.topRutas || [];
        graficoBarras('bi-chart-rutas',
            rutas.map(function (r) { return (r.origen || 'N/D') + ' → ' + (r.destino || 'N/D'); }),
            rutas.map(function (r) { return r.cantidad; }));

        var wh = data.webhookPorDia || [];
        graficoLinea('bi-chart-webhooks',
            wh.map(function (p) { return p.fecha.slice(5); }),
            wh.map(function (p) { return p.tasaExito; }),
            '#4ADE80');
    }

    function mostrarError(msg) {
        var b = $('bi-error');
        if (!b) return;
        b.style.display = 'flex';
        var t = $('bi-error-text');
        if (t) t.textContent = msg;
        var c = $('bi-carga');
        if (c) c.style.display = 'none';
    }

    function ocultarError() {
        var b = $('bi-error');
        if (b) b.style.display = 'none';
    }

    function setCargando(activo) {
        cargando = activo;
        var btn = $('bi-refresh');
        if (btn) { btn.disabled = activo; btn.textContent = activo ? 'Refrescando…' : 'Refrescar'; }
        var c = $('bi-carga');
        if (c) c.style.display = activo ? 'block' : 'none';
    }

    function cargar() {
        if (cargando) return;
        setCargando(true);
        ocultarError();
        fetch(URL_RESUMEN, { headers: { 'Accept': 'application/json' } })
            .then(function (r) {
                if (!r.ok) throw new Error('HTTP ' + r.status);
                return r.json();
            })
            .then(function (data) {
                renderCharts(data);
                setCargando(false);
            })
            .catch(function (err) {
                setCargando(false);
                mostrarError('No se pudieron cargar las métricas (' + err.message + ').');
            });
    }

    function refrescar() {
        if (cargando) return;
        setCargando(true);
        ocultarError();
        fetch(URL_REFRESH, { method: 'POST', headers: { 'Accept': 'application/json' } })
            .then(function (r) {
                if (!r.ok) throw new Error('HTTP ' + r.status);
                return r.json();
            })
            .then(function (data) {
                renderCharts(data);
                setCargando(false);
            })
            .catch(function (err) {
                setCargando(false);
                mostrarError('No se pudo refrescar (' + err.message + ').');
            });
    }

    function init() {
        var btn = $('bi-refresh');
        var retry = $('bi-retry');
        if (btn) btn.addEventListener('click', refrescar);
        if (retry) retry.addEventListener('click', cargar);
        cargar();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();