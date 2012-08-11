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
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.network.SystemMessageId;
import silentium.gameserver.network.serverpackets.MagicSkillUse;
import silentium.gameserver.templates.item.L2Item;
import silentium.gameserver.templates.item.L2Weapon;
import silentium.gameserver.utils.Broadcast;

public class BlessedSpiritShot implements IItemHandler
{
	@Override
	public synchronized void useItem(L2Playable playable, L2ItemInstance item, boolean forceUse)
	{
		if (!(playable instanceof L2PcInstance))
			return;

		L2PcInstance activeChar = (L2PcInstance) playable;
		L2ItemInstance weaponInst = activeChar.getActiveWeaponInstance();
		L2Weapon weaponItem = activeChar.getActiveWeaponItem();
		int itemId = item.getItemId();

		// Check if bss can be used
		if (weaponInst == null || weaponItem == null || weaponItem.getSpiritShotCount() == 0)
		{
			if (!activeChar.getAutoSoulShot().contains(itemId))
				activeChar.sendPacket(SystemMessageId.CANNOT_USE_SPIRITSHOTS);
			return;
		}

		// Check if bss is already active (it can be charged over SpiritShot)
		if (weaponInst.getChargedSpiritshot() != L2ItemInstance.CHARGED_NONE)
			return;

		// Check for correct grade
		final int weaponGrade = weaponItem.getCrystalType();
		boolean gradeCheck = true;

		switch (weaponGrade)
		{
			case L2Item.CRYSTAL_NONE:
				if (itemId != 3947)
					gradeCheck = false;
				break;
			case L2Item.CRYSTAL_D:
				if (itemId != 3948)
					gradeCheck = false;
				break;
			case L2Item.CRYSTAL_C:
				if (itemId != 3949)
					gradeCheck = false;
				break;
			case L2Item.CRYSTAL_B:
				if (itemId != 3950)
					gradeCheck = false;
				break;
			case L2Item.CRYSTAL_A:
				if (itemId != 3951)
					gradeCheck = false;
				break;
			case L2Item.CRYSTAL_S:
				if (itemId != 3952)
					gradeCheck = false;
				break;
		}

		if (!gradeCheck)
		{
			if (!activeChar.getAutoSoulShot().contains(itemId))
				activeChar.sendPacket(SystemMessageId.SPIRITSHOTS_GRADE_MISMATCH);

			return;
		}

		// Consume bss if player has enough of them
		if (!CustomConfig.UNLIM_SSHOTS)
		{
			if (!activeChar.destroyItemWithoutTrace("Consume", item.getObjectId(), weaponItem.getSpiritShotCount(), null, false))
			{
				if (!activeChar.disableAutoShot(itemId))
					activeChar.sendPacket(SystemMessageId.NOT_ENOUGH_SPIRITSHOTS);

				return;
			}
		}

		// Charge bss
		weaponInst.setChargedSpiritshot(L2ItemInstance.CHARGED_BLESSED_SPIRITSHOT);

		int skillId = 0;
		switch (itemId)
		{
			case 3947:
				skillId = 2061;
				break;
			case 3948:
				skillId = 2160;
				break;
			case 3949:
				skillId = 2161;
				break;
			case 3950:
				skillId = 2162;
				break;
			case 3951:
				skillId = 2163;
				break;
			case 3952:
				skillId = 2164;
				break;
		}

		// Send message to client
		activeChar.sendPacket(SystemMessageId.ENABLED_SPIRITSHOT);
		Broadcast.toSelfAndKnownPlayersInRadiusSq(activeChar, new MagicSkillUse(activeChar, activeChar, skillId, 1, 0, 0), 360000);
	}
}