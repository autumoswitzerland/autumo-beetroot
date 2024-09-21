# Dispatchers (or Distributed Modules)

Dispatchers are used to create custom distributed modules for the beetRoot client-server framework by using the secure built-in
communication and transport layer to communicate between the beetRoot server and the dedicated web servlet when beetRoot
is running with a dedicated web servlet. In both cases, the dispatchers themselves handle the context and the call logic
locally (in standalone mode) or remotely in the beetRoot server.

The implemnetations is done the following way:

- Basically, you create a factory that checks whether a call is made from a remote location (e.g. in a web container) or locally (within the beetRoot server).
This can be done by calling  <code>BeetRootConfigurationManager.getInstance().getServletContext();</code>.
- If a servlet context is present, you call from a web container, and in this case your factory returns a 
remote implementation that is part of the distributed module and that uses the {@link ch.autumo.beetroot.server.communication.ClientCommunicator} 
or the {@link ch.autumo.beetroot.server.communication.ClientFileTransfer} to call the server.
- If there is NO servlet context (null), the calls are made within the beetRoot server. In this case, the factory provides a 
local implementation that is part of the distributed module. The dispatcher implementation itself always calls the 
dispatcher implementation itself always calls the local implementation, and the dispatcher itself is automatically involved when remote calls 
are made via the remote implementation.
- All calls are secured either by SHA3 or SLL, depending on the configuration. For successful dispatching on the 
server side, you must register your dispatcher implementations in the application configuration <code>cfg/beetroot.cfg</code>; 
see keys <code>dispatcher_*</code>.

To see how a dispatcher is implemented in detail, see the [log dispatcher](https://github.com/autumoswitzerland/autumo-beetroot/blob/master/src/main/java/ch/autumo/beetroot/server/modules/log/).


<br>
<br>
Click <a href="../README.md">here</a> to go to the main page.

<p align="right"><a href="#top">&uarr;</a></p>
