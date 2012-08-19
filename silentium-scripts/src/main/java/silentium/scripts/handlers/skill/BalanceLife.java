/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.handlers.skill;

import silentium.gameserver.handler.ISkillHandler;
import silentium.gameserver.handler.SkillHandler;
import silentium.gameserver.model.L2Object;
import silentium.gameserver.model.L2Skill;
import silentium.gameserver.model.actor.L2Character;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.network.serverpackets.StatusUpdate;
import silentium.gameserver.templates.skills.L2SkillType;

/**
 * @author earendil
 */
public class BalanceLife implements ISkillHandler {
	private static final L2SkillType[] SKILL_IDS = { L2SkillType.BALANCE_LIFE };

	@Override
	public void useSkill(final L2Character activeChar, final L2Skill skill, final L2Object... targets) {
		final ISkillHandler handler = SkillHandler.getInstance().getSkillHandler(L2SkillType.BUFF);

		if (handler != null)
			handler.useSkill(activeChar, skill, targets);

		final L2PcInstance player = activeChar.getActingPlayer();

		double fullHP = 0;
		double currentHPs = 0;

		for (final L2Character target : (L2Character[]) targets) {
			// We should not heal if char is dead
			if (target == null || target.isDead())
				continue;

			// Player holding a cursed weapon can't be healed and can't heal
			if (target != activeChar) {
				if (target instanceof L2PcInstance && ((L2PcInstance) target).isCursedWeaponEquipped())
					continue;
				else if (player != null && player.isCursedWeaponEquipped())
					continue;
			}

			fullHP += target.getMaxHp();
			currentHPs += target.getCurrentHp();
		}

		final double percentHP = currentHPs / fullHP;

		for (final L2Character target : (L2Character[]) targets) {
			if (target == null || target.isDead())
				continue;

			// Player holding a cursed weapon can't be healed and can't heal
			if (target != activeChar) {
				if (target instanceof L2PcInstance && ((L2PcInstance) target).isCursedWeaponEquipped())
					continue;
				else if (player != null && player.isCursedWeaponEquipped())
					continue;
			}

			final double newHP = target.getMaxHp() * percentHP;

			target.setCurrentHp(newHP);

			final StatusUpdate su = new StatusUpdate(target);
			su.addAttribute(StatusUpdate.CUR_HP, (int) target.getCurrentHp());
			target.sendPacket(su);
		}
	}

	@Override
	public L2SkillType[] getSkillIds() {
		return SKILL_IDS;
	}
}