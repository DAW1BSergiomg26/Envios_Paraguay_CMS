import { useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { Zap, ShieldCheck, Package, ArrowRight } from 'lucide-react';
import { useAppleScroll } from '../hooks/useAppleScroll';
import './AppleHero.css';

const FEATURES = [
  {
    icon: Zap,
    title: 'Tiempo real',
    text: 'Seguimiento instantáneo de cada envío con actualizaciones al momento.',
  },
  {
    icon: ShieldCheck,
    title: 'Control por roles',
    text: 'Acceso protegido y trazable para cada operación del panel.',
  },
  {
    icon: Package,
    title: 'Operaciones ágiles',
    text: 'Carga, importación y gestión documental en un solo flujo.',
  },
];

export default function AppleHero() {
  const navigate = useNavigate();
  const sectionRef = useRef(null);

  useAppleScroll(sectionRef);

  const scrollToStats = () => {
    document.querySelector('.stats-grid')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  };

  return (
    <section ref={sectionRef} className="apple-hero" aria-label="Panel de control de envíos">
      <div className="apple-hero__glow apple-hero__glow--one" aria-hidden="true" />
      <div className="apple-hero__glow apple-hero__glow--two" aria-hidden="true" />

      <div className="apple-hero__badge">
        <span className="apple-hero__badge-dot" aria-hidden="true" />
        Panel de control
      </div>

      <h1 className="apple-hero__title">
        Gestiona tus envíos
        <span className="apple-hero__title-gradient"> con precisión</span>
      </h1>

      <p className="apple-hero__subtitle">
        Visión operativa en tiempo real de tu tracking internacional España ↔ Paraguay.
      </p>

      <div className="apple-hero__actions">
        <button type="button" className="apple-hero__btn apple-hero__btn--primary" onClick={() => navigate('/dashboard/envios/nuevo')}>
          Crear envío
          <ArrowRight className="apple-hero__btn-icon" size={18} aria-hidden="true" />
        </button>
        <button type="button" className="apple-hero__btn apple-hero__btn--ghost" onClick={scrollToStats}>
          Ver métricas
        </button>
      </div>

      <div className="apple-hero__cards">
        {FEATURES.map(({ icon: Icon, title, text }) => (
          <article key={title} className="apple-hero__card">
            <span className="apple-hero__card-icon">
              <Icon size={20} aria-hidden="true" />
            </span>
            <h3 className="apple-hero__card-title">{title}</h3>
            <p className="apple-hero__card-text">{text}</p>
          </article>
        ))}
      </div>
    </section>
  );
}
