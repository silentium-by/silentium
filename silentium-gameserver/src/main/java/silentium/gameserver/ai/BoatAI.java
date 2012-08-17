/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.ai;

import silentium.gameserver.model.L2CharPosition;
import silentium.gameserver.model.L2Object;
import silentium.gameserver.model.L2Skill;
import silentium.gameserver.model.actor.L2Character;
import silentium.gameserver.model.actor.instance.L2BoatInstance;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.network.serverpackets.VehicleDeparture;
import silentium.gameserver.network.serverpackets.VehicleInfo;
import silentium.gameserver.network.serverpackets.VehicleStarted;

/**
 * @author DS
 */
public class BoatAI extends CharacterAI
{
	public BoatAI(L2BoatInstance.AIAccessor accessor)
	{
		super(accessor);
	}

	@Override
	protected void moveTo(int x, int y, int z)
	{
		if (!_actor.isMovementDisabled())
		{
			if (!_clientMoving)
				_actor.broadcastPacket(new VehicleStarted(getActor(), 1));

			_clientMoving = true;
			_accessor.moveTo(x, y, z);
			_actor.broadcastPacket(new VehicleDeparture(getActor()));
		}
	}

	@Override
	protected void clientStopMoving(L2CharPosition pos)
	{
		if (_actor.isMoving())
			_accessor.stopMove(pos);

		if (_clientMoving || pos != null)
		{
			_clientMoving = false;
			_actor.broadcastPacket(new VehicleStarted(getActor(), 0));
			_actor.broadcastPacket(new VehicleInfo(getActor()));
		}
	}

	@Override
	public void describeStateToPlayer(L2PcInstance player)
	{
		if (_clientMoving)
			player.sendPacket(new VehicleDeparture(getActor()));
	}

	@Override
	public L2BoatInstance getActor()
	{
		return (L2BoatInstance) _actor;
	}

	@Override
	protected void onIntentionAttack(L2Character target)
	{
	}

	@Override
	protected void onIntentionCast(L2Skill skill, L2Object target)
	{
	}

	@Override
	protected void onIntentionFollow(L2Character target)
	{
	}

	@Override
	protected void onIntentionPickUp(L2Object item)
	{
	}

	@Override
	protected void onIntentionInteract(L2Object object)
	{
	}

	@Override
	protected void onEvtAttacked(L2Character attacker)
	{
	}

	@Override
	protected void onEvtAggression(L2Character target, int aggro)
	{
	}

	@Override
	protected void onEvtStunned(L2Character attacker)
	{
	}

	@Override
	protected void onEvtSleeping(L2Character attacker)
	{
	}

	@Override
	protected void onEvtRooted(L2Character attacker)
	{
	}

	@Override
	protected void onEvtForgetObject(L2Object object)
	{
	}

	@Override
	protected void onEvtCancel()
	{
	}

	@Override
	protected void onEvtDead()
	{
	}

	@Override
	protected void onEvtFakeDeath()
	{
	}

	@Override
	protected void onEvtFinishCasting()
	{
	}

	@Override
	protected void clientActionFailed()
	{
	}

	@Override
	protected void moveToPawn(L2Object pawn, int offset)
	{
	}

	@Override
	protected void clientStoppedMoving()
	{
	}
}