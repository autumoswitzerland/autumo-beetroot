/**
 * Dispatcher interfaces for creating your own distributed dispatchers (modules) for the beetRoot client-server framework.
 * <br><br>
 * Basically, you would create a factory that checks whether a call is made from a remote location (e.g. in a web container) 
 * or locally (within the beetRoot server). This can be determined by calling <code>BeetRootConfigurationManager.getInstance().getServletContext();</code>.
 * <br><br>
 * If there is a servlet context, you are calling from a web container and in this case your factory will return a 
 * remote implementation that is part of the distributed module and that uses the {@link ch.autumo.beetroot.server.communication.ClientCommunicator} 
 * or the {@link ch.autumo.beetroot.server.communication.ClientFileTransfer} to call the server.
 * <br><br>
 * If there is NO servlet context (null), the calls are made within the beetRoot server. In this case, the factory provides a 
 * local implementation that is part of the distributed module. The dispatcher implementation itself always calls the 
 * local implementation and the dispatcher itself is automatically involved when remote calls are made via the remote implementation.
 * <br><br>
 * All calls are secured either by SHA3 or by SLL, depending on your configuration. For successful dispatching on the 
 * server side, you must register your dispatcher implementations in the application configuration <code>cfg/beetroot.cfg</code>; 
 * see keys <code>dispatcher_*</code>.
 */
package ch.autumo.beetroot.server.modules;
