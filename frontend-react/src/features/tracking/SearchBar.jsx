import { useState, useRef } from 'react';

export default function SearchBar({ value, onChange, placeholder }) {
  const [local, setLocal] = useState(value || '');
  const [prevValue, setPrevValue] = useState(value || '');
  const timer = useRef(null);

  if (prevValue !== value) {
    setPrevValue(value);
    setLocal(value || '');
  }

  const handleChange = (e) => {
    const v = e.target.value;
    setLocal(v);
    clearTimeout(timer.current);
    timer.current = setTimeout(() => onChange(v), 300);
  };

  return (
    <div className="relative flex items-center">
      <span className="pointer-events-none absolute left-3 text-sm text-grafito-400">🔍</span>
      <input
        type="text"
        className="w-full rounded-md border border-grafito-600 bg-grafito-900 py-2 pl-9 pr-9 text-sm text-grafito-100 placeholder:text-grafito-400 outline-none transition-colors focus:border-[#d4762a]"
        value={local}
        onChange={handleChange}
        placeholder={placeholder || 'Buscar...'}
      />
      {local && (
        <button
          type="button"
          className="absolute right-3 text-grafito-300 transition-colors hover:text-white"
          onClick={() => { setLocal(''); onChange(''); }}
          aria-label="Limpiar búsqueda"
        >
          ✕
        </button>
      )}
    </div>
  );
}
