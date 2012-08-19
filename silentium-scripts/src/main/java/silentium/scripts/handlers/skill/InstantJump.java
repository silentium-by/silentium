/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.handlers.skill;

import silentium.gameserver.ai.CtrlIntention;
import silentium.gameserver.handler.ISkillHandler;
import silentium.gameserver.model.L2Object;
import silentium.gameserver.model.L2Skill;
import silentium.gameserver.model.actor.L2Character;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.network.SystemMessageId;
import silentium.gameserver.network.serverpackets.FlyToLocation;
import silentium.gameserver.network.serverpackets.FlyToLocation.FlyType;
import silentium.gameserver.network.serverpackets.SystemMessage;
import silentium.gameserver.network.serverpackets.ValidateLocation;
import silentium.gameserver.skills.Formulas;
import silentium.gameserver.templates.skills.L2SkillType;
import silentium.gameserver.utils.Util;

/**
 * @author Didldak Some parts taken from EffectWarp, which cannot be used for this case.
 */
public class InstantJump implements ISkillHandler {
	private static final L2SkillType[] SKILL_IDS = { L2SkillType.INSTANT_JUMP };

	@Override
	public void useSkill(final L2Character activeChar, final L2Skill skill, final L2Object... targets) {
		final L2Character target = (L2Character) targets[0];

		if (Formulas.calcPhysicalSkillEvasion(target, skill)) {
			if (activeChar instanceof L2PcInstance)
				activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_DODGES_ATTACK).addCharName(target));

			return;
		}

		int x = 0, y = 0, z = 0;

		final int px = target.getX();
		final int py = target.getY();
		double ph = Util.convertHeadingToDegree(target.getHeading());

		ph += 180;

		if (ph > 360)
			ph -= 360;

		ph = Math.PI * ph / 180;

		x = (int) (px + 25 * Math.cos(ph));
		y = (int) (py + 25 * Math.sin(ph));
		z = target.getZ();

		activeChar.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		activeChar.broadcastPacket(new FlyToLocation(activeChar, x, y, z, FlyType.DUMMY));
		activeChar.abortAttack();
		activeChar.abortCast();

		activeChar.setXYZ(x, y, z);
		activeChar.broadcastPacket(new ValidateLocation(activeChar));
	}

	/**
	 * @see silentium.gameserver.handler.ISkillHandler#getSkillIds()
	 */
	@Override
	public L2SkillType[] getSkillIds() {
		return SKILL_IDS;
	}
}