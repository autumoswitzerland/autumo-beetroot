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
 * Router interface.
 */
public interface Router {

	/**
	 * Get not implemented handler class.
	 * return not implemented handler class
	 */
	Class<?> getNotImplementedHandler();

	/**
	 * Get not found handler class.
	 * return not found handler class
	 */
	Class<?> getNotFoundHandler();

	/**
	 * Return the default routes. Default routes are the routes
	 * for the first page that should be shown, if someone
	 * enters URLs suc as '/' or '/index.html', etc.
	 * They should have have priority less than the default
	 * priority 100.
	 * 
	 * @return default routes.
	 */
	Route[] getDefaultRoutes();

	/**
	 * Get web application routes.
	 * @return routes
	 */
	Route[] getRoutes();

}