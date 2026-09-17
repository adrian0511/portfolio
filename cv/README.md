# CV

Fuente de los PDF que se descargan desde la web (`frontend/public/docs/CV_Adrian_Garces_ES|EN.pdf`).

```bash
pwsh cv/render.ps1      # HTML -> PDF, sobrescribe los dos ficheros de docs/
```

Los PDF **se generan, no se editan**: cualquier cambio se hace en `cv-es.html`,
`cv-en.html` o `cv.css` y se vuelve a renderizar. Antes había solo PDF (generados
con wkhtmltopdf desde un HTML que no estaba en ninguna parte), así que añadir una
línea de estudios obligaba a rehacer el documento entero.

## Lo lee primero una máquina

El CV pasa por un ATS antes que por una persona, y eso condiciona el diseño más
que el gusto. Lo que hay que respetar:

- **Nada de `letter-spacing`.** El CV anterior lo llevaba en el titular y en los
  encabezados de sección: al extraer el texto salía `D E S A R R O L L A D O R
  B A C K E N D · J A V A & S P R I N G B O O T`, así que el ATS no reconocía las
  secciones y el titular perdía "Java" y "Spring Boot", que es donde más pesan las
  palabras clave.
- **Una columna y nada de tablas.** Las habilidades iban en una tabla de dos
  columnas; ahora son líneas `Etiqueta: valores`. El orden de lectura tiene que
  ser el orden del DOM.
- **Encabezados con los nombres que un ATS busca literalmente**: Perfil,
  Habilidades técnicas, Experiencia en proyectos, Educación, Idiomas (y sus
  equivalentes en inglés).
- **Fechas en `MM/AAAA`**, no "2024 — 2026" a secas.
- **Fuentes de sistema**, sin webfonts ni iconos: nada que el parser no pueda leer
  como texto.
- **Los enlaces son clicables pero el texto visible sigue siendo la URL legible**
  (`github.com/adrian0511`, no "aquí"): la anotación la aprovecha quien lo abre en
  pantalla y el texto extraído sigue teniendo el dato.

## La foto va solo en el CV en español

`cv-es.html` lleva la foto de la web (`frontend/public/img/Avatar.jpg`); `cv-en.html`
no. En España la foto es la costumbre, pero en Reino Unido, Irlanda, EEUU o Alemania
se considera un sesgo y hay quien descarta el CV por llevarla. Para eso hay dos
versiones. La imagen es decorativa —no lleva texto dentro—, así que no cambia nada
de lo que extrae el ATS.

Para comprobar qué ve un ATS, extrae el texto del PDF con cualquier parser
(pdfjs, pdfminer, `pdftotext`) y léelo: si los encabezados no aparecen como
palabras normales, está roto.

## Cabe en una página, y va justo

La tipografía (8,5 pt / interlineado 1,3 / márgenes 11×13 mm) está ajustada al
límite: subirla medio punto pasa el documento a dos páginas. Si añades contenido,
**vuelve a renderizar y cuenta las páginas** antes de dar nada por bueno.

Se usa Chrome headless en vez de wkhtmltopdf: la 0.12.x va sobre un WebKit de 2012
sin flexbox y ya no se mantiene.
