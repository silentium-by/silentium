/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.handlers.item;

import silentium.gameserver.handler.IItemHandler;
import silentium.gameserver.model.L2ItemInstance;
import silentium.gameserver.model.L2Object;
import silentium.gameserver.model.actor.L2Playable;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.network.SystemMessageId;
import silentium.gameserver.network.serverpackets.MagicSkillUse;
import silentium.gameserver.templates.item.L2Item;
import silentium.gameserver.templates.item.L2Weapon;
import silentium.gameserver.templates.item.L2WeaponType;
import silentium.gameserver.utils.Broadcast;

/**
 * @author -Nemesiss-
 */
public class FishShots implements IItemHandler {
	private static final int[] SKILL_IDS = { 2181, 2182, 2183, 2184, 2185, 2186 };

	@Override
	public void useItem(final L2Playable playable, final L2ItemInstance item, final boolean forceUse) {
		if (!(playable instanceof L2PcInstance))
			return;

		final L2PcInstance activeChar = (L2PcInstance) playable;
		final L2ItemInstance weaponInst = activeChar.getActiveWeaponInstance();
		final L2Weapon weaponItem = activeChar.getActiveWeaponItem();

		if (weaponInst == null || weaponItem.getItemType() != L2WeaponType.FISHINGROD)
			return;

		// Fishshot is already active
		if (weaponInst.getChargedFishshot())
			return;

		final int FishshotId = item.getItemId();
		final int grade = weaponItem.getCrystalType();
		final int count = item.getCount();

		// 1479 - That is the wrong grade of soulshot for that fishing pole.
		if (grade == L2Item.CRYSTAL_NONE && FishshotId != 6535 || grade == L2Item.CRYSTAL_D && FishshotId != 6536 || grade == L2Item.CRYSTAL_C && FishshotId != 6537 || grade == L2Item.CRYSTAL_B && FishshotId != 6538 || grade == L2Item.CRYSTAL_A && FishshotId != 6539 || FishshotId != 6540 && grade == L2Item.CRYSTAL_S) {
			activeChar.sendPacket(SystemMessageId.WRONG_FISHINGSHOT_GRADE);
			return;
		}

		if (count < 1)
			return;

		weaponInst.setChargedFishshot(true);
		activeChar.destroyItemWithoutTrace("Consume", item.getObjectId(), 1, null, false);
		final L2Object oldTarget = activeChar.getTarget();
		activeChar.setTarget(activeChar);

		Broadcast.toSelfAndKnownPlayers(activeChar, new MagicSkillUse(activeChar, SKILL_IDS[grade], 1, 0, 0));
		activeChar.setTarget(oldTarget);
	}
}