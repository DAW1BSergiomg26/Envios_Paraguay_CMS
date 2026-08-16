import { ESTADOS } from './trackingConstants';

export default function MultiStatusFilter({ selected, onChange }) {
  const toggle = (value) => {
    const next = selected.includes(value)
      ? selected.filter((s) => s !== value)
      : [...selected, value];
    onChange(next);
  };

  return (
    <div className="flex flex-wrap gap-2">
      {ESTADOS.map((s) => {
        const active = selected.includes(s.value);
        return (
          <button
            key={s.value}
            type="button"
            className={`rounded-full border px-3 py-1 text-xs font-medium transition-colors ${
              active
                ? 'border-[#d4762a] bg-[#d4762a] text-white'
                : 'border-grafito-600 bg-grafito-900 text-grafito-200 hover:border-grafito-400'
            }`}
            onClick={() => toggle(s.value)}
          >
            {s.label}
          </button>
        );
      })}
    </div>
  );
}
