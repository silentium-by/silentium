/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.model.actor.instance;

import java.util.Collection;
import java.util.StringTokenizer;

import silentium.gameserver.ThreadPoolManager;
import silentium.gameserver.data.html.StaticHtmPath;
import silentium.gameserver.data.xml.MapRegionData;
import silentium.gameserver.model.L2World;
import silentium.gameserver.network.serverpackets.NpcHtmlMessage;
import silentium.gameserver.network.serverpackets.NpcSay;
import silentium.gameserver.templates.chars.L2NpcTemplate;

/**
 * @author Kerberos
 */
public class L2CastleTeleporterInstance extends L2NpcInstance
{
	private boolean _currentTask = false;
	private int delay;

	public L2CastleTeleporterInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		StringTokenizer st = new StringTokenizer(command, " ");
		String actualCommand = st.nextToken(); // Get actual command

		if (actualCommand.equalsIgnoreCase("tele"))
		{
			if (!getTask())
			{
				if (getCastle().getSiege().getIsInProgress() && getCastle().getSiege().getControlTowerCount() == 0)
					delay = 480000;
				else if (getCastle().getSiege().getIsInProgress())
					delay = 30000;
				else
					delay = 0;

				setTask(true);
				ThreadPoolManager.getInstance().scheduleGeneral(new oustAllPlayers(), delay);
			}

			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			html.setFile(StaticHtmPath.CastleTeleporterHtmPath + "/MassGK-1.htm");
			html.replace("%delay%", String.valueOf(getDelayInSeconds()));
			player.sendPacket(html);
			return;
		}
		super.onBypassFeedback(player, command);
	}

	@Override
	public void showChatWindow(L2PcInstance player)
	{
		String filename;
		if (!getTask())
		{
			if (getCastle().getSiege().getIsInProgress() && getCastle().getSiege().getControlTowerCount() == 0)
				filename = StaticHtmPath.CastleTeleporterHtmPath + "/MassGK-2.htm";
			else
				filename = StaticHtmPath.CastleTeleporterHtmPath + "/MassGK.htm";
		}
		else
			filename = StaticHtmPath.CastleTeleporterHtmPath + "/MassGK-1.htm";

		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		player.sendPacket(html);
	}

	void oustAllPlayers()
	{
		getCastle().oustAllPlayers();
	}

	class oustAllPlayers implements Runnable
	{
		@Override
		public void run()
		{
			try
			{
				// Make the region talk only during a siege
				if (getCastle().getSiege().getIsInProgress())
				{
					NpcSay cs = new NpcSay(getObjectId(), 1, getNpcId(), "The defenders of " + getCastle().getName() + " castle have been teleported to the inner castle.");

					int region = MapRegionData.getInstance().getMapRegion(getX(), getY());
					Collection<L2PcInstance> pls = L2World.getInstance().getAllPlayers().values();

					for (L2PcInstance player : pls)
					{
						if (region == MapRegionData.getInstance().getMapRegion(player.getX(), player.getY()))
							player.sendPacket(cs);
					}
				}
				oustAllPlayers();
				setTask(false);
			}
			catch (NullPointerException e)
			{
				e.printStackTrace();
			}
		}
	}

	private final int getDelayInSeconds()
	{
		if (delay > 0)
			return delay / 1000;

		return 0;
	}

	public boolean getTask()
	{
		return _currentTask;
	}

	public void setTask(boolean state)
	{
		_currentTask = state;
	}
}