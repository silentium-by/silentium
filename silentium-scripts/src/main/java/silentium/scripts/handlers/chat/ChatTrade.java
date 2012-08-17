/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.handlers.chat;

import java.util.Collection;

import silentium.gameserver.data.xml.MapRegionData;
import silentium.gameserver.handler.IChatHandler;
import silentium.gameserver.model.BlockList;
import silentium.gameserver.model.L2World;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.network.serverpackets.CreatureSay;

/**
 * A chat handler
 * 
 * @author durgus
 */
public class ChatTrade implements IChatHandler
{
	private static final int[] COMMAND_IDS = { 8 };

	/**
	 * Handle chat type 'trade'
	 * 
	 * @see silentium.gameserver.handler.IChatHandler#handleChat(int, silentium.gameserver.model.actor.instance.L2PcInstance, String, String)
	 */
	@Override
	public void handleChat(int type, L2PcInstance activeChar, String target, String text)
	{
		CreatureSay cs = new CreatureSay(activeChar.getObjectId(), type, activeChar.getName(), text);
		Collection<L2PcInstance> pls = L2World.getInstance().getAllPlayers().values();
		int region = MapRegionData.getInstance().getMapRegion(activeChar.getX(), activeChar.getY());

		for (L2PcInstance player : pls)
		{
			if (!BlockList.isBlocked(player, activeChar))
				if (region == MapRegionData.getInstance().getMapRegion(player.getX(), player.getY()))
					player.sendPacket(cs);
		}
	}

	/**
	 * Returns the chat types registered to this handler
	 * 
	 * @see silentium.gameserver.handler.IChatHandler#getChatTypeList()
	 */
	@Override
	public int[] getChatTypeList()
	{
		return COMMAND_IDS;
	}
}