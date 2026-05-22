import { useState, useEffect, useRef } from 'react';

export default function SearchBar({ value, onChange, placeholder }) {
  const [local, setLocal] = useState(value || '');
  const timer = useRef(null);

  useEffect(() => {
    setLocal(value || '');
  }, [value]);

  const handleChange = (e) => {
    const v = e.target.value;
    setLocal(v);
    clearTimeout(timer.current);
    timer.current = setTimeout(() => onChange(v), 300);
  };

  return (
    <div className="search-bar">
      <span className="search-icon">🔍</span>
      <input
        type="text"
        className="search-input"
        value={local}
        onChange={handleChange}
        placeholder={placeholder || 'Buscar...'}
      />
      {local && (
        <button className="search-clear" onClick={() => { setLocal(''); onChange(''); }}>
          ✕
        </button>
      )}
    </div>
  );
}
