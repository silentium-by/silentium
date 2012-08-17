/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.configs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.Properties;

public final class HexidConfig extends ConfigEngine
{
	public static int SERVER_ID;
	public static byte[] HEX_ID;

	public static void load()
	{
		try (InputStream is = new FileInputStream(HEXID_FILE))
		{
			Properties hexid = new Properties();
			hexid.load(is);
			is.close();

			SERVER_ID = Integer.parseInt(hexid.getProperty("ServerID"));
			HEX_ID = new BigInteger(hexid.getProperty("HexID"), 16).toByteArray();
		}
		catch (Exception e)
		{
			log.warn("Server failed to load " + HEXID_FILE + " file.", e);
		}
	}

	public static void saveHexid(int serverId, String string)
	{
		HexidConfig.saveHexid(serverId, string, HEXID_FILE);
	}

	public static void saveHexid(int serverId, String hexId, String fileName)
	{
		try
		{
			Properties hexSetting = new Properties();
			File file = new File(fileName);
			file.createNewFile();

			OutputStream out = new FileOutputStream(file);
			hexSetting.setProperty("ServerID", String.valueOf(serverId));
			hexSetting.setProperty("HexID", hexId);
			hexSetting.store(out, "the hexID to auth into login");
			out.close();
		}
		catch (Exception e)
		{
			log.warn("Failed to save hex id to " + fileName + " file.", e);
		}
	}
}