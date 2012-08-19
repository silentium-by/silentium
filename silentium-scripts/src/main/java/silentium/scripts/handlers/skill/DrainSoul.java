/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.handlers.skill;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import silentium.gameserver.handler.ISkillHandler;
import silentium.gameserver.model.L2Object;
import silentium.gameserver.model.L2Skill;
import silentium.gameserver.model.actor.L2Character;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.templates.skills.L2SkillType;

/**
 * @author _drunk_
 */
public class DrainSoul implements ISkillHandler {
	private static final Logger _log = LoggerFactory.getLogger(DrainSoul.class.getName());
	private static final L2SkillType[] SKILL_IDS = { L2SkillType.DRAIN_SOUL };

	@Override
	public void useSkill(final L2Character activeChar, final L2Skill skill, final L2Object... targets) {
		if (!(activeChar instanceof L2PcInstance))
			return;

		final L2Object[] targetList = skill.getTargetList(activeChar);

		if (targetList == null)
			return;

		_log.debug("Soul Crystal casting succeded.");
	}

	@Override
	public L2SkillType[] getSkillIds() {
		return SKILL_IDS;
	}
}
