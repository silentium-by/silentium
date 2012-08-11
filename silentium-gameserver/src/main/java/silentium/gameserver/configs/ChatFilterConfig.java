/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.configs;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.StringTokenizer;

public final class ChatFilterConfig extends ConfigEngine
{
	public static ArrayList<String> FILTER_LIST;

	public static void load()
	{
		try (FileInputStream fileInputStream = new FileInputStream(CHAT_FILTER_FILE); InputStreamReader streamReader
				= new InputStreamReader(fileInputStream, "UTF-8"); LineNumberReader numberReader = new LineNumberReader(streamReader))
		{
			FILTER_LIST = new ArrayList<>();
			String line;
			StringTokenizer st;

			while ((line = numberReader.readLine()) != null)
			{
				st = new StringTokenizer(line, "\n\r");

				if (st.hasMoreTokens())
					FILTER_LIST.add(st.nextToken());
			}
			log.info("Loaded " + FILTER_LIST.size() + " Filter Words.");
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new Error("Failed to Load " + CHAT_FILTER_FILE + " File.");
		}
	}
}