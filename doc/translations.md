# Language Management

beetRoot does not localize basic languages, such as the distinction between written 
differences between `de_CH` and `de_DE`, which is why there is only one translation 
file for each language. Decide on a variant for a translation.

The subdirectories contain the following translations:

- `web/lang/app/` : Translations for the output of internal application messages such as error messages.
- `web/lang/pw/` : Translations for password validation output.
- `web/lang/tmpl/` : Translations for all HTML templates in the `web/html` directory.

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
while using the same HTML template file for all languages through the use of translation tags. Your decision.

If you use translation tags, you need to populate `lang_*.properties` files in the `web/lang/tmpl` directory. Translation tags start with the prefix "l.".
Initially these translations can be populated by using the 'Translate templates' function in the PLANT generator; it will add the initial translations
in the file `web/lang/tmpl/lang.templates`. Start PLANT to learn more.

**Example**: The translation tag used in a HTML template

`{$l.yourprojects}`

becomes

`yourproject=Ihre Projekte`

in the translations file `web/lang/tmpl/lang_de.properties`.

## HTML template language processor (PLANT function)

PLANT has a built-in function to populate your template translations and inserts new translations into the `web/lang/tmp/lang.properties` file, while it 
updates the translated text in the HTML templates in `web/html/` as well as in all `columns.cfg` files. From there, you can copy all new translations 
into your effective language translation files, e.g. `web/lang/tmp/lang_es.properties` and translate them into the appropriate language.

## Example
- HTML template: `web/html/users/welcome.html`
- Translated text fragment within the above template: `Hello {$username}, welcome!`

The processor takes the text fragment and inserts it into the file `web/lang/tmp/lang.properties`:

`users.welcome.1=Hello {$username}, welcome!`

In this case, you must replace `{$username}` with `{0}` (Java message formatting) and adjust the translation key that replaces the text fragment in the 
HTML template ( `web/html/users/welcome.html` ):

`{$l.users.welcome.1,{$userName}}`

As you can see, the translation key uses the prefix `$l.` to distinguish between the language translations and the HTML variables of the template engine 
that are replaced by handlers. The template engine first replaces the handler variable, then the key is translated and the replaced handler 
variable is now inserted at the position `{0}` as defined in the translation file `web/lang/tmp/lang.properties`. This also works for more than 
one variable by adding further variables after the translation key (all separated by commas). Use increasing placeholders in the translation key, 
e.g. `{0}, {1}, {2}` etc.

The names of the translation keys are automatically generated based on the directory and the name of the template file including an ascending number. 
Of course, if no handler variable is present, you do not have to worry about formatting Java messages with the placeholders `{0}, {1}, {2}`, but can 
simply translate into all desired languages.


<br>
<br>
Click <a href="../README.md">here</a> to go to the main page.

<p align="right"><a href="#top">&uarr;</a></p>
