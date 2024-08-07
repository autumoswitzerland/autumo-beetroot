# Webapp Design and Javascript

All you need for this, is located here:

- web/css/*
- web/font/*
- web/img/*
- web/js/*

These resources are straightly routed by the user's URL request, e.g.:

  - `http://localhost:8080/img/myImage.png` -> `web/img/myImage.png`
  - `http://localhost:8080/js/myScript.js` -> `web/js/myScript.js`
  - etc.

Also, in this case, you never have to reference a servlet name in any HTML template, you always point to the root-path "/", no matter what!

A few words about existing stylesheets:

  - `base.css`, `bootstrap-beetroot.css`, `foundation.min.css`: Base styles, this is something you don't want to change in most cases unless you create your own page design from scratch; In this case, you could get rid of them all.

  - `web/css/style.css`: Adjust your general web-application style here.

  - `web/css/refs.css`: Add here styles that reference images, fonts, etc. per url-references, e.g.: `url('/img/...');`. This is necessary, so beetRoot can translate resource URL's for a servlet context correctly.

  - `web/css/jquery-ui.min.css`: Better tooltips.

  - `web/css/default.css`: Your default web-application styles and designs.

  - `web/css/theme-dark.css`: The default dark theme; you can add your own themes by naming it `theme-yourname.css` and HTTP-request it through the users' settings handler.


<br>
<br>
<a href="../README.md">[Main Page]</a>
