/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.authserver.configs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigEngine
{
	protected static final Logger log = LoggerFactory.getLogger(ConfigEngine.class.getName());

	public static final String LOGIN_CONFIGURATION_FILE = "./config/loginserver.properties";
	public static final String HEXID_FILE = "./config/hexid.txt";

	public static void init()
	{
		log.info("Initialize config system...");

		MainConfig.load();

		log.info("All configs loaded.");
	}
}
