/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GMAudit
{
	static
	{
		new File("log/GMAudit").mkdirs();
	}

	private static final Logger _log = LoggerFactory.getLogger(GMAudit.class.getName());

	public static void auditGMAction(String gmName, String action, String target, String params)
	{
		final File file = new File("log/GMAudit/" + gmName + ".txt");
		if (!file.exists())
			try
			{
				file.createNewFile();
			}
			catch (IOException e)
			{
			}

		try (FileWriter save = new FileWriter(file, true))
		{
			save.write(Util.formatDate(new Date(), "dd/MM/yyyy H:mm:ss") + ">" + gmName + ">" + action + ">" + target + ">" + params + "\r\n");
		}
		catch (IOException e)
		{
			_log.error("GMAudit for GM " + gmName + " could not be saved: ", e);
		}
	}

	public static void auditGMAction(String gmName, String action, String target)
	{
		auditGMAction(gmName, action, target, "");
	}
}
