/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.model.actor.instance;

import silentium.commons.utils.StringUtil;
import silentium.gameserver.ai.CtrlIntention;
import silentium.gameserver.data.html.HtmCache;
import silentium.gameserver.data.html.StaticHtmPath;
import silentium.gameserver.model.L2Object;
import silentium.gameserver.model.actor.L2Character;
import silentium.gameserver.model.actor.knownlist.NullKnownList;
import silentium.gameserver.network.L2GameClient;
import silentium.gameserver.network.serverpackets.ActionFailed;
import silentium.gameserver.network.serverpackets.MyTargetSelected;
import silentium.gameserver.network.serverpackets.NpcHtmlMessage;
import silentium.gameserver.network.serverpackets.ShowTownMap;
import silentium.gameserver.network.serverpackets.StaticObject;

/**
 * GODSON ROX!
 */
public class L2StaticObjectInstance extends L2Object
{
	/** The interaction distance of the L2StaticObjectInstance */
	public static final int INTERACTION_DISTANCE = 150;

	private int _staticObjectId;
	private int _type = -1; // 0 - map signs, 1 - throne , 2 - arena signs
	private ShowTownMap _map;

	public L2StaticObjectInstance(int objectId)
	{
		super(objectId);
	}

	@Override
	public void initKnownList()
	{
		setKnownList(new NullKnownList(this));
	}

	/**
	 * @return the StaticObjectId.
	 */
	public int getStaticObjectId()
	{
		return _staticObjectId;
	}

	/**
	 * @param StaticObjectId
	 *            The StaticObjectId to set.
	 */
	public void setStaticObjectId(int StaticObjectId)
	{
		_staticObjectId = StaticObjectId;
	}

	public int getType()
	{
		return _type;
	}

	public void setType(int type)
	{
		_type = type;
	}

	public void setMap(String texture, int x, int y)
	{
		_map = new ShowTownMap("town_map." + texture, x, y);
	}

	public ShowTownMap getMap()
	{
		return _map;
	}

	/**
	 * this is called when a player interacts with this NPC
	 * 
	 * @param player
	 */
	@Override
	public void onAction(L2PcInstance player)
	{
		if (getType() < 0 || !player.canTarget())
			return;

		// Check if the L2PcInstance already target the L2Npc
		if (this != player.getTarget())
		{
			// Set the target of the L2PcInstance player
			player.setTarget(this);
			player.sendPacket(new MyTargetSelected(getObjectId(), 0));
		}
		else
		{
			// Calculate the distance between the L2PcInstance and the L2Npc
			if (!player.isInsideRadius(this, INTERACTION_DISTANCE, false, false))
			{
				// Notify the L2PcInstance AI with AI_INTENTION_INTERACT
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
			}
			else
			{
				if (getType() == 2)
				{
					String filename = StaticHtmPath.NpcHtmPath + "signboard.htm";
					String content = HtmCache.getInstance().getHtm(filename);
					NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());

					if (content == null)
						html.setHtml("<html><body>Signboard is missing:<br>" + filename + "</body></html>");
					else
						html.setHtml(content);

					player.sendPacket(html);
				}
				else if (getType() == 0)
					player.sendPacket(getMap());

				// Send ActionFailed to the player in order to avoid he stucks
				player.sendPacket(ActionFailed.STATIC_PACKET);
			}
		}
	}

	@Override
	public void onActionShift(L2GameClient client)
	{
		L2PcInstance player = client.getActiveChar();
		if (player == null)
			return;

		if (player.getAccessLevel().isGm())
		{
			player.setTarget(this);
			player.sendPacket(new MyTargetSelected(getObjectId(), player.getLevel()));

			player.sendPacket(new StaticObject(this));

			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			final String html1 = StringUtil.concat("<html><body><center><font color=\"LEVEL\">Static Object Info</font></center><br><table border=0 width=270><tr>", "<td>Coords X,Y,Z: </td><td>", String.valueOf(getX()), ", ", String.valueOf(getY()), ", ", String.valueOf(getZ()), "</td></tr><tr><td>Object ID: </td><td>", String.valueOf(getObjectId()), "</td></tr><tr><td>Static Object ID: </td><td>", String.valueOf(getStaticObjectId()), "</td></tr><tr><td><br></td></tr><tr><td>Class: </td><td>", getClass().getSimpleName(), "</td></tr></table></body></html>");
			html.setHtml(html1);
			player.sendPacket(html);
		}

		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	@Override
	public boolean isAutoAttackable(L2Character attacker)
	{
		return false;
	}

	@Override
	public void sendInfo(L2PcInstance activeChar)
	{
		activeChar.sendPacket(new StaticObject(this));
	}
}