/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
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
 * Beast SpiritShot Handler
 *
 * @author Tempy
 */
public class BeastSpiritShot implements IItemHandler {
	@Override
	public void useItem(final L2Playable playable, final L2ItemInstance item, final boolean forceUse) {
		if (playable == null)
			return;

		final L2PcInstance activeOwner = playable.getActingPlayer();
		if (activeOwner == null)
			return;

		if (playable instanceof L2Summon) {
			activeOwner.sendPacket(SystemMessageId.PET_CANNOT_USE_ITEM);
			return;
		}

		final L2Summon activePet = activeOwner.getPet();
		if (activePet == null) {
			activeOwner.sendPacket(SystemMessageId.PETS_ARE_NOT_AVAILABLE_AT_THIS_TIME);
			return;
		}

		if (activePet.isDead()) {
			activeOwner.sendPacket(SystemMessageId.SOULSHOTS_AND_SPIRITSHOTS_ARE_NOT_AVAILABLE_FOR_A_DEAD_PET);
			return;
		}

		final int itemId = item.getItemId();
		final short shotConsumption = activePet.getSpiritShotsPerHit();

		if (!(item.getCount() > shotConsumption)) {
			// Not enough SpiritShots to use.
			if (!activeOwner.disableAutoShot(itemId))
				activeOwner.sendPacket(SystemMessageId.NOT_ENOUGH_SPIRITSHOTS_FOR_PET);
			return;
		}

		L2ItemInstance weaponInst = null;

		if (activePet instanceof L2PetInstance)
			weaponInst = activePet.getActiveWeaponInstance();

		if (weaponInst == null) {
			if (activePet.getChargedSpiritShot() != L2ItemInstance.CHARGED_NONE)
				return;

			if (itemId == 6647)
				activePet.setChargedSpiritShot(L2ItemInstance.CHARGED_BLESSED_SPIRITSHOT);
			else
				activePet.setChargedSpiritShot(L2ItemInstance.CHARGED_SPIRITSHOT);
		} else {
			// SpiritShots are already active.
			if (weaponInst.getChargedSpiritshot() != L2ItemInstance.CHARGED_NONE)
				return;

			if (itemId == 6647)
				weaponInst.setChargedSpiritshot(L2ItemInstance.CHARGED_BLESSED_SPIRITSHOT);
			else
				weaponInst.setChargedSpiritshot(L2ItemInstance.CHARGED_SPIRITSHOT);
		}

		if (!CustomConfig.UNLIM_SHOTS) {
			if (!activeOwner.destroyItemWithoutTrace("Consume", item.getObjectId(), shotConsumption, null, false)) {
				if (!activeOwner.disableAutoShot(itemId))
					activeOwner.sendPacket(SystemMessageId.NOT_ENOUGH_SPIRITSHOTS_FOR_PET);
				return;
			}
		}

		activeOwner.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.PET_USES_S1).addItemName(itemId));
		Broadcast.toSelfAndKnownPlayersInRadiusSq(activeOwner, new MagicSkillUse(activePet, activePet, itemId == 6646 ? 2008 : 2009, 1, 0, 0), 360000);
	}
}