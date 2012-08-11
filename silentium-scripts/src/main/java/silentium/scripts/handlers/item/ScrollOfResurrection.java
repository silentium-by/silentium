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
import silentium.gameserver.model.L2ItemInstance;
import silentium.gameserver.model.actor.L2Character;
import silentium.gameserver.model.actor.L2Playable;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.actor.instance.L2PetInstance;
import silentium.gameserver.model.entity.Castle;
import silentium.gameserver.model.entity.TvTEvent;
import silentium.gameserver.network.SystemMessageId;
import silentium.gameserver.network.serverpackets.ActionFailed;
import silentium.gameserver.network.serverpackets.SystemMessage;
import silentium.gameserver.tables.SkillTable;

public class ScrollOfResurrection implements IItemHandler
{
	@Override
	public void useItem(L2Playable playable, L2ItemInstance item, boolean forceUse)
	{
		if (!(playable instanceof L2PcInstance))
			return;

		if (!TvTEvent.onScrollUse(playable.getObjectId()))
		{
			playable.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		final L2PcInstance activeChar = (L2PcInstance) playable;
		if (activeChar.isSitting())
		{
			activeChar.sendPacket(SystemMessageId.CANT_MOVE_SITTING);
			return;
		}

		if (activeChar.isMovementDisabled())
			return;

		final L2Character target = (L2Character) activeChar.getTarget();
		if (target != null && target.isDead())
		{
			final int itemId = item.getItemId();
			boolean allIsOk = false;

			if (target instanceof L2PcInstance)
			{
				final L2PcInstance targetPlayer = (L2PcInstance) target;

				// Check if target isn't in a active siege zone.
				Castle castle = CastleManager.getInstance().getCastle(targetPlayer.getX(), targetPlayer.getY(), targetPlayer.getZ());
				if (castle != null && castle.getSiege().getIsInProgress())
				{
					activeChar.sendPacket(SystemMessageId.CANNOT_BE_RESURRECTED_DURING_SIEGE);
					return;
				}

				// Check if the target is in a festival.
				if (targetPlayer.isFestivalParticipant())
				{
					activeChar.sendMessage("You may not resurrect participants in a festival.");
					return;
				}

				if (targetPlayer.isReviveRequested())
				{
					if (targetPlayer.isRevivingPet())
						activeChar.sendPacket(SystemMessageId.MASTER_CANNOT_RES); // While a pet is attempting to resurrect, it
																					// cannot help in resurrecting its master.
					else
						activeChar.sendPacket(SystemMessageId.RES_HAS_ALREADY_BEEN_PROPOSED); // Resurrection is already been
																								// proposed.

					return;
				}
				else if (itemId == 6387) // Pet scrolls to ress a player.
				{
					activeChar.sendMessage("You do not have the correct scroll");
					return;
				}
				allIsOk = true;
			}
			else if (target instanceof L2PetInstance)
			{
				final L2PetInstance targetPet = (L2PetInstance) target;

				// check target is not in a active siege zone
				Castle castle = CastleManager.getInstance().getCastle(targetPet.getOwner().getX(), targetPet.getOwner().getY(), targetPet.getOwner().getZ());

				if (castle != null && castle.getSiege().getIsInProgress())
				{
					activeChar.sendPacket(SystemMessageId.CANNOT_BE_RESURRECTED_DURING_SIEGE);
					return;
				}

				if (targetPet.getOwner() != activeChar)
				{
					if (targetPet.getOwner().isReviveRequested())
					{
						if (targetPet.getOwner().isRevivingPet())
							activeChar.sendPacket(SystemMessageId.RES_HAS_ALREADY_BEEN_PROPOSED); // Resurrection is already been
																									// proposed.
						else
							activeChar.sendPacket(SystemMessageId.CANNOT_RES_PET2); // A pet cannot be resurrected while it's
																					// owner is in the process of resurrecting.

						return;
					}
				}
				allIsOk = true;
			}

			if (allIsOk)
			{
				if (!activeChar.destroyItem("Consume", item.getObjectId(), 1, null, false))
					return;

				int skillId = 0;

				switch (itemId)
				{
					case 737:
						skillId = 2014;
						break; // Scroll of Resurrection

					case 3936:
						skillId = 2049;
						break; // Blessed Scroll of Resurrection

					case 3959:
						skillId = 2062;
						break; // L2Day - Blessed Scroll of Resurrection

					case 6387:
						skillId = 2179;
						break; // Blessed Scroll of Resurrection: For Pets

					case 9157:
						skillId = 2321;
						break; // Blessed Scroll of Resurrection Event
				}

				if (skillId != 0)
				{
					activeChar.useMagic(SkillTable.getInstance().getInfo(skillId, 1), true, true);
					activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_DISAPPEARED).addItemName(itemId));
				}
			}
			else
				activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
		}
		else
			activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
	}
}