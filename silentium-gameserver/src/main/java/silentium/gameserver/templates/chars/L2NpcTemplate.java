/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.templates.chars;

import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javolution.util.FastList;
import javolution.util.FastMap;
import silentium.gameserver.data.xml.HerbDropData;
import silentium.gameserver.model.L2DropCategory;
import silentium.gameserver.model.L2DropData;
import silentium.gameserver.model.L2MinionData;
import silentium.gameserver.model.L2NpcAIData;
import silentium.gameserver.model.L2Skill;
import silentium.gameserver.model.actor.instance.L2XmassTreeInstance;
import silentium.gameserver.model.base.ClassId;
import silentium.gameserver.model.quest.Quest;
import silentium.gameserver.model.quest.Quest.QuestEventType;
import silentium.gameserver.templates.StatsSet;

/**
 * This class contains all generic data of a L2Spawn object.
 */
public final class L2NpcTemplate extends L2CharTemplate
{
	protected static final Logger _log = LoggerFactory.getLogger(Quest.class.getName());

	private final int _npcId;
	private final int _idTemplate;
	private final String _type;
	private final String _name;
	private final boolean _serverSideName;
	private final String _title;
	private final boolean _serverSideTitle;
	private final String _sex;
	private final byte _level;
	private final int _rewardExp;
	private final int _rewardSp;
	private final int _rHand;
	private final int _lHand;
	private final int _enchantEffect;
	private final int _corpseDecayTime;
	private int _dropHerbGroup;
	private Race _race;

	private final boolean _cantBeChampionMonster; // used for champion option ; avoid to popup champion quest mob.

	// Skills AI
	private final FastList<L2Skill> _buffSkills = new FastList<>();
	private final FastList<L2Skill> _negativeSkills = new FastList<>();
	private final FastList<L2Skill> _debuffSkills = new FastList<>();
	private final FastList<L2Skill> _atkSkills = new FastList<>();
	private final FastList<L2Skill> _rootSkills = new FastList<>();
	private final FastList<L2Skill> _stunSkills = new FastList<>();
	private final FastList<L2Skill> _sleepSkills = new FastList<>();
	private final FastList<L2Skill> _paralyzeSkills = new FastList<>();
	private final FastList<L2Skill> _fossilSkills = new FastList<>();
	private final FastList<L2Skill> _immobilizeSkills = new FastList<>();
	private final FastList<L2Skill> _healSkills = new FastList<>();
	private final FastList<L2Skill> _dotSkills = new FastList<>();
	private final FastList<L2Skill> _cotSkills = new FastList<>();
	private final FastList<L2Skill> _universalSkills = new FastList<>();
	private final FastList<L2Skill> _manaSkills = new FastList<>();
	private final FastList<L2Skill> _longRangeSkills = new FastList<>();
	private final FastList<L2Skill> _shortRangeSkills = new FastList<>();
	private final FastList<L2Skill> _generalSkills = new FastList<>();
	private final FastList<L2Skill> _suicideSkills = new FastList<>();

	private L2NpcAIData _AIdataStatic = new L2NpcAIData();

	public static enum AIType
	{
		FIGHTER, ARCHER, BALANCED, MAGE, HEALER, CORPSE
	}

	public static enum Race
	{
		UNDEAD, MAGICCREATURE, BEAST, ANIMAL, PLANT, HUMANOID, SPIRIT, ANGEL, DEMON, DRAGON, GIANT, BUG, FAIRIE, HUMAN, ELVE, DARKELVE, ORC, DWARVE, OTHER, NONLIVING, SIEGEWEAPON, DEFENDINGARMY, MERCENARIE, UNKNOWN
	}

	private final FastList<L2DropCategory> _categories = new FastList<>();
	private final List<L2MinionData> _minions = new FastList<>();
	private final List<ClassId> _teachInfo = new FastList<>();
	private final TIntObjectHashMap<L2Skill> _skills = new TIntObjectHashMap<>();
	private final Map<QuestEventType, Quest[]> _questEvents = new FastMap<>();

	/**
	 * Constructor of L2Character.<BR>
	 * <BR>
	 * 
	 * @param set
	 *            The StatsSet object to transfert data to the method
	 */
	public L2NpcTemplate(StatsSet set)
	{
		super(set);
		_npcId = set.getInteger("npcId");
		_idTemplate = set.getInteger("idTemplate");
		_type = set.getString("type");
		_name = set.getString("name");
		_serverSideName = set.getBool("serverSideName");
		_title = set.getString("title");
		_cantBeChampionMonster = (_title.equalsIgnoreCase("Quest Monster") || isType("L2Chest")) ? true : false;
		_serverSideTitle = set.getBool("serverSideTitle");
		_sex = set.getString("sex");
		_level = set.getByte("level");
		_rewardExp = set.getInteger("rewardExp");
		_rewardSp = set.getInteger("rewardSp");
		_rHand = set.getInteger("rhand");
		_lHand = set.getInteger("lhand");
		_enchantEffect = set.getInteger("enchant");
		_race = null;

		_corpseDecayTime = set.getInteger("corpseDecayTime");
		_dropHerbGroup = set.getInteger("dropHerbGroup");
		if (_dropHerbGroup > 0 && HerbDropData.getInstance().getHerbDroplist(_dropHerbGroup) == null)
		{
			_log.warn("Missing dropHerbGroup information for npcId: " + _npcId + ", dropHerbGroup: " + _dropHerbGroup);
			_dropHerbGroup = 0;
		}
	}

	public void addTeachInfo(ClassId classId)
	{
		_teachInfo.add(classId);
	}

	public List<ClassId> getTeachInfo()
	{
		return _teachInfo;
	}

	public boolean canTeach(ClassId classId)
	{
		// If the player is on a third class, fetch the class teacher information for its parent class.
		if (classId.level() == 3)
			return _teachInfo.contains(classId.getParent());

		return _teachInfo.contains(classId);
	}

	// Add a drop to a given category. If the category does not exist, create it.
	public void addDropData(L2DropData drop, int categoryType)
	{
		if (!drop.isQuestDrop())
		{
			// If the category doesn't already exist, create it first
			synchronized (_categories)
			{
				boolean catExists = false;
				for (L2DropCategory cat : _categories)
				{
					// If the category exists, add the drop to this category.
					if (cat.getCategoryType() == categoryType)
					{
						cat.addDropData(drop, isType("L2RaidBoss") || isType("L2GrandBoss"));
						catExists = true;
						break;
					}
				}

				// If the category doesn't exit, create it and add the drop
				if (!catExists)
				{
					L2DropCategory cat = new L2DropCategory(categoryType);
					cat.addDropData(drop, isType("L2RaidBoss") || isType("L2GrandBoss"));
					_categories.add(cat);
				}
			}
		}
	}

	public void addRaidData(L2MinionData minion)
	{
		_minions.add(minion);
	}

	public void addSkill(L2Skill skill)
	{
		if (!skill.isPassive())
		{
			if (skill.isSuicideAttack())
				addSuicideSkill(skill);
			else
			{
				addGeneralSkill(skill);
				switch (skill.getSkillType())
				{
					case BUFF:
						addBuffSkill(skill);
						break;

					case HEAL:
					case HOT:
					case HEAL_PERCENT:
					case HEAL_STATIC:
					case BALANCE_LIFE:
						addHealSkill(skill);
						break;

					case DEBUFF:
						addDebuffSkill(skill);
						addCOTSkill(skill);
						addRangeSkill(skill);
						break;

					case ROOT:
						addRootSkill(skill);
						addImmobilizeSkill(skill);
						addRangeSkill(skill);
						break;

					case SLEEP:
						addSleepSkill(skill);
						addImmobilizeSkill(skill);
						break;

					case STUN:
						addRootSkill(skill);
						addImmobilizeSkill(skill);
						addRangeSkill(skill);
						break;

					case PARALYZE:
						addParalyzeSkill(skill);
						addImmobilizeSkill(skill);
						addRangeSkill(skill);
						break;

					case PDAM:
					case MDAM:
					case BLOW:
					case DRAIN:
					case CHARGEDAM:
					case FATAL:
					case DEATHLINK:
					case MANADAM:
					case CPDAMPERCENT:
						addAtkSkill(skill);
						addUniversalSkill(skill);
						addRangeSkill(skill);
						break;

					case POISON:
					case DOT:
					case MDOT:
					case BLEED:
						addDOTSkill(skill);
						addRangeSkill(skill);
						break;

					case MUTE:
					case FEAR:
						addCOTSkill(skill);
						addRangeSkill(skill);
						break;

					case CANCEL:
					case NEGATE:
						addNegativeSkill(skill);
						addRangeSkill(skill);
						break;

					default:
						addUniversalSkill(skill);
						break;
				}
			}
		}
		_skills.put(skill.getId(), skill);
	}

	/**
	 * @return the list of all possible UNCATEGORIZED drops of this L2NpcTemplate.
	 */
	public FastList<L2DropCategory> getDropData()
	{
		return _categories;
	}

	/**
	 * @return the list of all possible item drops of this L2NpcTemplate. (ie full drops and part drops, mats, miscellaneous &
	 *         UNCATEGORIZED)
	 */
	public List<L2DropData> getAllDropData()
	{
		final List<L2DropData> list = new FastList<>();
		for (L2DropCategory tmp : _categories)
			list.addAll(tmp.getAllDrops());

		return list;
	}

	/**
	 * Empty all possible drops of this L2NpcTemplate.
	 */
	public synchronized void clearAllDropData()
	{
		while (!_categories.isEmpty())
		{
			_categories.getFirst().clearAllDrops();
			_categories.removeFirst();
		}
		_categories.clear();
	}

	/**
	 * @return the list of all Minions that must be spawn with the L2Npc using this L2NpcTemplate.
	 */
	public List<L2MinionData> getMinionData()
	{
		return _minions;
	}

	public TIntObjectHashMap<L2Skill> getSkills()
	{
		return _skills;
	}

	public L2Skill[] getSkillsArray()
	{
		return _skills.values(new L2Skill[0]);
	}

	public void addQuestEvent(Quest.QuestEventType EventType, Quest q)
	{
		if (_questEvents.get(EventType) == null)
			_questEvents.put(EventType, new Quest[] { q });
		else
		{
			Quest[] _quests = _questEvents.get(EventType);
			int len = _quests.length;

			// if only one registration per npc is allowed for this event type
			// then only register this NPC if not already registered for the specified event.
			// if a quest allows multiple registrations, then register regardless of count
			// In all cases, check if this new registration is replacing an older copy of the SAME quest
			if (!EventType.isMultipleRegistrationAllowed())
			{
				if (_quests[0].getName().equals(q.getName()) || L2NpcTemplate.isAssignableTo(q, _quests[0].getClass()))
					_quests[0] = q;
				else
					_log.warn("Quest event not allowed in multiple quests. Skipped addition of Event Type \"" + EventType + "\" for NPC \"" + getName() + "\" and quest \"" + q.getName() + "\".");
			}
			else
			{
				// be ready to add a new quest to a new copy of the list, with larger size than previously.
				Quest[] tmp = new Quest[len + 1];

				// loop through the existing quests and copy them to the new list. While doing so, also
				// check if this new quest happens to be just a replacement for a previously loaded quest.
				// If so, just save the updated reference and do NOT use the new list. Else, add the new
				// quest to the end of the new list
				for (int i = 0; i < len; i++)
				{
					if (_quests[i].getName().equals(q.getName()) || L2NpcTemplate.isAssignableTo(q, _quests[i].getClass()))
					{
						_quests[i] = q;
						return;
					}
					else if (L2NpcTemplate.isAssignableTo(_quests[i], q.getClass()))
						return;

					tmp[i] = _quests[i];
				}
				tmp[len] = q;
				_questEvents.put(EventType, tmp);
			}
		}
	}

	/**
	 * Checks if obj can be assigned to the Class represented by clazz.<br>
	 * This is true if, and only if, obj is the same class represented by clazz, or a subclass of it or obj implements the
	 * interface represented by clazz.
	 * 
	 * @param obj
	 * @param clazz
	 * @return
	 */
	public static boolean isAssignableTo(Object obj, Class<?> clazz)
	{
		return L2NpcTemplate.isAssignableTo(obj.getClass(), clazz);
	}

	public static boolean isAssignableTo(Class<?> sub, Class<?> clazz)
	{
		// if clazz represents an interface
		if (clazz.isInterface())
		{
			// check if obj implements the clazz interface
			Class<?>[] interfaces = sub.getInterfaces();
			for (Class<?> interface1 : interfaces)
			{
				if (clazz.getName().equals(interface1.getName()))
					return true;
			}
		}
		else
		{
			do
			{
				if (sub.getName().equals(clazz.getName()))
					return true;

				sub = sub.getSuperclass();
			}
			while (sub != null);
		}

		return false;
	}

	public Map<QuestEventType, Quest[]> getEventQuests()
	{
		return _questEvents;
	}

	public Quest[] getEventQuests(QuestEventType EventType)
	{
		return _questEvents.get(EventType);
	}

	public void setRace(int raceId)
	{
		switch (raceId)
		{
			case 1:
				_race = Race.UNDEAD;
				break;
			case 2:
				_race = Race.MAGICCREATURE;
				break;
			case 3:
				_race = Race.BEAST;
				break;
			case 4:
				_race = Race.ANIMAL;
				break;
			case 5:
				_race = Race.PLANT;
				break;
			case 6:
				_race = Race.HUMANOID;
				break;
			case 7:
				_race = Race.SPIRIT;
				break;
			case 8:
				_race = Race.ANGEL;
				break;
			case 9:
				_race = Race.DEMON;
				break;
			case 10:
				_race = Race.DRAGON;
				break;
			case 11:
				_race = Race.GIANT;
				break;
			case 12:
				_race = Race.BUG;
				break;
			case 13:
				_race = Race.FAIRIE;
				break;
			case 14:
				_race = Race.HUMAN;
				break;
			case 15:
				_race = Race.ELVE;
				break;
			case 16:
				_race = Race.DARKELVE;
				break;
			case 17:
				_race = Race.ORC;
				break;
			case 18:
				_race = Race.DWARVE;
				break;
			case 19:
				_race = Race.OTHER;
				break;
			case 20:
				_race = Race.NONLIVING;
				break;
			case 21:
				_race = Race.SIEGEWEAPON;
				break;
			case 22:
				_race = Race.DEFENDINGARMY;
				break;
			case 23:
				_race = Race.MERCENARIE;
				break;
			default:
				_race = Race.UNKNOWN;
				break;
		}
	}

	// -----------------------------------------------------------------------
	// Getters

	/**
	 * @return the npc id.
	 */
	public int getNpcId()
	{
		return _npcId;
	}

	/**
	 * @return the npc name.
	 */
	public String getName()
	{
		return _name;
	}

	/**
	 * @return the npc name.
	 */
	public String getTitle()
	{
		return _title;
	}

	/**
	 * @return the npc race.
	 */
	public Race getRace()
	{
		if (_race == null)
			_race = Race.UNKNOWN;

		return _race;
	}

	public String getType()
	{
		return _type;
	}

	/**
	 * @return the reward Exp.
	 */
	public int getRewardExp()
	{
		return _rewardExp;
	}

	/**
	 * @return the reward SP.
	 */
	public int getRewardSp()
	{
		return _rewardSp;
	}

	/**
	 * @return the right hand weapon.
	 */
	public int getRightHand()
	{
		return _rHand;
	}

	/**
	 * @return the right hand weapon.
	 */
	public int getLeftHand()
	{
		return _lHand;
	}

	/**
	 * @return the NPC sex.
	 */
	public String getSex()
	{
		return _sex;
	}

	/**
	 * @return the NPC level.
	 */
	public byte getLevel()
	{
		return _level;
	}

	/**
	 * @return the drop herb group.
	 */
	public int getDropHerbGroup()
	{
		return _dropHerbGroup;
	}

	/**
	 * @return the enchant effect.
	 */
	public int getEnchantEffect()
	{
		return _enchantEffect;
	}

	/**
	 * @return the Id template.
	 */
	public int getIdTemplate()
	{
		return _idTemplate;
	}

	/**
	 * @return true if the NPC uses server side name, false otherwise.
	 */
	public boolean isServerSideName()
	{
		return _serverSideName;
	}

	/**
	 * @return true if the NPC uses server side title, false otherwise.
	 */
	public boolean isServerSideTitle()
	{
		return _serverSideTitle;
	}

	/**
	 * @return the corpse decay time of the template.
	 */
	public int getCorpseDecayTime()
	{
		return _corpseDecayTime;
	}

	// -----------------------------------------------------------------------
	// Npc AI Data By ShanSoft

	public void setAIData(L2NpcAIData aidata)
	{
		_AIdataStatic = aidata;
	}

	public L2NpcAIData getAIDataStatic()
	{
		return _AIdataStatic;
	}

	public void addBuffSkill(L2Skill skill)
	{
		_buffSkills.add(skill);
	}

	public void addHealSkill(L2Skill skill)
	{
		_healSkills.add(skill);
	}

	public void addAtkSkill(L2Skill skill)
	{
		_atkSkills.add(skill);
	}

	public void addDebuffSkill(L2Skill skill)
	{
		_debuffSkills.add(skill);
	}

	public void addRootSkill(L2Skill skill)
	{
		_rootSkills.add(skill);
	}

	public void addSleepSkill(L2Skill skill)
	{
		_sleepSkills.add(skill);
	}

	public void addStunSkill(L2Skill skill)
	{
		_stunSkills.add(skill);
	}

	public void addParalyzeSkill(L2Skill skill)
	{
		_paralyzeSkills.add(skill);
	}

	public void addFossilSkill(L2Skill skill)
	{
		_fossilSkills.add(skill);
	}

	public void addNegativeSkill(L2Skill skill)
	{
		_negativeSkills.add(skill);
	}

	public void addImmobilizeSkill(L2Skill skill)
	{
		_immobilizeSkills.add(skill);
	}

	public void addDOTSkill(L2Skill skill)
	{
		_dotSkills.add(skill);
	}

	public void addUniversalSkill(L2Skill skill)
	{
		_universalSkills.add(skill);
	}

	public void addCOTSkill(L2Skill skill)
	{
		_cotSkills.add(skill);
	}

	public void addManaHealSkill(L2Skill skill)
	{
		_manaSkills.add(skill);
	}

	public void addGeneralSkill(L2Skill skill)
	{
		_generalSkills.add(skill);
	}

	public void addRangeSkill(L2Skill skill)
	{
		if (skill.getCastRange() <= 150 && skill.getCastRange() > 0)
			_shortRangeSkills.add(skill);
		else if (skill.getCastRange() > 150)
			_longRangeSkills.add(skill);
	}

	public void addSuicideSkill(L2Skill skill)
	{
		_suicideSkills.add(skill);
	}

	public FastList<L2Skill> getUniversalSkills()
	{
		return _universalSkills;
	}

	public FastList<L2Skill> getSuicideSkills()
	{
		return _suicideSkills;
	}

	public FastList<L2Skill> getNegativeSkills()
	{
		return _negativeSkills;
	}

	public FastList<L2Skill> getImmobilizeSkills()
	{
		return _immobilizeSkills;
	}

	public FastList<L2Skill> getGeneralSkills()
	{
		return _generalSkills;
	}

	public FastList<L2Skill> getHealSkills()
	{
		return _healSkills;
	}

	public FastList<L2Skill> getCostOverTimeSkills()
	{
		return _cotSkills;
	}

	public FastList<L2Skill> getDebuffSkills()
	{
		return _debuffSkills;
	}

	public FastList<L2Skill> getBuffSkills()
	{
		return _buffSkills;
	}

	public FastList<L2Skill> getAtkSkills()
	{
		return _atkSkills;
	}

	/**
	 * @return the long range skills.
	 */
	public FastList<L2Skill> getLongRangeSkills()
	{
		return _longRangeSkills;
	}

	/**
	 * @return the short range skills.
	 */
	public FastList<L2Skill> getShortRangeSkills()
	{
		return _shortRangeSkills;
	}

	// -----------------------------------------------------------------------
	// Misc

	public boolean isSpecialTree()
	{
		return _npcId == L2XmassTreeInstance.SPECIAL_TREE_ID;
	}

	public boolean isUndead()
	{
		return _race == Race.UNDEAD;
	}

	public boolean cantBeChampion()
	{
		return _cantBeChampionMonster;
	}

	/**
	 * Checks types, ignore case.
	 * 
	 * @param t
	 *            the type to check.
	 * @return true if the type are the same, false otherwise.
	 */
	public boolean isType(String t)
	{
		return _type.equalsIgnoreCase(t);
	}
}
