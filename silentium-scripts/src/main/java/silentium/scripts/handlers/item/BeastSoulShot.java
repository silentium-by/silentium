/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.handlers.item;

import silentium.gameserver.configs.CustomConfig;
import silentium.gameserver.handler.IItemHandler;
import silentium.gameserver.model.L2ItemInstance;
import silentium.gameserver.model.actor.L2Playable;
import silentium.gameserver.model.actor.L2Summon;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.actor.instance.L2PetInstance;
import silentium.gameserver.network.SystemMessageId;
import silentium.gameserver.network.serverpackets.MagicSkillUse;
import silentium.gameserver.network.serverpackets.SystemMessage;
import silentium.gameserver.utils.Broadcast;

/**
 * Beast SoulShot Handler
 *
 * @author Tempy
 */
public class BeastSoulShot implements IItemHandler
{
	@Override
	public void useItem(L2Playable playable, L2ItemInstance item, boolean forceUse)
	{
		if (playable == null)
			return;

		final L2PcInstance activeOwner = playable.getActingPlayer();
		if (activeOwner == null)
			return;

		if (playable instanceof L2Summon)
		{
			activeOwner.sendPacket(SystemMessageId.PET_CANNOT_USE_ITEM);
			return;
		}

		final L2Summon activePet = activeOwner.getPet();
		if (activePet == null)
		{
			activeOwner.sendPacket(SystemMessageId.PETS_ARE_NOT_AVAILABLE_AT_THIS_TIME);
			return;
		}

		if (activePet.isDead())
		{
			activeOwner.sendPacket(SystemMessageId.SOULSHOTS_AND_SPIRITSHOTS_ARE_NOT_AVAILABLE_FOR_A_DEAD_PET);
			return;
		}

		final int itemId = item.getItemId();
		short shotConsumption = activePet.getSoulShotsPerHit();

		if (!(item.getCount() > shotConsumption))
		{
			// Not enough Soulshots to use.
			if (!activeOwner.disableAutoShot(itemId))
				activeOwner.sendPacket(SystemMessageId.NOT_ENOUGH_SOULSHOTS_FOR_PET);
			return;
		}

		L2ItemInstance weaponInst = null;

		if (activePet instanceof L2PetInstance)
			weaponInst = ((L2PetInstance) activePet).getActiveWeaponInstance();

		if (weaponInst == null)
		{
			if (activePet.getChargedSoulShot() != L2ItemInstance.CHARGED_NONE)
				return;

			activePet.setChargedSoulShot(L2ItemInstance.CHARGED_SOULSHOT);
		}
		else
		{
			// SoulShots are already active.
			if (weaponInst.getChargedSoulshot() != L2ItemInstance.CHARGED_NONE)
				return;

			weaponInst.setChargedSoulshot(L2ItemInstance.CHARGED_SOULSHOT);
		}

		// If the player doesn't have enough beast soulshot remaining, remove any auto soulshot task.
		if (!CustomConfig.UNLIM_SHOTS)
		{
			if (!activeOwner.destroyItemWithoutTrace("Consume", item.getObjectId(), shotConsumption, null, false))
			{
				if (!activeOwner.disableAutoShot(itemId))
					activeOwner.sendPacket(SystemMessageId.NOT_ENOUGH_SOULSHOTS_FOR_PET);
				return;
			}
		}

		activeOwner.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.PET_USES_S1).addItemName(itemId));
		Broadcast.toSelfAndKnownPlayersInRadiusSq(activeOwner, new MagicSkillUse(activePet, activePet, 2033, 1, 0, 0), 360000);
	}
}