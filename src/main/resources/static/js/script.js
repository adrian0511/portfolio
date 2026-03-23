(function () {
    const dot = document.getElementById('c-dot');
    const ring = document.getElementById('c-ring');

    const hasRealMouse = window.matchMedia(
        '(hover: hover) and (pointer: fine)'
    ).matches;

    if (!hasRealMouse) return; // tablets/móviles: salir

    let mx = 0, my = 0;
    let rx = 0, ry = 0;
    let started = false;

    document.addEventListener('mousemove', function (e) {
        mx = e.clientX;
        my = e.clientY;

        if (!started) {
            // Primera vez: inicializar anillo en misma posición
            rx = mx; ry = my;
            dot.classList.add('vis');
            ring.classList.add('vis');
            started = true;
        }
    });

    (function loop() {
        if (started) {
            // Punto: sigue al instante
            dot.style.transform =
                'translate(' + (mx - 4.5) + 'px, ' + (my - 4.5) + 'px)';

            // Anillo: lag suave
            rx += (mx - rx) * 0.14;
            ry += (my - ry) * 0.14;
            ring.style.transform =
                'translate(' + (rx - 17) + 'px, ' + (ry - 17) + 'px)';
        }
        requestAnimationFrame(loop);
    })();

    // Efecto al hover en interactivos
    document.querySelectorAll('a, button').forEach(function (el) {
        el.addEventListener('mouseenter', function () {
            dot.style.width = '26px';
            dot.style.height = '26px';
            dot.style.background = 'transparent';
            dot.style.border = '1.5px solid var(--accent)';
            dot.style.borderRadius = '50%';
            // reajustar offset para el tamaño grande
        });
        el.addEventListener('mouseleave', function () {
            dot.style.width = '9px';
            dot.style.height = '9px';
            dot.style.background = 'var(--accent)';
            dot.style.border = 'none';
        });
    });
})();

/* ══ NAV SCROLL ══ */
var nav = document.getElementById('nav');
window.addEventListener('scroll', function () {
    nav.classList.toggle('solid', window.scrollY > 40);
}, { passive: true });

/* ══ HAMBURGER ══ */
var hbg = document.getElementById('hbg');
var drawer = document.getElementById('drawer');
var isOpen = false;

function closeDrawer() {
    isOpen = false;
    hbg.classList.remove('x');
    drawer.classList.remove('open');
    document.body.style.overflow = '';
}

hbg.addEventListener('click', function () {
    isOpen = !isOpen;
    hbg.classList.toggle('x', isOpen);
    drawer.classList.toggle('open', isOpen);
    document.body.style.overflow = isOpen ? 'hidden' : '';
});

document.addEventListener('keydown', function (e) {
    if (e.key === 'Escape') closeDrawer();
});

/* ══ REVEAL ON SCROLL ══ */
var io = new IntersectionObserver(function (entries) {
    entries.forEach(function (e, i) {
        if (e.isIntersecting)
            setTimeout(function () { e.target.classList.add('on'); }, i * 75);
    });
}, { threshold: 0.07, rootMargin: '0px 0px -28px 0px' });

document.querySelectorAll('.rv').forEach(function (el) { io.observe(el); });

const API_URL = "/api/projects";
let csrfToken = null;

fetch('/api/csrf-token')
    .then(response => {
        if (!response.ok) {
            throw new Error(`Error ${response.status}: ${response.statusText}`);
        }
        return response.json();
    })
    .then(data => {
        csrfToken = data.token;
        loadProjects();
    })
    .catch(error => {
        console.error("No se pudo obtener el token CSRF:", error);
        renderFallback();
    });

async function loadProjects() {
    const container = document.getElementById("projects-container");
    try {
        const res = await fetch(API_URL, {
            headers: {
                "X-CSRF-Token": csrfToken
            }
        });

        if (!res.ok) throw new Error("API error");

        const repos = await res.json();

        renderProjects(repos);

    } catch (error) {
        console.error("Error:", error);
        renderFallback();
    }
}

function renderProjects(repos) {
    const container = document.getElementById("projects-container");

    // limpiar todo
    container.innerHTML = "";

    repos.forEach((repo, index) => {
        const card = document.createElement("div");
        card.classList.add("pc");

        card.innerHTML = `
        <a href="${repo.html_url}" target="_blank"
           style="text-decoration:none;color:inherit;display:flex;flex-direction:column;gap:.85rem;height:100%">
          
          <div style="display:flex;justify-content:space-between;align-items:center">
            <span class="pnum">${String(index + 1).padStart(2, '0')}</span>
            <span class="ptag">${repo.topic || "Backend"}</span>
          </div>

          <h3>${repo.name}</h3>
          <p class="pdesc">${repo.description || "Proyecto backend con Spring Boot"}</p>

          <div class="plinks">
            <span class="pl">GitHub →</span>
          </div>

        </a>
      `;
        container.appendChild(card);
    });

    container.appendChild(createGithubCard());

    activateReveal();
}

function renderFallback() {
    const container = document.getElementById("projects-container");

    container.innerHTML = `
    <div class="pc rv" style="grid-column:span 3;text-align:center;justify-content:center;align-items:center">
      <h3 style="color:var(--muted)">No se pudieron cargar los proyectos</h3>
      <p class="pdesc">Puedes verlos directamente en GitHub</p>
    </div>
  `;

    container.appendChild(createGithubCard());

    activateReveal();
}

function createGithubCard() {
    const card = document.createElement("div");

    card.className = "pc rv";
    card.style.border = "1px dashed var(--border)";
    card.style.background = "transparent";
    card.style.justifyContent = "center";
    card.style.alignItems = "center";
    card.style.textAlign = "center";
    card.style.gap = ".5rem";
    card.style.minHeight = "180px";

    card.innerHTML = `
    <div style="font-size:1.4rem;color:var(--muted)">+</div>
    <div style="font-size:.72rem;color:var(--muted)">Más proyectos en GitHub</div>
    <a href="https://github.com/adrian0511" target="_blank"
      style="font-size:.63rem;color:var(--accent);text-decoration:none;letter-spacing:.1em;text-transform:uppercase;margin-top:.3rem">
      @adrian0511 →
    </a>
  `;

    return card;
}

function activateReveal() {
    document.querySelectorAll('.pc').forEach(el => io.observe(el));
}
