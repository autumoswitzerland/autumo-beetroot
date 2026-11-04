# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

The first release documented in this file is 3.3.0.

## [Unreleased]

### Changed

- Moved the patched NanoHTTPD router from `org.nanohttpd.router.RouterNanoHTTPD`
  to `ch.autumo.beetroot.server.HTTPServer` and renamed the class to `HTTPServer`.
  This is a breaking change for code that imports the old fully qualified name;
  all internal references and documentation links were updated.
- Default HTTPS protocols are now restricted to `TLSv1.2,TLSv1.3`; the obsolete
  `TLSv1` and `TLSv1.1` protocols were removed from `BaseServer`.
- The `ch.autumo.beetroot.server` package documentation now describes the
  server implementations.

### Fixed

- Restored the original NanoHTTPD BSD license notice in the moved `HTTPServer`
  class, alongside the autumo Apache 2.0 header.

## [3.3.0]

### Added

- Collapsible side menu with a button in the top bar, `Ctrl`/`Cmd` + `B` and the
  state kept in `localStorage` (`web/js/sidebar.js`).
- Design tokens (`:root` in `web/css/uncompressed/style.css`) for colours,
  radii and spacing; `web/css/theme-dark.css` only redefines them.
- Phosphor icon font 2.1.2 (`web/font/Phosphor.woff2`, MIT) with the `.ph-*` classes and
  icons for the table status columns, the paginator, the main menu and the side
  menu entries.
- The `{#message}` block can be placed in the layout or in any page template.
- `bin/minify-css.php` to build `web/css/base.min.css` and `style.min.css` from
  the stylesheets in `web/css/uncompressed/`.
- Translations for hiding and showing the navigation in all 18 language files.

### Changed

- The typeface is Inter (variable font) instead of Aileron.
- Content pages are laid out with CSS grid instead of floats; the footer stays at
  the bottom of the window on short pages.
- Side menu, tables, pagination and form fields use the design tokens; the
  controls share a radius of 8 px and the language menu is 2 px wider.
- `doc/design.md` documents the tokens, the fonts, the minifier and the side
  menu.

### Removed

- `web/css/bootstrap.min.css` and its `<link>`; the few rules used by the
  templates are part of `style.css` now.
- The 16 static Aileron font files.
- The equal height hack (`margin-bottom: -99999px; padding-bottom: 99999px`).
- All references to the status and password strength images in `web/img/status/`;
  they are replaced by Phosphor icons.

### Fixed

- Translating with a `null` or empty language falls back to the default language.
- Requests without a language prefix (e.g. a failed login) use the session
  language for their messages instead of translating with `null`.
- A message block in a page template is rendered once and does not duplicate the
  page content.
- Login and logout pages use the full width of the window.
- The current page is marked in the side menu with `aria-current="page"`.

