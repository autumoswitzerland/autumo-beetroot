# Webapp Design and Javascript

All you need for this, is located here:

- `web/css/*`
- `web/font/*`
- `web/img/*`
- `web/js/*`

These resources are straightly routed by the user's URL request, e.g.:

  - `http://localhost:8080/img/myImage.png` -> `web/img/myImage.png`
  - `http://localhost:8080/js/myScript.js` -> `web/js/myScript.js`
  - etc.

Also, in this case, you never have to reference a servlet name in any HTML template, you always point to the root-path "/", no matter what!

A few words about existing stylesheets. Uncompressed sources exist for
`base.css`, `foundation.css`, `password-strength.css` and `style.css` in
`web/css/uncompressed`; the other sheets are shipped as they are:

  - `base.min.css`, `foundation.min.css`: Base styles, this is something you don't want to change in most cases unless you create your own page design from scratch; In this case, you could get rid of them all.

  - `web/css/style.min.css`: The framework and component styles. It is generated from `web/css/uncompressed/style.css` and should not be edited; put your own design and overrides into `default.css`.

  - `web/css/jquery-ui.min.css`: Better tooltips.

  - `web/css/password-strength.min.css`: Password validator/strength script.
  
  - `web/css/refs.css`: Add here styles that reference images, fonts, etc. per url-references, e.g.: `url('/img/...');`. This is necessary, so beetRoot can translate resource URL's for a servlet context correctly.

  - `web/css/default.css`: Your default web-application styles and designs, including the design tokens of the light theme. Not minified, so you can edit it directly.

  - `web/css/theme-dark.css`: The default dark theme; you can add your own themes by naming it `theme-yourname.css` and HTTP-request it through the users' settings handler.

## Design Tokens

The colours and dimensions of the web UI are defined as CSS custom properties
in the `:root` block of `web/css/default.css` (light theme) and
`web/css/theme-dark.css` (dark theme). The tokens are grouped by area, e.g.
`--text`, `--link`, `--menu-icon`, `--sidebar-bg`, `--table-th` or
`--pager-active-bg`. The theme-relevant colours in `style.min.css` are
referenced via `var(--token)`, so a theme redefines these tokens in its own
`:root` block; a custom `theme-yourname.css` consists of the values you want
to override plus any additional rules you need (see `theme-dark.css`, which
also carries a few form-field rules for the dark theme). A few structural
colours are still hard-coded in `style.css`.

The load order in `web/html/blocks/head.html` is what makes this work:
`style.min.css` first, then `default.css`, then `theme-<name>.css`. Never edit
`style.min.css` or `web/css/uncompressed/style.css`; use `default.css` for the
light theme and a theme file for everything else.

## Fonts and Icons

Two fonts are shipped in `web/font` and declared with `@font-face` in
`web/css/refs.css`:

- Inter (`Inter-*.woff2`, `InterVariable*.woff2`): the text font.
- Phosphor (`Phosphor.woff2`, MIT licence, see `Phosphor-LICENSE.txt`): the
  icon font.

Icons are rendered as Phosphor glyphs instead of bitmap images:

	<span class="ph ph-check"></span>
	<span class="ph ph-info"></span>

The base class `ph` maps the glyph to the font, the class `ph-<name>` sets the
code point. The available names and code points are listed in
`phosphor-codepoints.properties` (see autumo-ui); the classes used by the
default templates are defined at the end of `style.css`.

## CSS Minification

The pages load the minified files in `web/css` (e.g. `style.min.css`), while
the sources live in `web/css/uncompressed`. After changing a source file,
regenerate the minified files with:

	php bin/minify-css.php

Without arguments the script compresses the project stylesheets; pass file
names to compress only those (`php bin/minify-css.php style.css`). `--list`
shows the files and `--help` prints the usage. The third-party sheets
(`foundation`, `jquery-ui`, `password-strength`) are shipped minified already.
`default.css` and the theme files are not minified and are edited directly.

## Collapsible Side Menu

`web/js/sidebar.js` toggles the left side menu (the `actions-sidebar` or
`service-sidebar` element). The state is stored in the `localStorage` of the
browser, separated per application by the `data-app` attribute of the script
tag (`{$serverName}`), and the toggle button is hidden on pages that do not
have a side menu. The styles live in the section "Collapsible side menu" of
`style.css`.


<br>
<br>
Click <a href="../README.md">here</a> to go to the main page.

<p align="right"><a href="#top">&uarr;</a></p>
