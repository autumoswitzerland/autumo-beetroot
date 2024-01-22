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

/**
 * Route.
 */
public class Route {

	public static final int PRIORITY = 100;
	public static final int DEFAULT_PRIORITY = 50;
	
	private String route = null;
	private int priority = DEFAULT_PRIORITY; //default
	private Class<?> handler = null;
	private Object initParameter[] = null;

	/**
	 * Create default route. Constructor should be used only for
	 * default routes. Handler and init parameters are used from
	 * configuration.
	 * 
	 * @param route route, e.g. '/:lang/tasks/index'.
	 */
	public Route(String route) {
		super();
		this.route = route;
		this.priority = DEFAULT_PRIORITY;
	}
	
	/**
	 * Create route. Should be used for specific routes that 
	 * address specific pages. 
	 * 
	 * @param route route, e.g. '/:lang/tasks/index'.
	 * @param handler handler class
	 * @param entityName entity name
	 */
	public Route(String route, Class<?> handler, String entityName) {
		super();
		this.route = route;
		this.priority = PRIORITY;
		this.handler = handler;
		this.initParameter = new String[] {entityName};
	}
	
	/**
	 * Create route. Should be used for specific routes that 
	 * address specific pages. 
	 * 
	 * @param route route, e.g. '/:lang/tasks/index'.
	 * @param handler handler class
	 * @param initParameter init parameter, first one must be entity name
	 */
	public Route(String route, Class<?> handler, Object... initParameter) {
		super();
		this.route = route;
		this.priority = PRIORITY;
		this.handler = handler;
		this.initParameter = initParameter;
	}

	/**
	 * Create route. For specific parameters.
	 * 
	 * @param route route, e.g. '/:lang/tasks/index'.
	 * @param priority lookup priority, default is 100
	 * @param handler handler
	 * @param initParameter init parameter, first one must be entity name
	 */
	public Route(String route, int priority, Class<?> handler, Object... initParameter) {
		super();
		this.route = route;
		this.priority = priority;
		this.handler = handler;
		this.initParameter = initParameter;
	}
	
	public String getRoute() {
		return route;
	}
	public int getPriority() {
		return priority;
	}
	public void setPriority(int priority) {
		this.priority = priority;
	}
	public Class<?> getHandler() {
		return handler;
	}
	public Object[] getInitParameter() {
		return initParameter;
	}
	
}
