/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.List;
import java.util.StringTokenizer;

import javolution.text.TextBuilder;
import javolution.util.FastList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import silentium.gameserver.data.html.HtmCache;
import silentium.gameserver.data.html.StaticHtmPath;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.network.clientpackets.Say2;
import silentium.gameserver.network.serverpackets.CreatureSay;
import silentium.gameserver.network.serverpackets.NpcHtmlMessage;
import silentium.gameserver.network.serverpackets.SystemMessage;
import silentium.gameserver.utils.Broadcast;

public class Announcements
{
	private static Logger _log = LoggerFactory.getLogger(Announcements.class.getName());

	private static Announcements _instance;
	private final List<String> _announcements = new FastList<>();

	private Announcements()
	{
		loadAnnouncements();
	}

	public static Announcements getInstance()
	{
		if (_instance == null)
			_instance = new Announcements();

		return _instance;
	}

	public void loadAnnouncements()
	{
		_announcements.clear();
		File file = new File("./config/announcements.txt");
		if (file.exists())
			readFromDisk(file);
		else
			_log.error("The announcements file (normally located to 'config/announcements.txt') doesn't exist.");
	}

	public void showAnnouncements(L2PcInstance activeChar)
	{
		for (int i = 0; i < _announcements.size(); i++)
		{
			CreatureSay cs = new CreatureSay(0, Say2.ANNOUNCEMENT, activeChar.getName(), _announcements.get(i));
			activeChar.sendPacket(cs);
		}
	}

	public void listAnnouncements(L2PcInstance activeChar)
	{
		String content = HtmCache.getInstance().getHtmForce(StaticHtmPath.AdminHtmPath + "announce.htm");
		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setHtml(content);
		TextBuilder replyMSG = new TextBuilder("<br>");
		for (int i = 0; i < _announcements.size(); i++)
		{
			replyMSG.append("<table width=260><tr><td width=220>" + _announcements.get(i) + "</td><td width=40>");
			replyMSG.append("<button value=\"Delete\" action=\"bypass -h admin_del_announcement " + i + "\" width=60 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr></table>");
		}
		adminReply.replace("%announces%", replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}

	public void addAnnouncement(String text)
	{
		_announcements.add(text);
		saveToDisk();
	}

	public void delAnnouncement(int line)
	{
		_announcements.remove(line);
		saveToDisk();
	}

	private void readFromDisk(File file)
	{
		try (LineNumberReader lnr = new LineNumberReader(new FileReader(file)))
		{
			int i = 0;
			String line = null;
			while ((line = lnr.readLine()) != null)
			{
				StringTokenizer st = new StringTokenizer(line, "\n\r");
				if (st.hasMoreTokens())
				{
					String announcement = st.nextToken();
					_announcements.add(announcement);

					i++;
				}
			}
			_log.info("Announcements: Loaded " + i + " Announcements.");
		}
		catch (IOException e1)
		{
			_log.error("Error reading announcements.", e1);
		}
	}

	private void saveToDisk()
	{
		final File file = new File("./config/announcements.txt");
		try (FileWriter save = new FileWriter(file))
		{
			for (int i = 0; i < _announcements.size(); i++)
			{
				save.write(_announcements.get(i));
				save.write("\r\n");
			}
		}
		catch (IOException e)
		{
			_log.warn("saving the announcements file has failed: " + e);
		}
	}

	public static void announceToAll(String text)
	{
		Broadcast.announceToOnlinePlayers(text);
	}

	public static void announceToAll(SystemMessage sm)
	{
		Broadcast.toAllOnlinePlayers(sm);
	}

	// Method for handling announcements from admin
	public static void handleAnnounce(String command, int lengthToTrim)
	{
		try
		{
			// Announce string to everyone on server
			announceToAll(command.substring(lengthToTrim));
		}
		catch (StringIndexOutOfBoundsException e)
		{
		}
	}
}
