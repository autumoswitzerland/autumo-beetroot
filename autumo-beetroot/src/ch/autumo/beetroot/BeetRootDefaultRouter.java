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

import ch.autumo.beetroot.handler.Error404Handler;
import ch.autumo.beetroot.handler.ExampleDownloadHandler;
import ch.autumo.beetroot.handler.ExampleUploadHandler;
import ch.autumo.beetroot.handler.HomeHandler;
import ch.autumo.beetroot.handler.NotImplementedHandler;
import ch.autumo.beetroot.handler.tasks.TasksAddHandler;
import ch.autumo.beetroot.handler.tasks.TasksDeleteHandler;
import ch.autumo.beetroot.handler.tasks.TasksEditHandler;
import ch.autumo.beetroot.handler.tasks.TasksIndexHandler;
import ch.autumo.beetroot.handler.tasks.TasksViewHandler;
import ch.autumo.beetroot.handler.users.ChangeHandler;
import ch.autumo.beetroot.handler.users.LoginHandler;
import ch.autumo.beetroot.handler.users.LogoutHandler;
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
			
			new Route("/:lang/properties", PropertiesIndexHandler.class, "properties"),
		    new Route("/:lang/properties/index", PropertiesIndexHandler.class, "properties"),
		    new Route("/:lang/properties/view", PropertiesViewHandler.class, "properties"),
		    new Route("/:lang/properties/edit", PropertiesEditHandler.class, "properties"),
		    new Route("/:lang/properties/add", PropertiesAddHandler.class, "properties"),
		    new Route("/:lang/properties/delete", PropertiesDeleteHandler.class, "properties")
		};
	}
	
}
