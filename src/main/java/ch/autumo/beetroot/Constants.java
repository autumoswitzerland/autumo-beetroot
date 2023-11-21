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
package ch.autumo.beetroot;

import java.sql.Types;

import jakarta.activation.MimetypesFileTypeMap;

/**
 * Constants. 
 */
public class Constants {

	/**
	 * App version.
	 */
	public static final String APP_VERSION = "2.0.1";
	
	/**
	 * Initialize mime types registry.
	 */
	static {
		// use our patched version of the mime type registry; 
		// we want this under our control!
		System.setProperty("jakarta.activation.spi.MimeTypeRegistryProvider", 
				"ch.autumo.beetroot.mime.MimeTypeRegistryProviderImpl");
		
		MIME_TYPES_MAP = new MimetypesFileTypeMap();
	}
	
	/**
	 * MIME types map.
	 */
	public static final MimetypesFileTypeMap MIME_TYPES_MAP;
	
	/**
	 * Default length for user key.
	 */
	public static final int SECRET_USER_KEY_DEFAULT_LEN = 20;
	
	
	// General
	//------------------------------------------------------------------------------
	
    /**
     * Config path.
     */
    public static final String CONFIG_PATH = "cfg/";
    
	/**
	 * General server config file.
	 */
	public static final String GENERAL_SRV_CFG_FILE = "beetroot.cfg";

	/**
	 * JSON extension.
	 */
	public static final String JSON_EXT = ".json";

	/**
	 * Server Command URI.
	 */
	public static final String URI_SRV_CMD = "srvcmd";
	
	/**
	 * Search page.
	 */
	public static final String SEARCH_PAGE = "search.html";
	
	/**
	 * User settings url part.
	 */
	public static final String USER_SETTINGS_URL_PART = "users/settings";
	
	/**
	 * Yes!
	 */
	public static final String YES = "yes";

	/**
	 * No!
	 */
	public static final String NO = "no";
	
	/**
	 * On Switch.
	 */
	public static final String ON = "On";

	/**
	 * Off Switch.
	 */
	public static final String OFF = "Off";
	
	/**
	 * No show identifier for not showing certain columns and values defined in 'columns.cfg'.
	 */
	public static final String GUI_COL_NO_SHOW = "NO_SHOW";
	
	/**
	 * QR image size.
	 */
	public static final int QR_IMG_SIZE = 320;
	
	
	// Server-specific
	//------------------------------------------------------------------------------

	/**
	 * Server/Servlet name.
	 */
	public static final String KEY_SERVER_NAME = "server_name";

	/**
	 * Admin server communication mode.
	 */
	public static final String KEY_ADMIN_COM_MODE = "admin_com_mode";

	/**
	 * Admin server communication in web tunnel mode: Verify host-name when using SSL certificates (HTTPS)?
	 */
	public static final String KEY_ADMIN_COM_HOSTNAME_VERIFY = "admin_com_host_verify";
	
	/**
	 * Communication encryption mode; none|sha3|ssl
	 */
	public static final String KEY_ADMIN_COM_ENC = "admin_com_encrypt";
	
	/**
	 * Admin server/shutdown host.
	 */
	public static final String KEY_ADMIN_HOST = "admin_host";
	
	/**
	 * Admin server/shutdown port.
	 */
	public static final String KEY_ADMIN_PORT = "admin_port";
	
	/**
	 * Start file server?
	 */
	public static final String KEY_ADMIN_FILE_SERVER = "admin_file_server_start";

	/**
	 * File storage implementation.
	 */
	public static final String KEY_ADMIN_FILE_STORAGE = "admin_file_storage";
	
	/**
	 * Admin file server buffer size.
	 */
	public static final String KEY_ADMIN_FILE_BUF_SIZE = "admin_file_buffer_size";
	
	/**
	 * File server port.
	 */
	public static final String KEY_ADMIN_FILE_PORT = "admin_file_port";

	/**
	 * File receiver port (file-store end-point).
	 */
	public static final String KEY_ADMIN_FILE_RECEIVER_PORT = "admin_file_receiver_port";
	
	/**
	 * Passwords in configuration encoded?
	 */
	public static final String KEY_ADMIN_PW_ENC = "admin_pw_encoded";

	/**
	 * SSL Certificate key-store.
	 */
	public static final String KEY_KEYSTORE_FILE = "keystore";

	/**
	 * Key-store password.
	 */
	public static final String KEY_KEYSTORE_PW = "keystore_password";

	/**
	 * Sec key seed key.
	 */
	public static final String SEC_KEY_SEED = "secret_key_seed";
	
	
	// Web-Server-specific
	//------------------------------------------------------------------------------
	
	/**
	 * Prefix for temporary files.
	 */
	public static final String KEY_WS_TMP_FILE_PREFIX = "ws_tmp_file_prefix";
	
	/**
	 * Web app name.
	 */
	public static final String KEY_WS_APP_NAME = "ws_app_name";

	/**
	 * Web temporary directory.
	 */
	public static final String KEY_WS_TMP_DIR = "ws_tmp_dir";
	
	/**
	 * Web server URL.
	 */
	public static final String KEY_WS_URL = "ws_url";

	/**
	 * Web server port.
	 */
	public static final String KEY_WS_PORT = "ws_port";
	
	/**
	 * Start web server?
	 */
	public static final String KEY_WS_START = "ws_start";

	/**
	 * HTTPS for web server?
	 */
	public static final String KEY_WS_HTTPS = "ws_https";
	
	/**
	 * Use CSRF tokens?
	 */
	public static final String KEY_WS_USE_CSRF_TOKENS = "ws_use_csrf_tokens";

	
	// Web-specific
	//------------------------------------------------------------------------------
	
	/**
	 * Placeholder variable for DB URL web context path.
	 */
	public static final String KEY_DB_URL_WEB_CONTEXT_PATH = "[WEB-CONTEXT-PATH]";
	
	/**
	 * How many records should be shown per index web page?
	 */
	public static final String KEY_WEB_MAX_RECORDS_PER_PAGE = "web_max_records_per_page";

	/**
	 * Default web handler class.
	 */
	public static final String KEY_WEB_DEFAULT_HANDLER = "web_default_handler";

	/**
	 * Default web handler entity.
	 */
	public static final String KEY_WEB_DEFAULT_ENTITY = "web_default_entity";

	/**
	 * Use password validator?
	 */
	public static final String KEY_WEB_PASSWORD_VALIDATOR = "web_pw_validator";
	
	/**
	 * Amount of referenced records to be loaded.
	 */
	public static final String KEY_WEB_MAX_REF_REC = "web_max_ref_records";

	
	// SQL specific
	//------------------------------------------------------------------------------
	
	/**
	 * SQL text types.
	 */
	public static final int[] SQL_TEXT_TYPES = new int[] { Types.CHAR, Types.VARCHAR, Types.LONGVARCHAR };
	
	/**
	 * SQL number types.
	 */
	public static final int[] SQL_NUMBER_TYPES = new int[] { Types.SMALLINT, Types.INTEGER, Types.BIGINT, Types.FLOAT, Types.REAL, Types.DOUBLE, Types.NUMERIC, Types.DECIMAL };
	
	/**
	 * SQL date types.
	 */
	public static final int[] SQL_DATE_TYPES = new int[] { Types.DATE, Types.TIME, Types.TIMESTAMP };
	
	/**
	 * SQL binary types.
	 */
	public static final int[] SQL_BINARY_TYPES = new int[] { Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY };

	/**
	 * SQL boolean types.
	 */
    public static final int[] SQL_BOOLEAN_TYPES = new int[] { Types.BOOLEAN, Types.BIT, Types.TINYINT };

    
	// Mail specific
	//------------------------------------------------------------------------------
    
    /**
     * Mail transport protocol.
     */
    public static final String MAIL_TRANSPORT_PROTOCOL = "mail.transport.protocol";
    
    /**
     * Mail host key.
     */
	public static final String MAIL_SMTP_HOST_KEY = "mail.smtp.host";

    /**
     * Mail SMTP key.
     */
	public static String MAIL_SMTP_PORT_KEY = "mail.smtp.port";
	
    /**
     * Mail auth key.
     */
	public static String MAIL_SMTP_AUTH_KEY = "mail.smtp.auth";
	
    /**
     * Mail TLS enable key.
     */
	public static String MAIL_SMTP_TLS_ENABLE_KEY = "mail.smtp.starttls.enable";
	
	
	
	// DB specific
	//------------------------------------------------------------------------------
	
	/**
	 * Passwords in DB encoded?
	 */
	public static final String KEY_DB_PW_ENC = "db_pw_encoded";
	
	/**
	 * JDBC H2 db.
	 */
	public static String JDBC_H2_DB = "jdbc:h2";
	
	/**
	 * JDBC mysql db.
	 */
	public static String JDBC_MYSQL_DB = "jdbc:mysql";
	
	/**
	 * JDBC maria db.
	 */
	public static String JDBC_MARIA_DB = "jdbc:mariadb";

	/**
	 * Oracle db.
	 */
	public static String JDBC_ORACLE_DB = "jdbc:oracle";

	/**
	 * Postgre db.
	 */
	public static String JDBC_POSTGRE_DB = "jdbc:postgresql";	

	/**
	 * Postgre db with PGJDBC-NG driver.
	 */
	public static String JDBC_POSTGRE_NG_DB = "jdbc:pgsql";	
	
}
