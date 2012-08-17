/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.skills.effects;

import silentium.gameserver.ai.CtrlIntention;
import silentium.gameserver.model.L2Effect;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.network.serverpackets.MyTargetSelected;
import silentium.gameserver.skills.Env;
import silentium.gameserver.templates.skills.L2EffectType;

/**
 * @author -Nemesiss-
 */
public class EffectTargetMe extends L2Effect
{
	public EffectTargetMe(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public L2EffectType getEffectType()
	{
		return L2EffectType.TARGET_ME;
	}

	/** Notify started */
	@Override
	public boolean onStart()
	{
		// work only on players, cause mobs got their own aggro system (AGGDAMAGE, AGGREMOVE, etc)
		if (getEffected() instanceof L2PcInstance)
		{
			// add an INTENTION_ATTACK, but only if victim got attacker as target
			if ((getEffected().getAI() == null || getEffected().getAI().getNextIntention() == null) && getEffected().getTarget() == getEffector())
				getEffected().getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, getEffector());

			// target the agressor
			getEffected().setTarget(getEffector());
			getEffected().sendPacket(new MyTargetSelected(getEffector().getObjectId(), 0));

			return true;
		}
		return false;
	}

	@Override
	public void onExit()
	{
	}

	@Override
	public boolean onActionTime()
	{
		// nothing
		return false;
	}
}