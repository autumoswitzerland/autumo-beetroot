# Language Management

## Base translations (used by Java code)

If you need translations within the Java code, simply add the keys of the language Java resource bundles in the `web/lang` directory to the specific language resource bundles, e.g. `lang_en.properties`.

You can also change the default messages of beetRoot. For each new language added or requested by the web app user, add the corresponding language Java resource bundle in the resource bundle directory.

## HTML template translations

Either you use different templates in the language directories and/or you use translation tags with templates. Both are possible at the same time. The use of only different templates in subdirectories has its advantages, while using the same HTMl template file for all languages through the use of translation tags. Your decision.

If you use translation tags, you need to populate `tmpl_lang_*.properties` files in the `web/lang` directory. Translation tags start with the prefix "l.".

**Example**: The translation tag used in a HTML template

`{$l.yourprojects}`

becomes

`yourproject=Ihre Projekte`

in the translations file `tmpl_lang_de.properties`.


<br>
<br>
Click <a href="../README.md">here</a> to go to the main page.

<p align="right"><a href="#top">&uarr;</a></p>
