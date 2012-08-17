/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.network.clientpackets;

import silentium.gameserver.model.L2World;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.network.SystemMessageId;
import silentium.gameserver.network.serverpackets.ExAskJoinPartyRoom;
import silentium.gameserver.network.serverpackets.SystemMessage;

/**
 * Format: (ch) S
 * 
 * @author -Wooden-
 */
public class RequestAskJoinPartyRoom extends L2GameClientPacket
{
	private static String _name;

	@Override
	protected void readImpl()
	{
		_name = readS();
	}

	@Override
	protected void runImpl()
	{
		final L2PcInstance _activeChar = getClient().getActiveChar();
		if (_activeChar == null)
			return;

		// Send PartyRoom invite request (with activeChar) name to the target
		final L2PcInstance _target = L2World.getInstance().getPlayer(_name);
		if (_target != null)
		{
			if (!_target.isProcessingRequest())
			{
				_activeChar.onTransactionRequest(_target);
				_target.sendPacket(new ExAskJoinPartyRoom(_activeChar.getName()));
			}
			else
				_activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_IS_BUSY_TRY_LATER).addPcName(_target));
		}
		else
			_activeChar.sendPacket(SystemMessageId.TARGET_IS_NOT_FOUND_IN_THE_GAME);
	}
}