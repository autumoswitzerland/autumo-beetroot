# Mailing

Mailing supports Eclipse's Jakarta (`jakarta.mail`) as well as Oracle's JavaMail (`javax.mail`) implementation as originally defined by the [JavaMail project](https://javaee.github.io/javamail).

By default, Jakarta is used. This possibly must be switched to JavaMail in certain environments that don't "interact" well with Jakarta. E.g., WebLogic uses Oracle's
original implementation when using their mail-sessions as it should be done in that container. When using JavaMail, such a mail-session must be specified in the beetRoot configuration.

Check the configuration `cfg/beetroot.cfg` for further mailing options. Some of them can be even overwritten by the application "Settings"; check the "Settings" page in the beetRoot Web application.

## Mail Templates

Mail templates are located in the following directories and follow the same lookup-patterns as for HTML templates:

HTML format:

  - web/html/en/email
  - web/html/de/email
  - web/html/email
  - etc.

Text format:

  - txt/html/en/email
  - txt/html/de/email
  - txt/html/email
  - etc.

beetRoot can send both HTML and text emails. Formats are configured with the parameter `mail_formats`.


**NOTE**: Java mail doesn't allow sending HTML with a head nor body-tag, hence you only are able to define HTML templates with tags that would be inside of a the body-tag. It is specification!


<br>
<br>
<a href="../README.md">[Main Page]</a>
