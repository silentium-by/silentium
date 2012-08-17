/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.handlers.chat;

import silentium.gameserver.handler.IChatHandler;
import silentium.gameserver.model.PartyMatchRoom;
import silentium.gameserver.model.PartyMatchRoomList;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.network.serverpackets.CreatureSay;

/**
 * A chat handler
 * 
 * @author Gnacik
 */
public class ChatPartyMatchRoom implements IChatHandler
{
	private static final int[] COMMAND_IDS = { 14 };

	/**
	 * Handle chat type 'PartyMatchRoom'
	 * 
	 * @see silentium.gameserver.handler.IChatHandler#handleChat(int, silentium.gameserver.model.actor.instance.L2PcInstance, String, String)
	 */
	@Override
	public void handleChat(int type, L2PcInstance activeChar, String target, String text)
	{
		if (activeChar.isInPartyMatchRoom())
		{
			PartyMatchRoom _room = PartyMatchRoomList.getInstance().getPlayerRoom(activeChar);
			if (_room != null)
			{
				CreatureSay cs = new CreatureSay(activeChar.getObjectId(), type, activeChar.getName(), text);
				for (L2PcInstance _member : _room.getPartyMembers())
					_member.sendPacket(cs);
			}
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