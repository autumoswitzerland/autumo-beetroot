package ch.autumo.beetroot.server.communication;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import ch.autumo.beetroot.BeetRootConfigurationManager;
import ch.autumo.beetroot.Constants;
import ch.autumo.beetroot.security.SecureApplicationHolder;
import ch.autumo.beetroot.server.message.ClientAnswer;
import ch.autumo.beetroot.server.message.HealthAnswer;
import ch.autumo.beetroot.server.message.ServerCommand;
import ch.autumo.beetroot.server.message.StopAnswer;
import ch.autumo.beetroot.transport.DefaultSocketFactory;
import ch.autumo.beetroot.transport.SecureSocketFactory;
import ch.autumo.beetroot.transport.SocketFactory;
import ch.autumo.beetroot.utils.security.SSLUtils;

public class ClientCommunicator extends Communicator {

	private static SocketFactory socketFactory = null;
	
	private static boolean pwEncoded = BeetRootConfigurationManager.getInstance().getYesOrNo(Constants.KEY_ADMIN_PW_ENC);
	
	private static int clientTimeout = -1;
	private static boolean sslSockets = false;

	private static boolean https = false;
	
	static {
		reInit();
	}
	
	/**
	 * Re-initialize client communicator.
	 */
	public static void reInit() {
		
		// read some undocumented settings if available
		clientTimeout = BeetRootConfigurationManager.getInstance().getIntNoWarn("client_timeout"); // in seconds !
		// SSL sockets?
		final String mode = BeetRootConfigurationManager.getInstance().getString(Constants.KEY_ADMIN_COM_ENC);
		sslSockets = (mode != null && mode.equalsIgnoreCase("ssl"));
		
		if (sslSockets) {
			try {
		        socketFactory = new SecureSocketFactory(SSLUtils.makeSSLSocketFactory(SSLUtils.getKeystoreFile(), SSLUtils.getKeystorePw()), null);
			} catch (Exception e) {
				LOG.error("Cannot make client calls secure (SSL)! ", e);
				System.err.println("Cannot make client calls secure (SSL)! ");
			}
		} else {
			socketFactory = new DefaultSocketFactory();
		}
		
		// Usually this has no relevance client-side if beetRoot web is deployed within a web-container
		// that defines the protocol, but is used if server commands should be send over https
		https = BeetRootConfigurationManager.getInstance().getYesOrNo(Constants.KEY_WS_HTTPS);
	}
	
	
	// Client-side
	//------------------------------------------------------------------------------
	
	/**
	 * Send a server command client side.
	 * 
	 * @param command server command
	 * @return client answer
	 * @throws Exception
	 */
	public static ClientAnswer sendServerCommand(ServerCommand command) throws Exception {
		
		Socket socket = null;
		DataOutputStream output = null;
		DataInputStream input = null;
		
		CloseableHttpClient httpClient = null;
		CloseableHttpResponse response = null;

		int timeout = command.getTimeout();
		
		try {
			
			if (clientTimeout > 0)
				timeout = clientTimeout * 1000;
				
			ClientAnswer answer = null;
			
			// A) HTTP / HTTPS tunneling - not for internal commands!
			if (!isInternalCommand(command) && command.getMode().equalsIgnoreCase("web") && !command.isForceSockets()) {
				
				final RequestConfig config = RequestConfig.custom()
						  .setConnectTimeout(timeout)
						  .setConnectionRequestTimeout(timeout)
						  .setSocketTimeout(timeout)
						  .setCookieSpec(CookieSpecs.STANDARD).build();
				
				if (https)
					httpClient = SSLUtils.makeSSLHttpClient(SSLUtils.getKeystoreFile(), SSLUtils.getKeystorePw(), config); // TODO doesn't work!
				else
					httpClient = HttpClientBuilder.create().setDefaultRequestConfig(config).build();
				
				final String apiKeyName = BeetRootConfigurationManager.getInstance().getString("web_api_key_name");
				String webApiKey = null;
				if (pwEncoded)
					webApiKey = BeetRootConfigurationManager.getInstance().getDecodedString("admin_com_web_api_key", SecureApplicationHolder.getInstance().getSecApp());
				else
					webApiKey = BeetRootConfigurationManager.getInstance().getString("admin_com_web_api_key");
				final HttpPost request = new HttpPost("/" + Constants.URI_SRV_CMD + "?" + apiKeyName + "=" + webApiKey);
				
				request.addHeader("user-agent", USER_AGENT);
				request.addHeader(HTTP_HEADER_ACCEPT_JSON[0], HTTP_HEADER_ACCEPT_JSON[1]);
				request.addHeader(HTTP_HEADER_CONTENTTYPE_JSON_UTF8[0], HTTP_HEADER_CONTENTTYPE_JSON_UTF8[1]);

				// JSON transfer string from command
				request.setEntity(new StringEntity(command.getJsonTransferString(), StandardCharsets.UTF_8.toString()));
				
				// HTTP or HTTPS
				if (https)
					response = httpClient.execute(new HttpHost(command.getHost(), command.getPort(), "https"), request);
				else
					response = httpClient.execute(new HttpHost(command.getHost(), command.getPort(), "http"), request);
				
				checkHttpResponse(response);
				final HttpEntity responseBodyentity = response.getEntity();
				final String json = EntityUtils.toString(responseBodyentity);
				
				if (command.getCommand().equals(CMD_STOP))
					return new StopAnswer(); // we cannot expect an answer, because the server is already down
				else if (command.getCommand().equals(CMD_HEALTH))
					return new HealthAnswer();
				else				
					answer = ClientAnswer.parseJson(json);

				return answer;
				
			// B) Default sockets 
			} else {
			
				socket = socketFactory.create(command.getHost(), command.getPort());
				socket.setSoTimeout(timeout);
				output = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
	
				output.writeInt(command.getDataLength());
				final PrintWriter writer = new PrintWriter(output, true);
				writer.println(command.getTransferString());
				
				LOG.trace("Server command '"+command.getCommand()+"' sent!");
				writer.flush();
				
				if (command.getCommand().equals(CMD_STOP)) {
					// we cannot expect an answer, because the server is already down
					return new StopAnswer();
				} else if (command.getCommand().equals(CMD_HEALTH)) {
					return new HealthAnswer();
				} else {
					// read answer
					input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
					answer = readAnswer(input);
				}
			}
			
			return answer;
			
		} catch (UnknownHostException e) {
			LOG.error(command.getServerName() + " admin server cannot be contacted at "+command.getHost()+":"+command.getPort()+"! Host seems to be unknown or cannot be resolved. [UHE]", e);
			throw e;
		} catch (IOException e) {
			LOG.error(command.getServerName() + " admin server cannot be contacted at "+command.getHost()+":"+command.getPort()+"! PS: Is it really running? [IO]", e);
			throw e;
		} finally {
			
			safeClose(response);
			safeClose(httpClient);
			
			safeClose(input);
			safeClose(output);
			safeClose(socket);
		}
	}

	/**
	 * Read an answer from the server client side.
	 * 
	 * @param in input stream
	 * @return client answer or null, if answer received was invalid
	 * @throws IOException
	 */
	public static ClientAnswer readAnswer(DataInputStream in) throws IOException {
	    return ClientAnswer.parse(read(in));
	}
	
}
