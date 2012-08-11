/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.handlers.skill;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import silentium.commons.utils.Rnd;
import silentium.gameserver.ai.AttackableAI;
import silentium.gameserver.ai.CtrlEvent;
import silentium.gameserver.ai.CtrlIntention;
import silentium.gameserver.handler.ISkillHandler;
import silentium.gameserver.handler.SkillHandler;
import silentium.gameserver.model.L2Effect;
import silentium.gameserver.model.L2ItemInstance;
import silentium.gameserver.model.L2Object;
import silentium.gameserver.model.L2Skill;
import silentium.gameserver.model.actor.L2Attackable;
import silentium.gameserver.model.actor.L2Character;
import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.L2Summon;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.actor.instance.L2SiegeSummonInstance;
import silentium.gameserver.model.base.Experience;
import silentium.gameserver.network.SystemMessageId;
import silentium.gameserver.network.serverpackets.SystemMessage;
import silentium.gameserver.skills.Env;
import silentium.gameserver.skills.Formulas;
import silentium.gameserver.skills.Stats;
import silentium.gameserver.templates.skills.L2SkillType;

/**
 * This Handles Disabler skills
 *
 * @author _drunk_
 */
public class Disablers implements ISkillHandler
{
	private static final L2SkillType[] SKILL_IDS = { L2SkillType.STUN, L2SkillType.ROOT, L2SkillType.SLEEP, L2SkillType.CONFUSION, L2SkillType.AGGDAMAGE, L2SkillType.AGGREDUCE, L2SkillType.AGGREDUCE_CHAR, L2SkillType.AGGREMOVE, L2SkillType.MUTE, L2SkillType.FAKE_DEATH, L2SkillType.NEGATE, L2SkillType.CANCEL_DEBUFF, L2SkillType.PARALYZE, L2SkillType.ERASE, L2SkillType.BETRAY };

	protected static final Logger _log = LoggerFactory.getLogger(Disablers.class.getName());

	@Override
	public void useSkill(L2Character activeChar, L2Skill skill, L2Object[] targets)
	{
		L2SkillType type = skill.getSkillType();

		byte shld = 0;
		boolean ss = false;
		boolean sps = false;
		boolean bss = false;

		L2ItemInstance weaponInst = activeChar.getActiveWeaponInstance();
		if (weaponInst != null)
		{
			if (skill.isMagic())
			{
				if (weaponInst.getChargedSpiritshot() == L2ItemInstance.CHARGED_BLESSED_SPIRITSHOT)
				{
					bss = true;
					if (skill.getId() != 1020) // vitalize
						weaponInst.setChargedSpiritshot(L2ItemInstance.CHARGED_NONE);
				}
				else if (weaponInst.getChargedSpiritshot() == L2ItemInstance.CHARGED_SPIRITSHOT)
				{
					sps = true;
					if (skill.getId() != 1020) // vitalize
						weaponInst.setChargedSpiritshot(L2ItemInstance.CHARGED_NONE);
				}
			}
			else
				ss = true;
		}
		// If there is no weapon equipped, check for an active summon.
		else if (activeChar instanceof L2Summon)
		{
			L2Summon activeSummon = (L2Summon) activeChar;

			if (skill.isMagic())
			{
				if (activeSummon.getChargedSpiritShot() == L2ItemInstance.CHARGED_BLESSED_SPIRITSHOT)
				{
					bss = true;
					activeSummon.setChargedSpiritShot(L2ItemInstance.CHARGED_NONE);
				}
				else if (activeSummon.getChargedSpiritShot() == L2ItemInstance.CHARGED_SPIRITSHOT)
				{
					sps = true;
					activeSummon.setChargedSpiritShot(L2ItemInstance.CHARGED_NONE);
				}
			}
			else
				ss = true;
		}
		else if (activeChar instanceof L2Npc)
		{
			ss = ((L2Npc) activeChar)._soulshotcharged;
			((L2Npc) activeChar)._soulshotcharged = false;
			bss = ((L2Npc) activeChar)._spiritshotcharged;
			((L2Npc) activeChar)._spiritshotcharged = false;
		}

		for (L2Object obj : targets)
		{
			if (!(obj instanceof L2Character))
				continue;
			L2Character target = (L2Character) obj;
			if (target.isDead() || (target.isInvul() && !target.isParalyzed())) // bypass if target is null, dead or invul
																				// (excluding invul from Petrification)
				continue;

			shld = Formulas.calcShldUse(activeChar, target, skill);

			switch (type)
			{
				case BETRAY:
				{
					if (Formulas.calcSkillSuccess(activeChar, target, skill, shld, ss, sps, bss))
						skill.getEffects(activeChar, target, new Env(shld, ss, sps, bss));
					else
						activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_RESISTED_YOUR_S2).addCharName(target).addSkillName(skill));
					break;
				}
				case FAKE_DEATH:
				{
					// stun/fakedeath is not mdef dependant, it depends on lvl difference, target CON and power of stun
					skill.getEffects(activeChar, target, new Env(shld, ss, sps, bss));
					break;
				}
				case ROOT:
				case STUN:
				{
					if (Formulas.calcSkillReflect(target, skill) == Formulas.SKILL_REFLECT_SUCCEED)
						target = activeChar;

					if (Formulas.calcSkillSuccess(activeChar, target, skill, shld, ss, sps, bss))
						skill.getEffects(activeChar, target, new Env(shld, ss, sps, bss));
					else
					{
						if (activeChar instanceof L2PcInstance)
							activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_RESISTED_YOUR_S2).addCharName(target).addSkillName(skill.getDisplayId()));
					}
					break;
				}
				case SLEEP:
				case PARALYZE: // use same as root for now
				{
					if (Formulas.calcSkillReflect(target, skill) == Formulas.SKILL_REFLECT_SUCCEED)
						target = activeChar;

					if (Formulas.calcSkillSuccess(activeChar, target, skill, shld, ss, sps, bss))
						skill.getEffects(activeChar, target, new Env(shld, ss, sps, bss));
					else
					{
						if (activeChar instanceof L2PcInstance)
							activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_RESISTED_YOUR_S2).addCharName(target).addSkillName(skill.getDisplayId()));
					}
					break;
				}
				case MUTE:
				{
					if (Formulas.calcSkillReflect(target, skill) == Formulas.SKILL_REFLECT_SUCCEED)
						target = activeChar;

					if (Formulas.calcSkillSuccess(activeChar, target, skill, shld, ss, sps, bss))
					{
						// stop same type effect if available
						L2Effect[] effects = target.getAllEffects();
						for (L2Effect e : effects)
						{
							if (e.getSkill().getSkillType() == type)
								e.exit();
						}
						skill.getEffects(activeChar, target, new Env(shld, ss, sps, bss));
					}
					else
					{
						if (activeChar instanceof L2PcInstance)
							activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_RESISTED_YOUR_S2).addCharName(target).addSkillName(skill.getDisplayId()));
					}
					break;
				}
				case CONFUSION:
				{
					// do nothing if not on mob
					if (target instanceof L2Attackable)
					{
						if (Formulas.calcSkillSuccess(activeChar, target, skill, shld, ss, sps, bss))
						{
							L2Effect[] effects = target.getAllEffects();
							for (L2Effect e : effects)
							{
								if (e.getSkill().getSkillType() == type)
									e.exit();
							}
							skill.getEffects(activeChar, target, new Env(shld, ss, sps, bss));
						}
						else
						{
							if (activeChar instanceof L2PcInstance)
								activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_RESISTED_YOUR_S2).addCharName(target).addSkillName(skill));
						}
					}
					else
						activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
					break;
				}
				case AGGDAMAGE:
				{
					if (target instanceof L2Attackable)
						target.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, activeChar, (int) ((150 * skill.getPower()) / (target.getLevel() + 7)));

					skill.getEffects(activeChar, target, new Env(shld, ss, sps, bss));
					break;
				}
				case AGGREDUCE:
				{
					// these skills needs to be rechecked
					if (target instanceof L2Attackable)
					{
						skill.getEffects(activeChar, target, new Env(shld, ss, sps, bss));

						double aggdiff = ((L2Attackable) target).getHating(activeChar) - target.calcStat(Stats.AGGRESSION, ((L2Attackable) target).getHating(activeChar), target, skill);

						if (skill.getPower() > 0)
							((L2Attackable) target).reduceHate(null, (int) skill.getPower());
						else if (aggdiff > 0)
							((L2Attackable) target).reduceHate(null, (int) aggdiff);
					}
					break;
				}
				case AGGREDUCE_CHAR:
				{
					// these skills needs to be rechecked
					if (Formulas.calcSkillSuccess(activeChar, target, skill, shld, ss, sps, bss))
					{
						if (target instanceof L2Attackable)
						{
							L2Attackable targ = (L2Attackable) target;
							targ.stopHating(activeChar);
							if (targ.getMostHated() == null && targ.hasAI() && targ.getAI() instanceof AttackableAI)
							{
								((AttackableAI) targ.getAI()).setGlobalAggro(-25);
								targ.clearAggroList();
								targ.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
								targ.setWalking();
							}
						}
						skill.getEffects(activeChar, target, new Env(shld, ss, sps, bss));
					}
					else
					{
						if (activeChar instanceof L2PcInstance)
							activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_RESISTED_YOUR_S2).addCharName(target).addSkillName(skill));

						target.getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, activeChar);
					}
					break;
				}
				case AGGREMOVE:
				{
					// these skills needs to be rechecked
					if (target instanceof L2Attackable && !target.isRaid())
					{
						if (Formulas.calcSkillSuccess(activeChar, target, skill, shld, ss, sps, bss))
						{
							if (skill.getTargetType() == L2Skill.SkillTargetType.TARGET_UNDEAD)
							{
								if (target.isUndead())
									((L2Attackable) target).reduceHate(null, ((L2Attackable) target).getHating(((L2Attackable) target).getMostHated()));
							}
							else
								((L2Attackable) target).reduceHate(null, ((L2Attackable) target).getHating(((L2Attackable) target).getMostHated()));
						}
						else
						{
							if (activeChar instanceof L2PcInstance)
								activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_RESISTED_YOUR_S2).addCharName(target).addSkillName(skill));

							target.getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, activeChar);
						}
					}
					else
						target.getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, activeChar);
					break;
				}
				case ERASE:
				{
					if (Formulas.calcSkillSuccess(activeChar, target, skill, shld, ss, sps, bss)
					// doesn't affect siege golem or wild hog cannon
							&& !(target instanceof L2SiegeSummonInstance))
					{
						L2PcInstance summonOwner = null;
						L2Summon summonPet = null;
						summonOwner = ((L2Summon) target).getOwner();
						summonPet = summonOwner.getPet();
						if (summonPet != null)
						{
							summonPet.unSummon(summonOwner);
							summonOwner.sendPacket(SystemMessageId.YOUR_SERVITOR_HAS_VANISHED);
						}
					}
					else
					{
						if (activeChar instanceof L2PcInstance)
							activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_RESISTED_YOUR_S2).addCharName(target).addSkillName(skill));
					}
					break;
				}
				case CANCEL_DEBUFF:
				{
					L2Effect[] effects = target.getAllEffects();

					if (effects == null || effects.length == 0)
						break;

					int count = (skill.getMaxNegatedEffects() > 0) ? 0 : -2;
					for (L2Effect e : effects)
					{
						if (e == null || !e.getSkill().isDebuff() || !e.getSkill().canBeDispeled())
							continue;

						e.exit();

						if (count > -1)
						{
							count++;
							if (count >= skill.getMaxNegatedEffects())
								break;
						}
					}

					break;
				}
				case NEGATE:
				{
					if (Formulas.calcSkillReflect(target, skill) == Formulas.SKILL_REFLECT_SUCCEED)
						target = activeChar;

					if (skill.getNegateId().length != 0)
					{
						for (int i = 0; i < skill.getNegateId().length; i++)
						{
							if (skill.getNegateId()[i] != 0)
								target.stopSkillEffects(skill.getNegateId()[i]);
						}
					}
					// all others negate type skills
					else
					{
						int removedBuffs = (skill.getMaxNegatedEffects() > 0) ? 0 : -2;

						for (L2SkillType skillType : skill.getNegateStats())
						{
							if (removedBuffs > skill.getMaxNegatedEffects())
								break;

							switch (skillType)
							{
								case BUFF:
									int lvlmodifier = 52 + skill.getMagicLevel() * 2;
									if (skill.getMagicLevel() == 12)
										lvlmodifier = (Experience.MAX_LEVEL - 1);
									int landrate = 90;
									if ((target.getLevel() - lvlmodifier) > 0)
										landrate = 90 - 4 * (target.getLevel() - lvlmodifier);

									landrate = (int) activeChar.calcStat(Stats.CANCEL_VULN, landrate, target, null);

									if (Rnd.get(100) < landrate)
										removedBuffs += negateEffect(target, L2SkillType.BUFF, -1, skill.getMaxNegatedEffects());
									break;
								case HEAL:
									ISkillHandler Healhandler = SkillHandler.getInstance().getSkillHandler(L2SkillType.HEAL);
									if (Healhandler == null)
									{
										_log.error("Couldn't find skill handler for HEAL.");
										continue;
									}
									L2Character tgts[] = new L2Character[] { target };
									Healhandler.useSkill(activeChar, skill, tgts);
									break;
								default:
									removedBuffs += negateEffect(target, skillType, skill.getNegateLvl(), skill.getMaxNegatedEffects());
									break;
							}
						}
					}
					if (Formulas.calcSkillSuccess(activeChar, target, skill, shld, ss, sps, bss))
						skill.getEffects(activeChar, target, new Env(shld, ss, sps, bss));
				}
			}
		}

		// self Effect :]
		if (skill.hasSelfEffects())
		{
			final L2Effect effect = activeChar.getFirstEffect(skill.getId());
			if (effect != null && effect.isSelfEffect())
			{
				// Replace old effect with new one.
				effect.exit();
			}
			skill.getEffectsSelf(activeChar);
		}
	}

	private static int negateEffect(L2Character target, L2SkillType type, int negateLvl, int maxRemoved)
	{
		return negateEffect(target, type, negateLvl, 0, maxRemoved);
	}

	private static int negateEffect(L2Character target, L2SkillType type, int negateLvl, int skillId, int maxRemoved)
	{
		L2Effect[] effects = target.getAllEffects();
		int count = (maxRemoved <= 0) ? -2 : 0;
		for (L2Effect e : effects)
		{
			if (negateLvl == -1) // if power is -1 the effect is always removed without power/lvl check ^^
			{
				if (e.getSkill().getSkillType() == type || (e.getSkill().getEffectType() != null && e.getSkill().getEffectType() == type))
				{
					if (skillId != 0)
					{
						if (skillId == e.getSkill().getId() && count < maxRemoved)
						{
							e.exit();
							if (count > -1)
								count++;
						}
					}
					else if (count < maxRemoved)
					{
						e.exit();
						if (count > -1)
							count++;
					}
				}
			}
			else
			{
				boolean cancel = false;
				if (e.getSkill().getEffectType() != null && e.getSkill().getEffectAbnormalLvl() >= 0)
				{
					if (e.getSkill().getEffectType() == type && e.getSkill().getEffectAbnormalLvl() <= negateLvl)
						cancel = true;
				}
				else if (e.getSkill().getSkillType() == type && e.getSkill().getAbnormalLvl() <= negateLvl)
					cancel = true;

				if (cancel)
				{
					if (skillId != 0)
					{
						if (skillId == e.getSkill().getId() && count < maxRemoved)
						{
							e.exit();
							if (count > -1)
								count++;
						}
					}
					else if (count < maxRemoved)
					{
						e.exit();
						if (count > -1)
							count++;
					}
				}
			}
		}

		return (maxRemoved <= 0) ? count + 2 : count;
	}

	@Override
	public L2SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}
