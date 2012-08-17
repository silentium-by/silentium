/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import silentium.gameserver.model.L2Object;
import silentium.gameserver.model.L2Skill;
import silentium.gameserver.model.actor.L2Character;
import silentium.gameserver.templates.skills.L2SkillType;

public interface ISkillHandler
{
	public static Logger _log = LoggerFactory.getLogger(ISkillHandler.class.getName());

	/**
	 * this is the worker method that is called when using a skill.
	 * 
	 * @param activeChar
	 *            The L2Character who uses that skill.
	 * @param skill
	 *            The skill object itself.
	 * @param targets
	 *            Eventual targets.
	 */
	public void useSkill(L2Character activeChar, L2Skill skill, L2Object[] targets);

	/**
	 * this method is called at initialization to register all the skill ids automatically
	 * 
	 * @return all known itemIds
	 */
	public L2SkillType[] getSkillIds();
}
