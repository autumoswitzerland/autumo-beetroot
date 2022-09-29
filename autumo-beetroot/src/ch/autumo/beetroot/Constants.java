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

import java.sql.Types;

import jakarta.activation.MimetypesFileTypeMap;

/**
 * Constants. 
 */
public class Constants {

	/**
	 * App version.
	 */
	public static final String APP_VERSION = "1.3.0";
	
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
	public static final int SECRET_USER_KEY_DEFAILUT_LEN = 20;
	
	
	// Config specific
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
	 * Admin server/shutdown port.
	 */
	public static final String KEY_ADMIN_PORT = "admin_port";

	/**
	 * Admin server/shutdown host.
	 */
	public static final String KEY_ADMIN_HOST = "admin_host";
	
	/**
	 * Passwords in configuration encoded?
	 */
	public static final String KEY_ADMIN_PW_ENC = "admin_pw_encoded";

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
	 * HTTPS fro web server?
	 */
	public static final String KEY_WS_HTTPS = "ws_https";
	
	/**
	 * SSL certficate keystore.
	 */
	public static final String KEY_KEYSTORE_FILE = "ws_ks";

	/**
	 * Use CSRF tokens?
	 */
	public static final String KEY_WS_USE_CSRF_TOKNES = "ws_use_csrf_tokens";
	
	/**
	 * Web server password.
	 */
	public static final String KEY_WS_KEYSTORE_PW = "ws_ks_password";

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
	 * Yes!
	 */
	public static final String YES = "yes";

	/**
	 * Sec key seed key.
	 */
	public static final String SEC_KEY_SEED = "secret_key_seed";

	/**
	 * No show identifier for not showing certain columns and values defined in 'columns.cfg'.
	 */
	public static final String GUI_COL_NO_SHOW = "NO_SHOW";
	
	/**
	 * QR image size.
	 */
	public static final int QR_IMG_SIZE = 320;

	
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
	
}
