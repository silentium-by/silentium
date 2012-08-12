/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.handlers.item;

import silentium.gameserver.handler.IItemHandler;
import silentium.gameserver.instancemanager.CastleManager;
import silentium.gameserver.instancemanager.MercTicketManager;
import silentium.gameserver.model.L2ItemInstance;
import silentium.gameserver.model.actor.L2Playable;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.entity.Castle;
import silentium.gameserver.model.entity.sevensigns.SevenSigns;
import silentium.gameserver.network.SystemMessageId;
import silentium.gameserver.network.serverpackets.SystemMessage;

/**
 * Handler to use mercenary tickets.<br>
 * <br>
 * Check constraints:
 * <ul>
 * <li>Tickets may only be used in a castle</li>
 * <li>Only specific tickets may be used in each castle (different tickets for each castle)</li>
 * <li>Only the owner of that castle may use them</li>
 * <li>tickets cannot be used during siege</li>
 * <li>Check if max number of tickets has been reached</li>
 * <li>Check if max number of tickets from this ticket's TYPE has been reached</li>
 * </ul>
 * If allowed, call the MercTicketManager to add the item and spawn in the world.<br>
 * Remove the item from the person's inventory.
 */
public class MercTicket implements IItemHandler
{
	@Override
	public void useItem(L2Playable playable, L2ItemInstance item, boolean forceUse)
	{
		final L2PcInstance activeChar = (L2PcInstance) playable;
		if (activeChar == null)
			return;

		final Castle castle = CastleManager.getInstance().getCastle(activeChar);
		if (castle == null)
			return;

		final int castleId = castle.getCastleId();
		final int itemId = item.getItemId();

		// add check that certain tickets can only be placed in certain castles
		if (MercTicketManager.getInstance().getTicketCastleId(itemId) != castleId)
		{
			activeChar.sendPacket(SystemMessageId.MERCENARIES_CANNOT_BE_POSITIONED_HERE);
			return;
		}

		if (!activeChar.isCastleLord(castleId))
		{
			activeChar.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_AUTHORITY_TO_POSITION_MERCENARIES);
			return;
		}

		if (castle.getSiege().getIsInProgress())
		{
			activeChar.sendPacket(SystemMessageId.THIS_MERCENARY_CANNOT_BE_POSITIONED_ANYMORE);
			return;
		}

		// Checking Seven Signs Quest Period
		if (SevenSigns.getInstance().getCurrentPeriod() != SevenSigns.PERIOD_SEAL_VALIDATION)
		{
			activeChar.sendPacket(SystemMessageId.THIS_MERCENARY_CANNOT_BE_POSITIONED_ANYMORE);
			return;
		}

		// Checking the Seal of Strife status
		switch (SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE))
		{
			case SevenSigns.CABAL_NULL:
				if (SevenSigns.getInstance().checkIsDawnPostingTicket(itemId))
				{
					activeChar.sendPacket(SystemMessageId.THIS_MERCENARY_CANNOT_BE_POSITIONED_ANYMORE);
					return;
				}
				break;

			case SevenSigns.CABAL_DUSK:
				if (!SevenSigns.getInstance().checkIsRookiePostingTicket(itemId))
				{
					activeChar.sendPacket(SystemMessageId.THIS_MERCENARY_CANNOT_BE_POSITIONED_ANYMORE);
					return;
				}
				break;
		}

		if (MercTicketManager.getInstance().isAtCasleLimit(item.getItemId()))
		{
			activeChar.sendPacket(SystemMessageId.THIS_MERCENARY_CANNOT_BE_POSITIONED_ANYMORE);
			return;
		}

		if (MercTicketManager.getInstance().isAtTypeLimit(item.getItemId()))
		{
			activeChar.sendPacket(SystemMessageId.THIS_MERCENARY_CANNOT_BE_POSITIONED_ANYMORE);
			return;
		}
		if (MercTicketManager.getInstance().isTooCloseToAnotherTicket(activeChar.getX(), activeChar.getY(), activeChar.getZ()))
		{
			activeChar.sendPacket(SystemMessageId.POSITIONING_CANNOT_BE_DONE_BECAUSE_DISTANCE_BETWEEN_MERCENARIES_TOO_SHORT);
			return;
		}

		MercTicketManager.getInstance().addTicket(item.getItemId(), activeChar, null);
		activeChar.destroyItem("Consume", item.getObjectId(), 1, null, false); // Remove item from char's inventory
		activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.PLACE_CURRENT_LOCATION_DIRECTION).addItemName(itemId));
	}
}