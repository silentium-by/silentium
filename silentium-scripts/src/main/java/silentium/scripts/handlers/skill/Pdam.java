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
import silentium.gameserver.model.L2Effect;
import silentium.gameserver.model.L2ItemInstance;
import silentium.gameserver.model.L2Object;
import silentium.gameserver.model.L2Skill;
import silentium.gameserver.model.actor.L2Character;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.network.SystemMessageId;
import silentium.gameserver.network.serverpackets.SystemMessage;
import silentium.gameserver.skills.Env;
import silentium.gameserver.skills.Formulas;
import silentium.gameserver.templates.item.L2WeaponType;
import silentium.gameserver.templates.skills.L2SkillType;

public class Pdam implements ISkillHandler
{
	private static Logger _log = LoggerFactory.getLogger(Pdam.class.getName());

	private static final L2SkillType[] SKILL_IDS = { L2SkillType.PDAM, L2SkillType.FATAL };

	@Override
	public void useSkill(L2Character activeChar, L2Skill skill, L2Object[] targets)
	{
		if (activeChar.isAlikeDead())
			return;

		int damage = 0;

		_log.debug("Begin Skill processing in Pdam.java " + skill.getSkillType());

		L2ItemInstance weapon = activeChar.getActiveWeaponInstance();
		boolean soul = (weapon != null && weapon.getChargedSoulshot() == L2ItemInstance.CHARGED_SOULSHOT && weapon.getItemType() != L2WeaponType.DAGGER);

		for (L2Character target : (L2Character[]) targets)
		{
			if (activeChar instanceof L2PcInstance && target instanceof L2PcInstance && ((L2PcInstance) target).isFakeDeath())
				target.stopFakeDeath(true);
			else if (target.isDead())
				continue;

			// Calculate skill evasion. As PDAM skillType is used for bow, make an exception with this weapon.
			boolean skillIsEvaded = Formulas.calcPhysicalSkillEvasion(target, skill);
			if (weapon != null && weapon.getItemType() != L2WeaponType.BOW)
			{
				if (skillIsEvaded)
				{
					if (activeChar instanceof L2PcInstance)
						((L2PcInstance) activeChar).sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_DODGES_ATTACK).addCharName(target));

					if (target instanceof L2PcInstance)
						((L2PcInstance) target).sendPacket(SystemMessage.getSystemMessage(SystemMessageId.AVOIDED_S1_ATTACK).addCharName(activeChar));

					// no futher calculations needed.
					continue;
				}
			}

			final boolean dual = activeChar.isUsingDualWeapon();
			final byte shld = Formulas.calcShldUse(activeChar, target);

			// PDAM critical chance not affected by buffs, only by STR. Only some skills are meant to crit.
			boolean crit = false;
			if (skill.getBaseCritRate() > 0)
				crit = Formulas.calcCrit(skill.getBaseCritRate() * 10 * Formulas.getSTRBonus(activeChar));

			if (!crit && (skill.getCondition() & L2Skill.COND_CRIT) != 0)
				damage = 0;
			else
				damage = (int) Formulas.calcPhysDam(activeChar, target, skill, shld, false, dual, soul);

			if (crit)
				damage *= 2; // PDAM Critical damage always 2x and not affected by buffs

			final byte reflect = Formulas.calcSkillReflect(target, skill);

			if (skill.hasEffects())
			{
				L2Effect[] effects;
				if ((reflect & Formulas.SKILL_REFLECT_SUCCEED) != 0)
				{
					activeChar.stopSkillEffects(skill.getId());
					effects = skill.getEffects(target, activeChar);
					if (effects != null && effects.length > 0)
						activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT).addSkillName(skill));
				}
				else
				{
					// activate attacked effects, if any
					target.stopSkillEffects(skill.getId());
					effects = skill.getEffects(activeChar, target, new Env(shld, false, false, false));
					if (effects != null && effects.length > 0)
						target.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT).addSkillName(skill));
				}
			}

			if (damage > 0)
			{
				activeChar.sendDamageMessage(target, damage, false, crit, false);

				// Possibility of a lethal strike
				Formulas.calcLethalHit(activeChar, target, skill);

				target.reduceCurrentHp(damage, activeChar, skill);

				// vengeance reflected damage
				if ((reflect & Formulas.SKILL_REFLECT_VENGEANCE) != 0)
				{
					if (target instanceof L2PcInstance)
						target.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.COUNTERED_S1_ATTACK).addCharName(activeChar));

					if (activeChar instanceof L2PcInstance)
						activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_PERFORMING_COUNTERATTACK).addCharName(target));

					// Formula from Diego post, 700 from rpg tests
					double vegdamage = (700 * target.getPAtk(activeChar) / activeChar.getPDef(target));
					activeChar.reduceCurrentHp(vegdamage, target, skill);
				}
			}
			else
				// No damage
				activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.ATTACK_FAILED));
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

		if (soul && weapon != null)
			weapon.setChargedSoulshot(L2ItemInstance.CHARGED_NONE);
	}

	@Override
	public L2SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}
