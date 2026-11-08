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
package ch.autumo.beetroot.handler.system;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.core.LogEvent;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.autumo.beetroot.logging.LogEventAppender;
import ch.autumo.beetroot.server.websocket.WebSocketAction;
import ch.autumo.beetroot.server.websocket.WebSocketContext;

/**
 * WebSocket action that streams the in-memory beetRoot log.
 *
 * <p>
 * The action sends a snapshot of the current log when it is started and then
 * pushes every new log event. Log events are formatted exactly like on the
 * server log page.
 * </p>
 */
public class LogTailWebSocketAction implements WebSocketAction {

	/** Route path of this action as registered in {@code cfg/routing.xml}. */
	public static final String ROUTE_PATH = "/system/log/tail";

	private static final Logger LOG = LoggerFactory.getLogger(LogTailWebSocketAction.class);

	private static final int QUEUE_SIZE = 10000;

	private final BlockingQueue<LogEvent> queue = new LinkedBlockingQueue<LogEvent>(QUEUE_SIZE);
	private final AtomicBoolean running = new AtomicBoolean(false);

	private volatile LogEventAppender.LogEventListener listener;
	private volatile Thread worker;

	@Override
	public void onStart(WebSocketContext context, JSONObject request) throws Exception {
		listener = new LogEventAppender.LogEventListener() {
			@Override
			public void onLogEvent(LogEvent event) {
				// Never block the logging thread; drop events if the client is too slow.
				queue.offer(event);
			}
		};
		running.set(true);
		LogEventAppender.addLogEventListener(listener);

		// Send the snapshot after the listener was registered. Events that
		// arrive while the snapshot is built are queued and sent afterwards.
		sendSnapshot(context);

		worker = new Thread(new Runnable() {
			@Override
			public void run() {
				drain(context);
			}
		}, "beetroot-ws-logtail");
		worker.setDaemon(true);
		worker.start();
	}

	@Override
	public void onMessage(WebSocketContext context, JSONObject message) throws Exception {
		if ("snapshot".equalsIgnoreCase(message.optString("command", null))) {
			sendSnapshot(context);
		}
	}

	@Override
	public void onStop(WebSocketContext context) {
		running.set(false);
		LogEventAppender.removeLogEventListener(listener);
		listener = null;
		final Thread current = worker;
		if (current != null) {
			current.interrupt();
			worker = null;
		}
		queue.clear();
	}

	private void sendSnapshot(WebSocketContext context) throws IOException {
		final JSONArray lines = new JSONArray();
		for (LogEvent event : LogEventAppender.getLogEvents()) {
			lines.put(LogHandler.formatEvent(event));
		}
		context.sendSnapshot(lines);
	}

	private void drain(WebSocketContext context) {
		while (running.get() && context.isOpen()) {
			try {
				final LogEvent event = queue.poll(1, TimeUnit.SECONDS);
				if (event == null) {
					continue;
				}
				context.sendEvent(LogHandler.formatEvent(event));
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			} catch (IOException e) {
				LOG.debug("Couldn't push log event to WebSocket client.", e);
				break;
			} catch (Exception e) {
				LOG.warn("Couldn't format log event for WebSocket client.", e);
			}
		}
	}

}
