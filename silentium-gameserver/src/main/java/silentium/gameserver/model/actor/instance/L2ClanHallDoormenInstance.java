/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.model.actor.instance;

import silentium.gameserver.data.html.StaticHtmPath;
import silentium.gameserver.instancemanager.ClanHallManager;
import silentium.gameserver.model.L2Clan;
import silentium.gameserver.model.entity.ClanHall;
import silentium.gameserver.network.serverpackets.ActionFailed;
import silentium.gameserver.network.serverpackets.NpcHtmlMessage;
import silentium.gameserver.tables.ClanTable;
import silentium.gameserver.templates.chars.L2NpcTemplate;

public class L2ClanHallDoormenInstance extends L2DoormenInstance
{
	private ClanHall _clanHall = null;

	public L2ClanHallDoormenInstance(int objectID, L2NpcTemplate template)
	{
		super(objectID, template);
	}

	@Override
	public void showChatWindow(L2PcInstance player)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);

		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());

		if (getClanHall() != null)
		{
			L2Clan owner = ClanTable.getInstance().getClan(getClanHall().getOwnerId());
			if (isOwnerClan(player))
			{
				html.setFile(StaticHtmPath.ClanHallDoormenHtmPath + "doormen.htm");
				html.replace("%clanname%", owner.getName());
			}
			else
			{
				if (owner != null && owner.getLeader() != null)
				{
					html.setFile(StaticHtmPath.ClanHallDoormenHtmPath + "doormen-no.htm");
					html.replace("%leadername%", owner.getLeaderName());
					html.replace("%clanname%", owner.getName());
				}
				else
				{
					html.setFile(StaticHtmPath.ClanHallDoormenHtmPath + "emptyowner.htm");
					html.replace("%hallname%", getClanHall().getName());
				}
			}
		}
		else
			return;

		html.replace("%objectId%", String.valueOf(getObjectId()));
		player.sendPacket(html);
	}

	@Override
	protected final void openDoors(L2PcInstance player, String command)
	{
		getClanHall().openCloseDoors(true);
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(StaticHtmPath.ClanHallDoormenHtmPath + "doormen-opened.htm");
		html.replace("%objectId%", String.valueOf(getObjectId()));
		player.sendPacket(html);
	}

	@Override
	protected final void closeDoors(L2PcInstance player, String command)
	{
		getClanHall().openCloseDoors(false);
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(StaticHtmPath.ClanHallDoormenHtmPath + "doormen-closed.htm");
		html.replace("%objectId%", String.valueOf(getObjectId()));
		player.sendPacket(html);
	}

	private final ClanHall getClanHall()
	{
		if (_clanHall == null)
			_clanHall = ClanHallManager.getInstance().getNearbyClanHall(getX(), getY(), 500);

		return _clanHall;
	}

	@Override
	protected final boolean isOwnerClan(L2PcInstance player)
	{
		if (player.getClan() != null && getClanHall() != null)
		{
			if (player.getClanId() == getClanHall().getOwnerId())
				return true;
		}
		return false;
	}
}