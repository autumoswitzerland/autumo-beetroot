/**
 *
 * If your Java Script file name ends with 'search.js', you can use the
 * beetRoot variable '{$servletName}'.
 * 
 * Example:
 * 
 * - Script : 'livesearch.js' or 'tasks-search.js'
 * - Code   : let fetchUrl = 
 *     `{$servletName}/tasks/livesearch?q=${encodeURIComponent(query)}`;
 *
 * The variable '{$servletName}' is either replaced by the servlet name 
 * or by an empty character string if the beetRoot standalone server is 
 * used.
 * 
 */
