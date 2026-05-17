(function() {
    'use strict';

    // Scroll reveal
    if (window.IntersectionObserver) {
        var reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
        if (!reduced) {
            var els = document.querySelectorAll('.reveal');
            if (els.length) {
                var obs = new IntersectionObserver(function(entries) {
                    entries.forEach(function(entry) {
                        if (entry.isIntersecting) {
                            entry.target.classList.add('reveal-visible');
                            obs.unobserve(entry.target);
                        }
                    });
                }, { threshold: 0.15, rootMargin: '0px 0px -40px 0px' });
                els.forEach(function(el) { obs.observe(el); });
            }
        }
    }

    // Lightbox
    var lb = document.getElementById('lightbox');
    if (lb) {
        var lbImg = document.getElementById('lbImg');
        var lbTit = document.getElementById('lbTit');
        var lbCer = document.getElementById('lbCer');
        var lbPrev = document.getElementById('lbPrev');
        var lbNext = document.getElementById('lbNext');
        var items = [];
        var idx = 0;

        function abrir(src, tit, i) {
            lbImg.src = '';
            lbImg.alt = tit || '';
            lbImg.src = src;
            lbTit.textContent = tit || '';
            idx = i;
            lb.classList.add('active');
            document.body.style.overflow = 'hidden';
        }

        function cerrar() {
            lb.classList.remove('active');
            document.body.style.overflow = '';
        }

        function mostrar(i) {
            if (i < 0) i = items.length - 1;
            if (i >= items.length) i = 0;
            var item = items[i];
            if (item) {
                var src = item.getAttribute('data-src');
                var tit = item.getAttribute('data-title') || '';
                if (src) {
                    lbImg.src = '';
                    lbImg.src = src;
                    lbTit.textContent = tit;
                    idx = i;
                }
            }
        }

        var galItems = document.querySelectorAll('.gal-item');
        galItems.forEach(function(item, i) {
            items.push(item);
            item.addEventListener('click', function(e) {
                var src = this.getAttribute('data-src');
                var tit = this.getAttribute('data-title') || '';
                if (src) abrir(src, tit, i);
            });
        });

        if (lbCer) lbCer.addEventListener('click', cerrar);

        lb.addEventListener('click', function(e) {
            if (e.target === lb) cerrar();
        });

        if (lbPrev) {
            lbPrev.addEventListener('click', function(e) {
                e.stopPropagation();
                mostrar(idx - 1);
            });
        }

        if (lbNext) {
            lbNext.addEventListener('click', function(e) {
                e.stopPropagation();
                mostrar(idx + 1);
            });
        }

        document.addEventListener('keydown', function(e) {
            if (!lb.classList.contains('active')) return;
            if (e.key === 'Escape') cerrar();
            if (e.key === 'ArrowLeft') mostrar(idx - 1);
            if (e.key === 'ArrowRight') mostrar(idx + 1);
        });
    }

    // Page fade-in (always add class; CSS @media handles reduced-motion)
    window.addEventListener('load', function() {
        document.body.classList.add('page-loaded');
    });

    // Toast auto-dismiss
    var toasts = document.querySelectorAll('.toast');
    toasts.forEach(function(t) {
        setTimeout(function() {
            t.style.transition = 'opacity 0.4s ease, transform 0.4s ease';
            t.style.opacity = '0';
            t.style.transform = 'translateY(-12px)';
            setTimeout(function() { t.style.display = 'none'; }, 400);
        }, 4000);
    });

    // Ambient particles
    if (!window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
        var colores = ['rgba(255,200,100,0.35)', 'rgba(212,118,42,0.25)', 'rgba(255,255,255,0.15)'];
        for (var i = 0; i < 8; i++) {
            var p = document.createElement('div');
            p.className = 'ambient-particle';
            var size = 2 + Math.random() * 3;
            p.style.width = size + 'px';
            p.style.height = size + 'px';
            p.style.left = Math.random() * 100 + '%';
            p.style.top = Math.random() * 100 + '%';
            p.style.background = colores[Math.floor(Math.random() * colores.length)];
            p.style.setProperty('--drift-duration', (20 + Math.random() * 20) + 's');
            p.style.setProperty('--particle-opacity', (0.03 + Math.random() * 0.05));
            p.style.animationDelay = (Math.random() * 15) + 's';
            document.body.appendChild(p);
        }
    }
})();
