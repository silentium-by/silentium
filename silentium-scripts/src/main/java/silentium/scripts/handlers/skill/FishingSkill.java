/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.handlers.skill;

import silentium.gameserver.handler.ISkillHandler;
import silentium.gameserver.model.L2Fishing;
import silentium.gameserver.model.L2ItemInstance;
import silentium.gameserver.model.L2Object;
import silentium.gameserver.model.L2Skill;
import silentium.gameserver.model.actor.L2Character;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.network.SystemMessageId;
import silentium.gameserver.network.serverpackets.ActionFailed;
import silentium.gameserver.templates.item.L2Weapon;
import silentium.gameserver.templates.skills.L2SkillType;

public class FishingSkill implements ISkillHandler {
	private static final L2SkillType[] SKILL_IDS = { L2SkillType.PUMPING, L2SkillType.REELING };

	@Override
	public void useSkill(final L2Character activeChar, final L2Skill skill, final L2Object... targets) {
		if (!(activeChar instanceof L2PcInstance))
			return;

		final L2PcInstance player = (L2PcInstance) activeChar;

		final L2Fishing fish = player.getFishCombat();
		if (fish == null) {
			if (skill.getSkillType() == L2SkillType.PUMPING) {
				// Pumping skill is available only while fishing
				player.sendPacket(SystemMessageId.CAN_USE_PUMPING_ONLY_WHILE_FISHING);
			} else if (skill.getSkillType() == L2SkillType.REELING) {
				// Reeling skill is available only while fishing
				player.sendPacket(SystemMessageId.CAN_USE_REELING_ONLY_WHILE_FISHING);
			}
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		final L2Weapon weaponItem = player.getActiveWeaponItem();
		final L2ItemInstance weaponInst = activeChar.getActiveWeaponInstance();

		if (weaponInst == null || weaponItem == null)
			return;

		int SS = 1;
		int pen = 0;
		if (weaponInst.getChargedFishshot())
			SS = 2;
		final double gradebonus = 1 + weaponItem.getCrystalType() * 0.1;
		int dmg = (int) (skill.getPower() * gradebonus * SS);

		if (player.getSkillLevel(1315) <= skill.getLevel() - 2) // 1315 - Fish Expertise
		{
			// Penalty
			player.sendPacket(SystemMessageId.REELING_PUMPING_3_LEVELS_HIGHER_THAN_FISHING_PENALTY);
			pen = 50;
			final int penatlydmg = dmg - pen;
			dmg = penatlydmg;
		}

		if (SS > 1)
			weaponInst.setChargedFishshot(false);

		if (skill.getSkillType() == L2SkillType.REELING) // Realing
			fish.useRealing(dmg, pen);
		else
			// Pumping
			fish.usePomping(dmg, pen);
	}

	@Override
	public L2SkillType[] getSkillIds() {
		return SKILL_IDS;
	}
}