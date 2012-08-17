/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.network.clientpackets;

import silentium.gameserver.instancemanager.CastleManager;
import silentium.gameserver.model.L2Object;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.actor.instance.L2StaticObjectInstance;
import silentium.gameserver.network.serverpackets.ChairSit;

public final class RequestChangeWaitType extends L2GameClientPacket
{
	private boolean _typeStand;

	@Override
	protected void readImpl()
	{
		_typeStand = (readD() == 1);
	}

	@Override
	protected void runImpl()
	{
		final L2PcInstance player = getClient().getActiveChar();
		if (player == null)
			return;

		L2Object target = player.getTarget();

		if (player.getMountType() != 0) // prevent sit/stand if you riding
			return;

		// Only available for /stand command. /sit is bypassed.
		if (player.isFakeDeath() && _typeStand)
		{
			player.stopFakeDeath(true);
			return;
		}

		if (target != null && !player.isSitting() && target instanceof L2StaticObjectInstance && ((L2StaticObjectInstance) target).getType() == 1 && CastleManager.getInstance().getCastle(target) != null && player.isInsideRadius(target, L2StaticObjectInstance.INTERACTION_DISTANCE, false, false))
		{
			ChairSit cs = new ChairSit(player, ((L2StaticObjectInstance) target).getStaticObjectId());
			player.sendPacket(cs);
			player.sitDown();
			player.broadcastPacket(cs);
		}

		if (_typeStand)
			player.standUp();
		else
			player.sitDown();
	}
}