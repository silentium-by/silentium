/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.tables;

import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import silentium.gameserver.model.L2Skill;
import silentium.gameserver.skills.SkillsEngine;

public class SkillTable
{
	private static final L2Skill[] EMPTY_SKILLS = new L2Skill[0];

	private final TIntObjectHashMap<L2Skill> _skills;
	private final TIntIntHashMap _skillMaxLevel;

	private static final L2Skill[] _heroSkills = new L2Skill[5];
	private static final int[] _heroSkillsId = { 395, 396, 1374, 1375, 1376 };

	private static final L2Skill[] _nobleSkills = new L2Skill[8];
	private static final int[] _nobleSkillsId = { 325, 326, 327, 1323, 1324, 1325, 1326, 1327 };

	public static SkillTable getInstance()
	{
		return SingletonHolder._instance;
	}

	protected SkillTable()
	{
		_skills = new TIntObjectHashMap<>();
		_skillMaxLevel = new TIntIntHashMap();

		load();
	}

	private void load()
	{
		SkillsEngine.getInstance().loadAllSkills(_skills);

		// Stores max level of skills in a map for future uses.
		for (final L2Skill skill : _skills.values(EMPTY_SKILLS))
		{
			final int skillId = skill.getId();
			final int skillLvl = skill.getLevel();

			// Only non-enchanted skills
			if (skillLvl < 99)
			{
				final int maxLvl = _skillMaxLevel.get(skillId);
				if (skillLvl > maxLvl)
					_skillMaxLevel.put(skillId, skillLvl);
			}
		}

		// Loading FrequentSkill enumeration values
		for (FrequentSkill sk : FrequentSkill.values())
			sk._skill = getInfo(sk._id, sk._level);

		for (int i = 0; i < _heroSkillsId.length; i++)
			_heroSkills[i] = getInfo(_heroSkillsId[i], 1);

		for (int i = 0; i < _nobleSkills.length; i++)
			_nobleSkills[i] = getInfo(_nobleSkillsId[i], 1);
	}

	public void reload()
	{
		_skills.clear();
		_skillMaxLevel.clear();

		load();
	}

	/**
	 * Provides the skill hash
	 * 
	 * @param skill
	 *            The L2Skill to be hashed
	 * @return SkillTable.getSkillHashCode(skill.getId(), skill.getLevel())
	 */
	public static int getSkillHashCode(L2Skill skill)
	{
		return getSkillHashCode(skill.getId(), skill.getLevel());
	}

	/**
	 * Centralized method for easier change of the hashing sys
	 * 
	 * @param skillId
	 *            The Skill Id
	 * @param skillLevel
	 *            The Skill Level
	 * @return The Skill hash number
	 */
	public static int getSkillHashCode(int skillId, int skillLevel)
	{
		return skillId * 256 + skillLevel;
	}

	public L2Skill getInfo(int skillId, int level)
	{
		return _skills.get(getSkillHashCode(skillId, level));
	}

	public final int getMaxLevel(final int skillId)
	{
		return _skillMaxLevel.get(skillId);
	}

	/**
	 * @param addNoble
	 *            if true, will add also Advanced headquarters.
	 * @return an array with siege skills.
	 */
	public L2Skill[] getSiegeSkills(boolean addNoble)
	{
		L2Skill[] temp = new L2Skill[2 + (addNoble ? 1 : 0)];
		int i = 0;

		temp[i++] = _skills.get(SkillTable.getSkillHashCode(246, 1));
		temp[i++] = _skills.get(SkillTable.getSkillHashCode(247, 1));

		if (addNoble)
			temp[i++] = _skills.get(SkillTable.getSkillHashCode(326, 1));

		return temp;
	}

	public static L2Skill[] getHeroSkills()
	{
		return _heroSkills;
	}

	public static boolean isHeroSkill(int skillid)
	{
		for (int id : _heroSkillsId)
			if (id == skillid)
				return true;

		return false;
	}

	public static L2Skill[] getNobleSkills()
	{
		return _nobleSkills;
	}

	/**
	 * Enum to hold some important references to frequently used (hardcoded) skills in core
	 * 
	 * @author DrHouse
	 */
	public static enum FrequentSkill
	{
		LUCKY(194, 1), SEAL_OF_RULER(246, 1), BUILD_HEADQUARTERS(247, 1), STRIDER_SIEGE_ASSAULT(325, 1), DWARVEN_CRAFT(1321, 1), COMMON_CRAFT(1322, 1), LARGE_FIREWORK(2025, 1), SPECIAL_TREE_RECOVERY_BONUS(2139, 1), VOID_BURST(3630, 1), VOID_FLOW(3631, 1), RAID_CURSE(4215, 1), WYVERN_BREATH(4289, 1), ARENA_CP_RECOVERY(4380, 1), RAID_CURSE2(4515, 1), VARKA_KETRA_PETRIFICATION(4578, 1), FAKE_PETRIFICATION(4616, 1), THE_VICTOR_OF_WAR(5074, 1), THE_VANQUISHED_OF_WAR(5075, 1), BLESSING_OF_PROTECTION(5182, 1), FIREWORK(5965, 1);

		protected final int _id;
		protected final int _level;
		protected L2Skill _skill = null;

		private FrequentSkill(int id, int level)
		{
			_id = id;
			_level = level;
		}

		public L2Skill getSkill()
		{
			return _skill;
		}
	}

	private static class SingletonHolder
	{
		protected static final SkillTable _instance = new SkillTable();
	}
}