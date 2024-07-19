# CRUD Hooks

You can receive notifications using the [EventHanfler](https://github.com/autumoswitzerland/autumo-beetroot/blob/master/src/main/java/ch/autumo/beetroot/crud/EventHandler.java)
by registering an entity and a listener that is called back. The following callback methods are available:

- [CreateListener](https://github.com/autumoswitzerland/autumo-beetroot/blob/master/src/main/java/ch/autumo/beetroot/crud/CreateListener.java)
  - `afterCreate(Model bean)`
- [UpdateListener](https://github.com/autumoswitzerland/autumo-beetroot/blob/master/src/main/java/ch/autumo/beetroot/crud/UpdateListener.java)
  - `beforeUpdate(Model bean)`
  - `afterUpdate(Model bean)`
- [DeleteListener](https://github.com/autumoswitzerland/autumo-beetroot/blob/master/src/main/java/ch/autumo/beetroot/crud/DeleteListener.java)
  - `beforeDelete(Model bean)`


<br>
<br>
<a href="../README.md">[Main Page]</a>
