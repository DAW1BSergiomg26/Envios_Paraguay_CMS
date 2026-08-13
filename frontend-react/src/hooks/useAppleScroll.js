import { useEffect } from 'react';
import { gsap } from 'gsap';
import { ScrollTrigger } from 'gsap/ScrollTrigger';

gsap.registerPlugin(ScrollTrigger);

const DEFAULT_OPTIONS = {
  y: 48,
  duration: 1,
  start: 'top 85%',
};

export function useAppleScroll(ref, options = {}) {
  const { y, duration, start } = { ...DEFAULT_OPTIONS, ...options };

  useEffect(() => {
    const element = ref?.current;
    if (!element) return undefined;

    if (typeof window === 'undefined') return undefined;
    const prefersReducedMotion = window.matchMedia?.('(prefers-reduced-motion: reduce)')?.matches;
    if (prefersReducedMotion) return undefined;

    const ctx = gsap.context(() => {
      gsap.fromTo(
        element,
        { opacity: 0, y, scale: 0.96 },
        {
          opacity: 1,
          y: 0,
          scale: 1,
          duration,
          ease: 'power3.out',
          scrollTrigger: {
            trigger: element,
            start,
            once: true,
          },
        }
      );
    });

    return () => ctx.revert();
  }, [ref, y, duration, start]);
}

export default useAppleScroll;
