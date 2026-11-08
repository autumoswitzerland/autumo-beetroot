/**
 *
 * Copyright (c) 2023 autumo Ltd. Switzerland, Michael Gasche
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ch.autumo.beetroot.routing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ch.autumo.beetroot.BeetRootConfigurationManager;
import ch.autumo.beetroot.handler.Error404Handler;
import ch.autumo.beetroot.handler.Handler;
import ch.autumo.beetroot.handler.NotImplementedHandler;
import ch.autumo.beetroot.server.websocket.WebSocketAction;

/**
 * Default router.
 */
public class BeetRootDefaultRouter implements Router {

	/**
	 * Logger.
	 */
	protected static final Logger LOG = LoggerFactory.getLogger(BeetRootDefaultRouter.class.getName());

	/**
	 * Default constructor.
	 */
	public BeetRootDefaultRouter() {
	}
	
	/**
	 * Get not implemented handler class.
	 * return not implemented handler class
	 */
	public Class<?> getNotImplementedHandler() {
		return NotImplementedHandler.class;
	}

	/**
	 * Get not found handler class.
	 * return not found handler class
	 */
	public Class<?> getNotFoundHandler() {
		return Error404Handler.class;
	}

	/**
	 * Return the default routes. Default routes are the routes
	 * for the first page that should be shown, if someone
	 * enters URLs suc as '/' or '/index.html', etc.
	 * They should have have priority less than the default
	 * priority 100.
	 *
	 * @return default routes.
	 */
	@Override
	public List<Route> getDefaultRoutes() {
		final List<Route> routes =  new ArrayList<Route>();
		routes.add(new Route("/"));
		routes.add(new Route("/index.html"));
		routes.add(new Route("/:lang/"));
		routes.add(new Route("/:lang/index.html"));
		return routes;
	}

	/**
	 * Get web application routes.
	 * @return routes
	 */
	@Override
	public List<Route> getRoutes() {

		final List<Route> routes =  new ArrayList<Route>();

		final Document xmlDoc = this.loadRoutingDocument();

		// Packages
		NodeList packages = xmlDoc.getElementsByTagName("Package");
		if (packages.getLength() < 1)
			throw new RuntimeException("At least one package must be defined!");

		for (int i = 0; i < packages.getLength(); i++) {

			final Node p = packages.item(i);
			final Element pe = (Element) p;
			String packageName = pe.getAttribute("name");
			if (packageName == null || packageName.length() == 0) {
				LOG.error("Package name is missing for package at index '" + i + "'!");
				throw new RuntimeException("package name is missing for package at index '" + i + "'!");
			}

			// Routes; a package may contain only WebSocket actions.
			final NodeList rts = pe.getElementsByTagName("Route");
			if (rts.getLength() < 1) {
				continue;
			}

			for (int j = 0; j < rts.getLength(); j++) {

				final Node r = rts.item(j);
				final Element re = (Element) r;
				String currPath = re.getAttribute("path");
				if (currPath == null || currPath.length() == 0) {
					LOG.error("Package('"+i+"')-Router('"+j+"'): path is missing!");
					throw new RuntimeException("Package('"+i+"')-Router('"+j+"'): path is missing!");
				}
				String currHandler = re.getAttribute("handler");
				if (currHandler == null || currHandler.length() == 0) {
					LOG.error("Package('"+i+"')-Router('"+j+"'): handler is missing!");
					throw new RuntimeException("Package('"+i+"')-Router('"+j+"'): handler is missing!");
				}
				String currName = re.getAttribute("name");
				if (currName == null || currName.length() == 0) {
					LOG.error("Package('"+i+"')-Router('"+j+"'): name is missing!");
					throw new RuntimeException("Package('"+i+"')-Router('"+j+"'): name is missing!");
				}

				currPath = currPath.trim();
				currHandler = currHandler.trim();
				currName = currName.trim().toLowerCase();

				if (packageName.endsWith("."))
					packageName = packageName.substring(0, packageName.length()-1);
				packageName = packageName.trim();

				final String className = packageName + "." + currHandler;
				//Class<Handler> handlerClass = null;
				Class<Handler> handlerClass = null;
				try {
					@SuppressWarnings("unchecked")
					final Class<Handler> clz = (Class<Handler>) Class.forName(className);
					handlerClass = clz;
				} catch (ClassNotFoundException e) {
					LOG.error("Class '"+className+"' not found or isn't a handler!", e);
					throw new RuntimeException("Class '"+className+"' not found or isn't a handler!", e);
				}

				routes.add(new Route(currPath, handlerClass, currName));
			}
		}

		return routes;
	}

	/**
	 * Read the routing configuration.
	 *
	 * @return routing document
	 */
	private Document loadRoutingDocument() {
		final String path = BeetRootConfigurationManager.getInstance().getFullConfigBasePath();
		try {
			return BeetRootConfigurationManager.getInstance()
					.getXMLModuleConfigWithFullPath(path + "routing.xml", "Router");
		} catch (Exception e) {
			throw new RuntimeException("Couldn' read routing configuration 'cfg/routing.xml'!", e);
		}
	}

	/**
	 * Get the WebSocket action routes from {@code cfg/routing.xml}.
	 *
	 * <p>
	 * WebSocket actions are registered inside a {@code <Package>} with
	 * {@code <WebSocket path="..." handler="..." name="..." />} elements.
	 * </p>
	 *
	 * @return WebSocket action routes
	 */
	@Override
	public List<WebSocketActionRoute> getWebSocketActionRoutes() {

		final List<WebSocketActionRoute> routes = new ArrayList<WebSocketActionRoute>();

		final Document xmlDoc = this.loadRoutingDocument();
		final NodeList packages = xmlDoc.getElementsByTagName("Package");

		for (int i = 0; i < packages.getLength(); i++) {

			final Element pe = (Element) packages.item(i);
			String packageName = pe.getAttribute("name");
			if (packageName == null || packageName.length() == 0) {
				continue;
			}

			final NodeList websockets = pe.getElementsByTagName("WebSocket");
			for (int j = 0; j < websockets.getLength(); j++) {

				final Element we = (Element) websockets.item(j);
				String currPath = we.getAttribute("path");
				String currHandler = we.getAttribute("handler");
				String currName = we.getAttribute("name");

				if (currPath == null || currPath.trim().length() == 0) {
					LOG.error("Package('" + i + "')-WebSocket('" + j + "'): path is missing!");
					throw new RuntimeException("Package('" + i + "')-WebSocket('" + j + "'): path is missing!");
				}
				if (currPath.contains(":lang")) {
					LOG.error("Package('" + i + "')-WebSocket('" + j + "'): ':lang' is not supported in WebSocket action paths!");
					throw new RuntimeException("Package('" + i + "')-WebSocket('" + j + "'): ':lang' is not supported in WebSocket action paths!");
				}
				if (currHandler == null || currHandler.trim().length() == 0) {
					LOG.error("Package('" + i + "')-WebSocket('" + j + "'): handler is missing!");
					throw new RuntimeException("Package('" + i + "')-WebSocket('" + j + "'): handler is missing!");
				}
				if (currName == null || currName.trim().length() == 0) {
					LOG.error("Package('" + i + "')-WebSocket('" + j + "'): name is missing!");
					throw new RuntimeException("Package('" + i + "')-WebSocket('" + j + "'): name is missing!");
				}

				currPath = currPath.trim();
				currHandler = currHandler.trim();
				currName = currName.trim().toLowerCase();

				if (packageName.endsWith(".")) {
					packageName = packageName.substring(0, packageName.length() - 1);
				}
				packageName = packageName.trim();

				final String className = packageName + "." + currHandler;
				try {
					final Class<?> clz = Class.forName(className);
					if (!WebSocketAction.class.isAssignableFrom(clz)) {
						throw new RuntimeException("Class '" + className + "' doesn't implement WebSocketAction!");
					}
					routes.add(new WebSocketActionRoute(currPath, clz, currName));
				} catch (ClassNotFoundException e) {
					LOG.error("Class '" + className + "' not found or isn't a WebSocket action!", e);
					throw new RuntimeException("Class '" + className + "' not found or isn't a WebSocket action!", e);
				}
			}
		}

		return routes;
	}

	/**
	 * Merge routes! Use this method within your custom router
	 * and the overwritten {@link #getRoutes()} method to merge
	 * the base routes in this class with your additional custom
	 * routes, otherwise the base routes are not accessible!
	 *
	 * @param baseRoutes base routes
	 * @param customRoutes custom routes
	 * @return merged routes
	 */
	protected static Route[] merge(Route baseRoutes[], Route customRoutes[]) {
		Route result[] = Arrays.copyOf(baseRoutes, baseRoutes.length + customRoutes.length);
	    System.arraycopy(customRoutes, 0, result, baseRoutes.length, customRoutes.length);
	    return result;
	}

	/**
	 * Merge routes! Use this method within your custom router
	 * and the overwritten {@link #getRoutes()} method to merge
	 * the base routes in this class with your additional custom
	 * routes, otherwise the base routes are not accessible!
	 *
	 * @param baseRoutes base routes
	 * @param customRoutes custom routes
	 * @return merged routes
	 */
	protected static List<Route> merge(List<Route> baseRoutes, List<Route> customRoutes) {
		baseRoutes.addAll(customRoutes);
		return baseRoutes;
	}

}
