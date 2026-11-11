/**
*
* Naming convention for servlet-aware JavaScript:
* 
* If a JavaScript file's name ends with 'search.js' (e.g. 'livesearch.js',
* 'tasks-search.js'), the template engine will automatically replace the
* beetRoot variable '{$servletName}' at load time.
* 
* Replacement behavior:
* - In a servlet deployment: '{$servletName}' is replaced with the servlet's
*   context name/path.
* - In the beetRoot standalone server: '{$servletName}' is replaced with an
*   empty string.
* 
* Example:
* 
* let fetchUrl = `{$servletName}/tasks/livesearch?q=${encodeURIComponent(query)}`;
* 
* Files not ending with 'search.js' are not processed this way and
* '{$servletName}' will remain unchanged if used.
* 
*/
