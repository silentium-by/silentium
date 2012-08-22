/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.authserver.configs;

import silentium.commons.configuration.annotations.PropertiesFile;
import silentium.commons.configuration.annotations.Property;

@PropertiesFile(propertiesPatch = "./config/loginserver.properties")
public final class MainConfig {
	@Property(key = "AcceptNewGameServer", defaultValue = "True")
	public static boolean ACCEPT_NEW_GAMESERVER;
	
	@Property(key = "AutoCreateAccounts", defaultValue = "True")
	public static boolean AUTO_CREATE_ACCOUNTS;
	
	@Property(key = "Developer", defaultValue = "false")
	public static boolean DEVELOPER;
	
	@Property(key = "EnableFloodProtection", defaultValue = "True")
	public static boolean FLOOD_PROTECTION;
	
	@Property(key = "LogLoginController", defaultValue = "False")
	public static boolean LOG_LOGIN_CONTROLLER;
	
	@Property(key = "PacketHandlerDebug", defaultValue = "False")
	public static boolean PACKET_HANDLER_DEBUG;
	
	@Property(key = "ShowLicence", defaultValue = "true")
	public static boolean SHOW_LICENCE;
	
	@Property(key = "FastConnectionLimit", defaultValue = "15")
	public static int FAST_CONNECTION_LIMIT;
	
	@Property(key = "FastConnectionTime", defaultValue = "350")
	public static int FAST_CONNECTION_TIME;
	
	@Property(key = "LoginPort", defaultValue = "9014")
	public static int GAME_SERVER_LOGIN_PORT;
	
	@Property(key = "LoginBlockAfterBan", defaultValue = "600")
	public static int LOGIN_BLOCK_AFTER_BAN;
	
	@Property(key = "LoginTryBeforeBan", defaultValue = "10")
	public static int LOGIN_TRY_BEFORE_BAN;
	
	@Property(key = "MaxConnectionPerIP", defaultValue = "50")
	public static int MAX_CONNECTION_PER_IP;
	
	@Property(key = "NormalConnectionTime", defaultValue = "700")
	public static int NORMAL_CONNECTION_TIME;
	
	@Property(key = "LoginserverPort", defaultValue = "2106")
	public static int PORT_LOGIN;
	
	@Property(key = "ExternalHostname", defaultValue = "localhost")
	public static String EXTERNAL_HOSTNAME;
	
	@Property(key = "LoginHostname", defaultValue = "*")
	public static String GAME_SERVER_LOGIN_HOST;
	
	@Property(key = "InternalHostname", defaultValue = "localhost")
	public static String INTERNAL_HOSTNAME;
	
	@Property(key = "LoginserverHostname", defaultValue = "*")
	public static String LOGIN_BIND_ADDRESS;

	public static int MMO_HELPER_BUFFER_COUNT = 20;
	public static int MMO_MAX_READ_PER_PASS = 12;
	public static int MMO_MAX_SEND_PER_PASS = 12;
	public static int MMO_SELECTOR_SLEEP_TIME = 20;
}