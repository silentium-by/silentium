/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.configs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigEngine
{
	protected static final Logger log = LoggerFactory.getLogger(ConfigEngine.class.getName());

	public static final String CHAT_FILTER_FILE = "./config/chatfilter.txt";
	public static final String CLANS_FILE = "./config/clans.properties";
	public static final String CUSTOM_FILE = "./config/custom.properties";
	public static final String EVENTS_FILE = "./config/events.properties";
	public static final String FLOOD_PROTECTOR_FILE = "./config/floodprotector.properties";
	public static final String HEXID_FILE = "./config/hexid.txt";
	public static final String NPCS_FILE = "./config/npcs.properties";
	public static final String PLAYERS_FILE = "./config/players.properties";
	public static final String SERVER_FILE = "./config/server.properties";
	public static final String SIEGE_FILE = "./config/siege.properties";
	public static final String TVT_FILE = "./config/tvt.properties";

	public static void init()
	{
		log.info("Initialize config system...");
		ChatFilterConfig.load();
		ClansConfig.load();
		CustomConfig.load();
		EventsConfig.load();
		FProtectorConfig.load();
		HexidConfig.load();
		MainConfig.load();
		NPCConfig.load();
		PlayersConfig.load();
		TvTConfig.load();
		log.info("All configs loaded.");
	}
}