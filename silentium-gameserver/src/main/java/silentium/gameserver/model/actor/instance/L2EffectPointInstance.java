/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.model.actor.instance;

import silentium.gameserver.model.actor.L2Character;
import silentium.gameserver.network.serverpackets.ActionFailed;
import silentium.gameserver.templates.chars.L2NpcTemplate;

public class L2EffectPointInstance extends L2NpcInstance
{
	private final L2PcInstance _owner;

	public L2EffectPointInstance(int objectId, L2NpcTemplate template, L2Character owner)
	{
		super(objectId, template);
		_owner = owner == null ? null : owner.getActingPlayer();
	}

	@Override
	public L2PcInstance getActingPlayer()
	{
		return _owner;
	}

	/**
	 * this is called when a player interacts with this NPC
	 * 
	 * @param player
	 */
	@Override
	public void onAction(L2PcInstance player)
	{
		// Send ActionFailed to the player in order to avoid he stucks
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
}