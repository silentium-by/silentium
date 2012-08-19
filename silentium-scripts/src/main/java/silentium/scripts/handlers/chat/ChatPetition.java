/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.handlers.chat;

import silentium.gameserver.handler.IChatHandler;
import silentium.gameserver.instancemanager.PetitionManager;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.network.SystemMessageId;

/**
 * A chat handler
 *
 * @author durgus
 */
public class ChatPetition implements IChatHandler {
	private static final int[] COMMAND_IDS = { 6, 7 };

	/**
	 * Handle chat type 'petition player'
	 *
	 * @see silentium.gameserver.handler.IChatHandler#handleChat(int, silentium.gameserver.model.actor.instance.L2PcInstance, String, String)
	 */
	@Override
	public void handleChat(final int type, final L2PcInstance activeChar, final String target, final String text) {
		if (!PetitionManager.getInstance().isPlayerInConsultation(activeChar)) {
			activeChar.sendPacket(SystemMessageId.YOU_ARE_NOT_IN_PETITION_CHAT);
			return;
		}

		PetitionManager.getInstance().sendActivePetitionMessage(activeChar, text);
	}

	/**
	 * Returns the chat types registered to this handler
	 *
	 * @see silentium.gameserver.handler.IChatHandler#getChatTypeList()
	 */
	@Override
	public int[] getChatTypeList() {
		return COMMAND_IDS;
	}
}
