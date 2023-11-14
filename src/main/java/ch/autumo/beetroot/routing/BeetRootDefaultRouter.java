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

import java.util.Arrays;

import ch.autumo.beetroot.handler.Error404Handler;
import ch.autumo.beetroot.handler.ExampleDownloadHandler;
import ch.autumo.beetroot.handler.ExampleUploadHandler;
import ch.autumo.beetroot.handler.HomeHandler;
import ch.autumo.beetroot.handler.NotImplementedHandler;
import ch.autumo.beetroot.handler.tasks.TasksAddHandler;
import ch.autumo.beetroot.handler.tasks.TasksDeleteHandler;
import ch.autumo.beetroot.handler.tasks.TasksEditHandler;
import ch.autumo.beetroot.handler.tasks.TasksIndexHandler;
import ch.autumo.beetroot.handler.tasks.TasksRESTIndexHandler;
import ch.autumo.beetroot.handler.tasks.TasksViewHandler;
import ch.autumo.beetroot.handler.users.ChangeHandler;
import ch.autumo.beetroot.handler.users.LoginHandler;
import ch.autumo.beetroot.handler.users.LogoutHandler;
import ch.autumo.beetroot.handler.users.NewQRCodeHandler;
import ch.autumo.beetroot.handler.users.ResetHandler;
import ch.autumo.beetroot.handler.users.SettingsHandler;
import ch.autumo.beetroot.handler.users.UsersAddHandler;
import ch.autumo.beetroot.handler.users.UsersDeleteHandler;
import ch.autumo.beetroot.handler.users.UsersEditHandler;
import ch.autumo.beetroot.handler.users.UsersIndexHandler;
import ch.autumo.beetroot.handler.users.UsersViewHandler;
import planted.beetroot.handler.properties.PropertiesAddHandler;
import planted.beetroot.handler.properties.PropertiesDeleteHandler;
import planted.beetroot.handler.properties.PropertiesEditHandler;
import planted.beetroot.handler.properties.PropertiesIndexHandler;
import planted.beetroot.handler.properties.PropertiesViewHandler;

/**
 * Default router.
 */
public class BeetRootDefaultRouter implements Router {

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
	public Route[] getDefaultRoutes() {
		
		return new Route[] {
				new Route("/"),
				new Route("/index.html"),
				new Route("/:lang/"),
				new Route("/:lang/index.html")
			};
	}
	
	/**
	 * Get web application routes.
	 * @return routes
	 */
	@Override
	public Route[] getRoutes() {
		
		return new Route[] {
				
			/** Home */
			new Route("/:lang/home", HomeHandler.class, "home"),
			new Route("/:lang/home/index", HomeHandler.class, "home"),
			
			/** Files */
			new Route("/:lang/files/view", ExampleDownloadHandler.class, "files"),
			new Route("/:lang/files/add", ExampleUploadHandler.class, "files"),
			
			/** Tasks */
			new Route("/:lang/tasks", TasksIndexHandler.class, "tasks"),
			new Route("/:lang/tasks/index", TasksIndexHandler.class, "tasks"),
			new Route("/:lang/tasks/view", TasksViewHandler.class, "tasks"),
			new Route("/:lang/tasks/edit", TasksEditHandler.class, "tasks"),
			new Route("/:lang/tasks/add", TasksAddHandler.class, "tasks"),
			new Route("/:lang/tasks/delete", TasksDeleteHandler.class, "tasks"),
			new Route("/:lang/tasks/index.json", TasksRESTIndexHandler.class, "tasks"),
			
			/** Users */
			new Route("/:lang/users", UsersIndexHandler.class, "users"),
			new Route("/:lang/users/index", UsersIndexHandler.class, "users"),
			new Route("/:lang/users/view", UsersViewHandler.class, "users"),
			new Route("/:lang/users/edit", UsersEditHandler.class, "users"),
			new Route("/:lang/users/add", UsersAddHandler.class, "users"),
			new Route("/:lang/users/delete", UsersDeleteHandler.class, "users"),
			new Route("/:lang/users/login", LoginHandler.class, "login"),
			new Route("/:lang/users/logout", LogoutHandler.class, "login"),
			new Route("/:lang/users/reset", ResetHandler.class, "reset"),
			new Route("/:lang/users/change", ChangeHandler.class, "change"),	
			new Route("/:lang/users/settings", SettingsHandler.class, "settings"),
			new Route("/:lang/users/newqrcode", NewQRCodeHandler.class, "users"),
			
			new Route("/:lang/properties", PropertiesIndexHandler.class, "properties"),
		    new Route("/:lang/properties/index", PropertiesIndexHandler.class, "properties"),
		    new Route("/:lang/properties/view", PropertiesViewHandler.class, "properties"),
		    new Route("/:lang/properties/edit", PropertiesEditHandler.class, "properties"),
		    new Route("/:lang/properties/add", PropertiesAddHandler.class, "properties"),
		    new Route("/:lang/properties/delete", PropertiesDeleteHandler.class, "properties")
		};
	}
	
	/**
	 * Merge routes! Use this method within your custom router
	 * and the overwritten {@link #getRoutes()} method to merge
	 * the base routes in this class with your additional custom
	 * routes, otherwise the base routes are not accessible!
	 * 
	 * @param baseRoutes base routes
	 * @param customRoutes custom routes
	 * @return
	 */
	protected static Route[] merge(Route baseRoutes[], Route customRoutes[]) {
		Route result[] = Arrays.copyOf(baseRoutes, baseRoutes.length + customRoutes.length);
	    System.arraycopy(customRoutes, 0, result, baseRoutes.length, customRoutes.length);
	    return result;
	}
	
}
