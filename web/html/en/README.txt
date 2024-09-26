
Use this directory if you want to overwrite HTML templates. To do this, set
up the same directory structure as under "web/html/" and name the HTML templates
exactly the same as under "web/html/" and their directory structure.
directory structure.

This is practical if, apart from the standard language translation, you want to
design the HTML templates differently for different languages.

beetRoot first loads HTML templates, depending on the language selected by the
user, from such language directories and if such a directory is not available
from the base directory "web/html/".
