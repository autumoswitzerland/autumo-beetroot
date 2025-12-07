# Language Management

beetRoot does not differentiate between regional variants of a language, such as `de_CH` and `de_DE`. Therefore, there is only one translation file per language. You should decide on a variant for your translations.

The subdirectories contain the following translations:

- `web/lang/app/` : Translations for internal application messages, such as error messages.  
- `web/lang/pw/` : Translations for password validation messages.  
- `web/lang/tmpl/` : Translations for all HTML templates in the `web/html` directory.  

All translation files use UTF-8 encoding.

### Adding New Languages

1. Translate the files in `web/lang/`.
2. Add the language to the `web_languages` key in `cfg/beetroot.cfg`.
3. Add the full language name in `cfg/languages.cfg` (UTF-8 encoding).
4. Add new styles for the flag icon in `web/css/refs.css` and add the icon to `web/img/lang`.

**Note**: The included German and English translations have been reviewed. Other languages were automatically translated and should be reviewed if used. Corrections are welcome.

## Base Translations (Used by Java Code)

For translations used in Java code (exceptions, messages, etc.), add the language keys to the corresponding language resource bundles (e.g., `lang_en.properties`) located in the `web/lang/app` directory and use the [LanguageManager](https://github.com/autumoswitzerland/autumo-beetroot/blob/master/src/main/java/ch/autumo/beetroot/LanguageManager.java).

You can also customize the default messages of beetRoot. For each new language added or requested by a web app user, include the corresponding Java resource bundle in the resource bundle directory.

## HTML Template Translations with Placeholders

You can either use different templates in the language-specific directories or use translation tags within templates. Both approaches can be used simultaneously. Using separate templates in language subdirectories has its advantages, but the recommended approach is to use the same HTML template file for all languages with translation tags.

The translation lookup order:

1. `web/html/<user-lang>/<entity>/<template>.html`
2. `web/html/<default-lang>/<entity>/<template>.html` (first language defined by `web_languages` in `beetroot.cfg`)
3. `web/html/<entity>/<translated-template>.html`

If you use translation tags, you need to populate `lang_*.properties` files in the `web/lang/tmpl` directory. Translation tags start with the prefix `l.`. Initially these translations can be populated by using the 'Translate templates' function in the [PLANT](plant.md) generator; it will add the initial translations to the file `web/lang/tmpl/lang.templates`. Start PLANT to learn more and see the following chapter below.

**Example:**  

In a template, the translation tag used 

`{$l.yourprojects}`

becomes at runtime

`Ihre Projekte`

if you define

`yourproject=Ihre Projekte`

in the translations file `web/lang/tmpl/lang_de.properties` when the language German is chosen.


## HTML Template Language Processor (PLANT Function)

PLANT populates template translations and inserts them into the file `web/lang/tmpl/lang.properties`. It also replaces text fragments in templates located in all subdirectories of `web/html/` as well as in all `columns.cfg` files. You can then copy the extracted translations from `web/lang/tmpl/lang.properties` into the desired language file, e.g., `web/lang/tmpl/lang_es.properties`.

**Note**: This is a destructive process and should only be done initially! Running it a second time will convert extracted values into translation keys, which is not the intended outcome.

### Example

- HTML template: `web/html/users/welcome.html`
- Translated text fragment in the above template: `Hello {$username}, welcome!`

The processor takes the text fragment and inserts it into the file `web/lang/tmp/lang.properties`:

`users.welcome.1=Hello {$username}, welcome!`

this special case with a standard template variable, you must replace `{$username}` with `{0}` (for Java message formatting) and adjust the translation key that replaces the text fragment in the HTML template (`web/html/users/welcome.html`):

`{$l.users.welcome.1,{$userName}}`

As you can see, the translation key uses the prefix `$l.` to distinguish between language translations and the HTML template variables that are replaced by handlers. The template engine first replaces the handler variables, then applies the translation, and finally inserts the replaced handler variables at the positions `{0}`, `{1}`, etc., as defined in the translation file `web/lang/tmp/lang.properties`.

This approach also works with multiple variables by adding additional variables after the translation key, separated by commas. Use sequential placeholders in the translation key, e.g., `{0}, {1}, {2}`, and so on.

The names of the translation keys are automatically generated based on the directory and the template file name, including an ascending number.

If no handler variables are present, you do not need to worry about formatting Java messages with placeholders like `{0}, {1}, {2}`; you can simply translate the text into all desired languages.

## Language-Specific Templates

As mentioned above, you can always override translated templates by adding additional templates to specific language directories. The lookup sequence follows this order:

1. `web/html/<user-lang>/<entity>/<template>.html`
2. `web/html/<default-lang>/<entity>/<template>.html` (first language defined by `web_languages` in `beetroot.cfg`)
3. `web/html/<entity>/<translated-template>.html`

We recommend using templates with translation placeholders (HTML template translations) rather than translating the templates themselves, as direct template translation usually only makes sense for minor patches.


<br>
<br>
Click <a href="../README.md">here</a> to go to the main page.

<p align="right"><a href="#top">&uarr;</a></p>
