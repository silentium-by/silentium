/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.skills.effects;

import silentium.gameserver.model.L2Effect;
import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.instance.L2SiegeSummonInstance;
import silentium.gameserver.network.serverpackets.StartRotation;
import silentium.gameserver.network.serverpackets.StopRotation;
import silentium.gameserver.skills.Env;
import silentium.gameserver.templates.skills.L2EffectType;

/**
 * @author decad Implementation of the Bluff Effect
 */
public class EffectBluff extends L2Effect
{
	public EffectBluff(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public L2EffectType getEffectType()
	{
		return L2EffectType.BLUFF; // test for bluff effect
	}

	/** Notify started */
	@Override
	public boolean onStart()
	{
		if (getEffected() instanceof L2Npc)
			return false;

		if (getEffected() instanceof L2Npc && ((L2Npc) getEffected()).getNpcId() == 35062)
			return false;

		if (getEffected() instanceof L2SiegeSummonInstance)
			return false;

		getEffected().broadcastPacket(new StartRotation(getEffected().getObjectId(), getEffected().getHeading(), 1, 65535));
		getEffected().broadcastPacket(new StopRotation(getEffected().getObjectId(), getEffector().getHeading(), 65535));
		getEffected().setHeading(getEffector().getHeading());
		return true;
	}

	@Override
	public boolean onActionTime()
	{
		return false;
	}
}