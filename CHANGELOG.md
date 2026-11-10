# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

The first release documented in this file is 3.3.0.

## [3.4.0]

### Added

- General WebSocket facility for selected actions: a single
  endpoint (`websocket_path`, default `/ws`) with JSON action dispatch over
  action paths registered in `cfg/routing.xml` via `<WebSocket>` elements;
  handshake authentication with the regular web session, per-action access
  through `hasAccess(Session)`, origin check, connection limit, heartbeat and
  a dedicated socket timeout (`websocket_*` configuration keys). WebSocket
  connections are closed when their user session ends.
- Jakarta WebSocket transport for servlet containers (e.g. Tomcat, Jetty,
  WebLogic), sharing the same transport-neutral protocol/action layer as the
  standalone NanoHTTPD WebSocket transport.
- `websocket_port` configuration key for distributed deployments where the
  browser must connect to the beetRoot WebSocket server on a different port
  than the HTTP/UI server (falls back to `ws_port` if not set). The client
  module `web/js/websockets.js` accepts an optional `port` when subscribing.
- Generic client module `web/js/websockets.js` with automatic reconnection,
  status callbacks and a reusable fallback countdown helper
  (`BeetRootWS.startFallbackCountdown`) for periodic reload fallback.
- Live log tail: the system log page streams the in-memory log via the
  registered action `/system/log/tail`
  (`ch.autumo.beetroot.handler.system.LogTailWebSocketAction`), with a status
  indicator in the top bar and periodic reload as fallback.
- Documentation: `doc/websockets.md`.

### Changed

- Moved the patched NanoHTTPD router from `org.nanohttpd.router.RouterNanoHTTPD`
  to `ch.autumo.beetroot.server.HTTPServer`. This is an internal refactoring;
  applications, handlers, templates and routing configurations are not affected.
- Default HTTPS protocols are now restricted to `TLSv1.2,TLSv1.3`; the obsolete
  `TLSv1` and `TLSv1.1` protocols were removed from `BaseServer`.
- The design tokens (`:root`) moved from `web/css/uncompressed/style.css` to
  `web/css/default.css`, the stylesheet for your own adjustments. Themes such as
  `web/css/theme-dark.css` override them with their own `:root` block. New token
  `--heading-4-line` for the underline of `h4` in content blocks.
- The `ch.autumo.beetroot.server` package documentation now describes the
  server implementations.

### Fixed

- Restored the original NanoHTTPD BSD license notice in the moved `HTTPServer`
  class, alongside the autumo Apache 2.0 header.
- The error heading (`h4`) on the error page uses its intended style again: the
  selector in `style.css` matches the template class (`errMsg`, was `errMSg`), so
  the error colour applies and the underline is omitted. The underline itself is
  now the token `--heading-4-line` and therefore visible in the dark theme.
- `web_html_ref_pre_url_part` is only applied inside a servlet container. A value
  configured in standalone mode is ignored instead of prefixing all HTML
  references and web resources. Leading and trailing slashes in the configured
  value are ignored.
- Avoid noisy errors when sending WebSocket messages to connections that are
  already closed (e.g. during shutdown) by checking the connection state before
  sending.

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

