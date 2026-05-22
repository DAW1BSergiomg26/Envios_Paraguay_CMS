import { useState } from 'react';
import { exportToCSV, exportToExcel } from '../services/exportUtils';
import { useToast } from '../context/NotificationContext';

export default function ExportButtons({ envios }) {
  const [exporting, setExporting] = useState(null);
  const { showSuccess, showError: showErrToast } = useToast();

  const handleExport = async (format) => {
    setExporting(format);
    await new Promise(r => setTimeout(r, 100));
    try {
      if (format === 'csv') {
        exportToCSV(envios);
        showSuccess(`${envios.length} envíos exportados a CSV`);
      } else {
        exportToExcel(envios);
        showSuccess(`${envios.length} envíos exportados a Excel`);
      }
    } catch (err) {
      showErrToast('Error al exportar');
    } finally {
      setExporting(null);
    }
  };

  return (
    <div className="export-buttons">
      <button
        className="export-btn export-btn--csv"
        onClick={() => handleExport('csv')}
        disabled={exporting !== null || envios.length === 0}
        title="Exportar a CSV"
      >
        {exporting === 'csv' ? (
          <span className="export-btn__spinner" />
        ) : (
          <svg className="export-btn__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
            <polyline points="7 10 12 15 17 10" />
            <line x1="12" y1="15" x2="12" y2="3" />
          </svg>
        )}
        <span>CSV</span>
      </button>
      <button
        className="export-btn export-btn--xlsx"
        onClick={() => handleExport('xlsx')}
        disabled={exporting !== null || envios.length === 0}
        title="Exportar a Excel"
      >
        {exporting === 'xlsx' ? (
          <span className="export-btn__spinner" />
        ) : (
          <svg className="export-btn__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <rect x="3" y="3" width="18" height="18" rx="2" />
            <line x1="8" y1="9" x2="16" y2="9" />
            <line x1="8" y1="13" x2="16" y2="13" />
            <line x1="8" y1="17" x2="12" y2="17" />
          </svg>
        )}
        <span>Excel</span>
      </button>
    </div>
  );
}
