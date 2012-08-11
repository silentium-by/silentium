/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.tables;

import gnu.trove.map.hash.TIntObjectHashMap;
import javolution.util.FastList;
import javolution.util.FastMap;
import silentium.commons.database.DatabaseFactory;
import silentium.gameserver.model.*;
import silentium.gameserver.model.base.ClassId;
import silentium.gameserver.model.quest.Quest;
import silentium.gameserver.model.quest.Quest.QuestEventType;
import silentium.gameserver.skills.Formulas;
import silentium.gameserver.templates.StatsSet;
import silentium.gameserver.templates.chars.L2NpcTemplate;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NpcTable
{
	private static Logger _log = LoggerFactory.getLogger(NpcTable.class.getName());

	private final TIntObjectHashMap<L2NpcTemplate> _npcs = new TIntObjectHashMap<>();

	public static NpcTable getInstance()
	{
		return SingletonHolder._instance;
	}

	protected NpcTable()
	{
		load();
	}

	public void reloadAllNpc()
	{
		_npcs.clear();
		load();
	}

	private void load()
	{
		loadNpcs(0);
		loadNpcsSkills(0);
		loadNpcsDrop(0);
		loadNpcsSkillLearn(0);
		loadMinions(0);
		loadNpcsAI(0);
	}

	/**
	 * Id equals to zero or lesser means all.
	 *
	 * @param id
	 *            of the NPC to load.
	 */
	public void loadNpcs(int id)
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			PreparedStatement statement = null;
			if (id > 0)
			{
				statement = con.prepareStatement("SELECT * FROM npc WHERE id = ?");
				statement.setInt(1, id);
			}
			else
				statement = con.prepareStatement("SELECT * FROM npc ORDER BY id");

			ResultSet rset = statement.executeQuery();

			while (rset.next())
			{
				StatsSet npcDat = new StatsSet();
				int npcId = rset.getInt("id");

				assert npcId < 1000000;

				npcDat.set("npcId", npcId);
				npcDat.set("idTemplate", rset.getInt("idTemplate"));
				int level = rset.getInt("level");
				npcDat.set("level", level);
				npcDat.set("jClass", rset.getString("class"));

				npcDat.set("baseShldDef", 0);
				npcDat.set("baseShldRate", 0);
				npcDat.set("baseCritRate", 38);

				npcDat.set("name", rset.getString("name"));
				npcDat.set("serverSideName", rset.getBoolean("serverSideName"));
				npcDat.set("title", rset.getString("title"));
				npcDat.set("serverSideTitle", rset.getBoolean("serverSideTitle"));
				npcDat.set("collision_radius", rset.getDouble("collision_radius"));
				npcDat.set("collision_height", rset.getDouble("collision_height"));
				npcDat.set("sex", rset.getString("sex"));
				npcDat.set("type", rset.getString("type"));
				npcDat.set("baseAtkRange", rset.getInt("attackrange"));
				npcDat.set("rewardExp", rset.getInt("exp"));
				npcDat.set("rewardSp", rset.getInt("sp"));
				npcDat.set("rhand", rset.getInt("rhand"));
				npcDat.set("lhand", rset.getInt("lhand"));
				npcDat.set("baseWalkSpd", rset.getInt("walkspd"));
				npcDat.set("enchant", rset.getInt("enchant"));

				npcDat.set("baseSTR", npcDat.getInteger("str", Formulas.BASENPCSTR));
				npcDat.set("baseCON", npcDat.getInteger("con", Formulas.BASENPCCON));
				npcDat.set("baseDEX", npcDat.getInteger("dex", Formulas.BASENPCDEX));
				npcDat.set("baseINT", npcDat.getInteger("int", Formulas.BASENPCINT));
				npcDat.set("baseWIT", npcDat.getInteger("wit", Formulas.BASENPCWIT));
				npcDat.set("baseMEN", npcDat.getInteger("men", Formulas.BASENPCMEN));

				// Calculating stats by using BaseStats (STR, DEX, CON, MEN, WIT, INT) FIXME: NPC stats
				/*
				 * if (rset.getString("type").equalsIgnoreCase("L2Pet")) {
				 */
				npcDat.set("baseRunSpd", rset.getInt("runspd"));
				npcDat.set("basePAtkSpd", rset.getInt("atkspd"));
				npcDat.set("baseMAtkSpd", rset.getInt("matkspd"));
				npcDat.set("basePAtk", rset.getInt("patk"));
				npcDat.set("baseMAtk", rset.getInt("matk"));
				npcDat.set("baseMDef", rset.getInt("mdef"));
				npcDat.set("basePDef", rset.getInt("pdef"));
				/*
				 * } else { npcDat.set("baseRunSpd", Formulas.calcNpcMoveBonus(DEX, rset.getInt("runspd")));
				 * npcDat.set("basePAtkSpd", Formulas.calcNpcPatkSpdBonus(DEX, rset.getInt("atkspd"))); npcDat.set("baseMAtkSpd",
				 * Formulas.calcNpcMatkSpdBonus(WIT, rset.getInt("matkspd"))); npcDat.set("basePAtk",
				 * Formulas.calcNpcPatkBonus(STR, rset.getInt("patk"), level)); npcDat.set("baseMAtk",
				 * Formulas.calcNpcMatkBonus(INT, rset.getInt("matk"), level)); npcDat.set("baseMDef",
				 * Formulas.calcNpcMdefBonus(MEN, rset.getInt("mdef"), level)); npcDat.set("basePDef",
				 * Formulas.calcNpcPdefBonus(rset.getInt("pdef"), level)); }
				 */

				npcDat.set("corpseDecayTime", rset.getInt("corpseDecayTime"));
				npcDat.set("dropHerbGroup", rset.getInt("dropHerbGroup"));

				// FIXME NPC stats
				// npcDat.set("baseHpMax", Formulas.calcNpcHpBonus(CON, rset.getInt("hp")));
				// npcDat.set("baseMpMax", Formulas.calcNpcMpBonus(MEN, rset.getInt("mp")));
				npcDat.set("baseHpMax", rset.getInt("hp"));
				npcDat.set("baseMpMax", rset.getInt("mp"));
				npcDat.set("baseCpMax", 0);

				npcDat.set("baseHpReg", rset.getFloat("hpreg") > 0 ? rset.getFloat("hpreg") : 1.5 + ((level - 1) / 10.0));
				npcDat.set("baseMpReg", rset.getFloat("mpreg") > 0 ? rset.getFloat("mpreg") : 0.9 + 0.3 * ((level - 1) / 10.0));

				_npcs.put(npcId, new L2NpcTemplate(npcDat));
			}

			rset.close();
			statement.close();

			_log.info("NpcTable: Loaded " + _npcs.size() + " NPC templates.");
		}
		catch (Exception e)
		{
			_log.error("NPCTable: Error creating NPC table.", e);
		}
	}

	/**
	 * Id equals to zero or lesser means all.
	 *
	 * @param id
	 *            of the NPC to load it's skills.
	 */
	public void loadNpcsSkills(int id)
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			PreparedStatement statement = null;

			if (id > 0)
			{
				statement = con.prepareStatement("SELECT * FROM npc_skills WHERE npcid = ?");
				statement.setInt(1, id);
			}
			else
				statement = con.prepareStatement("SELECT * FROM npc_skills ORDER BY npcid");

			ResultSet rset = statement.executeQuery();

			L2NpcTemplate npcDat = null;
			L2Skill npcSkill = null;

			int cnt = 0;

			while (rset.next())
			{
				int mobId = rset.getInt("npcid");
				npcDat = _npcs.get(mobId);

				if (npcDat == null)
				{
					_log.warn("NPCTable: Skill data for undefined NPC. npcId: " + mobId);
					continue;
				}

				int skillId = rset.getInt("skillid");
				int level = rset.getInt("level");

				// Set up the npc's race.
				if (skillId == L2Skill.SKILL_NPC_RACE)
				{
					npcDat.setRace(level);
					continue;
				}

				npcSkill = SkillTable.getInstance().getInfo(skillId, level);
				if (npcSkill == null)
					continue;

				cnt++;
				npcDat.addSkill(npcSkill);
			}

			rset.close();
			statement.close();

			_log.info("NpcTable: Loaded " + cnt + " npc skills.");
		}
		catch (Exception e)
		{
			_log.error("NPCTable: Error reading NPC skills table.", e);
		}
	}

	/**
	 * Id equals to zero or lesser means all.
	 *
	 * @param id
	 *            of the NPC to load it's drops.
	 */
	public void loadNpcsDrop(int id)
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			PreparedStatement statement = null;

			if (id > 0)
			{
				statement = con.prepareStatement("SELECT * FROM droplist WHERE mobId = ? ORDER BY mobId, chance DESC");
				statement.setInt(1, id);
			}
			else
				statement = con.prepareStatement("SELECT * FROM droplist ORDER BY mobId, chance DESC");

			ResultSet rset = statement.executeQuery();
			L2DropData dropDat = null;
			L2NpcTemplate npcDat = null;

			int cnt = 0;

			while (rset.next())
			{
				int mobId = rset.getInt("mobId");
				npcDat = _npcs.get(mobId);
				if (npcDat == null)
				{
					_log.warn("NPCTable: Drop data for undefined NPC. npcId: " + mobId);
					continue;
				}

				dropDat = new L2DropData();
				dropDat.setItemId(rset.getInt("itemId"));
				dropDat.setMinDrop(rset.getInt("min"));
				dropDat.setMaxDrop(rset.getInt("max"));
				dropDat.setChance(rset.getInt("chance"));

				if (ItemTable.getInstance().getTemplate(dropDat.getItemId()) == null)
				{
					_log.warn("Drop data for undefined item template! NpcId: " + mobId + " itemId: " + dropDat.getItemId());
					continue;
				}
				cnt++;
				npcDat.addDropData(dropDat, rset.getInt("category"));
			}

			rset.close();
			statement.close();

			_log.info("NpcTable: Loaded " + cnt + " drops.");
		}
		catch (Exception e)
		{
			_log.error("NPCTable: Error reading NPC dropdata.", e);
		}
	}

	/**
	 * Id equals to zero or lesser means all.
	 *
	 * @param id
	 *            of the NPC to load it's skill learn list.
	 */
	private void loadNpcsSkillLearn(int id)
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			PreparedStatement statement = null;

			if (id > 0)
			{
				statement = con.prepareStatement("SELECT * FROM skill_learn WHERE npc_id = ?");
				statement.setInt(1, id);
			}
			else
				statement = con.prepareStatement("SELECT * FROM skill_learn");

			ResultSet rset = statement.executeQuery();

			int cnt = 0;

			while (rset.next())
			{
				int npcId = rset.getInt("npc_id");
				int classId = rset.getInt("class_id");
				L2NpcTemplate npc = getTemplate(npcId);

				if (npc == null)
				{
					_log.warn("NPCTable: Error getting NPC template ID " + npcId + " while trying to load skill trainer data.");
					continue;
				}
				cnt++;
				npc.addTeachInfo(ClassId.values()[classId]);
			}

			rset.close();
			statement.close();

			_log.info("NpcTable: Loaded " + cnt + " Skill Learn.");
		}
		catch (Exception e)
		{
			_log.error("NPCTable: Error reading NPC trainer data.", e);
		}
	}

	/**
	 * Id equals to zero or lesser means all.
	 *
	 * @param id
	 *            of the NPC to load it's minions.
	 */
	public void loadMinions(int id)
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			PreparedStatement statement = null;

			if (id > 0)
			{
				statement = con.prepareStatement("SELECT * FROM minions WHERE boss_id = ?");
				statement.setInt(1, id);
			}
			else
				statement = con.prepareStatement("SELECT * FROM minions ORDER BY boss_id");

			ResultSet rset = statement.executeQuery();

			L2MinionData minionDat = null;
			L2NpcTemplate npcDat = null;

			int cnt = 0;

			while (rset.next())
			{
				int raidId = rset.getInt("boss_id");

				npcDat = _npcs.get(raidId);
				if (npcDat == null)
				{
					_log.warn("Minion references undefined boss NPC. Boss NpcId: " + raidId);
					continue;
				}

				minionDat = new L2MinionData();
				minionDat.setMinionId(rset.getInt("minion_id"));
				minionDat.setAmountMin(rset.getInt("amount_min"));
				minionDat.setAmountMax(rset.getInt("amount_max"));

				cnt++;
				npcDat.addRaidData(minionDat);
			}

			rset.close();
			statement.close();

			_log.info("NpcTable: Loaded " + cnt + " minions.");
		}
		catch (Exception e)
		{
			_log.error("NPCTable: Error loading minion data.", e);
		}
	}

	/**
	 * Id equals to zero or lesser means all.
	 *
	 * @param id
	 *            of the NPC to load it's AI data.
	 */
	public void loadNpcsAI(int id)
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			PreparedStatement statement = null;

			if (id > 0)
			{
				statement = con.prepareStatement("SELECT * FROM npc_ai_data WHERE npc_id = ?");
				statement.setInt(1, id);
			}
			else
				statement = con.prepareStatement("SELECT * FROM npc_ai_data ORDER BY npc_id");

			ResultSet rset = statement.executeQuery();

			L2NpcAIData npcAIDat = null;
			L2NpcTemplate npcDat = null;

			int cnt = 0;

			while (rset.next())
			{
				int npcId = rset.getInt("npc_id");

				npcDat = _npcs.get(npcId);
				if (npcDat == null)
				{
					_log.error("NPCTable: AI Data Error with id : " + npcId);
					continue;
				}

				npcAIDat = new L2NpcAIData();
				npcAIDat.setMinSkillChance(rset.getInt("min_skill_chance"));
				npcAIDat.setMaxSkillChance(rset.getInt("max_skill_chance"));
				npcAIDat.setPrimaryAttack(rset.getInt("primary_attack"));
				npcAIDat.setCanMove(rset.getInt("can_move"));
				npcAIDat.setShortRangeSkill(rset.getInt("minrangeskill"));
				npcAIDat.setShortRangeChance(rset.getInt("minrangechance"));
				npcAIDat.setLongRangeSkill(rset.getInt("maxrangeskill"));
				npcAIDat.setLongRangeChance(rset.getInt("maxrangechance"));
				npcAIDat.setSoulShot(rset.getInt("soulshot"));
				npcAIDat.setSpiritShot(rset.getInt("spiritshot"));
				npcAIDat.setSpiritShotChance(rset.getInt("spschance"));
				npcAIDat.setSoulShotChance(rset.getInt("sschance"));
				npcAIDat.setIsChaos(rset.getInt("is_chaos"));
				npcAIDat.setAggro(rset.getInt("aggro"));
				npcAIDat.setClan(rset.getString("clan"));
				npcAIDat.setClanRange(rset.getInt("clan_range"));
				npcAIDat.setEnemyClan(rset.getString("enemy_clan"));
				npcAIDat.setEnemyRange(rset.getInt("enemy_range"));
				npcAIDat.setAi(rset.getString("ai_type"));

				npcDat.setAIData(npcAIDat);
				cnt++;
			}

			rset.close();
			statement.close();

			_log.info("NpcTable: Loaded " + cnt + " AIs.");
		}
		catch (Exception e)
		{
			_log.error("NPCTable: Error reading NPC AI Data: " + e.getMessage(), e);
		}
	}

	public void reloadNpc(int id)
	{
		try
		{
			// save a copy of the old data
			L2NpcTemplate old = getTemplate(id);

			TIntObjectHashMap<L2Skill> skills = new TIntObjectHashMap<>();
			List<L2MinionData> minions = new FastList<>();
			Map<QuestEventType, Quest[]> quests = new FastMap<>();
			List<ClassId> classIds = new FastList<>();
			FastList<L2DropCategory> categories = new FastList<>();

			if (old != null)
			{
				skills.putAll(old.getSkills());
				categories.addAll(old.getDropData());
				classIds.addAll(old.getTeachInfo());
				minions.addAll(old.getMinionData());

				if (!old.getEventQuests().isEmpty())
					quests.putAll(old.getEventQuests());
			}

			loadNpcs(id);
			loadNpcsSkills(id);
			loadNpcsDrop(id);
			loadNpcsSkillLearn(id);
			loadMinions(id);
			loadNpcsAI(id);

			// restore additional data from saved copy
			L2NpcTemplate created = getTemplate(id);

			if ((old != null) && (created != null))
			{
				if (!skills.isEmpty())
				{
					for (L2Skill skill : skills.values(new L2Skill[0]))
						created.addSkill(skill);
				}

				for (ClassId classId : classIds)
					created.addTeachInfo(classId);

				if (!minions.isEmpty())
				{
					for (L2MinionData minion : minions)
						created.addRaidData(minion);
				}

				if (!quests.isEmpty())
					created.getEventQuests().putAll(quests);
			}
		}
		catch (Exception e)
		{
			_log.warn("NPCTable: Could not reload data for NPC " + id + ": " + e.getMessage(), e);
		}
	}

	public void saveNpc(StatsSet npc)
	{
		Map<String, Object> set = npc.getSet();

		int length = 0;

		for (Object obj : set.keySet())
		{
			// 15 is just guessed npc name length
			length += ((String) obj).length() + 7 + 15;
		}

		final StringBuilder sbValues = new StringBuilder(length);

		for (Object obj : set.keySet())
		{
			final String name = (String) obj;

			if (!name.equalsIgnoreCase("npcId"))
			{
				if (sbValues.length() > 0)
				{
					sbValues.append(", ");
				}

				sbValues.append(name);
				sbValues.append(" = '");
				sbValues.append(set.get(name));
				sbValues.append('\'');
			}
		}

		try (Connection con = DatabaseFactory.getConnection())
		{
			final StringBuilder sbQuery = new StringBuilder(sbValues.length() + 28);
			sbQuery.append("UPDATE npc SET ");
			sbQuery.append(sbValues.toString());
			sbQuery.append(" WHERE id = ?");

			PreparedStatement statement = con.prepareStatement(sbQuery.toString());
			statement.setInt(1, npc.getInteger("npcId"));
			statement.executeUpdate();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warn("NPCTable: Could not store new NPC data in database: " + e.getMessage(), e);
		}
	}

	public L2NpcTemplate getTemplate(int id)
	{
		return _npcs.get(id);
	}

	public L2NpcTemplate getTemplateByName(String name)
	{
		for (L2NpcTemplate npcTemplate : _npcs.values(new L2NpcTemplate[0]))
			if (npcTemplate.getName().equalsIgnoreCase(name))
				return npcTemplate;

		return null;
	}

	public List<L2NpcTemplate> getAllOfLevel(int... lvls)
	{
		final List<L2NpcTemplate> list = new FastList<>();
		for (int lvl : lvls)
		{
			for (L2NpcTemplate t : _npcs.values(new L2NpcTemplate[0]))
			{
				if (t.getLevel() == lvl)
					list.add(t);
			}
		}
		return list;
	}

	public List<L2NpcTemplate> getAllMonstersOfLevel(int... lvls)
	{
		final List<L2NpcTemplate> list = new FastList<>();
		for (int lvl : lvls)
		{
			for (L2NpcTemplate t : _npcs.values(new L2NpcTemplate[0]))
			{
				if ((t.getLevel() == lvl) && t.isType("L2Monster"))
					list.add(t);
			}
		}
		return list;
	}

	/**
	 * @param letters
	 *            of all the NPC templates which its name start with.
	 * @return the template list for the given letter.
	 */
	public List<L2NpcTemplate> getAllNpcStartingWith(String... letters)
	{
		final List<L2NpcTemplate> list = new FastList<>();
		for (String letter : letters)
		{
			for (L2NpcTemplate t : _npcs.values(new L2NpcTemplate[0]))
			{
				if (t.getName().startsWith(letter) && t.isType("L2Npc"))
					list.add(t);
			}
		}
		return list;
	}

	public List<L2NpcTemplate> getAllNpcOfClassType(String... classTypes)
	{
		final List<L2NpcTemplate> list = new FastList<>();
		for (String classType : classTypes)
		{
			for (L2NpcTemplate t : _npcs.values(new L2NpcTemplate[0]))
			{
				if (t.isType(classType))
					list.add(t);
			}
		}
		return list;
	}

	private static class SingletonHolder
	{
		protected static final NpcTable _instance = new NpcTable();
	}
}
