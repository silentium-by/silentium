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
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.network.SystemMessageId;
import silentium.gameserver.network.serverpackets.MagicSkillUse;
import silentium.gameserver.templates.item.L2Item;
import silentium.gameserver.templates.item.L2Weapon;
import silentium.gameserver.utils.Broadcast;

public class SpiritShot implements IItemHandler {
	@Override
	public synchronized void useItem(final L2Playable playable, final L2ItemInstance item, final boolean forceUse) {
		if (!(playable instanceof L2PcInstance))
			return;

		final L2PcInstance activeChar = (L2PcInstance) playable;
		final L2ItemInstance weaponInst = activeChar.getActiveWeaponInstance();
		final L2Weapon weaponItem = activeChar.getActiveWeaponItem();
		final int itemId = item.getItemId();

		// Check if sps can be used
		if (weaponInst == null || weaponItem.getSpiritShotCount() == 0) {
			if (!activeChar.getAutoSoulShot().contains(itemId))
				activeChar.sendPacket(SystemMessageId.CANNOT_USE_SPIRITSHOTS);
			return;
		}

		// Check if sps is already active
		if (weaponInst.getChargedSpiritshot() != L2ItemInstance.CHARGED_NONE)
			return;

		final int weaponGrade = weaponItem.getCrystalType();
		boolean gradeCheck = true;

		switch (weaponGrade) {
			case L2Item.CRYSTAL_NONE:
				if (itemId != 5790 && itemId != 2509)
					gradeCheck = false;
				break;
			case L2Item.CRYSTAL_D:
				if (itemId != 2510)
					gradeCheck = false;
				break;
			case L2Item.CRYSTAL_C:
				if (itemId != 2511)
					gradeCheck = false;
				break;
			case L2Item.CRYSTAL_B:
				if (itemId != 2512)
					gradeCheck = false;
				break;
			case L2Item.CRYSTAL_A:
				if (itemId != 2513)
					gradeCheck = false;
				break;
			case L2Item.CRYSTAL_S:
				if (itemId != 2514)
					gradeCheck = false;
				break;
		}

		if (!gradeCheck) {
			if (!activeChar.getAutoSoulShot().contains(itemId))
				activeChar.sendPacket(SystemMessageId.SPIRITSHOTS_GRADE_MISMATCH);

			return;
		}

		// Consume sps if player has enough of them
		if (!CustomConfig.UNLIM_SSHOTS) {
			if (!activeChar.destroyItemWithoutTrace("Consume", item.getObjectId(), weaponItem.getSpiritShotCount(), null, false)) {
				if (!activeChar.disableAutoShot(itemId))
					activeChar.sendPacket(SystemMessageId.NOT_ENOUGH_SPIRITSHOTS);

				return;
			}
		}

		// Charge sps
		weaponInst.setChargedSpiritshot(L2ItemInstance.CHARGED_SPIRITSHOT);

		int skillId = 0;
		switch (itemId) {
			case 2509:
			case 5790:
				skillId = 2061;
				break;
			case 2510:
				skillId = 2155;
				break;
			case 2511:
				skillId = 2156;
				break;
			case 2512:
				skillId = 2157;
				break;
			case 2513:
				skillId = 2158;
				break;
			case 2514:
				skillId = 2159;
				break;
		}

		// Send message to client
		activeChar.sendPacket(SystemMessageId.ENABLED_SPIRITSHOT);
		Broadcast.toSelfAndKnownPlayersInRadiusSq(activeChar, new MagicSkillUse(activeChar, activeChar, skillId, 1, 0, 0), 360000);
	}
}