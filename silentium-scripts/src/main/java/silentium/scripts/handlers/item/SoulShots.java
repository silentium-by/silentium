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
import silentium.gameserver.skills.Stats;
import silentium.gameserver.templates.item.L2Item;
import silentium.gameserver.templates.item.L2Weapon;
import silentium.gameserver.utils.Broadcast;

public class SoulShots implements IItemHandler
{
	@Override
	public void useItem(L2Playable playable, L2ItemInstance item, boolean forceUse)
	{
		if (!(playable instanceof L2PcInstance))
			return;

		L2PcInstance activeChar = (L2PcInstance) playable;
		L2ItemInstance weaponInst = activeChar.getActiveWeaponInstance();
		L2Weapon weaponItem = activeChar.getActiveWeaponItem();
		int itemId = item.getItemId();

		// Check if soulshot can be used
		if (weaponInst == null || weaponItem.getSoulShotCount() == 0)
		{
			if (!activeChar.getAutoSoulShot().contains(itemId))
				activeChar.sendPacket(SystemMessageId.CANNOT_USE_SOULSHOTS);
			return;
		}

		final int weaponGrade = weaponItem.getCrystalType();
		boolean gradeCheck = true;

		switch (weaponGrade)
		{
			case L2Item.CRYSTAL_NONE:
				if (itemId != 5789 && itemId != 1835)
					gradeCheck = false;
				break;
			case L2Item.CRYSTAL_D:
				if (itemId != 1463)
					gradeCheck = false;
				break;
			case L2Item.CRYSTAL_C:
				if (itemId != 1464)
					gradeCheck = false;
				break;
			case L2Item.CRYSTAL_B:
				if (itemId != 1465)
					gradeCheck = false;
				break;
			case L2Item.CRYSTAL_A:
				if (itemId != 1466)
					gradeCheck = false;
				break;
			case L2Item.CRYSTAL_S:
				if (itemId != 1467)
					gradeCheck = false;
				break;
		}

		if (!gradeCheck)
		{
			if (!activeChar.getAutoSoulShot().contains(itemId))
				activeChar.sendPacket(SystemMessageId.SOULSHOTS_GRADE_MISMATCH);

			return;
		}

		activeChar.soulShotLock.lock();

		try
		{
			// Check if soulshot is already active
			if (weaponInst.getChargedSoulshot() != L2ItemInstance.CHARGED_NONE)
				return;

			// Consume Soul shots if player has enough of them
			int saSSCount = (int) activeChar.getStat().calcStat(Stats.SOULSHOT_COUNT, 0, null, null);
			int SSCount = saSSCount == 0 ? weaponItem.getSoulShotCount() : saSSCount;

			if (!CustomConfig.UNLIM_SSHOTS)
			{
				if (!activeChar.destroyItemWithoutTrace("Consume", item.getObjectId(), SSCount, null, false))
				{
					if (!activeChar.disableAutoShot(itemId))
						activeChar.sendPacket(SystemMessageId.NOT_ENOUGH_SOULSHOTS);

					return;
				}
			}

			// Charge soulshot
			weaponInst.setChargedSoulshot(L2ItemInstance.CHARGED_SOULSHOT);
		}
		finally
		{
			activeChar.soulShotLock.unlock();
		}

		int skillId = 0;
		switch (itemId)
		{
			case 1835:
			case 5789:
				skillId = 2039;
				break;
			case 1463:
				skillId = 2150;
				break;
			case 1464:
				skillId = 2151;
				break;
			case 1465:
				skillId = 2152;
				break;
			case 1466:
				skillId = 2153;
				break;
			case 1467:
				skillId = 2154;
				break;
		}

		// Send message to client
		activeChar.sendPacket(SystemMessageId.ENABLED_SOULSHOT);
		Broadcast.toSelfAndKnownPlayersInRadiusSq(activeChar, new MagicSkillUse(activeChar, activeChar, skillId, 1, 0, 0), 360000);
	}
}