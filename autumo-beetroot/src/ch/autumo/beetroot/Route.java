/**
 * Copyright (c) 2022, autumo Ltd. Switzerland, Michael Gasche
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */
package ch.autumo.beetroot;

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
