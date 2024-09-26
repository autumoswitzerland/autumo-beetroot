# Language Management

beetRoot does not localize basic languages, such as the distinction between written 
differences between `de_CH` and `de_DE`, which is why there is only one translation 
file for each language. Decide on a variant for a translation.

The subdirectories contain the following translations:

- `app/` : Translations for the output of internal application messages such as error messages.
- `pw/` : Translations for password validation output.
- `tmpl/` : Translations for all HTML templates in the `web/html` directory.

All translation files are in the UTF-8 format.

If you want to add languages:

- Translate translation files in `web/lang/`.
- In `cfg/beetroot.cfg` add language to key `web_languages`.
- In `cfg/languages.cfg` add full language name.
- In `web/css/refs.css` add new styles for flag icon used in the menu and of course add an icon to `web/img/lang`.

Note: The enclosed German and English translations are mostly reviewed, but the other languages are not, 
because they have been translated automatically. If you use one of these languages, you should review the 
translations. Corrections are gladly accepted!

## Base translations (used by Java code)

If you need translations within the Java code, simply add the keys of the language Java resource 
bundles in the `web/lang/app` directory to the specific language resource bundles, e.g. `lang_en.properties`.

You can also change the default messages of beetRoot. For each new language added or requested 
by the web app user, add the corresponding language Java resource bundle in the resource bundle directory.

## HTML template translations

Either you use different templates in the language directories and/or you use translation tags with templates. 
Both are possible at the same time. The use of only different templates in subdirectories has its advantages, 
while using the same HTMl template file for all languages through the use of translation tags. Your decision.

If you use translation tags, you need to populate `lang_*.properties` files in the `web/lang/tmpl` directory. Translation tags start with the prefix "l.".
Initially these translations can be populated by using the 'Translate templates' function in the PLANT generator; it will add the initial translations
in the file `web/lang/tmpl/lang.templates`. Start PLANT to learn more.

**Example**: The translation tag used in a HTML template

`{$l.yourprojects}`

becomes

`yourproject=Ihre Projekte`

in the translations file `tmpl_lang_de.properties`.


<br>
<br>
Click <a href="../README.md">here</a> to go to the main page.

<p align="right"><a href="#top">&uarr;</a></p>
