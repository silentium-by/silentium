/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;

import javolution.util.FastList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import silentium.gameserver.configs.MainConfig;
import silentium.gameserver.data.xml.SkillTreeData;
import silentium.gameserver.geo.GeoData;
import silentium.gameserver.model.actor.L2Attackable;
import silentium.gameserver.model.actor.L2Character;
import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.L2Playable;
import silentium.gameserver.model.actor.L2Summon;
import silentium.gameserver.model.actor.instance.L2ArtefactInstance;
import silentium.gameserver.model.actor.instance.L2ChestInstance;
import silentium.gameserver.model.actor.instance.L2CubicInstance;
import silentium.gameserver.model.actor.instance.L2DoorInstance;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.actor.instance.L2PetInstance;
import silentium.gameserver.model.actor.instance.L2SiegeFlagInstance;
import silentium.gameserver.model.actor.instance.L2SummonInstance;
import silentium.gameserver.model.entity.TvTEvent;
import silentium.gameserver.network.SystemMessageId;
import silentium.gameserver.network.serverpackets.SystemMessage;
import silentium.gameserver.skills.Env;
import silentium.gameserver.skills.Formulas;
import silentium.gameserver.skills.Stats;
import silentium.gameserver.skills.basefuncs.Func;
import silentium.gameserver.skills.basefuncs.FuncTemplate;
import silentium.gameserver.skills.conditions.Condition;
import silentium.gameserver.skills.effects.EffectTemplate;
import silentium.gameserver.tables.ItemTable;
import silentium.gameserver.tables.SkillTable;
import silentium.gameserver.taskmanager.DecayTaskManager;
import silentium.gameserver.templates.StatsSet;
import silentium.gameserver.templates.item.L2Armor;
import silentium.gameserver.templates.item.L2ArmorType;
import silentium.gameserver.templates.skills.L2SkillType;
import silentium.gameserver.utils.Util;

public abstract class L2Skill implements IChanceSkillTrigger
{
	protected static final Logger _log = LoggerFactory.getLogger(L2Skill.class.getName());

	private static final L2Object[] _emptyTargetList = new L2Object[0];

	public static final int SKILL_LUCKY = 194;
	public static final int SKILL_CREATE_COMMON = 1320;
	public static final int SKILL_CREATE_DWARVEN = 172;
	public static final int SKILL_CRYSTALLIZE = 248;
	public static final int SKILL_DIVINE_INSPIRATION = 1405;
	public static final int SKILL_NPC_RACE = 4416;

	public static final boolean geoEnabled = MainConfig.GEODATA > 0;

	public static enum SkillOpType
	{
		OP_PASSIVE, OP_ACTIVE, OP_TOGGLE
	}

	/** Target types of skills : SELF, PARTY, CLAN, PET... */
	public static enum SkillTargetType
	{
		TARGET_NONE, TARGET_SELF, TARGET_ONE, TARGET_PARTY, TARGET_ALLY, TARGET_CLAN, TARGET_PET, TARGET_AREA, TARGET_FRONT_AREA, TARGET_BEHIND_AREA, TARGET_AURA, TARGET_FRONT_AURA, TARGET_BEHIND_AURA, TARGET_CORPSE, TARGET_UNDEAD, TARGET_AREA_UNDEAD, TARGET_CORPSE_ALLY, TARGET_CORPSE_CLAN, TARGET_CORPSE_PLAYER, TARGET_CORPSE_PET, TARGET_ITEM, TARGET_AREA_CORPSE_MOB, TARGET_CORPSE_MOB, TARGET_UNLOCKABLE, TARGET_HOLY, TARGET_PARTY_MEMBER, TARGET_PARTY_OTHER, TARGET_SUMMON, TARGET_AREA_SUMMON, TARGET_ENEMY_SUMMON, TARGET_OWNER_PET, TARGET_GROUND
	}

	// conditional values
	public static final int COND_RUNNING = 0x0001;
	public static final int COND_WALKING = 0x0002;
	public static final int COND_SIT = 0x0004;
	public static final int COND_BEHIND = 0x0008;
	public static final int COND_CRIT = 0x0010;
	public static final int COND_LOWHP = 0x0020;
	public static final int COND_ROBES = 0x0040;
	public static final int COND_CHARGES = 0x0080;
	public static final int COND_SHIELD = 0x0100;

	private static final Func[] _emptyFunctionSet = new Func[0];
	private static final L2Effect[] _emptyEffectSet = new L2Effect[0];

	private final int _id;
	private final int _level;

	private int _displayId;
	private final String _name;
	private final SkillOpType _operateType;

	private final boolean _magic;

	private final int _mpConsume;
	private final int _mpInitialConsume;
	private final int _hpConsume;

	private final int _targetConsume;
	private final int _targetConsumeId;

	private final int _itemConsume; // items consumption
	private final int _itemConsumeId;

	private final int _castRange;
	private final int _effectRange;

	private final int _abnormalLvl; // Abnormal levels for skills and their canceling
	private final int _effectAbnormalLvl;

	private final int _hitTime; // all times in milliseconds
	private final int _coolTime;

	private final int _reuseDelay;
	private final int _equipDelay;

	private final int _buffDuration;

	/** Target type of the skill : SELF, PARTY, CLAN, PET... */
	private final SkillTargetType _targetType;

	private final double _power;

	private final int _magicLevel;

	private final int _negateLvl; // abnormalLvl is negated with negateLvl
	private final int[] _negateId; // cancels the effect of skill ID
	private final L2SkillType[] _negateStats; // lists the effect types that are canceled
	private final int _maxNegatedEffects; // maximum number of effects to negate

	private final int _levelDepend;

	private final int _skillRadius; // Effecting area of the skill, in radius.

	private final L2SkillType _skillType;
	private final L2SkillType _effectType;

	private final int _effectId;
	private final int _effectPower;
	private final int _effectLvl;

	private final boolean _ispotion;
	private final byte _element;

	private final boolean _ignoreResists;

	private final boolean _staticReuse;
	private final boolean _staticHitTime;

	private final int _reuseHashCode;

	private final Stats _stat;

	private final int _condition;
	private final int _conditionValue;

	private final boolean _overhit;
	private final boolean _killByDOT;
	private final boolean _isSuicideAttack;

	private final boolean _isDemonicSkill;
	private final boolean _isFlyingSkill;
	private final boolean _isStriderSkill;

	private final boolean _isSiegeSummonSkill;

	private final int _weaponsAllowed;

	private final boolean _nextActionIsAttack;

	private final int _minPledgeClass;

	private final boolean _isOffensive;
	private final int _maxCharges;
	private final int _numCharges;

	private final int _triggeredId;
	private final int _triggeredLevel;
	protected ChanceCondition _chanceCondition = null;
	private final String _chanceType;

	private final String _flyType;
	private final int _flyRadius;
	private final float _flyCourse;

	private final int _feed;

	private final boolean _isHeroSkill; // If true the skill is a Hero Skill

	private final int _baseCritRate; // percent of success for skill critical hit (especially for PDAM & BLOW - they're not
										// affected by rCrit values or buffs). Default loads -1 for all other skills but 0 to PDAM
										// & BLOW
	private final int _lethalEffect1; // percent of success for lethal 1st effect (hit cp to 1 or if mob hp to 50%) (only for PDAM
										// skills)
	private final int _lethalEffect2; // percent of success for lethal 2nd effect (hit cp,hp to 1 or if mob hp to 1) (only for
										// PDAM skills)
	private final boolean _directHpDmg; // If true then dmg is being make directly
	private final boolean _isDance; // If true then casting more dances will cost more MP
	private final int _nextDanceCost;
	private final float _sSBoost; // If true skill will have SoulShot boost (power*2)
	private final int _aggroPoints;

	protected List<Condition> _preCondition;
	protected List<Condition> _itemPreCondition;
	protected FuncTemplate[] _funcTemplates;
	protected EffectTemplate[] _effectTemplates;
	protected EffectTemplate[] _effectTemplatesSelf;

	private final String _attribute;

	private final boolean _isDebuff;
	private final boolean _stayAfterDeath; // skill should stay after death

	private final boolean _removedOnAnyActionExceptMove;
	private final boolean _removedOnDamage;

	private final boolean _canBeReflected;
	private final boolean _canBeDispeled;

	private final boolean _isClanSkill;

	private final boolean _ignoreShield;

	private final boolean _simultaneousCast;

	private L2ExtractableSkill _extractableItems = null;

	protected L2Skill(StatsSet set)
	{
		_id = set.getInteger("skill_id");
		_level = set.getInteger("level");

		_displayId = set.getInteger("displayId", _id);
		_name = set.getString("name");
		_operateType = set.getEnum("operateType", SkillOpType.class);

		_magic = set.getBool("isMagic", false);
		_ispotion = set.getBool("isPotion", false);

		_mpConsume = set.getInteger("mpConsume", 0);
		_mpInitialConsume = set.getInteger("mpInitialConsume", 0);
		_hpConsume = set.getInteger("hpConsume", 0);

		_targetConsume = set.getInteger("targetConsumeCount", 0);
		_targetConsumeId = set.getInteger("targetConsumeId", 0);

		_itemConsume = set.getInteger("itemConsumeCount", 0);
		_itemConsumeId = set.getInteger("itemConsumeId", 0);

		_castRange = set.getInteger("castRange", 0);
		_effectRange = set.getInteger("effectRange", -1);

		_abnormalLvl = set.getInteger("abnormalLvl", -1);
		_effectAbnormalLvl = set.getInteger("effectAbnormalLvl", -1); // support for a separate effect abnormal lvl, e.g. poison
																		// inside a different skill
		_negateLvl = set.getInteger("negateLvl", -1);

		_hitTime = set.getInteger("hitTime", 0);
		_coolTime = set.getInteger("coolTime", 0);

		_reuseDelay = set.getInteger("reuseDelay", 0);
		_equipDelay = set.getInteger("equipDelay", 0);

		_buffDuration = set.getInteger("buffDuration", 0);

		_skillRadius = set.getInteger("skillRadius", 80);

		_targetType = set.getEnum("target", SkillTargetType.class);

		_power = set.getFloat("power", 0.f);

		_attribute = set.getString("attribute", "");
		String str = set.getString("negateStats", "");

		if (str.isEmpty())
			_negateStats = new L2SkillType[0];
		else
		{
			String[] stats = str.split(" ");
			L2SkillType[] array = new L2SkillType[stats.length];

			for (int i = 0; i < stats.length; i++)
			{
				L2SkillType type = null;
				try
				{
					type = Enum.valueOf(L2SkillType.class, stats[i]);
				}
				catch (Exception e)
				{
					throw new IllegalArgumentException("SkillId: " + _id + "Enum value of type " + L2SkillType.class.getName() + " required, but found: " + stats[i]);
				}

				array[i] = type;
			}
			_negateStats = array;
		}

		String negateId = set.getString("negateId", null);
		if (negateId != null)
		{
			String[] valuesSplit = negateId.split(",");
			_negateId = new int[valuesSplit.length];
			for (int i = 0; i < valuesSplit.length; i++)
			{
				_negateId[i] = Integer.parseInt(valuesSplit[i]);
			}
		}
		else
			_negateId = new int[0];

		_maxNegatedEffects = set.getInteger("maxNegated", 0);

		_magicLevel = set.getInteger("magicLvl", SkillTreeData.getInstance().getMinSkillLevel(_id, _level));
		_levelDepend = set.getInteger("lvlDepend", 0);
		_ignoreResists = set.getBool("ignoreResists", false);

		_staticReuse = set.getBool("staticReuse", false);
		_staticHitTime = set.getBool("staticHitTime", false);

		String reuseHash = set.getString("sharedReuse", null);
		if (reuseHash != null)
		{
			try
			{
				String[] valuesSplit = reuseHash.split("-");
				_reuseHashCode = SkillTable.getSkillHashCode(Integer.parseInt(valuesSplit[0]), Integer.parseInt(valuesSplit[1]));
			}
			catch (Exception e)
			{
				throw new IllegalArgumentException("SkillId: " + _id + " invalid sharedReuse value: " + reuseHash + ", \"skillId-skillLvl\" required");
			}
		}
		else
			_reuseHashCode = SkillTable.getSkillHashCode(_id, _level);

		_stat = set.getEnum("stat", Stats.class, null);
		_ignoreShield = set.getBool("ignoreShld", false);

		_skillType = set.getEnum("skillType", L2SkillType.class);
		_effectType = set.getEnum("effectType", L2SkillType.class, null);

		_effectId = set.getInteger("effectId", 0);
		_effectPower = set.getInteger("effectPower", 0);
		_effectLvl = set.getInteger("effectLevel", 0);

		_element = set.getByte("element", (byte) -1);

		_condition = set.getInteger("condition", 0);
		_conditionValue = set.getInteger("conditionValue", 0);

		_overhit = set.getBool("overHit", false);
		_killByDOT = set.getBool("killByDOT", false);
		_isSuicideAttack = set.getBool("isSuicideAttack", false);

		_isDemonicSkill = set.getBool("isDemonicSkill", false);
		_isFlyingSkill = set.getBool("isFlyingSkill", false);
		_isStriderSkill = set.getBool("isStriderSkill", false);

		_isSiegeSummonSkill = set.getBool("isSiegeSummonSkill", false);

		String weaponsAllowedString = set.getString("weaponsAllowed", null);
		if (weaponsAllowedString != null && !weaponsAllowedString.trim().isEmpty())
		{
			int mask = 0;
			StringTokenizer st = new StringTokenizer(weaponsAllowedString, ",");
			while (st.hasMoreTokens())
			{
				int old = mask;
				String item = st.nextToken().trim();
				if (ItemTable._weaponTypes.containsKey(item))
					mask |= ItemTable._weaponTypes.get(item).mask();

				if (ItemTable._armorTypes.containsKey(item)) // for shield
					mask |= ItemTable._armorTypes.get(item).mask();

				if (old == mask)
					_log.info("[weaponsAllowed] Unknown item type name: " + item);
			}
			_weaponsAllowed = mask;
		}
		else
			_weaponsAllowed = 0;

		_nextActionIsAttack = set.getBool("nextActionAttack", false);

		_minPledgeClass = set.getInteger("minPledgeClass", 0);

		_triggeredId = set.getInteger("triggeredId", 0);
		_triggeredLevel = set.getInteger("triggeredLevel", 0);
		_chanceType = set.getString("chanceType", "");
		if (!_chanceType.isEmpty() && !_chanceType.isEmpty())
			_chanceCondition = ChanceCondition.parse(set);

		_isOffensive = set.getBool("offensive", isSkillTypeOffensive());
		_maxCharges = set.getInteger("maxCharges", 0);
		_numCharges = set.getInteger("numCharges", 0);

		_isHeroSkill = SkillTable.isHeroSkill(_id);

		_baseCritRate = set.getInteger("baseCritRate", (_skillType == L2SkillType.PDAM || _skillType == L2SkillType.BLOW) ? 0 : -1);
		_lethalEffect1 = set.getInteger("lethal1", 0);
		_lethalEffect2 = set.getInteger("lethal2", 0);

		_directHpDmg = set.getBool("dmgDirectlyToHp", false);
		_isDance = set.getBool("isDance", false);
		_nextDanceCost = set.getInteger("nextDanceCost", 0);
		_sSBoost = set.getFloat("SSBoost", 0.f);
		_aggroPoints = set.getInteger("aggroPoints", 0);

		_isDebuff = set.getBool("isDebuff", false);
		_stayAfterDeath = set.getBool("stayAfterDeath", false);

		_removedOnAnyActionExceptMove = set.getBool("removedOnAnyActionExceptMove", false);
		_removedOnDamage = set.getBool("removedOnDamage", _skillType == L2SkillType.SLEEP);

		_flyType = set.getString("flyType", null);
		_flyRadius = set.getInteger("flyRadius", 0);
		_flyCourse = set.getFloat("flyCourse", 0);

		_feed = set.getInteger("feed", 0);

		_canBeReflected = set.getBool("canBeReflected", true);
		_canBeDispeled = set.getBool("canBeDispeled", true);

		_isClanSkill = set.getBool("isClanSkill", false);

		_simultaneousCast = set.getBool("simultaneousCast", false);

		String capsuled_items = set.getString("capsuled_items_skill", null);
		if (capsuled_items != null)
		{
			if (capsuled_items.isEmpty())
				_log.warn("Empty extractable data for skill: " + _id);

			_extractableItems = parseExtractableSkill(_id, _level, capsuled_items);
		}
	}

	public abstract void useSkill(L2Character caster, L2Object[] targets);

	public final boolean isPotion()
	{
		return _ispotion;
	}

	public final int getConditionValue()
	{
		return _conditionValue;
	}

	public final L2SkillType getSkillType()
	{
		return _skillType;
	}

	public final byte getElement()
	{
		return _element;
	}

	/**
	 * @return the target type of the skill : SELF, PARTY, CLAN, PET...
	 */
	public final SkillTargetType getTargetType()
	{
		return _targetType;
	}

	public final int getCondition()
	{
		return _condition;
	}

	public final boolean isOverhit()
	{
		return _overhit;
	}

	public final boolean killByDOT()
	{
		return _killByDOT;
	}

	public final boolean isSuicideAttack()
	{
		return _isSuicideAttack;
	}

	public final boolean isDemonicSkill()
	{
		return _isDemonicSkill;
	}

	public final boolean isFlyingSkill()
	{
		return _isFlyingSkill;
	}

	public final boolean isStriderSkill()
	{
		return _isStriderSkill;
	}

	public final boolean isSiegeSummonSkill()
	{
		return _isSiegeSummonSkill;
	}

	/**
	 * @param activeChar
	 * @param target
	 * @return the power of the skill.
	 */
	public final double getPower(L2Character activeChar, L2Character target)
	{
		if (activeChar == null)
			return getPower();

		switch (_skillType)
		{
			case DEATHLINK:
				return getPower() * Math.pow(1.7165 - activeChar.getCurrentHp() / activeChar.getMaxHp(), 2) * 0.577;
			case FATAL:
				return getPower() * 3.5 * (1 - target.getCurrentHp() / target.getMaxHp());
			default:
				return getPower();
		}
	}

	public final double getPower()
	{
		return _power;
	}

	public final L2SkillType[] getNegateStats()
	{
		return _negateStats;
	}

	public final int getAbnormalLvl()
	{
		return _abnormalLvl;
	}

	public final int getNegateLvl()
	{
		return _negateLvl;
	}

	public final int[] getNegateId()
	{
		return _negateId;
	}

	public final int getMagicLevel()
	{
		return _magicLevel;
	}

	public final int getMaxNegatedEffects()
	{
		return _maxNegatedEffects;
	}

	public final int getLevelDepend()
	{
		return _levelDepend;
	}

	/**
	 * @return true if skill should ignore all resistances.
	 */
	public final boolean ignoreResists()
	{
		return _ignoreResists;
	}

	public int getTriggeredId()
	{
		return _triggeredId;
	}

	public int getTriggeredLevel()
	{
		return _triggeredLevel;
	}

	public boolean triggerAnotherSkill()
	{
		return _triggeredId > 1;
	}

	/**
	 * @return true if skill effects should be removed on any action except movement
	 */
	public final boolean isRemovedOnAnyActionExceptMove()
	{
		return _removedOnAnyActionExceptMove;
	}

	/**
	 * @return true if skill effects should be removed on damage
	 */
	public final boolean isRemovedOnDamage()
	{
		return _removedOnDamage;
	}

	/**
	 * @return the additional effect power or base probability.
	 */
	public final double getEffectPower()
	{
		if (_effectTemplates != null)
			for (EffectTemplate et : _effectTemplates)
				if (et.effectPower > 0)
					return et.effectPower;

		if (_effectPower > 0)
			return _effectPower;

		// to let damage dealing skills having proper resist even without specified effectPower
		switch (_skillType)
		{
			case PDAM:
				return 20;
			case MDAM:
				return 20;
			default:
				// to let debuffs succeed even without specified power
				return (_power <= 0 || 100 < _power) ? 20 : _power;
		}
	}

	/**
	 * @return the additional effect Id.
	 */
	public final int getEffectId()
	{
		return _effectId;
	}

	/**
	 * @return the additional effect level.
	 */
	public final int getEffectLvl()
	{
		return _effectLvl;
	}

	public final int getEffectAbnormalLvl()
	{
		return _effectAbnormalLvl;
	}

	/**
	 * @return the additional effect skill type (ex : STUN, PARALYZE,...).
	 */
	public final L2SkillType getEffectType()
	{
		if (_effectTemplates != null)
			for (EffectTemplate et : _effectTemplates)
				if (et.effectType != null)
					return et.effectType;

		if (_effectType != null)
			return _effectType;

		// to let damage dealing skills having proper resist even without specified effectType
		switch (_skillType)
		{
			case PDAM:
				return L2SkillType.STUN;
			case MDAM:
				return L2SkillType.PARALYZE;
			default:
				return _skillType;
		}
	}

	/**
	 * @return true if character should attack target after skill
	 */
	public final boolean nextActionIsAttack()
	{
		return _nextActionIsAttack;
	}

	/**
	 * @return Returns the buffDuration.
	 */
	public final int getBuffDuration()
	{
		return _buffDuration;
	}

	/**
	 * @return Returns the castRange.
	 */
	public final int getCastRange()
	{
		return _castRange;
	}

	/**
	 * @return Returns the effectRange.
	 */
	public final int getEffectRange()
	{
		return _effectRange;
	}

	/**
	 * @return Returns the hpConsume.
	 */
	public final int getHpConsume()
	{
		return _hpConsume;
	}

	/**
	 * @return Returns the boolean _isDebuff.
	 */
	public final boolean isDebuff()
	{
		return _isDebuff;
	}

	/**
	 * @return the skill id.
	 */
	public final int getId()
	{
		return _id;
	}

	public int getDisplayId()
	{
		return _displayId;
	}

	public void setDisplayId(int id)
	{
		_displayId = id;
	}

	public final Stats getStat()
	{
		return _stat;
	}

	/**
	 * @return the _targetConsumeId.
	 */
	public final int getTargetConsumeId()
	{
		return _targetConsumeId;
	}

	/**
	 * @return the targetConsume.
	 */
	public final int getTargetConsume()
	{
		return _targetConsume;
	}

	/**
	 * @return the itemConsume.
	 */
	public final int getItemConsume()
	{
		return _itemConsume;
	}

	/**
	 * @return the itemConsumeId.
	 */
	public final int getItemConsumeId()
	{
		return _itemConsumeId;
	}

	/**
	 * @return the level.
	 */
	public final int getLevel()
	{
		return _level;
	}

	/**
	 * @return the magic.
	 */
	public final boolean isMagic()
	{
		return _magic;
	}

	/**
	 * @return true to set static reuse.
	 */
	public final boolean isStaticReuse()
	{
		return _staticReuse;
	}

	/**
	 * @return true to set static hittime.
	 */
	public final boolean isStaticHitTime()
	{
		return _staticHitTime;
	}

	/**
	 * @return Returns the mpConsume.
	 */
	public final int getMpConsume()
	{
		return _mpConsume;
	}

	/**
	 * @return Returns the mpInitialConsume.
	 */
	public final int getMpInitialConsume()
	{
		return _mpInitialConsume;
	}

	/**
	 * @return Returns the name.
	 */
	public final String getName()
	{
		return _name;
	}

	/**
	 * @return Returns the reuseDelay.
	 */
	public final int getReuseDelay()
	{
		return _reuseDelay;
	}

	public final int getEquipDelay()
	{
		return _equipDelay;
	}

	public final int getReuseHashCode()
	{
		return _reuseHashCode;
	}

	public final int getHitTime()
	{
		return _hitTime;
	}

	/**
	 * @return Returns the coolTime.
	 */
	public final int getCoolTime()
	{
		return _coolTime;
	}

	public final int getSkillRadius()
	{
		return _skillRadius;
	}

	public final boolean isActive()
	{
		return _operateType == SkillOpType.OP_ACTIVE;
	}

	public final boolean isPassive()
	{
		return _operateType == SkillOpType.OP_PASSIVE;
	}

	public final boolean isToggle()
	{
		return _operateType == SkillOpType.OP_TOGGLE;
	}

	public boolean isChance()
	{
		return _chanceCondition != null && isPassive();
	}

	public final boolean isDance()
	{
		return _isDance;
	}

	public final int getNextDanceMpCost()
	{
		return _nextDanceCost;
	}

	public final float getSSBoost()
	{
		return _sSBoost;
	}

	public final int getAggroPoints()
	{
		return _aggroPoints;
	}

	public final boolean useSoulShot()
	{
		return ((getSkillType() == L2SkillType.PDAM) || (getSkillType() == L2SkillType.STUN) || (getSkillType() == L2SkillType.CHARGEDAM));
	}

	public final boolean useSpiritShot()
	{
		return isMagic();
	}

	public final boolean useFishShot()
	{
		return ((getSkillType() == L2SkillType.PUMPING) || (getSkillType() == L2SkillType.REELING));
	}

	public final int getWeaponsAllowed()
	{
		return _weaponsAllowed;
	}

	public boolean isSimultaneousCast()
	{
		return _simultaneousCast;
	}

	public int getMinPledgeClass()
	{
		return _minPledgeClass;
	}

	public String getAttributeName()
	{
		return _attribute;
	}

	public boolean ignoreShield()
	{
		return _ignoreShield;
	}

	public boolean canBeReflected()
	{
		return _canBeReflected;
	}

	public boolean canBeDispeled()
	{
		return _canBeDispeled;
	}

	public boolean isClanSkill()
	{
		return _isClanSkill;
	}

	public final String getFlyType()
	{
		return _flyType;
	}

	public final int getFlyRadius()
	{
		return _flyRadius;
	}

	public int getFeed()
	{
		return _feed;
	}

	public final float getFlyCourse()
	{
		return _flyCourse;
	}

	public final int getMaxCharges()
	{
		return _maxCharges;
	}

	@Override
	public boolean triggersChanceSkill()
	{
		return _triggeredId > 0 && isChance();
	}

	@Override
	public int getTriggeredChanceId()
	{
		return _triggeredId;
	}

	@Override
	public int getTriggeredChanceLevel()
	{
		return _triggeredLevel;
	}

	@Override
	public ChanceCondition getTriggeredChanceCondition()
	{
		return _chanceCondition;
	}

	public final boolean isPvpSkill()
	{
		switch (_skillType)
		{
			case DOT:
			case BLEED:
			case POISON:
			case DEBUFF:
			case AGGDEBUFF:
			case STUN:
			case ROOT:
			case FEAR:
			case SLEEP:
			case MDOT:
			case MUTE:
			case WEAKNESS:
			case PARALYZE:
			case CANCEL:
			case MAGE_BANE:
			case WARRIOR_BANE:
			case BETRAY:
			case AGGDAMAGE:
			case AGGREDUCE_CHAR:
			case MANADAM:
				return true;
			default:
				return false;
		}
	}

	public final boolean is7Signs()
	{
		if (_id > 4360 && _id < 4367)
			return true;
		return false;
	}

	public final boolean isStayAfterDeath()
	{
		return _stayAfterDeath;
	}

	public final boolean isOffensive()
	{
		return _isOffensive;
	}

	public final boolean isHeroSkill()
	{
		return _isHeroSkill;
	}

	public final int getNumCharges()
	{
		return _numCharges;
	}

	public final int getBaseCritRate()
	{
		return _baseCritRate;
	}

	public final int getLethalChance1()
	{
		return _lethalEffect1;
	}

	public final int getLethalChance2()
	{
		return _lethalEffect2;
	}

	public final boolean getDmgDirectlyToHP()
	{
		return _directHpDmg;
	}

	public final boolean isSkillTypeOffensive()
	{
		switch (_skillType)
		{
			case PDAM:
			case MDAM:
			case CPDAMPERCENT:
			case DOT:
			case BLEED:
			case POISON:
			case AGGDAMAGE:
			case DEBUFF:
			case AGGDEBUFF:
			case STUN:
			case ROOT:
			case CONFUSION:
			case ERASE:
			case BLOW:
			case FATAL:
			case FEAR:
			case DRAIN:
			case SLEEP:
			case CHARGEDAM:
			case DEATHLINK:
			case DETECT_WEAKNESS:
			case MANADAM:
			case MDOT:
			case MUTE:
			case SOULSHOT:
			case SPIRITSHOT:
			case SPOIL:
			case WEAKNESS:
			case SWEEP:
			case PARALYZE:
			case DRAIN_SOUL:
			case AGGREDUCE:
			case CANCEL:
			case MAGE_BANE:
			case WARRIOR_BANE:
			case AGGREMOVE:
			case AGGREDUCE_CHAR:
			case BETRAY:
			case DELUXE_KEY_UNLOCK:
			case SOW:
			case HARVEST:
			case INSTANT_JUMP:
				return true;
			default:
				return isDebuff();
		}
	}

	public final boolean getWeaponDependancy(L2Character activeChar)
	{
		int weaponsAllowed = getWeaponsAllowed();
		// check to see if skill has a weapon dependency.
		if (weaponsAllowed == 0)
			return true;

		int mask = 0;

		if (activeChar.getActiveWeaponItem() != null)
			mask |= activeChar.getActiveWeaponItem().getItemType().mask();
		if (activeChar.getSecondaryWeaponItem() != null && activeChar.getSecondaryWeaponItem() instanceof L2Armor)
			mask |= ((L2ArmorType) activeChar.getSecondaryWeaponItem().getItemType()).mask();

		if ((mask & weaponsAllowed) != 0)
			return true;

		activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED).addSkillName(this));
		return false;
	}

	public boolean checkCondition(L2Character activeChar, L2Object target, boolean itemOrWeapon)
	{
		List<Condition> preCondition = _preCondition;
		if (itemOrWeapon)
			preCondition = _itemPreCondition;

		if (preCondition == null || preCondition.isEmpty())
			return true;

		for (Condition cond : preCondition)
		{
			Env env = new Env();
			env.player = activeChar;
			if (target instanceof L2Character)
				env.target = (L2Character) target;
			env.skill = this;

			if (!cond.test(env))
			{
				String msg = cond.getMessage();
				int msgId = cond.getMessageId();
				if (msgId != 0)
				{
					SystemMessage sm = SystemMessage.getSystemMessage(msgId);
					if (cond.isAddName())
						sm.addSkillName(_id);
					activeChar.sendPacket(sm);
				}
				else if (msg != null)
				{
					activeChar.sendMessage(msg);
				}
				return false;
			}
		}
		return true;
	}

	public final L2Object[] getTargetList(L2Character activeChar, boolean onlyFirst)
	{
		// Init to null the target of the skill
		L2Character target = null;

		// Get the L2Objcet targeted by the user of the skill at this moment
		L2Object objTarget = activeChar.getTarget();
		// If the L2Object targeted is a L2Character, it becomes the L2Character target
		if (objTarget instanceof L2Character)
		{
			target = (L2Character) objTarget;
		}

		return getTargetList(activeChar, onlyFirst, target);
	}

	/**
	 * @param activeChar
	 *            The L2Character who use the skill
	 * @param onlyFirst
	 * @param target
	 * @return all targets of the skill in a table in function of the skill type.
	 */
	public final L2Object[] getTargetList(L2Character activeChar, boolean onlyFirst, L2Character target)
	{
		List<L2Character> targetList = new FastList<>();

		// Get the target type of the skill
		// (ex : ONE, SELF, HOLY, PET, AURA, AURA_CLOSE, AREA, MULTIFACE, PARTY, CLAN, CORPSE_PLAYER, CORPSE_MOB, CORPSE_CLAN,
		// UNLOCKABLE, ITEM, UNDEAD)
		SkillTargetType targetType = getTargetType();

		// Get the type of the skill
		// (ex : PDAM, MDAM, DOT, BLEED, POISON, HEAL, HOT, MANAHEAL, MANARECHARGE, AGGDAMAGE, BUFF, DEBUFF, STUN, ROOT,
		// RESURRECT, PASSIVE...)
		L2SkillType skillType = getSkillType();

		switch (targetType)
		{
		// The skill can only be used on the L2Character targeted, or on the caster itself
			case TARGET_ONE:
			{
				boolean canTargetSelf = false;
				switch (skillType)
				{
					case BUFF:
					case HEAL:
					case HOT:
					case HEAL_PERCENT:
					case MANARECHARGE:
					case MANAHEAL:
					case NEGATE:
					case CANCEL_DEBUFF:
					case REFLECT:
					case COMBATPOINTHEAL:
					case SEED:
					case BALANCE_LIFE:
						canTargetSelf = true;
						break;
				}

				// Check for null target or any other invalid target
				if (target == null || target.isDead() || (target == activeChar && !canTargetSelf))
				{
					activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
					return _emptyTargetList;
				}

				// If a target is found, return it in a table else send a system message TARGET_IS_INCORRECT
				return new L2Character[] { target };
			}
			case TARGET_SELF:
			case TARGET_GROUND:
			{
				return new L2Character[] { activeChar };
			}
			case TARGET_HOLY:
			{
				if (activeChar instanceof L2PcInstance)
				{
					if (target instanceof L2ArtefactInstance)
						return new L2Character[] { target };
				}

				return _emptyTargetList;
			}
			case TARGET_PET:
			{
				target = activeChar.getPet();
				if (target != null && !target.isDead())
					return new L2Character[] { target };

				return _emptyTargetList;
			}
			case TARGET_SUMMON:
			{
				target = activeChar.getPet();
				if (target != null && !target.isDead() && target instanceof L2SummonInstance)
					return new L2Character[] { target };

				return _emptyTargetList;
			}
			case TARGET_OWNER_PET:
			{
				if (activeChar instanceof L2Summon)
				{
					target = ((L2Summon) activeChar).getOwner();
					if (target != null && !target.isDead())
						return new L2Character[] { target };
				}

				return _emptyTargetList;
			}
			case TARGET_CORPSE_PET:
			{
				if (activeChar instanceof L2PcInstance)
				{
					target = activeChar.getPet();
					if (target != null && target.isDead())
						return new L2Character[] { target };
				}

				return _emptyTargetList;
			}
			case TARGET_AURA:
			case TARGET_FRONT_AURA:
			case TARGET_BEHIND_AURA:
			{
				final boolean srcInArena = (activeChar.isInsideZone(L2Character.ZONE_PVP) && !activeChar.isInsideZone(L2Character.ZONE_SIEGE));

				final L2PcInstance sourcePlayer = activeChar.getActingPlayer();

				// Go through the L2Character _knownList
				final Collection<L2Character> objs = activeChar.getKnownList().getKnownCharactersInRadius(getSkillRadius());
				if (getSkillType() == L2SkillType.DUMMY)
				{
					if (onlyFirst)
						return new L2Character[] { activeChar };

					targetList.add(activeChar);
					for (L2Character obj : objs)
					{
						if (!(obj == activeChar || obj == sourcePlayer || obj instanceof L2Npc || obj instanceof L2Attackable))
							continue;
						targetList.add(obj);
					}
				}
				else
				{
					for (L2Character obj : objs)
					{
						if (obj instanceof L2Attackable || obj instanceof L2Playable)
						{
							switch (targetType)
							{
								case TARGET_FRONT_AURA:
									if (!obj.isInFrontOf(activeChar))
										continue;
									break;
								case TARGET_BEHIND_AURA:
									if (!obj.isBehind(activeChar))
										continue;
									break;
							}

							if (!checkForAreaOffensiveSkills(activeChar, obj, this, srcInArena))
								continue;

							if (onlyFirst)
								return new L2Character[] { obj };

							targetList.add(obj);
						}
					}
				}
				return targetList.toArray(new L2Character[targetList.size()]);
			}
			case TARGET_AREA_SUMMON:
			{
				target = activeChar.getPet();
				if (target == null || !(target instanceof L2SummonInstance) || target.isDead())
					return _emptyTargetList;

				if (onlyFirst)
					return new L2Character[] { target };

				final boolean srcInArena = (activeChar.isInsideZone(L2Character.ZONE_PVP) && !activeChar.isInsideZone(L2Character.ZONE_SIEGE));
				final Collection<L2Character> objs = target.getKnownList().getKnownCharacters();
				final int radius = getSkillRadius();

				for (L2Character obj : objs)
				{
					if (obj == null || obj == target || obj == activeChar)
						continue;

					if (!Util.checkIfInRange(radius, target, obj, true))
						continue;

					if (!(obj instanceof L2Attackable || obj instanceof L2Playable))
						continue;

					if (!checkForAreaOffensiveSkills(activeChar, obj, this, srcInArena))
						continue;

					targetList.add(obj);
				}

				if (targetList.isEmpty())
					return _emptyTargetList;

				return targetList.toArray(new L2Character[targetList.size()]);
			}
			case TARGET_AREA:
			case TARGET_FRONT_AREA:
			case TARGET_BEHIND_AREA:
			{
				if (((target == null || target == activeChar || target.isAlikeDead()) && getCastRange() >= 0) || (!(target instanceof L2Attackable || target instanceof L2Playable)))
				{
					activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
					return _emptyTargetList;
				}

				final L2Character origin;
				final boolean srcInArena = (activeChar.isInsideZone(L2Character.ZONE_PVP) && !activeChar.isInsideZone(L2Character.ZONE_SIEGE));
				final int radius = getSkillRadius();

				if (getCastRange() >= 0)
				{
					if (!checkForAreaOffensiveSkills(activeChar, target, this, srcInArena))
						return _emptyTargetList;

					if (onlyFirst)
						return new L2Character[] { target };

					origin = target;
					targetList.add(origin); // Add target to target list
				}
				else
					origin = activeChar;

				final Collection<L2Character> objs = activeChar.getKnownList().getKnownCharacters();
				for (L2Character obj : objs)
				{
					if (!(obj instanceof L2Attackable || obj instanceof L2Playable))
						continue;

					if (obj == origin)
						continue;

					if (Util.checkIfInRange(radius, origin, obj, true))
					{
						switch (targetType)
						{
							case TARGET_FRONT_AREA:
								if (!obj.isInFrontOf(activeChar))
									continue;
								break;
							case TARGET_BEHIND_AREA:
								if (!obj.isBehind(activeChar))
									continue;
								break;
						}

						if (!checkForAreaOffensiveSkills(activeChar, obj, this, srcInArena))
							continue;

						targetList.add(obj);
					}
				}

				if (targetList.isEmpty())
					return _emptyTargetList;

				return targetList.toArray(new L2Character[targetList.size()]);
			}
			case TARGET_PARTY:
			{
				if (onlyFirst)
					return new L2Character[] { activeChar };

				targetList.add(activeChar);

				final int radius = getSkillRadius();

				L2PcInstance player = activeChar.getActingPlayer();
				if (activeChar instanceof L2Summon)
				{
					if (addCharacter(activeChar, player, radius, false))
						targetList.add(player);
				}
				else if (activeChar instanceof L2PcInstance)
				{
					if (addSummon(activeChar, player, radius, false))
						targetList.add(player.getPet());
				}

				if (activeChar.isInParty())
				{
					// Get a list of Party Members
					for (L2PcInstance partyMember : activeChar.getParty().getPartyMembers())
					{
						if (partyMember == null || partyMember == player)
							continue;

						if (addCharacter(activeChar, partyMember, radius, false))
							targetList.add(partyMember);

						if (addSummon(activeChar, partyMember, radius, false))
							targetList.add(partyMember.getPet());
					}
				}
				return targetList.toArray(new L2Character[targetList.size()]);
			}
			case TARGET_PARTY_MEMBER:
			{
				if (target != null && (target == activeChar || (activeChar.isInParty() && target.isInParty() && activeChar.getParty().getPartyLeaderOID() == target.getParty().getPartyLeaderOID()) || (activeChar instanceof L2PcInstance && target instanceof L2Summon && activeChar.getPet() == target) || (activeChar instanceof L2Summon && target instanceof L2PcInstance && activeChar == target.getPet())))
				{
					if (!target.isDead())
					{
						// If a target is found, return it in a table else send a system message TARGET_IS_INCORRECT
						return new L2Character[] { target };
					}
					return _emptyTargetList;
				}

				activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
				return _emptyTargetList;
			}
			case TARGET_PARTY_OTHER:
			{
				if (target != null && target != activeChar && activeChar.isInParty() && target.isInParty() && activeChar.getParty().getPartyLeaderOID() == target.getParty().getPartyLeaderOID())
				{
					if (!target.isDead())
					{
						if (target instanceof L2PcInstance)
						{
							switch (getId())
							{
							// FORCE BUFFS may cancel here but there should be a proper condition
								case 426:
									if (!((L2PcInstance) target).isMageClass())
										return new L2Character[] { target };
									return _emptyTargetList;

								case 427:
									if (((L2PcInstance) target).isMageClass())
										return new L2Character[] { target };

									return _emptyTargetList;
							}
						}
						return new L2Character[] { target };
					}
					return _emptyTargetList;
				}

				activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
				return _emptyTargetList;
			}
			case TARGET_CORPSE_ALLY:
			case TARGET_ALLY:
			{
				if (activeChar instanceof L2Playable)
				{
					final L2PcInstance player = activeChar.getActingPlayer();

					if (player == null)
						return _emptyTargetList;

					if (player.isInOlympiadMode())
						return new L2Character[] { player };

					final boolean isCorpseType = targetType == SkillTargetType.TARGET_CORPSE_ALLY;

					if (!isCorpseType)
					{
						if (onlyFirst)
							return new L2Character[] { player };

						targetList.add(player);
					}

					final int radius = getSkillRadius();

					if (addSummon(activeChar, player, radius, isCorpseType))
						targetList.add(player.getPet());

					if (player.getClan() != null)
					{
						// Get all visible objects in a spherical area near the L2Character
						final Collection<L2PcInstance> objs = activeChar.getKnownList().getKnownPlayersInRadius(radius);

						for (L2PcInstance obj : objs)
						{
							if (obj == null)
								continue;
							if ((obj.getAllyId() == 0 || obj.getAllyId() != player.getAllyId()) && (obj.getClan() == null || obj.getClanId() != player.getClanId()))
								continue;

							if (player.isInDuel())
							{
								if (player.getDuelId() != obj.getDuelId())
									continue;
								if (player.isInParty() && obj.isInParty() && player.getParty().getPartyLeaderOID() != obj.getParty().getPartyLeaderOID())
									continue;
							}

							// Don't add this target if this is a Pc->Pc pvp
							// casting and pvp condition not met
							if (!player.checkPvpSkill(obj, this))
								continue;

							if (!TvTEvent.checkForTvTSkill(player, obj, this))
								continue;

							if (!onlyFirst && addSummon(activeChar, obj, radius, isCorpseType))
								targetList.add(obj.getPet());

							if (!addCharacter(activeChar, obj, radius, isCorpseType))
								continue;

							if (isCorpseType)
							{
								// Siege battlefield resurrect has been made possible for participants
								if (getSkillType() == L2SkillType.RESURRECT)
								{
									if (obj.isInsideZone(L2Character.ZONE_SIEGE) && !obj.isInSiege())
										continue;
								}
							}

							if (onlyFirst)
								return new L2Character[] { obj };

							targetList.add(obj);
						}
					}
				}
				return targetList.toArray(new L2Character[targetList.size()]);
			}
			case TARGET_CORPSE_CLAN:
			case TARGET_CLAN:
			{
				if (activeChar instanceof L2Playable)
				{
					final L2PcInstance player = activeChar.getActingPlayer();

					if (player == null)
						return _emptyTargetList;

					if (player.isInOlympiadMode())
						return new L2Character[] { player };

					final boolean isCorpseType = targetType == SkillTargetType.TARGET_CORPSE_CLAN;

					if (!isCorpseType)
					{
						if (onlyFirst)
							return new L2Character[] { player };

						targetList.add(player);
					}

					final int radius = getSkillRadius();
					final L2Clan clan = player.getClan();

					if (addSummon(activeChar, player, radius, isCorpseType))
						targetList.add(player.getPet());

					if (clan != null)
					{
						L2PcInstance obj;
						// Get Clan Members
						for (L2ClanMember member : clan.getMembers())
						{
							obj = member.getPlayerInstance();

							if (obj == null || obj == player)
								continue;

							if (player.isInDuel())
							{
								if (player.getDuelId() != obj.getDuelId())
									continue;
								if (player.isInParty() && obj.isInParty() && player.getParty().getPartyLeaderOID() != obj.getParty().getPartyLeaderOID())
									continue;
							}

							// Don't add this target if this is a Pc->Pc pvp casting and pvp condition not met
							if (!player.checkPvpSkill(obj, this))
								continue;

							if (!TvTEvent.checkForTvTSkill(player, obj, this))
								continue;

							if (!onlyFirst && addSummon(activeChar, obj, radius, isCorpseType))
								targetList.add(obj.getPet());

							if (!addCharacter(activeChar, obj, radius, isCorpseType))
								continue;

							if (isCorpseType)
							{
								if (getSkillType() == L2SkillType.RESURRECT)
								{
									// check target is not in a active siege zone
									if (obj.isInsideZone(L2Character.ZONE_SIEGE) && !obj.isInSiege())
										continue;
								}
							}

							if (onlyFirst)
								return new L2Character[] { obj };

							targetList.add(obj);
						}
					}
				}
				else if (activeChar instanceof L2Npc)
				{
					// for buff purposes, returns friendly mobs nearby and mob itself
					final L2Npc npc = (L2Npc) activeChar;
					if (npc.getClan() == null || npc.getClan().isEmpty())
						return new L2Character[] { activeChar };

					targetList.add(activeChar);
					final Collection<L2Object> objs = activeChar.getKnownList().getKnownObjects().values();
					for (L2Object newTarget : objs)
					{
						if (newTarget instanceof L2Npc && npc.getClan().equals(((L2Npc) newTarget).getClan()))
						{
							// Bypass buff if target is dead
							if (((L2Npc) newTarget).isDead())
								continue;

							if (!Util.checkIfInRange(getCastRange(), activeChar, newTarget, true))
								continue;

							targetList.add((L2Npc) newTarget);
						}
					}
				}

				return targetList.toArray(new L2Character[targetList.size()]);
			}
			case TARGET_CORPSE_PLAYER:
			{
				if (target != null && target.isDead())
				{
					final L2PcInstance player;
					if (activeChar instanceof L2PcInstance)
						player = (L2PcInstance) activeChar;
					else
						player = null;

					final L2PcInstance targetPlayer;
					if (target instanceof L2PcInstance)
						targetPlayer = (L2PcInstance) target;
					else
						targetPlayer = null;

					final L2PetInstance targetPet;
					if (target instanceof L2PetInstance)
						targetPet = (L2PetInstance) target;
					else
						targetPet = null;

					if (player != null && (targetPlayer != null || targetPet != null))
					{
						boolean condGood = true;

						if (getSkillType() == L2SkillType.RESURRECT)
						{
							if (targetPlayer != null)
							{
								// check target is not in a active siege zone
								if (targetPlayer.isInsideZone(L2Character.ZONE_SIEGE) && !targetPlayer.isInSiege())
								{
									condGood = false;
									activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.CANNOT_BE_RESURRECTED_DURING_SIEGE));
								}

								if (targetPlayer.isFestivalParticipant()) // Check to see if the current player target is in a
																			// festival.
								{
									condGood = false;
									activeChar.sendMessage("You may not resurrect participants in a festival.");
								}

								if (targetPlayer.isReviveRequested())
								{
									if (targetPlayer.isRevivingPet())
										player.sendPacket(SystemMessageId.MASTER_CANNOT_RES); // While a pet is attempting to
																								// resurrect, it cannot help in
																								// resurrecting its master.
									else
										player.sendPacket(SystemMessageId.RES_HAS_ALREADY_BEEN_PROPOSED); // Resurrection is
																											// already been
																											// proposed.
									condGood = false;
								}
							}
							else if (targetPet != null)
							{
								if (targetPet.getOwner() != player)
								{
									if (targetPet.getOwner().isReviveRequested())
									{
										if (targetPet.getOwner().isRevivingPet())
											player.sendPacket(SystemMessageId.RES_HAS_ALREADY_BEEN_PROPOSED); // Resurrection is
																												// already been
																												// proposed.
										else
											player.sendPacket(SystemMessageId.CANNOT_RES_PET2); // A pet cannot be resurrected
																								// while it's owner is in the
																								// process of resurrecting.
										condGood = false;
									}
								}
							}
						}

						if (condGood)
						{
							if (!onlyFirst)
							{
								targetList.add(target);
								return targetList.toArray(new L2Object[targetList.size()]);
							}

							return new L2Character[] { target };
						}
					}
				}
				activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
				return _emptyTargetList;
			}
			case TARGET_CORPSE_MOB:
			{
				final boolean isSummon = target instanceof L2SummonInstance;
				if (!(isSummon || target instanceof L2Attackable) || !target.isDead())
				{
					activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
					return _emptyTargetList;
				}

				// Corpse mob only available for half time
				switch (getSkillType())
				{
					case SUMMON:
					{
						if (isSummon && ((L2SummonInstance) target).getOwner() != null && ((L2SummonInstance) target).getOwner().getObjectId() == activeChar.getObjectId())
							return _emptyTargetList;
					}
					case DRAIN:
					{
						if (DecayTaskManager.getInstance().getTasks().containsKey(target) && (System.currentTimeMillis() - DecayTaskManager.getInstance().getTasks().get(target)) > DecayTaskManager.DEFAULT_DECAY_TIME / 2)
						{
							activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.CORPSE_TOO_OLD_SKILL_NOT_USED));
							return _emptyTargetList;
						}
					}
				}

				if (!onlyFirst)
				{
					targetList.add(target);
					return targetList.toArray(new L2Object[targetList.size()]);
				}

				return new L2Character[] { target };

			}
			case TARGET_AREA_CORPSE_MOB:
			{
				if ((!(target instanceof L2Attackable)) || !target.isDead())
				{
					activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
					return _emptyTargetList;
				}

				if (onlyFirst)
					return new L2Character[] { target };

				targetList.add(target);

				final boolean srcInArena = (activeChar.isInsideZone(L2Character.ZONE_PVP) && !activeChar.isInsideZone(L2Character.ZONE_SIEGE));

				final int radius = getSkillRadius();
				final Collection<L2Character> objs = activeChar.getKnownList().getKnownCharacters();
				for (L2Character obj : objs)
				{
					if (!(obj instanceof L2Attackable || obj instanceof L2Playable) || !Util.checkIfInRange(radius, target, obj, true))
						continue;

					if (!checkForAreaOffensiveSkills(activeChar, obj, this, srcInArena))
						continue;

					targetList.add(obj);
				}

				if (targetList.isEmpty())
					return _emptyTargetList;

				return targetList.toArray(new L2Character[targetList.size()]);
			}
			case TARGET_UNLOCKABLE:
			{
				if (!(target instanceof L2DoorInstance) && !(target instanceof L2ChestInstance))
					return _emptyTargetList;

				if (!onlyFirst)
				{
					targetList.add(target);
					return targetList.toArray(new L2Object[targetList.size()]);
				}

				return new L2Character[] { target };

			}
			case TARGET_UNDEAD:
			{
				if (target instanceof L2Npc || target instanceof L2SummonInstance)
				{
					if (!target.isUndead() || target.isDead())
					{
						activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
						return _emptyTargetList;
					}

					if (!onlyFirst)
						targetList.add(target);
					else
						return new L2Character[] { target };

					return targetList.toArray(new L2Object[targetList.size()]);
				}

				activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
				return _emptyTargetList;
			}
			case TARGET_AREA_UNDEAD:
			{
				final L2Character cha;
				final int radius = getSkillRadius();
				if (getCastRange() >= 0 && (target instanceof L2Npc || target instanceof L2SummonInstance) && target.isUndead() && !target.isAlikeDead())
				{
					cha = target;

					if (!onlyFirst)
						targetList.add(cha); // Add target to target list
					else
						return new L2Character[] { cha };
				}
				else
					cha = activeChar;

				final Collection<L2Character> objs = activeChar.getKnownList().getKnownCharacters();
				for (L2Character obj : objs)
				{
					if (!Util.checkIfInRange(radius, cha, obj, true))
						continue;
					if (obj instanceof L2Npc)
						target = obj;
					else if (obj instanceof L2SummonInstance)
						target = obj;
					else
						continue;

					if (!target.isAlikeDead()) // If target is not dead/fake death
					{
						if (!target.isUndead())
							continue;

						if (geoEnabled && !GeoData.getInstance().canSeeTarget(activeChar, target))
							continue;

						if (!onlyFirst)
							targetList.add(obj);
						else
							return new L2Character[] { obj };
					}
				}

				if (targetList.isEmpty())
					return _emptyTargetList;

				return targetList.toArray(new L2Character[targetList.size()]);
			}
			case TARGET_ENEMY_SUMMON:
			{
				if (target instanceof L2Summon)
				{
					L2Summon targetSummon = (L2Summon) target;
					if (activeChar instanceof L2PcInstance && activeChar.getPet() != targetSummon && !targetSummon.isDead() && (targetSummon.getOwner().getPvpFlag() != 0 || targetSummon.getOwner().getKarma() > 0) || (targetSummon.getOwner().isInsideZone(L2Character.ZONE_PVP) && ((L2PcInstance) activeChar).isInsideZone(L2Character.ZONE_PVP)) || (targetSummon.getOwner().isInDuel() && ((L2PcInstance) activeChar).isInDuel() && targetSummon.getOwner().getDuelId() == ((L2PcInstance) activeChar).getDuelId()))
						return new L2Character[] { targetSummon };
				}
				return _emptyTargetList;
			}/*
			 * // npc only for now - untested case TARGET_CLAN_MEMBER: { if (activeChar instanceof L2Npc) { // for buff purposes, returns
			 * friendly mobs nearby and mob itself final L2Npc npc = (L2Npc) activeChar; if (npc.getClan() == null || npc.getClan().isEmpty()) {
			 * return new L2Character[]{activeChar}; } final Collection<L2Object> objs = activeChar.getKnownList().getKnownObjects().values();
			 * for (L2Object newTarget : objs) { if (newTarget instanceof L2Npc && npc.getClan().equals(((L2Npc) newTarget).getClan())) { if
			 * (!Util.checkIfInRange(getCastRange(), activeChar, newTarget, true)) continue; if (((L2Npc) newTarget).getFirstEffect(this) !=
			 * null) continue; targetList.add((L2Npc) newTarget); break; // found } } if (targetList.isEmpty()) targetList.add(npc); } else
			 * return _emptyTargetList; return targetList.toArray(new L2Character[targetList.size()]); }
			 */
			default:
			{
				activeChar.sendMessage("Target type of skill is not currently handled");
				return _emptyTargetList;
			}
		}// end switch
	}

	public final L2Object[] getTargetList(L2Character activeChar)
	{
		return getTargetList(activeChar, false);
	}

	public final L2Object getFirstOfTargetList(L2Character activeChar)
	{
		L2Object[] targets = getTargetList(activeChar, true);
		if (targets.length == 0)
			return null;

		return targets[0];
	}

	/*
	 * Check if should be target added to the target list false if target is dead, target same as caster, target inside peace zone, target in the
	 * same party with caster, caster can see target. Additional checks if not in PvP zones (arena, siege): target in not the same clan and
	 * alliance with caster, and usual skill PvP check. Caution: distance is not checked.
	 */
	public static final boolean checkForAreaOffensiveSkills(L2Character caster, L2Character target, L2Skill skill, boolean sourceInArena)
	{
		if (target == null || target.isDead() || target == caster)
			return false;

		final L2PcInstance player = caster.getActingPlayer();
		final L2PcInstance targetPlayer = target.getActingPlayer();
		if (player != null)
		{
			if (targetPlayer != null)
			{
				if (targetPlayer == caster || targetPlayer == player)
					return false;

				if (targetPlayer.inObserverMode())
					return false;

				if (skill.isOffensive() && player.getSiegeState() > 0 && player.isInsideZone(L2Character.ZONE_SIEGE) && player.getSiegeState() == targetPlayer.getSiegeState())
					return false;

				if (target.isInsideZone(L2Character.ZONE_PEACE))
					return false;

				if (player.isInParty() && targetPlayer.isInParty())
				{
					// Same party
					if (player.getParty().getPartyLeaderOID() == targetPlayer.getParty().getPartyLeaderOID())
						return false;

					// Same commandchannel
					if (player.getParty().getCommandChannel() != null && player.getParty().getCommandChannel() == targetPlayer.getParty().getCommandChannel())
						return false;
				}

				if (!TvTEvent.checkForTvTSkill(player, targetPlayer, skill))
					return false;

				if (!sourceInArena && !(targetPlayer.isInsideZone(L2Character.ZONE_PVP) && !targetPlayer.isInsideZone(L2Character.ZONE_SIEGE)))
				{
					if (player.getAllyId() != 0 && player.getAllyId() == targetPlayer.getAllyId())
						return false;

					if (player.getClanId() != 0 && player.getClanId() == targetPlayer.getClanId())
						return false;

					if (!player.checkPvpSkill(targetPlayer, skill, (caster instanceof L2Summon)))
						return false;
				}
			}
		}
		else
		{
			// target is mob
			if (targetPlayer == null && target instanceof L2Attackable && caster instanceof L2Attackable)
			{
				String casterEnemyClan = ((L2Attackable) caster).getEnemyClan();
				if (casterEnemyClan == null || casterEnemyClan.isEmpty())
					return false;

				String targetClan = ((L2Attackable) target).getClan();
				if (targetClan == null || targetClan.isEmpty())
					return false;

				if (!casterEnemyClan.equals(targetClan))
					return false;
			}
		}

		if (geoEnabled && !GeoData.getInstance().canSeeTarget(caster, target))
			return false;

		return true;
	}

	public static final boolean addSummon(L2Character caster, L2PcInstance owner, int radius, boolean isDead)
	{
		final L2Summon summon = owner.getPet();

		if (summon == null)
			return false;

		return addCharacter(caster, summon, radius, isDead);
	}

	public static final boolean addCharacter(L2Character caster, L2Character target, int radius, boolean isDead)
	{
		if (isDead != target.isDead())
			return false;

		if (radius > 0 && !Util.checkIfInRange(radius, caster, target, true))
			return false;

		return true;

	}

	public final Func[] getStatFuncs(L2Effect effect, L2Character player)
	{
		if (_funcTemplates == null)
			return _emptyFunctionSet;

		if (!(player instanceof L2Playable) && !(player instanceof L2Attackable))
			return _emptyFunctionSet;

		ArrayList<Func> funcs = new ArrayList<>(_funcTemplates.length);

		Env env = new Env();
		env.player = player;
		env.skill = this;

		Func f;

		for (FuncTemplate t : _funcTemplates)
		{

			f = t.getFunc(env, this); // skill is owner
			if (f != null)
				funcs.add(f);
		}
		if (funcs.isEmpty())
			return _emptyFunctionSet;

		return funcs.toArray(new Func[funcs.size()]);
	}

	public boolean hasEffects()
	{
		return (_effectTemplates != null && _effectTemplates.length > 0);
	}

	public EffectTemplate[] getEffectTemplates()
	{
		return _effectTemplates;
	}

	public boolean hasSelfEffects()
	{
		return (_effectTemplatesSelf != null && _effectTemplatesSelf.length > 0);
	}

	/**
	 * @param effector
	 * @param effected
	 * @param env
	 *            parameters for secondary effects (shield and ss/bss/bsss)
	 * @return an array with the effects that have been added to effector
	 */
	public final L2Effect[] getEffects(L2Character effector, L2Character effected, Env env)
	{
		if (!hasEffects() || isPassive())
			return _emptyEffectSet;

		// doors and siege flags cannot receive any effects
		if (effected instanceof L2DoorInstance || effected instanceof L2SiegeFlagInstance)
			return _emptyEffectSet;

		if (effector != effected)
		{
			if (isOffensive() || isDebuff())
			{
				if (effected.isInvul())
					return _emptyEffectSet;

				if (effector instanceof L2PcInstance && ((L2PcInstance) effector).isGM())
				{
					if (!((L2PcInstance) effector).getAccessLevel().canGiveDamage())
						return _emptyEffectSet;
				}
			}
		}

		ArrayList<L2Effect> effects = new ArrayList<>(_effectTemplates.length);

		if (env == null)
			env = new Env();

		env.skillMastery = Formulas.calcSkillMastery(effector, this);
		env.player = effector;
		env.target = effected;
		env.skill = this;

		for (EffectTemplate et : _effectTemplates)
		{
			boolean success = true;

			if (et.effectPower > -1)
				success = Formulas.calcEffectSuccess(effector, effected, et, this, env.shld, env.ss, env.sps, env.bss);

			if (success)
			{
				L2Effect e = et.getEffect(env);
				if (e != null)
				{
					e.scheduleEffect();
					effects.add(e);
				}
			}
			// display fail message only for effects with icons
			else if (et.icon && effector instanceof L2PcInstance)
			{
				SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_RESISTED_YOUR_S2);
				sm.addCharName(effected);
				sm.addSkillName(this);
				((L2PcInstance) effector).sendPacket(sm);
			}
		}

		if (effects.isEmpty())
			return _emptyEffectSet;

		return effects.toArray(new L2Effect[effects.size()]);
	}

	/**
	 * Warning: this method doesn't consider modifier (shield, ss, sps, bss) for secondary effects
	 * 
	 * @param effector
	 * @param effected
	 * @return An array of L2Effect.
	 */
	public final L2Effect[] getEffects(L2Character effector, L2Character effected)
	{
		return getEffects(effector, effected, null);
	}

	/**
	 * This method has suffered some changes in CT2.2 ->CT2.3<br>
	 * Effect engine is now supporting secondary effects with independent success/fail calculus from effect skill. Env parameter has been added
	 * to pass parameters like soulshot, spiritshots, blessed spiritshots or shield deffence. Some other optimizations have been done <br>
	 * <br>
	 * This new feature works following next rules: <li>To enable feature, effectPower must be over -1 (check DocumentSkill#attachEffect for
	 * further information)</li> <li>If main skill fails, secondary effect always fail</li>
	 * 
	 * @param effector
	 * @param effected
	 * @param env
	 *            parameters for secondary effects (shield and ss/bss/bsss)
	 * @return An array of L2Effect.
	 */
	public final L2Effect[] getEffects(L2CubicInstance effector, L2Character effected, Env env)
	{
		if (!hasEffects() || isPassive())
			return _emptyEffectSet;

		if (effector.getOwner() != effected)
		{
			if (isDebuff() || isOffensive())
			{
				if (effected.isInvul())
					return _emptyEffectSet;

				if (effector.getOwner().isGM() && !effector.getOwner().getAccessLevel().canGiveDamage())
					return _emptyEffectSet;
			}
		}

		ArrayList<L2Effect> effects = new ArrayList<>(_effectTemplates.length);

		if (env == null)
			env = new Env();

		env.player = effector.getOwner();
		env.cubic = effector;
		env.target = effected;
		env.skill = this;

		for (EffectTemplate et : _effectTemplates)
		{
			boolean success = true;
			if (et.effectPower > -1)
				success = Formulas.calcEffectSuccess(effector.getOwner(), effected, et, this, env.shld, env.ss, env.sps, env.bss);

			if (success)
			{
				L2Effect e = et.getEffect(env);
				if (e != null)
				{
					e.scheduleEffect();
					effects.add(e);
				}
			}
		}

		if (effects.isEmpty())
			return _emptyEffectSet;

		return effects.toArray(new L2Effect[effects.size()]);
	}

	public final L2Effect[] getEffectsSelf(L2Character effector)
	{
		if (!hasSelfEffects() || isPassive())
			return _emptyEffectSet;

		List<L2Effect> effects = new ArrayList<>(_effectTemplatesSelf.length);

		Env env = new Env();
		env.player = effector;
		env.target = effector;
		env.skill = this;

		for (EffectTemplate et : _effectTemplatesSelf)
		{
			L2Effect e = et.getEffect(env);
			if (e != null)
			{
				e.setSelfEffect();
				e.scheduleEffect();
				effects.add(e);
			}
		}
		if (effects.isEmpty())
			return _emptyEffectSet;

		return effects.toArray(new L2Effect[effects.size()]);
	}

	public final void attach(FuncTemplate f)
	{
		if (_funcTemplates == null)
			_funcTemplates = new FuncTemplate[] { f };
		else
		{
			int len = _funcTemplates.length;
			FuncTemplate[] tmp = new FuncTemplate[len + 1];
			System.arraycopy(_funcTemplates, 0, tmp, 0, len);
			tmp[len] = f;
			_funcTemplates = tmp;
		}
	}

	public final void attach(EffectTemplate effect)
	{
		if (_effectTemplates == null)
			_effectTemplates = new EffectTemplate[] { effect };
		else
		{
			int len = _effectTemplates.length;
			EffectTemplate[] tmp = new EffectTemplate[len + 1];
			System.arraycopy(_effectTemplates, 0, tmp, 0, len);
			tmp[len] = effect;
			_effectTemplates = tmp;
		}
	}

	public final void attachSelf(EffectTemplate effect)
	{
		if (_effectTemplatesSelf == null)
		{
			_effectTemplatesSelf = new EffectTemplate[] { effect };
		}
		else
		{
			int len = _effectTemplatesSelf.length;
			EffectTemplate[] tmp = new EffectTemplate[len + 1];
			System.arraycopy(_effectTemplatesSelf, 0, tmp, 0, len);
			tmp[len] = effect;
			_effectTemplatesSelf = tmp;
		}
	}

	public final void attach(Condition c, boolean itemOrWeapon)
	{
		if (itemOrWeapon)
		{
			if (_itemPreCondition == null)
				_itemPreCondition = new FastList<>();
			_itemPreCondition.add(c);
		}
		else
		{
			if (_preCondition == null)
				_preCondition = new FastList<>();
			_preCondition.add(c);
		}
	}

	/**
	 * @param skillId
	 * @param skillLvl
	 * @param values
	 * @return L2ExtractableSkill
	 * @author Zoey76
	 */
	private L2ExtractableSkill parseExtractableSkill(int skillId, int skillLvl, String values)
	{
		String[] lineSplit = values.split(";");

		final List<L2ExtractableProductItem> product_temp = new ArrayList<>();

		for (int i = 0; i <= (lineSplit.length - 1); i++)
		{
			final String[] lineSplit2 = lineSplit[i].split(",");

			if (lineSplit2.length < 3)
				_log.warn("Extractable skills data: Error in Skill Id: " + skillId + " Level: " + skillLvl + " -> wrong seperator!");

			int[] production = null;
			int[] amount = null;
			double chance = 0;
			int prodId = 0;
			int quantity = 0;
			try
			{
				int k = 0;
				production = new int[(lineSplit2.length - 1) / 2];
				amount = new int[(lineSplit2.length - 1) / 2];
				for (int j = 0; j < (lineSplit2.length - 1); j++)
				{
					prodId = Integer.parseInt(lineSplit2[j]);
					quantity = Integer.parseInt(lineSplit2[j += 1]);
					if ((prodId <= 0) || (quantity <= 0))
						_log.warn("Extractable skills data: Error in Skill Id: " + skillId + " Level: " + skillLvl + " wrong production Id: " + prodId + " or wrond quantity: " + quantity + "!");
					production[k] = prodId;
					amount[k] = quantity;
					k++;
				}
				chance = Double.parseDouble(lineSplit2[lineSplit2.length - 1]);
			}
			catch (Exception e)
			{
				_log.warn("Extractable skills data: Error in Skill Id: " + skillId + " Level: " + skillLvl + " -> incomplete/invalid production data or wrong seperator!");
			}

			product_temp.add(new L2ExtractableProductItem(production, amount, chance));
		}

		if (product_temp.isEmpty())
			_log.warn("Extractable skills data: Error in Skill Id: " + skillId + " Level: " + skillLvl + " -> There are no production items!");

		return new L2ExtractableSkill(SkillTable.getSkillHashCode(this), product_temp);
	}

	public L2ExtractableSkill getExtractableSkill()
	{
		return _extractableItems;
	}

	@Override
	public String toString()
	{
		return "" + _name + "[id=" + _id + ",lvl=" + _level + "]";
	}
}
