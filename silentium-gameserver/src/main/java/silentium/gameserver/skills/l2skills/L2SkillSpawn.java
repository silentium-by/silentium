/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.skills.l2skills;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import silentium.commons.utils.Rnd;
import silentium.gameserver.model.L2Object;
import silentium.gameserver.model.L2Skill;
import silentium.gameserver.model.L2Spawn;
import silentium.gameserver.model.actor.L2Character;
import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.tables.NpcTable;
import silentium.gameserver.templates.StatsSet;
import silentium.gameserver.templates.chars.L2NpcTemplate;

public class L2SkillSpawn extends L2Skill
{
	private final int _npcId;
	private final int _despawnDelay;
	private final boolean _summonSpawn;
	private final boolean _randomOffset;

	public L2SkillSpawn(StatsSet set)
	{
		super(set);
		_npcId = set.getInteger("npcId", 0);
		_despawnDelay = set.getInteger("despawnDelay", 0);
		_summonSpawn = set.getBool("isSummonSpawn", false);
		_randomOffset = set.getBool("randomOffset", true);
	}

	@Override
	public void useSkill(L2Character caster, L2Object[] targets)
	{
		if (caster.isAlikeDead())
			return;

		if (_npcId == 0)
		{
			_log.warn("NPC ID not defined for skill ID: " + getId());
			return;
		}

		final L2NpcTemplate template = NpcTable.getInstance().getTemplate(_npcId);
		if (template == null)
		{
			_log.warn("Spawn of the nonexisting NPC ID: " + _npcId + ", skill ID: " + getId());
			return;
		}

		try
		{
			final L2Spawn spawn = new L2Spawn(template);
			spawn.setHeading(-1);

			if (_randomOffset)
			{
				spawn.setLocx(caster.getX() + (Rnd.nextBoolean() ? Rnd.get(20, 50) : Rnd.get(-50, -20)));
				spawn.setLocy(caster.getY() + (Rnd.nextBoolean() ? Rnd.get(20, 50) : Rnd.get(-50, -20)));
			}
			else
			{
				spawn.setLocx(caster.getX());
				spawn.setLocy(caster.getY());
			}
			spawn.setLocz(caster.getZ() + 20);

			spawn.stopRespawn();
			L2Npc npc = spawn.doSpawn(_summonSpawn);

			if (_despawnDelay > 0)
				npc.scheduleDespawn(_despawnDelay);
		}
		catch (Exception e)
		{
			_log.warn("Exception while spawning NPC ID: " + _npcId + ", skill ID: " + getId() + ", exception: " + e.getMessage(), e);
		}
	}
}
