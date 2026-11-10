# CRUD Hooks

You can receive notifications using the [EventHandler](https://github.com/autumoswitzerland/autumo-beetroot/blob/master/src/main/java/ch/autumo/beetroot/crud/EventHandler.java)
by registering an entity and a listener that is called back. The following callback methods are available:

- [CreateListener](https://github.com/autumoswitzerland/autumo-beetroot/blob/master/src/main/java/ch/autumo/beetroot/crud/CreateListener.java)
  - `afterCreate(Model bean)`
- [UpdateListener](https://github.com/autumoswitzerland/autumo-beetroot/blob/master/src/main/java/ch/autumo/beetroot/crud/UpdateListener.java)
  - `beforeUpdate(Model bean)` — return `true` to abort the update, otherwise `false`
  - `afterUpdate(Model bean)`
- [DeleteListener](https://github.com/autumoswitzerland/autumo-beetroot/blob/master/src/main/java/ch/autumo/beetroot/crud/DeleteListener.java)
  - `beforeDelete(Model bean)` — return `true` to abort the deletion, otherwise `false`


<br>
<br>
Click <a href="../README.md">here</a> to go to the main page.

<p align="right"><a href="#top">&uarr;</a></p>
