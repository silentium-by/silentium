/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.templates.item;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javolution.util.FastList;
import silentium.commons.utils.StringUtil;
import silentium.gameserver.handler.ISkillHandler;
import silentium.gameserver.handler.SkillHandler;
import silentium.gameserver.model.L2Effect;
import silentium.gameserver.model.L2ItemInstance;
import silentium.gameserver.model.L2Object;
import silentium.gameserver.model.L2Skill;
import silentium.gameserver.model.actor.L2Character;
import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.quest.Quest;
import silentium.gameserver.skills.Env;
import silentium.gameserver.skills.Formulas;
import silentium.gameserver.skills.SkillHolder;
import silentium.gameserver.skills.basefuncs.Func;
import silentium.gameserver.skills.basefuncs.FuncTemplate;
import silentium.gameserver.skills.conditions.Condition;
import silentium.gameserver.skills.conditions.ConditionGameChance;
import silentium.gameserver.templates.StatsSet;
import silentium.gameserver.templates.skills.L2SkillType;

/**
 * This class is dedicated to the management of weapons.
 */
public final class L2Weapon extends L2Item
{
	private final L2WeaponType _type;
	private final int _rndDam;
	private final int _soulShotCount;
	private final int _spiritShotCount;
	private final int _mpConsume;
	private final boolean _isMagical;

	private SkillHolder _enchant4Skill = null; // skill that activates when item is enchanted +4 (for duals)

	// Attached skills for Special Abilities
	private SkillHolder _skillsOnCast;
	private Condition _skillsOnCastCondition = null;
	private SkillHolder _skillsOnCrit;
	private Condition _skillsOnCritCondition = null;

	private final int _reuseDelay;

	/**
	 * Constructor for Weapon.<BR>
	 * <BR>
	 * <U><I>Variables filled :</I></U>
	 * <UL>
	 * <LI>_soulShotCount & _spiritShotCount</LI>
	 * <LI>_pDam & _mDam & _rndDam</LI>
	 * <LI>_critical</LI>
	 * <LI>_hitModifier</LI>
	 * <LI>_avoidModifier</LI>
	 * <LI>_shieldDes & _shieldDefRate</LI>
	 * <LI>_atkSpeed & _AtkReuse</LI>
	 * <LI>_mpConsume</LI>
	 * <LI>_isMagical</LI>
	 * </UL>
	 *
	 * @param set
	 *            : StatsSet designating the set of couples (key,value) caracterizing the armor
	 * @see L2Item constructor
	 */
	public L2Weapon(StatsSet set)
	{
		super(set);
		_type = L2WeaponType.valueOf(set.getString("weapon_type", "none").toUpperCase());
		_type1 = L2Item.TYPE1_WEAPON_RING_EARRING_NECKLACE;
		_type2 = L2Item.TYPE2_WEAPON;
		_soulShotCount = set.getInteger("soulshots", 0);
		_spiritShotCount = set.getInteger("spiritshots", 0);
		_rndDam = set.getInteger("random_damage", 0);
		_mpConsume = set.getInteger("mp_consume", 0);
		_reuseDelay = set.getInteger("reuse_delay", 0);
		_isMagical = set.getBool("is_magical", false);

		String skill = set.getString("enchant4_skill", null);
		if (skill != null)
		{
			String[] info = skill.split("-");

			if (info != null && info.length == 2)
			{
				int id = 0;
				int level = 0;
				try
				{
					id = Integer.parseInt(info[0]);
					level = Integer.parseInt(info[1]);
				}
				catch (Exception nfe)
				{
					// Incorrect syntax, dont add new skill
					_log.info(StringUtil.concat("> Couldnt parse ", skill, " in weapon enchant skills! item ", toString()));
				}
				if (id > 0 && level > 0)
					_enchant4Skill = new SkillHolder(id, level);
			}
		}

		skill = set.getString("oncast_skill", null);
		if (skill != null)
		{
			String[] info = skill.split("-");
			String infochance = set.getString("oncast_chance", null);
			if (info != null && info.length == 2)
			{
				int id = 0;
				int level = 0;
				int chance = 0;
				try
				{
					id = Integer.parseInt(info[0]);
					level = Integer.parseInt(info[1]);
					if (infochance != null)
						chance = Integer.parseInt(infochance);
				}
				catch (Exception nfe)
				{
					// Incorrect syntax, dont add new skill
					_log.info(StringUtil.concat("> Couldnt parse ", skill, " in weapon oncast skills! item ", toString()));
				}
				if (id > 0 && level > 0 && chance > 0)
				{
					_skillsOnCast = new SkillHolder(id, level);
					if (infochance != null)
						_skillsOnCastCondition = new ConditionGameChance(chance);
				}
			}
		}

		skill = set.getString("oncrit_skill", null);
		if (skill != null)
		{
			String[] info = skill.split("-");
			String infochance = set.getString("oncrit_chance", null);
			if (info != null && info.length == 2)
			{
				int id = 0;
				int level = 0;
				int chance = 0;
				try
				{
					id = Integer.parseInt(info[0]);
					level = Integer.parseInt(info[1]);
					if (infochance != null)
						chance = Integer.parseInt(infochance);
				}
				catch (Exception nfe)
				{
					// Incorrect syntax, dont add new skill
					_log.info(StringUtil.concat("> Couldnt parse ", skill, " in weapon oncrit skills! item ", toString()));
				}
				if (id > 0 && level > 0 && chance > 0)
				{
					_skillsOnCrit = new SkillHolder(id, level);
					if (infochance != null)
						_skillsOnCritCondition = new ConditionGameChance(chance);
				}
			}
		}
	}

	/**
	 * @return the type of weapon.
	 */
	@Override
	public L2WeaponType getItemType()
	{
		return _type;
	}

	/**
	 * @return the ID of the Etc item after applying the mask.
	 */
	@Override
	public int getItemMask()
	{
		return getItemType().mask();
	}

	/**
	 * @return the quantity of SoulShot used.
	 */
	public int getSoulShotCount()
	{
		return _soulShotCount;
	}

	/**
	 * @return the quatity of SpiritShot used.
	 */
	public int getSpiritShotCount()
	{
		return _spiritShotCount;
	}

	/**
	 * @return the random damage inflicted by the weapon
	 */
	public int getRandomDamage()
	{
		return _rndDam;
	}

	/**
	 * @return the Reuse Delay of the L2Weapon.
	 */
	public int getReuseDelay()
	{
		return _reuseDelay;
	}

	/**
	 * @return true or false if weapon is considered as a mage weapon.
	 */
	public final boolean isMagical()
	{
		return _isMagical;
	}

	/**
	 * @return the MP consumption of the weapon.
	 */
	public int getMpConsume()
	{
		return _mpConsume;
	}

	/**
	 * @return The skill player obtains when he equiped weapon +4 or more (for duals SA)
	 */
	public L2Skill getEnchant4Skill()
	{
		if (_enchant4Skill == null)
			return null;

		return _enchant4Skill.getSkill();
	}

	/**
	 * @param instance
	 *            : L2ItemInstance pointing out the weapon
	 * @param player
	 *            : L2Character pointing out the player
	 * @return An array of Func objects containing the list of functions used by the weapon.
	 */
	@Override
	public Func[] getStatFuncs(L2ItemInstance instance, L2Character player)
	{
		if (_funcTemplates == null || _funcTemplates.length == 0)
			return _emptyFunctionSet;

		ArrayList<Func> funcs = new ArrayList<>(_funcTemplates.length);

		Env env = new Env();
		env.player = player;
		env.item = instance;
		Func f;

		for (FuncTemplate t : _funcTemplates)
		{
			f = t.getFunc(env, instance);
			if (f != null)
				funcs.add(f);
		}

		return funcs.toArray(new Func[funcs.size()]);
	}

	/**
	 * @param caster
	 *            : L2Character pointing out the caster
	 * @param target
	 *            : L2Character pointing out the target
	 * @param crit
	 *            : boolean tells whether the hit was critical
	 * @return An array of L2Effect of skills associated with the item to be triggered onHit.
	 */
	public L2Effect[] getSkillEffects(L2Character caster, L2Character target, boolean crit)
	{
		if (_skillsOnCrit == null || !crit)
			return _emptyEffectSet;
		List<L2Effect> effects = new FastList<>();

		if (_skillsOnCritCondition != null)
		{
			Env env = new Env();
			env.player = caster;
			env.target = target;
			env.skill = _skillsOnCrit.getSkill();
			if (!_skillsOnCritCondition.test(env))
				return _emptyEffectSet; // Skill condition not met
		}

		byte shld = Formulas.calcShldUse(caster, target, _skillsOnCrit.getSkill());
		if (!Formulas.calcSkillSuccess(caster, target, _skillsOnCrit.getSkill(), shld, false, false, false))
			return _emptyEffectSet; // These skills should not work on RaidBoss

		if (target.getFirstEffect(_skillsOnCrit.getSkill().getId()) != null)
			target.getFirstEffect(_skillsOnCrit.getSkill().getId()).exit();

		for (L2Effect e : _skillsOnCrit.getSkill().getEffects(caster, target, new Env(shld, false, false, false)))
			effects.add(e);

		if (effects.isEmpty())
			return _emptyEffectSet;

		return effects.toArray(new L2Effect[effects.size()]);
	}

	/**
	 * @param caster
	 *            : L2Character pointing out the caster
	 * @param target
	 *            : L2Character pointing out the target
	 * @param trigger
	 *            : L2Skill pointing out the skill triggering this action
	 * @return An array of L2Effect associated with the item to be triggered onCast.
	 */
	public L2Effect[] getSkillEffects(L2Character caster, L2Character target, L2Skill trigger)
	{
		if (_skillsOnCast == null)
			return _emptyEffectSet;

		if (trigger.isOffensive() != _skillsOnCast.getSkill().isOffensive())
			return _emptyEffectSet; // Trigger only same type of skill

		if (trigger.isToggle() && _skillsOnCast.getSkill().getSkillType() == L2SkillType.BUFF)
			return _emptyEffectSet; // No buffing with toggle skills

		if (!trigger.isMagic() && _skillsOnCast.getSkill().getSkillType() == L2SkillType.BUFF)
			return _emptyEffectSet; // No buffing with not magic skills

		if (_skillsOnCastCondition != null)
		{
			Env env = new Env();
			env.player = caster;
			env.target = target;
			env.skill = _skillsOnCast.getSkill();
			if (!_skillsOnCastCondition.test(env))
				return _emptyEffectSet;
		}

		byte shld = Formulas.calcShldUse(caster, target, _skillsOnCast.getSkill());
		if (_skillsOnCast.getSkill().isOffensive() && !Formulas.calcSkillSuccess(caster, target, _skillsOnCast.getSkill(), shld, false, false, false))
			return _emptyEffectSet;

		// Get the skill handler corresponding to the skill type
		ISkillHandler handler = SkillHandler.getInstance().getSkillHandler(_skillsOnCast.getSkill().getSkillType());

		L2Character[] targets = new L2Character[1];
		targets[0] = target;

		// Launch the magic skill and calculate its effects
		if (handler != null)
			handler.useSkill(caster, _skillsOnCast.getSkill(), targets);
		else
			_skillsOnCast.getSkill().useSkill(caster, targets);

		// notify quests of a skill use
		if (caster instanceof L2PcInstance)
		{
			// Mobs in range 1000 see spell
			Collection<L2Object> objs = caster.getKnownList().getKnownObjects().values();
			for (L2Object spMob : objs)
			{
				if (spMob instanceof L2Npc)
				{
					L2Npc npcMob = (L2Npc) spMob;

					if (npcMob.getTemplate().getEventQuests(Quest.QuestEventType.ON_SKILL_SEE) != null)
						for (Quest quest : npcMob.getTemplate().getEventQuests(Quest.QuestEventType.ON_SKILL_SEE))
							quest.notifySkillSee(npcMob, (L2PcInstance) caster, _skillsOnCast.getSkill(), targets, false);
				}
			}
		}
		return _emptyEffectSet;
	}
}