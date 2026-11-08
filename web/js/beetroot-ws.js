/**
 *
 * Copyright (c) 2026 autumo Ltd. Switzerland, Michael Gasche
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

/**
 * beetRoot WebSocket action client.
 *
 * This is the generic client counterpart of the beetRoot WebSocket facility.
 * It connects to the configured endpoint, starts a server-side action by its
 * action path and dispatches snapshots and events to the given callbacks.
 * Reconnects are handled automatically with an exponential backoff.
 *
 * The server protocol is JSON based:
 *
 *   -> {"action":"/system/log/tail"}
 *   <- {"action":"/system/log/tail","type":"snapshot","data":[...]}
 *   <- {"action":"/system/log/tail","type":"event","data":...}
 *   <- {"action":"/system/log/tail","type":"error","message":"..."}
 *
 * Usage:
 *
 *   var live = BeetRootWS.subscribe({
 *       path: '/ws',
 *       action: '/system/log/tail',
 *       onSnapshot: function (data) {},
 *       onEvent: function (data) {},
 *       onError: function (message) {},
 *       onStatus: function (state) {},
 *       onGiveUp: function () {}
 *   });
 *
 *   live.send({command: 'snapshot'});   // sends the action and a command
 *   live.isOpen();                      // true while the socket is open
 *   live.close();                       // closes the socket, stops reconnects
 *
 * Status values passed to onStatus: 'connecting', 'open', 'closed',
 * 'reconnecting', 'giveup' and 'unsupported'.
 */
(function (global) {
	"use strict";

	var DEFAULTS = {
		path: "/ws",
		action: null,
		maxReconnectAttempts: 5,
		baseReconnectDelay: 2000,
		maxReconnectDelay: 30000
	};

	function noop() {
	}

	function Connection(options) {
		var key;
		this.settings = {};
		for (key in DEFAULTS) {
			if (DEFAULTS.hasOwnProperty(key)) {
				this.settings[key] = DEFAULTS[key];
			}
		}
		for (key in options) {
			if (options.hasOwnProperty(key)) {
				this.settings[key] = options[key];
			}
		}

		this.socket = null;
		this.closed = false;
		this.givenUp = false;
		this.attempts = 0;
		this.reconnectTimer = null;

		this.onSnapshot = this.settings.onSnapshot || noop;
		this.onEvent = this.settings.onEvent || noop;
		this.onError = this.settings.onError || noop;
		this.onMessage = this.settings.onMessage || noop;
		this.onStatus = this.settings.onStatus || noop;
		this.onOpen = this.settings.onOpen || noop;
		this.onClose = this.settings.onClose || noop;
		this.onGiveUp = this.settings.onGiveUp || noop;
	}

	Connection.prototype.url = function () {
		var protocol = global.location.protocol === "https:" ? "wss:" : "ws:";
		var path = this.settings.path || DEFAULTS.path;
		if (path.charAt(0) !== "/") {
			path = "/" + path;
		}
		return protocol + "//" + global.location.host + path;
	};

	Connection.prototype.open = function () {
		var self = this;
		if (this.closed || this.givenUp) {
			return;
		}
		if (!global.BeetRootWS.isSupported()) {
			this.onStatus("unsupported");
			this.giveUp();
			return;
		}

		this.onStatus("connecting");
		try {
			this.socket = new WebSocket(this.url());
		} catch (e) {
			this.socket = null;
			this.onStatus("closed");
			this.scheduleReconnect();
			return;
		}

		this.socket.onopen = function () {
			self.attempts = 0;
			self.onStatus("open");
			if (self.settings.action) {
				self.send({});
			}
			self.onOpen(self);
		};

		this.socket.onmessage = function (event) {
			var message;
			try {
				message = JSON.parse(event.data);
			} catch (e) {
				return;
			}
			if (message === null || typeof message !== "object") {
				return;
			}
			if (self.settings.action && message.action && message.action !== self.settings.action) {
				// Another action on the same connection; ignore it.
				return;
			}
			switch (message.type) {
				case "snapshot":
					self.onSnapshot(message.data);
					break;
				case "event":
					self.onEvent(message.data);
					break;
				case "error":
					self.onError(message.message || "Unknown error.");
					break;
				default:
					self.onMessage(message);
			}
		};

		this.socket.onclose = function () {
			self.socket = null;
			if (self.closed) {
				return;
			}
			self.onStatus("closed");
			self.onClose();
			self.scheduleReconnect();
		};

		this.socket.onerror = function () {
			if (self.socket !== null) {
				try {
					self.socket.close();
				} catch (e) {
				}
			}
		};
	};

	Connection.prototype.scheduleReconnect = function () {
		var self = this;
		if (this.closed || this.givenUp || this.reconnectTimer !== null) {
			return;
		}
		if (this.attempts >= this.settings.maxReconnectAttempts) {
			this.giveUp();
			return;
		}
		this.attempts++;
		var delay = Math.min(
			this.settings.baseReconnectDelay * Math.pow(1.5, this.attempts - 1),
			this.settings.maxReconnectDelay
		);
		this.onStatus("reconnecting");
		this.reconnectTimer = global.setTimeout(function () {
			self.reconnectTimer = null;
			self.open();
		}, delay);
	};

	Connection.prototype.giveUp = function () {
		if (this.givenUp) {
			return;
		}
		this.givenUp = true;
		this.onStatus("giveup");
		this.onGiveUp();
	};

	Connection.prototype.send = function (message) {
		if (this.socket === null || this.socket.readyState !== WebSocket.OPEN) {
			return false;
		}
		var payload = {};
		var key;
		if (this.settings.action) {
			payload.action = this.settings.action;
		}
		if (message) {
			for (key in message) {
				if (message.hasOwnProperty(key)) {
					payload[key] = message[key];
				}
			}
		}
		this.socket.send(JSON.stringify(payload));
		return true;
	};

	Connection.prototype.isOpen = function () {
		return this.socket !== null && this.socket.readyState === WebSocket.OPEN;
	};

	Connection.prototype.close = function () {
		this.closed = true;
		if (this.reconnectTimer !== null) {
			global.clearTimeout(this.reconnectTimer);
			this.reconnectTimer = null;
		}
		if (this.socket !== null) {
			var socket = this.socket;
			this.socket = null;
			socket.onopen = null;
			socket.onmessage = null;
			socket.onclose = null;
			socket.onerror = null;
			try {
				socket.close();
			} catch (e) {
			}
		}
		this.onStatus("closed");
	};

	global.BeetRootWS = {

		/**
		 * Is the WebSocket API available in this browser?
		 */
		isSupported: function () {
			return "WebSocket" in global;
		},

		/**
		 * Subscribe to a server-side WebSocket action. This opens a dedicated
		 * connection for the action and returns a handle to control it.
		 */
		subscribe: function (options) {
			var connection = new Connection(options || {});
			connection.open();
			return connection;
		}
	};

})(window);
