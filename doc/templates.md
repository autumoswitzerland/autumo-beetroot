# Default HTML Templates

The following default HTML templates are present, which can be customized if necessary:

- `web/html/blocks/*.html`:
	- Defines the layout of the page with its general elements such as header, header bar, menu, admin menu, language menu, message block (notifications) and script section (Javascript).
	- They can be copied to other language directories (e.g. `web/html/blocks/en/*.html`) if they are to be language-specific, which is not necessary in most cases. They also
serve as fallback templates if the user has requested a language that is not found or the web app has not yet been translated into this language.
	- **NOTE**: Here, as with the generated HTML templates, the lookup algorithm is:
		a) First search for templates in the directory of the desired language (2-letter ISO code)
		b) If not found, try the default language; this is the one that is defined first in the configuration, see parameter `web_languages`.
		c) If still not found, use the templates by omitting the language code or the language directory in the `web/html` directory structure.

E.g.: the English translation for `index.html` and the entity `tasks` is here: web/html/en/tasks/index.html”. If this resource is not available, then `web/html/tasks/index.html` is looked up. The same applies to the resource `colums.cfg`.

**NOTE**: Valid for templates and all other HTML files that are added to the structure of the `web/html` directory:

- The relative URL (without host, port and servlet name) requested by the web app user is not translated 1 to 1 by the directory structure, but by **routing**; see [Routing](routing.md).

The following template variables are always parsed and you can use them as often as you like:

- `{$lang}` : User's language, 2-ISO-code
- `{$user}` : User login name
- `{$userfull}` : Full user name (first and last name if available, otherwise login name)
- `{$userlink}` : Link to user profile
- `{$title}` : Page title (within add, edit, view and list)
- `{$id}` : Obfuscated object id (within add, edit and view)
- `{$dbid}` : Real database id (within add, edit and view); cannot be used to access entities per URL!
- `{$csrfToken}` : CSRF token
- `{$theme}` : The currently default chosen style theme, e.g. `default`
- `{$antitheme}` : The default style theme to which can be switched to, e.g. `dark`
- `{$displayName}` : Display name taken from the bean entity (not in `index.html`)


<br>
<br>
Click <a href="../README.md">here</a> to go to the main page.

<p align="right"><a href="#top">&uarr;</a></p>
