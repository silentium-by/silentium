/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.handlers.chat;

import java.util.Collection;
import java.util.StringTokenizer;

import silentium.gameserver.handler.IChatHandler;
import silentium.gameserver.handler.IVoicedCommandHandler;
import silentium.gameserver.handler.VoicedCommandHandler;
import silentium.gameserver.model.BlockList;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.network.serverpackets.CreatureSay;

/**
 * A chat handler
 * 
 * @author durgus
 */
public class ChatAll implements IChatHandler
{
	private static final int[] COMMAND_IDS = { 0 };

	/**
	 * Handle chat type 'all'
	 * 
	 * @see silentium.gameserver.handler.IChatHandler#handleChat(int, silentium.gameserver.model.actor.instance.L2PcInstance, String, String)
	 */
	@Override
	public void handleChat(int type, L2PcInstance activeChar, String target, String text)
	{
		if (text.startsWith("."))
		{
			StringTokenizer st = new StringTokenizer(text);
			IVoicedCommandHandler vch;
			String command = "";
			if (st.countTokens() > 1)
			{
				command = st.nextToken().substring(1);
				target = text.substring(command.length() + 2);
				vch = VoicedCommandHandler.getInstance().getHandler(command);
			}
			else
			{
				command = text.substring(1);
				vch = VoicedCommandHandler.getInstance().getHandler(command);
			}
			if (vch != null)
				vch.useVoicedCommand(command, activeChar, target);
		}
		else
		{
			CreatureSay cs = new CreatureSay(activeChar.getObjectId(), type, activeChar.getName(), text);
			Collection<L2PcInstance> plrs = activeChar.getKnownList().getKnownPlayers().values();

			for (L2PcInstance player : plrs)
			{
				if (player != null && activeChar.isInsideRadius(player, 1250, false, true) && !BlockList.isBlocked(player, activeChar))
					player.sendPacket(cs);
			}

			activeChar.sendPacket(cs);
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