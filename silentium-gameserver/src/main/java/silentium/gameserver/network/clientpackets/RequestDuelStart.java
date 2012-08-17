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
import silentium.gameserver.network.serverpackets.ExDuelAskStart;
import silentium.gameserver.network.serverpackets.SystemMessage;

/**
 * Format:(ch) Sd
 * 
 * @author -Wooden-
 */
public final class RequestDuelStart extends L2GameClientPacket
{
	private String _player;
	private int _partyDuel;

	@Override
	protected void readImpl()
	{
		_player = readS();
		_partyDuel = readD();
	}

	@Override
	protected void runImpl()
	{
		final L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;

		final L2PcInstance targetChar = L2World.getInstance().getPlayer(_player);
		if (targetChar == null || activeChar == targetChar)
		{
			activeChar.sendPacket(SystemMessageId.THERE_IS_NO_OPPONENT_TO_RECEIVE_YOUR_CHALLENGE_FOR_A_DUEL);
			return;
		}

		// Check if duel is possible
		if (!activeChar.canDuel())
		{
			activeChar.sendPacket(SystemMessageId.YOU_ARE_UNABLE_TO_REQUEST_A_DUEL_AT_THIS_TIME);
			return;
		}

		if (!targetChar.canDuel())
		{
			activeChar.sendPacket(targetChar.getNoDuelReason());
			return;
		}

		// Players may not be too far apart
		if (!activeChar.isInsideRadius(targetChar, 250, false, false))
		{
			activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_RECEIVE_A_DUEL_CHALLENGE_BECAUSE_S1_IS_TOO_FAR_AWAY).addPcName(targetChar));
			return;
		}

		// Duel is a party duel
		if (_partyDuel == 1)
		{
			// Player must be in a party & the party leader
			if (!activeChar.isInParty() || !(activeChar.getParty().isLeader(activeChar)))
			{
				activeChar.sendMessage("You have to be the leader of a party in order to request a party duel.");
				return;
			}

			// Target must be in a party
			if (!targetChar.isInParty())
			{
				activeChar.sendPacket(SystemMessageId.SINCE_THE_PERSON_YOU_CHALLENGED_IS_NOT_CURRENTLY_IN_A_PARTY_THEY_CANNOT_DUEL_AGAINST_YOUR_PARTY);
				return;
			}

			// Target may not be of the same party
			if (activeChar.getParty().getPartyMembers().contains(targetChar))
			{
				activeChar.sendMessage("This player is a member of your own party.");
				return;
			}

			// Check if every player is ready for a duel
			for (L2PcInstance temp : activeChar.getParty().getPartyMembers())
			{
				if (!temp.canDuel())
				{
					activeChar.sendPacket(SystemMessageId.YOU_ARE_UNABLE_TO_REQUEST_A_DUEL_AT_THIS_TIME);
					return;
				}
			}

			for (L2PcInstance temp : targetChar.getParty().getPartyMembers())
			{
				if (!temp.canDuel())
				{
					activeChar.sendPacket(SystemMessageId.THE_OPPOSING_PARTY_IS_CURRENTLY_UNABLE_TO_ACCEPT_A_CHALLENGE_TO_A_DUEL);
					return;
				}
			}

			final L2PcInstance partyLeader = targetChar.getParty().getLeader();

			// Send request to targetChar's party leader
			if (!partyLeader.isProcessingRequest())
			{
				activeChar.onTransactionRequest(partyLeader);
				partyLeader.sendPacket(new ExDuelAskStart(activeChar.getName(), _partyDuel));

				log.debug(activeChar.getName() + " requested a duel with " + partyLeader.getName());

				activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_PARTY_HAS_BEEN_CHALLENGED_TO_A_DUEL).addPcName(partyLeader));
				targetChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_PARTY_HAS_CHALLENGED_YOUR_PARTY_TO_A_DUEL).addPcName(activeChar));
			}
			else
				activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_IS_BUSY_TRY_LATER).addPcName(partyLeader));
		}
		else
		// 1vs1 duel
		{
			if (!targetChar.isProcessingRequest())
			{
				activeChar.onTransactionRequest(targetChar);
				targetChar.sendPacket(new ExDuelAskStart(activeChar.getName(), _partyDuel));

				log.debug(activeChar.getName() + " requested a duel with " + targetChar.getName());

				activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_HAS_BEEN_CHALLENGED_TO_A_DUEL).addPcName(targetChar));
				targetChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_HAS_CHALLENGED_YOU_TO_A_DUEL).addPcName(activeChar));
			}
			else
				activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_IS_BUSY_TRY_LATER).addPcName(targetChar));
		}
	}
}