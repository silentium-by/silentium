/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.handlers.skill;

import silentium.gameserver.handler.ISkillHandler;
import silentium.gameserver.model.L2Object;
import silentium.gameserver.model.L2Skill;
import silentium.gameserver.model.actor.L2Character;
import silentium.gameserver.templates.skills.L2SkillType;

/**
 * @author Forsaiken
 */
public class GiveSp implements ISkillHandler {
	private static final L2SkillType[] SKILL_IDS = { L2SkillType.GIVE_SP };

	/**
	 * @see silentium.gameserver.handler.ISkillHandler#useSkill(silentium.gameserver.model.actor.L2Character, silentium.gameserver.model.L2Skill,
	 *      silentium.gameserver.model.L2Object[])
	 */
	@Override
	public void useSkill(final L2Character activeChar, final L2Skill skill, final L2Object... targets) {
		for (final L2Object obj : targets) {
			final L2Character target = (L2Character) obj;
			if (target != null) {
				final int spToAdd = (int) skill.getPower();
				target.addExpAndSp(0, spToAdd);
			}
		}
	}

	/**
	 * @see silentium.gameserver.handler.ISkillHandler#getSkillIds()
	 */
	@Override
	public L2SkillType[] getSkillIds() {
		return SKILL_IDS;
	}
}