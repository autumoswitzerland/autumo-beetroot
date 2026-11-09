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
package ch.autumo.beetroot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import ch.autumo.beetroot.handler.users.User;
import ch.autumo.beetroot.logging.LogEventAppender;
import ch.autumo.beetroot.utils.Helper;

/**
 * Integration test for the general WebSocket facility.
 *
 * <p>
 * The test starts a real beetRoot web server, performs the WebSocket handshake
 * over a raw socket and exercises the registered log-tail action. It also
 * verifies that a handshake without a valid user session is rejected.
 * </p>
 */
public class WebSocketIntegrationTest {

	private static final String SESSION_TOKEN = "TESTWEBSOCKETSESSIONTOKEN";
	private static final String SESSION_COOKIE = "__SESSION_ID__";
	private static final String ACTION = "/system/log/tail";

	private static BeetRootWebServer server;
	private static int port;
	private static String host;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {

		// This test runs against the regular application configuration
		// ('cfg/beetroot.cfg'), like the real application does.
		final BeetRootConfigurationManager cfg = BeetRootConfigurationManager.getInstance();
		cfg.initialize("cfg/beetroot.cfg");

		// Test-scope settings, so the test doesn't depend on configuration
		// values of the developer's setup: WebSockets are activated and the
		// session store is separated from the regular one.
		cfg.updateProperty("websocket_enabled", Constants.YES);
		cfg.updateProperty("ws_user_sessions", ".beetroot-ws-itest-sessions");

		BeetRootDatabaseManager.getInstance().initialize();

		// Initialise the session manager with the test configuration.
		SessionManager.getInstance();

		server = new BeetRootWebServer(0);
		server.start();

		port = server.getListeningPort();
		assertTrue("Test server didn't start!", port > 0);
		host = "localhost:" + port;

		// Add an authenticated session after the server loaded its session
		// store; loading replaces the in-memory session map.
		addSession(SESSION_TOKEN);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		if (server != null) {
			server.stop();
		}
		removeSession(SESSION_TOKEN);
		// Remove the test session store again.
		final File sessionFile = new File(Helper.USER_HOME + Helper.FILE_SEPARATOR + ".beetroot-ws-itest-sessions");
		if (sessionFile.exists()) {
			sessionFile.delete();
		}
	}

	@Test
	public void testHandshakeWithoutSessionIsUnauthorized() throws Exception {
		try (Client client = new Client(host, null)) {
			final int status = client.handshake("/ws");
			assertEquals("A WebSocket handshake without a user session must be rejected.", 401, status);
		}
	}

	@Test
	public void testHandshakeAndLogTailAction() throws Exception {
		try (Client client = new Client(host, SESSION_TOKEN)) {
			final int status = client.handshake("/ws");
			assertEquals("WebSocket handshake must switch protocols.", 101, status);

			// Start the configured log-tail action.
			client.sendText("{\"action\":\"" + ACTION + "\"}");

			// The action pushes a snapshot immediately.
			final JSONObject message = client.readJsonMessage();
			assertNotNull("No message received from the WebSocket action.", message);
			assertEquals(ACTION, message.optString("action"));
			assertEquals("snapshot", message.optString("type"));
			assertTrue("Snapshot data must be a JSON array.", message.has("data"));

			// Push a log event through the appender and verify live delivery.
			final LogEvent event = Log4jLogEvent.newBuilder()
					.setLoggerName("ch.autumo.beetroot.WebSocketIntegrationTest")
					.setLevel(Level.INFO)
					.setMessage(new SimpleMessage("WS_LIVE_TEST_EVENT"))
					.setTimeMillis(System.currentTimeMillis())
					.build();
			LogEventAppender.createAppender("ws-integration-test").append(event);

			JSONObject pushed = null;
			for (int i = 0; i < 10; i++) {
				final JSONObject candidate = client.readJsonMessage();
				if ("event".equals(candidate.optString("type"))
						&& candidate.toString().contains("WS_LIVE_TEST_EVENT")) {
					pushed = candidate;
					break;
				}
			}
			assertNotNull("Live log event wasn't pushed to the WebSocket client.", pushed);
			assertEquals(ACTION, pushed.optString("action"));

			client.sendClose();
		}
	}

	@Test
	public void testLogPageServesWebSocketClient() throws Exception {
		try (Client client = new Client(host, SESSION_TOKEN)) {
			final Client.HttpResponse response = client.get("/en/system/log");
			assertEquals("Log page must be served.", 200, response.status);
			assertTrue("WebSocket client script must be included.", response.body.contains("/js/websockets.js"));
			assertTrue("Live status element must be rendered.", response.body.contains("id=\"live_status\""));
			assertTrue("Log page must subscribe to the WebSocket action.", response.body.contains("BeetRootWS.subscribe"));
			assertFalse("WebSocket configuration variable wasn't replaced.", response.body.contains("{$websocketEnabled}"));
			assertFalse("WebSocket path variable wasn't replaced.", response.body.contains("{$websocketPath}"));
			assertFalse("WebSocket action variable wasn't replaced.", response.body.contains("{$websocketActionPath}"));
			assertTrue("WebSocket action path must be rendered.", response.body.contains("/system/log/tail"));
		}
	}

	private static void addSession(String token) throws Exception {
		// Simulate a logged-in user exactly like the real login flow does:
		// 'setUserData' also sets the user DB id, the roles and the 2FA flag.
		// A synthetic session without a matching DB row would make the server
		// fail while loading the user settings.
		final List<Model> users = Model.listAll(User.class);
		assertTrue("No user found in the configured database!", users != null && !users.isEmpty());

		final User user = (User) users.get(0);
		final Session session = new Session(token);
		// The real login stores roles lower-case in the session; see
		// BeetRootWebServer 'dbRoles.toLowerCase()'.
		session.setUserData(user, "administrator", "");

		sessions().put(token, session);
	}

	private static void removeSession(String token) throws Exception {
		sessions().remove(token);
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Session> sessions() throws Exception {
		final Field field = SessionManager.class.getDeclaredField("sessions");
		field.setAccessible(true);
		return (Map<String, Session>) field.get(null);
	}

	/**
	 * Minimal raw WebSocket client used by the integration test.
	 */
	private static final class Client implements AutoCloseable {

		private final Socket socket;
		private final OutputStream out;
		private final DataInputStream in;
		private final String sessionToken;
		private final Random random = new Random();

		private Client(String host, String sessionToken) throws IOException {
			this.sessionToken = sessionToken;
			final int colon = host.lastIndexOf(':');
			final String hostName = host.substring(0, colon);
			final int port = Integer.parseInt(host.substring(colon + 1));
			this.socket = new Socket(hostName, port);
			this.socket.setSoTimeout(10000);
			this.out = socket.getOutputStream();
			this.in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
		}

		private int handshake(String path) throws IOException {
			final String key = "dGhlIHNhbXBsZSBub25jZQ==";
			final StringBuilder request = new StringBuilder();
			request.append("GET ").append(path).append(" HTTP/1.1\r\n");
			request.append("Host: ").append(hostHeader()).append("\r\n");
			request.append("Upgrade: websocket\r\n");
			request.append("Connection: Upgrade\r\n");
			request.append("Sec-WebSocket-Key: ").append(key).append("\r\n");
			request.append("Sec-WebSocket-Version: 13\r\n");
			request.append("Origin: http://").append(hostHeader()).append("\r\n");
			if (sessionToken != null) {
				request.append("Cookie: ").append(SESSION_COOKIE).append('=').append(sessionToken).append("\r\n");
			}
			request.append("\r\n");

			out.write(request.toString().getBytes(StandardCharsets.ISO_8859_1));
			out.flush();

			final Map<String, String> headers = new LinkedHashMap<String, String>();
			final String statusLine = readLine();
			final int status = parseStatus(statusLine);
			String line;
			while ((line = readLine()) != null && line.length() > 0) {
				final int colon = line.indexOf(':');
				if (colon > 0) {
					headers.put(line.substring(0, colon).trim().toLowerCase(), line.substring(colon + 1).trim());
				}
			}
			if (status == 101) {
				assertEquals("websocket", headers.get("upgrade").toLowerCase());
				assertNotNull("WebSocket accept header is missing.", headers.get("sec-websocket-accept"));
			}
			return status;
		}

		private HttpResponse get(String path) throws IOException {
			final StringBuilder request = new StringBuilder();
			request.append("GET ").append(path).append(" HTTP/1.1\r\n");
			request.append("Host: ").append(hostHeader()).append("\r\n");
			request.append("Connection: close\r\n");
			if (sessionToken != null) {
				request.append("Cookie: ").append(SESSION_COOKIE).append('=').append(sessionToken).append("\r\n");
			}
			request.append("\r\n");
			out.write(request.toString().getBytes(StandardCharsets.ISO_8859_1));
			out.flush();

			final HttpResponse response = new HttpResponse();
			response.status = parseStatus(readLine());

			final Map<String, String> headers = new LinkedHashMap<String, String>();
			String line;
			while ((line = readLine()) != null && line.length() > 0) {
				final int colon = line.indexOf(':');
				if (colon > 0) {
					headers.put(line.substring(0, colon).trim().toLowerCase(), line.substring(colon + 1).trim());
				}
			}

			final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			final String contentLength = headers.get("content-length");
			if (contentLength != null) {
				final byte[] data = new byte[Integer.parseInt(contentLength)];
				in.readFully(data);
				buffer.write(data, 0, data.length);
			} else {
				int b;
				while ((b = in.read()) != -1) {
					buffer.write(b);
				}
			}
			response.body = new String(buffer.toByteArray(), StandardCharsets.UTF_8);
			return response;
		}

		private String hostHeader() {
			final int localPort = socket.getPort();
			return "localhost:" + localPort;
		}

		private static int parseStatus(String statusLine) throws IOException {
			if (statusLine == null) {
				throw new EOFException("Server closed the connection during the WebSocket handshake.");
			}
			final String[] parts = statusLine.split(" ");
			if (parts.length < 2) {
				throw new IOException("Invalid HTTP status line: " + statusLine);
			}
			return Integer.parseInt(parts[1]);
		}

		private String readLine() throws IOException {
			final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			int c;
			while ((c = in.read()) != -1) {
				if (c == '\n') {
					break;
				}
				if (c != '\r') {
					buffer.write(c);
				}
			}
			if (c == -1 && buffer.size() == 0) {
				return null;
			}
			return new String(buffer.toByteArray(), StandardCharsets.ISO_8859_1);
		}

		private void sendText(String text) throws IOException {
			sendFrame(0x1, text.getBytes(StandardCharsets.UTF_8));
		}

		private void sendClose() throws IOException {
			sendFrame(0x8, new byte[] { 0x03, (byte) 0xE8 });
		}

		private void sendFrame(int opcode, byte[] payload) throws IOException {
			final ByteArrayOutputStream frame = new ByteArrayOutputStream();
			frame.write(0x80 | opcode);
			if (payload.length <= 125) {
				frame.write(0x80 | payload.length);
			} else if (payload.length <= 0xFFFF) {
				frame.write(0x80 | 126);
				frame.write((payload.length >>> 8) & 0xFF);
				frame.write(payload.length & 0xFF);
			} else {
				frame.write(0x80 | 127);
				final long length = payload.length;
				for (int i = 7; i >= 0; i--) {
					frame.write((int) ((length >>> (i * 8)) & 0xFF));
				}
			}

			final byte[] mask = new byte[4];
			random.nextBytes(mask);
			frame.write(mask);
			for (int i = 0; i < payload.length; i++) {
				frame.write((byte) (payload[i] ^ mask[i % 4]));
			}

			out.write(frame.toByteArray());
			out.flush();
		}

		private JSONObject readJsonMessage() throws IOException {
			while (true) {
				final int first = in.readUnsignedByte();
				final int second = in.readUnsignedByte();
				final int opcode = first & 0x0F;
				long length = second & 0x7F;
				if (length == 126) {
					length = in.readUnsignedShort();
				} else if (length == 127) {
					length = in.readLong();
				}
				if ((second & 0x80) != 0) {
					// Server frames must not be masked; skip the mask anyway.
					in.readFully(new byte[4]);
				}
				final byte[] payload = new byte[(int) length];
				in.readFully(payload);

				if (opcode == 0x1) {
					return new JSONObject(new String(payload, StandardCharsets.UTF_8));
				}
				if (opcode == 0x8) {
					throw new EOFException("Server closed the WebSocket connection.");
				}
				// Ignore ping/pong and continuation frames for this test.
			}
		}

		@Override
		public void close() throws IOException {
			socket.close();
		}

		private static final class HttpResponse {
			private int status;
			private String body;
		}
	}

}
