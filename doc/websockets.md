# WebSockets

beetRoot ships a general WebSocket facility for selected actions. It is not tied
to a single page or feature: actions are registered like normal routes in
`cfg/routing.xml`, are addressed by their action path over a single WebSocket
endpoint and push data to the client whenever they want.

Typical use cases are live views such as the log tail (`/system/log/tail` is
registered out of the box), progress updates or any other server-side
notification that should reach the browser instantly.

<br>

## Server side

### Registration

WebSocket actions are registered inside a `<Package>` of `cfg/routing.xml` with
a `<WebSocket>` element:

```XML
	<Package name="ch.autumo.beetroot.handler.system">
	    <Route path="/:lang/system/log" handler="LogHandler" name="log" />
	    <WebSocket path="/system/log/tail" handler="LogTailWebSocketAction" name="log" />
	</Package>
```

- `path`: The action path the client sends in the JSON message. Action paths
  are **not language specific** (no `:lang` placeholder!); an action that needs
  the user language reads it from the session.
- `handler`: A class that implements
  `ch.autumo.beetroot.server.websocket.WebSocketAction` in the configured package.
- `name`: Action name.

A package may contain only WebSocket actions; packages without `<Route>` entries
are fine.

### Actions

A WebSocket action implements the interface
`ch.autumo.beetroot.server.websocket.WebSocketAction`:

```Java
	public class MyAction implements WebSocketAction {

		@Override
		public void onStart(WebSocketContext context, JSONObject request) throws Exception {
			// Called once when the client starts the action; register
			// listeners or send an initial snapshot with context.sendSnapshot(...)
		}

		@Override
		public void onMessage(WebSocketContext context, JSONObject message) throws Exception {
			// Called for every further message addressed to the running action
		}

		@Override
		public void onStop(WebSocketContext context) {
			// Called when the action is stopped or the connection is closed
		}

		@Override
		public boolean hasAccess(Session userSession) {
			// Optional; default allows every authenticated user
			return true;
		}
	}
```

The `WebSocketContext` is used to push data to the client:

| Method | Purpose |
|---|---|
| `sendSnapshot(JSONArray)` | Pushes `{"type":"snapshot","data":[...]}` |
| `sendEvent(Object)` | Pushes `{"type":"event","data":...}` |
| `sendMessage(JSONObject)` | Pushes a raw JSON message |
| `sendError(String)` | Pushes `{"type":"error","message":"..."}` |
| `getSession()` | The authenticated user session |
| `getPathParams()` | Path parameters of the action path (e.g. `/tasks/:id`) |
| `isOpen()` | `true` while the connection is open |

The out-of-box log tail action is a good reference:
`ch.autumo.beetroot.handler.system.LogTailWebSocketAction`.

<br>

## Protocol

A single endpoint is used (see `websocket_path` below). The client starts an
action by sending its action path:

```JSON
	{"action":"/system/log/tail"}
```

Further messages addressed to the same action are forwarded to it:

```JSON
	{"action":"/system/log/tail","command":"snapshot"}
```

Every pushed message carries the action path, so a client can multiplex several
actions over one connection:

```JSON
	{"action":"/system/log/tail","type":"snapshot","data":["...","..."]}
	{"action":"/system/log/tail","type":"event","data":"..."}
	{"action":"/system/log/tail","type":"error","message":"Access denied."}
```

An action can be stopped with `{"action":"...","command":"stop"}`; it is stopped
automatically when the connection is closed.

<br>

## Security

- The handshake is authenticated with the regular web user session cookie
  (`__SESSION_ID__` by default). A handshake without a valid, logged-in user
  session is answered with `401 Unauthorized`; an expired session is refreshed
  like a normal HTTP request.
- Per-action access is controlled with `hasAccess(Session)` using the existing
  authorization model; no new permission model is introduced.
- The `Origin` header is checked if `websocket_origin_check=yes` (default):
  only the same host as the `Host` header and the hosts listed in
  `websocket_allowed_origins` are accepted, `*` allows every origin.
- The connection limit (`websocket_max_connections`) is enforced with
  `503 Service Unavailable`.

WebSocket runs over the existing web port; with HTTPS enabled it automatically
uses `wss://` as well.

<br>

## Lifecycle and resources

- A heartbeat (`websocket_ping_interval`) detects dead connections; pings are
  sent by the server.
- The socket timeout of upgraded connections is controlled with
  `websocket_timeout` and is independent of `connection_timeout`.
- WebSocket connections do not outlive their user session: on logout or session
  destruction all connections of that session are closed.
- Every connection blocks one thread in the NanoHTTPD thread pool; keep
  `websocket_max_connections` reasonable.

**Note**: The WebSocket facility is not available when beetRoot runs inside a
servlet container (web archive), because the underlying NanoHTTPD interceptor
is not involved in that mode. The configuration is ignored there.

<br>

## Configuration

All keys are located in the `Websockets` section of `cfg/beetroot.cfg`:

| Key | Default | Description |
|---|---|---|
| `websocket_enabled` | `no` | Enable the WebSocket endpoint. |
| `websocket_path` | `/ws` | Endpoint path. |
| `websocket_timeout` | `0` | Socket timeout of upgraded connections in seconds; `0` disables the timeout. Independent of `connection_timeout`. |
| `websocket_ping_interval` | `30` | Server heartbeat in seconds; `0` disables pings. |
| `websocket_max_connections` | `200` | Maximum number of concurrent connections. |
| `websocket_max_message_size` | `1048576` | Maximum message size in bytes; oversized messages are closed. |
| `websocket_origin_check` | `yes` | Check the `Origin` header on the handshake. |
| `websocket_allowed_origins` | (empty) | Comma-separated origin whitelist; `*` allows every origin. |

<br>

## Client side

The generic client module `web/js/beetroot-ws.js` (loaded on every page via
`web/html/blocks/head.html`) connects to the endpoint, starts an action and
dispatches its messages to callbacks:

```HTML
	<script src="/js/beetroot-ws.js" defer></script>
```

```JavaScript
	var live = BeetRootWS.subscribe({
		path: '/ws',                       // endpoint, default '/ws'
		action: '/system/log/tail',        // action path from 'cfg/routing.xml'
		onSnapshot: function (data) {},    // {"type":"snapshot","data":[...]}
		onEvent: function (data) {},       // {"type":"event","data":...}
		onError: function (message) {},    // {"type":"error","message":"..."}
		onStatus: function (state) {},     // see below
		onGiveUp: function () {}           // no more reconnect attempts
	});

	live.send({command: 'snapshot'});      // send to the running action
	live.isOpen();                         // true while the socket is open
	live.close();                          // close and stop reconnects
```

The handle returned by `subscribe()` reconnects automatically with an
exponential backoff. The status callback receives:

| State | Meaning |
|---|---|
| `connecting` | Connection attempt. |
| `open` | Connected; the action was started. |
| `closed` | Connection closed. |
| `reconnecting` | Reconnect attempt scheduled. |
| `giveup` | Reconnect limit reached. |
| `unsupported` | Browser has no WebSocket support. |

The log page `web/html/blocks/script_log.html` uses this module for its live
tail and falls back to periodic reloads if WebSockets are disabled or
unavailable.

<br>
<br>
Click <a href="../README.md">here</a> to go to the main page.

<p align="right"><a href="#top">&uarr;</a></p>
