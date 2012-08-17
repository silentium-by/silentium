/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.model.actor.instance;

import java.util.StringTokenizer;

import silentium.gameserver.data.html.StaticHtmPath;
import silentium.gameserver.data.xml.DoorData;
import silentium.gameserver.data.xml.TeleportLocationData;
import silentium.gameserver.model.L2TeleportLocation;
import silentium.gameserver.network.serverpackets.ActionFailed;
import silentium.gameserver.network.serverpackets.NpcHtmlMessage;
import silentium.gameserver.templates.chars.L2NpcTemplate;

/**
 * L2Doormen is the mother class of L2ClanHallDoormen and L2CastleDoormen.
 */
public class L2DoormenInstance extends L2NpcInstance
{
	public L2DoormenInstance(int objectID, L2NpcTemplate template)
	{
		super(objectID, template);
	}

	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		if (command.startsWith("Chat"))
		{
			showChatWindow(player);
			return;
		}
		else if (command.startsWith("open_doors"))
		{
			if (isOwnerClan(player))
			{
				if (isUnderSiege())
					cannotManageDoors(player);
				else
					openDoors(player, command);
			}
			return;
		}
		else if (command.startsWith("close_doors"))
		{
			if (isOwnerClan(player))
			{
				if (isUnderSiege())
					cannotManageDoors(player);
				else
					closeDoors(player, command);
			}
			return;
		}
		else if (command.startsWith("tele"))
		{
			if (isOwnerClan(player))
				doTeleport(player, command);
			return;
		}

		super.onBypassFeedback(player, command);
	}

	@Override
	public void showChatWindow(L2PcInstance player)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);

		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());

		if (!isOwnerClan(player))
			html.setFile(StaticHtmPath.DoormenHtmPath + getTemplate().getNpcId() + "-no.htm");
		else
			html.setFile(StaticHtmPath.DoormenHtmPath + getTemplate().getNpcId() + ".htm");

		html.replace("%objectId%", String.valueOf(getObjectId()));
		player.sendPacket(html);
	}

	protected void openDoors(L2PcInstance player, String command)
	{
		StringTokenizer st = new StringTokenizer(command.substring(10), ", ");
		st.nextToken();

		while (st.hasMoreTokens())
		{
			DoorData.getInstance().getDoor(Integer.parseInt(st.nextToken())).openMe();
		}
	}

	protected void closeDoors(L2PcInstance player, String command)
	{
		StringTokenizer st = new StringTokenizer(command.substring(11), ", ");
		st.nextToken();

		while (st.hasMoreTokens())
		{
			DoorData.getInstance().getDoor(Integer.parseInt(st.nextToken())).closeMe();
		}
	}

	protected void cannotManageDoors(L2PcInstance player)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);

		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(StaticHtmPath.DoormenHtmPath + getTemplate().getNpcId() + "-busy.htm");
		player.sendPacket(html);
	}

	protected void doTeleport(L2PcInstance player, String command)
	{
		final int whereTo = Integer.parseInt(command.substring(5).trim());
		L2TeleportLocation list = TeleportLocationData.getInstance().getTemplate(whereTo);
		if (list != null)
		{
			if (!player.isAlikeDead())
				player.teleToLocation(list.getLocX(), list.getLocY(), list.getLocZ(), false);
		}
		else
			_log.warn("No teleport destination with id: " + whereTo);

		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	protected boolean isOwnerClan(L2PcInstance player)
	{
		return true;
	}

	protected boolean isUnderSiege()
	{
		return false;
	}
}
