/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.handlers.chat;

import silentium.gameserver.handler.IChatHandler;
import silentium.gameserver.model.BlockList;
import silentium.gameserver.model.L2World;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.network.SystemMessageId;
import silentium.gameserver.network.serverpackets.CreatureSay;

/**
 * A chat handler
 * 
 * @author durgus
 */
public class ChatTell implements IChatHandler
{
	private static final int[] COMMAND_IDS = { 2 };

	/**
	 * Handle chat type 'tell'
	 * 
	 * @see silentium.gameserver.handler.IChatHandler#handleChat(int, silentium.gameserver.model.actor.instance.L2PcInstance, String, String)
	 */
	@Override
	public void handleChat(int type, L2PcInstance activeChar, String target, String text)
	{
		// Return if no target is set.
		if (target == null)
			return;

		final L2PcInstance receiver = L2World.getInstance().getPlayer(target);
		if (receiver != null)
		{
			if (activeChar.equals(receiver))
			{
				activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
				return;
			}

			if (receiver.isInJail() || receiver.isChatBanned())
			{
				activeChar.sendPacket(SystemMessageId.TARGET_IS_CHAT_BANNED);
				return;
			}

			if (receiver.getClient().isDetached())
			{
				activeChar.sendPacket(SystemMessageId.TARGET_IS_NOT_FOUND_IN_THE_GAME);
				return;
			}

			if (!activeChar.isGM() && (receiver.isInRefusalMode() || BlockList.isBlocked(receiver, activeChar)))
			{
				activeChar.sendPacket(SystemMessageId.THE_PERSON_IS_IN_MESSAGE_REFUSAL_MODE);
				return;
			}

			receiver.sendPacket(new CreatureSay(activeChar.getObjectId(), type, activeChar.getName(), text));
			activeChar.sendPacket(new CreatureSay(activeChar.getObjectId(), type, "->" + receiver.getName(), text));
		}
		else
			activeChar.sendPacket(SystemMessageId.TARGET_IS_NOT_FOUND_IN_THE_GAME);
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