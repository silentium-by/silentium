/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.handlers.skill;

import silentium.gameserver.handler.ISkillHandler;
import silentium.gameserver.handler.SkillHandler;
import silentium.gameserver.model.L2ItemInstance;
import silentium.gameserver.model.L2Object;
import silentium.gameserver.model.L2Skill;
import silentium.gameserver.model.actor.L2Character;
import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.L2Summon;
import silentium.gameserver.model.actor.instance.L2DoorInstance;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.actor.instance.L2SiegeFlagInstance;
import silentium.gameserver.network.SystemMessageId;
import silentium.gameserver.network.serverpackets.StatusUpdate;
import silentium.gameserver.network.serverpackets.SystemMessage;
import silentium.gameserver.skills.Stats;
import silentium.gameserver.templates.skills.L2SkillType;

public class Heal implements ISkillHandler
{
	private static final L2SkillType[] SKILL_IDS = { L2SkillType.HEAL, L2SkillType.HEAL_STATIC };

	@Override
	public void useSkill(L2Character activeChar, L2Skill skill, L2Object[] targets)
	{
		// check for other effects
		ISkillHandler handler = SkillHandler.getInstance().getSkillHandler(L2SkillType.BUFF);

		if (handler != null)
			handler.useSkill(activeChar, skill, targets);

		double power = skill.getPower() + activeChar.calcStat(Stats.HEAL_PROFICIENCY, 0, null, null);

		switch (skill.getSkillType())
		{
			case HEAL_STATIC:
				break;
			default:
				final L2ItemInstance weaponInst = activeChar.getActiveWeaponInstance();
				double staticShotBonus = 0;
				int mAtkMul = 1; // mAtk multiplier
				if (weaponInst != null && weaponInst.getChargedSpiritshot() != L2ItemInstance.CHARGED_NONE)
				{
					if (activeChar instanceof L2PcInstance && ((L2PcInstance) activeChar).isMageClass())
					{
						staticShotBonus = skill.getMpConsume(); // static bonus for spiritshots

						if (weaponInst.getChargedSpiritshot() == L2ItemInstance.CHARGED_BLESSED_SPIRITSHOT)
						{
							mAtkMul = 4;
							staticShotBonus *= 2.4; // static bonus for blessed spiritshots
						}
						else
							mAtkMul = 2;
					}
					else
					{
						// shot dynamic bonus
						if (weaponInst.getChargedSpiritshot() == L2ItemInstance.CHARGED_BLESSED_SPIRITSHOT)
							mAtkMul *= 4; // 16x/8x/4x s84/s80/other
						else
							mAtkMul += 1; // 5x/3x/1x s84/s80/other
					}

					weaponInst.setChargedSpiritshot(L2ItemInstance.CHARGED_NONE);
				}
				// If there is no weapon equipped, check for an active summon.
				else if (activeChar instanceof L2Summon && ((L2Summon) activeChar).getChargedSpiritShot() != L2ItemInstance.CHARGED_NONE)
				{
					staticShotBonus = skill.getMpConsume(); // static bonus for spiritshots

					if (((L2Summon) activeChar).getChargedSpiritShot() == L2ItemInstance.CHARGED_BLESSED_SPIRITSHOT)
					{
						staticShotBonus *= 2.4; // static bonus for blessed spiritshots
						mAtkMul = 4;
					}
					else
						mAtkMul = 2;

					((L2Summon) activeChar).setChargedSpiritShot(L2ItemInstance.CHARGED_NONE);
				}
				else if (activeChar instanceof L2Npc && ((L2Npc) activeChar)._spiritshotcharged)
				{
					staticShotBonus = 2.4 * skill.getMpConsume(); // always blessed spiritshots
					mAtkMul = 4;

					((L2Npc) activeChar)._spiritshotcharged = false;
				}

				power += staticShotBonus + Math.sqrt(mAtkMul * activeChar.getMAtk(activeChar, null));
		}

		double hp;
		for (L2Character target : (L2Character[]) targets)
		{
			// We should not heal if char is dead/invul
			if (target == null || target.isDead() || target.isInvul())
				continue;

			if (target instanceof L2DoorInstance || target instanceof L2SiegeFlagInstance)
				continue;

			// Player holding a cursed weapon can't be healed and can't heal
			if (target != activeChar)
			{
				if (target instanceof L2PcInstance && ((L2PcInstance) target).isCursedWeaponEquipped())
					continue;
				else if (activeChar instanceof L2PcInstance && ((L2PcInstance) activeChar).isCursedWeaponEquipped())
					continue;
			}

			switch (skill.getSkillType())
			{
				case HEAL_PERCENT:
					hp = target.getMaxHp() * power / 100.0;
					break;
				default:
					hp = power;
					hp *= target.calcStat(Stats.HEAL_EFFECTIVNESS, 100, null, null) / 100;
			}

			// If you have full HP and you get HP buff, u will receive 0HP restored message
			if ((target.getCurrentHp() + hp) >= target.getMaxHp())
				hp = target.getMaxHp() - target.getCurrentHp();

			if (hp < 0)
				hp = 0;

			target.setCurrentHp(hp + target.getCurrentHp());
			StatusUpdate su = new StatusUpdate(target);
			su.addAttribute(StatusUpdate.CUR_HP, (int) target.getCurrentHp());
			target.sendPacket(su);

			if (target instanceof L2PcInstance)
			{
				if (skill.getId() == 4051)
					target.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.REJUVENATING_HP));
				else
				{
					if (activeChar instanceof L2PcInstance && activeChar != target)
						target.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S2_HP_RESTORED_BY_S1).addCharName(activeChar).addNumber((int) hp));
					else
						target.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_HP_RESTORED).addNumber((int) hp));
				}
			}
		}
	}

	@Override
	public L2SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}