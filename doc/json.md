# JSON REST API

beetRoot comes with an "out-of-the-box" JSON REST API that serves any entity from the application. The API uses an API key that is defined within the "Settings" by the key
`web.json.api.key`. The API key name itself can be changed in the beetRoot configuration `cfg/beetroot.cfg`.

A REST API call looks like this:

- `http://localhost:8778/tasks/index.json?apiKey=c56950c47cc9f055a17395d6bf94222d&page=1&fetchsize=2&sort=id&direction=desc&page=1`

Example Answer:

```JSON
	{
	    "tasks": [
	        {
	            "id": "5",
	            "name": "Task 5",
	            "active": "false",
	            "laststatus": "false"
	        },
	        {
	            "id": "4",
	            "name": "Task 4",
	            "active": "false",
	            "laststatus": "true"
	        }
	    ],
	    "paginator": {
	        "itemsPerPage": 2,
	        "itemsTotal": 2,
	        "lastPage": 1,
	    }
	}
```

As you can see, you can iterate with the `paginator` object through pages with your REST calls - the same way as you would navigate within an HTML `index` page.

JSON templates can be handled like HTML templates: Put them into the directory `web/html/..`. No user languages are used in any way by using this API. Therefore, you can dismiss the
HTML template language directories and place the template, e.g. for entity `tasks`, directly here `web/html/tasks/index.json`; it looks like this:

```JSON
	{
	    "tasks": [
	        {$data}
	    ],
	    {$paginator}
	}
```

Also, you can create an own `columns.cfg` that is specific for the JSON request in this directory, for example looking like this:

```properties
	list_json.id=is
	list_json.name=name
	list_json.active=active
	list_json.laststatus=laststatus
```

It never has been easier using a REST API!


<br>
<br>
Click <a href="../README.md">here</a> to go to the main page.

<p align="right"><a href="#top">&uarr;</a></p>
