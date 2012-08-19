/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.authserver.configs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

public final class MainConfig {
	private static final Logger log = LoggerFactory.getLogger(MainConfig.class);

	public static String LOGIN_BIND_ADDRESS;
	public static int PORT_LOGIN;
	public static boolean ACCEPT_NEW_GAMESERVER;
	public static int REQUEST_ID;
	public static boolean ACCEPT_ALTERNATE_ID;
	public static int LOGIN_TRY_BEFORE_BAN;
	public static int LOGIN_BLOCK_AFTER_BAN;
	public static boolean LOG_LOGIN_CONTROLLER;
	public static boolean SHOW_LICENCE;
	public static int IP_UPDATE_TIME;
	public static boolean FORCE_GGAUTH;
	public static boolean AUTO_CREATE_ACCOUNTS;
	public static boolean FLOOD_PROTECTION;
	public static int FAST_CONNECTION_LIMIT;
	public static int NORMAL_CONNECTION_TIME;
	public static int FAST_CONNECTION_TIME;
	public static int MAX_CONNECTION_PER_IP;

	public static String EXTERNAL_HOSTNAME;
	public static String INTERNAL_HOSTNAME;
	public static int GAME_SERVER_LOGIN_PORT;
	public static String GAME_SERVER_LOGIN_HOST;

	public static boolean DEBUG;
	public static boolean DEVELOPER;
	public static boolean PACKET_HANDLER_DEBUG;

	public static int MMO_SELECTOR_SLEEP_TIME = 20; // default 20
	public static int MMO_MAX_SEND_PER_PASS = 12; // default 12
	public static int MMO_MAX_READ_PER_PASS = 12; // default 12
	public static int MMO_HELPER_BUFFER_COUNT = 20; // default 20

	public static void load() {
		log.info("Loading loginserver configuration files.");

		final File config = new File("./config/loginserver.properties");

		try (InputStream is = new FileInputStream(config)) {
			final Properties server = new Properties();
			server.load(is);

			GAME_SERVER_LOGIN_HOST = server.getProperty("LoginHostname", "*");
			GAME_SERVER_LOGIN_PORT = Integer.parseInt(server.getProperty("LoginPort", "9013"));

			LOGIN_BIND_ADDRESS = server.getProperty("LoginserverHostname", "*");
			PORT_LOGIN = Integer.parseInt(server.getProperty("LoginserverPort", "2106"));

			DEBUG = Boolean.parseBoolean(server.getProperty("Debug", "false"));
			DEVELOPER = Boolean.parseBoolean(server.getProperty("Developer", "false"));
			PACKET_HANDLER_DEBUG = Boolean.parseBoolean(server.getProperty("PacketHandlerDebug", "False"));
			ACCEPT_NEW_GAMESERVER = Boolean.parseBoolean(server.getProperty("AcceptNewGameServer", "True"));
			REQUEST_ID = Integer.parseInt(server.getProperty("RequestServerID", "0"));
			ACCEPT_ALTERNATE_ID = Boolean.parseBoolean(server.getProperty("AcceptAlternateID", "True"));

			LOGIN_TRY_BEFORE_BAN = Integer.parseInt(server.getProperty("LoginTryBeforeBan", "10"));
			LOGIN_BLOCK_AFTER_BAN = Integer.parseInt(server.getProperty("LoginBlockAfterBan", "600"));

			LOG_LOGIN_CONTROLLER = Boolean.parseBoolean(server.getProperty("LogLoginController", "False"));

			INTERNAL_HOSTNAME = server.getProperty("InternalHostname", "localhost");
			EXTERNAL_HOSTNAME = server.getProperty("ExternalHostname", "localhost");

			SHOW_LICENCE = Boolean.parseBoolean(server.getProperty("ShowLicence", "true"));
			IP_UPDATE_TIME = Integer.parseInt(server.getProperty("IpUpdateTime", "15"));
			FORCE_GGAUTH = Boolean.parseBoolean(server.getProperty("ForceGGAuth", "false"));

			AUTO_CREATE_ACCOUNTS = Boolean.parseBoolean(server.getProperty("AutoCreateAccounts", "True"));

			FLOOD_PROTECTION = Boolean.parseBoolean(server.getProperty("EnableFloodProtection", "True"));
			FAST_CONNECTION_LIMIT = Integer.parseInt(server.getProperty("FastConnectionLimit", "15"));
			NORMAL_CONNECTION_TIME = Integer.parseInt(server.getProperty("NormalConnectionTime", "700"));
			FAST_CONNECTION_TIME = Integer.parseInt(server.getProperty("FastConnectionTime", "350"));
			MAX_CONNECTION_PER_IP = Integer.parseInt(server.getProperty("MaxConnectionPerIP", "50"));
		} catch (Exception e) {
			log.error("Server failed to load ./config/loginserver.properties file.", e);
		}
	}
}