/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.handlers.skill;

import silentium.gameserver.handler.ISkillHandler;
import silentium.gameserver.model.L2Object;
import silentium.gameserver.model.L2Skill;
import silentium.gameserver.model.actor.L2Character;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.network.SystemMessageId;
import silentium.gameserver.network.serverpackets.StatusUpdate;
import silentium.gameserver.network.serverpackets.SystemMessage;
import silentium.gameserver.skills.Stats;
import silentium.gameserver.templates.skills.L2SkillType;

public class ManaHeal implements ISkillHandler {
	private static final L2SkillType[] SKILL_IDS = { L2SkillType.MANAHEAL, L2SkillType.MANARECHARGE };

	@Override
	public void useSkill(final L2Character actChar, final L2Skill skill, final L2Object... targets) {
		for (final L2Character target : (L2Character[]) targets) {
			if (target.isInvul())
				continue;

			double mp = skill.getPower();

			mp = skill.getSkillType() == L2SkillType.MANAHEAL_PERCENT ? target.getMaxMp() * mp / 100.0 : skill.getSkillType() == L2SkillType.MANARECHARGE ? target.calcStat(Stats.RECHARGE_MP_RATE, mp, null, null) : mp;

			// It's not to be the IL retail way, but it make the message more logical
			if (target.getCurrentMp() + mp >= target.getMaxMp())
				mp = target.getMaxMp() - target.getCurrentMp();

			target.setCurrentMp(mp + target.getCurrentMp());
			final StatusUpdate sump = new StatusUpdate(target);
			sump.addAttribute(StatusUpdate.CUR_MP, (int) target.getCurrentMp());
			target.sendPacket(sump);

			if (actChar instanceof L2PcInstance && actChar != target)
				target.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S2_MP_RESTORED_BY_S1).addCharName(actChar).addNumber((int) mp));
			else
				target.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_MP_RESTORED).addNumber((int) mp));
		}
	}

	@Override
	public L2SkillType[] getSkillIds() {
		return SKILL_IDS;
	}
}