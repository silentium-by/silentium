/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.handlers.skill;

import silentium.gameserver.handler.ISkillHandler;
import silentium.gameserver.model.L2ItemInstance;
import silentium.gameserver.model.L2Object;
import silentium.gameserver.model.L2Skill;
import silentium.gameserver.model.actor.L2Character;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.network.SystemMessageId;
import silentium.gameserver.network.serverpackets.SystemMessage;
import silentium.gameserver.skills.Formulas;
import silentium.gameserver.templates.item.L2WeaponType;
import silentium.gameserver.templates.skills.L2SkillType;

/**
 * @author _tomciaaa_
 */
public class StrSiegeAssault implements ISkillHandler {
	private static final L2SkillType[] SKILL_IDS = { L2SkillType.STRSIEGEASSAULT };

	@Override
	public void useSkill(final L2Character activeChar, final L2Skill skill, final L2Object... targets) {
		if (!(activeChar instanceof L2PcInstance))
			return;

		final L2PcInstance player = (L2PcInstance) activeChar;

		// Checks
		if (player.checkIfOkToUseStriderSiegeAssault(skill)) {
			// damage calculation
			int damage = 0;

			for (final L2Character target : (L2Character[]) targets) {
				final L2ItemInstance weapon = activeChar.getActiveWeaponInstance();
				if (target.isAlikeDead())
					continue;

				final boolean dual = activeChar.isUsingDualWeapon();
				final byte shld = Formulas.calcShldUse(activeChar, target);
				final boolean crit = Formulas.calcCrit(activeChar.getCriticalHit(target, skill));
				final boolean soul = weapon != null && weapon.getChargedSoulshot() == L2ItemInstance.CHARGED_SOULSHOT && weapon.getItemType() != L2WeaponType.DAGGER;

				damage = !crit && (skill.getCondition() & L2Skill.COND_CRIT) != 0 ? 0 : (int) Formulas.calcPhysDam(activeChar, target, skill, shld, crit, dual, soul);

				if (damage > 0) {
					activeChar.sendDamageMessage(target, damage, false, false, false);
					target.reduceCurrentHp(damage, activeChar, skill);
				} else
					activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.ATTACK_FAILED));

				if (soul && weapon != null)
					weapon.setChargedSoulshot(L2ItemInstance.CHARGED_NONE);
			}
		}
	}

	@Override
	public L2SkillType[] getSkillIds() {
		return SKILL_IDS;
	}
}