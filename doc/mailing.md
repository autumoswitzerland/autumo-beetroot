# Mailing

Mailing supports Eclipse's Jakarta (`jakarta.mail`) as well as Oracle's JavaMail (`javax.mail`) implementation, as originally defined by the [JavaMail project](https://javaee.github.io/javamail).

By default, Jakarta is used; some environments or application servers may require the Javax implementation instead.

You can also use a JNDI mail session. To do so, configure `mail_session_name` in `cfg/beetroot.cfg`. This value can be overridden via a database property: add a JNDI name using the property key `mail.session.name` to the `properties` database table, or configure it through the web app settings.

## Mail Templates

As with HTML templates (see [Translations](translations.md)), you can use different templates in the language-specific directories, or use translation tags within email templates—which is the recommended approach. Both methods can be used simultaneously.

Mail templates are located in the following directories and follow the same lookup order as HTML templates:

**HTML format:**

1. `web/html/<user-lang>/email/<template>.html`
2. `web/html/<default-lang>/email/<template>.html` (first language defined by `web_languages` in `beetroot.cfg`)
3. `web/html/email/<translated-template>.html`

**Text format:**

1. `web/txt/<user-lang>/email/<template>.txt`
2. `web/txt/<default-lang>/email/<template>.txt` (first language defined by `web_languages` in `beetroot.cfg`)
3. `web/txt/email/<translated-template>.txt`

beetRoot can send both HTML and text emails. Formats are configured with the parameter `mail_formats`. We recommend using templates with translation placeholders rather than translating the templates themselves; translating the templates usually only makes sense for minor translation patches.

**NOTE:** JavaMail does not allow sending HTML with a `<head>` or `<body>` tag. Therefore, HTML templates should only include tags that would normally appear inside the `<body>` tag. This is a specification requirement.

<br>
<br>
Click <a href="../README.md">here</a> to go to the main page.

<p align="right"><a href="#top">&uarr;</a></p>
