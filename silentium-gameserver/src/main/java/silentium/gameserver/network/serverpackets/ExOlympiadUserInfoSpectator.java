/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.network.serverpackets;

import silentium.gameserver.model.actor.instance.L2PcInstance;

/**
 * @author godson
 */
public class ExOlympiadUserInfoSpectator extends L2GameServerPacket
{
	private static int _side;
	private static L2PcInstance _player;

	/**
	 * @param player
	 * @param side
	 *            (1 = right, 2 = left)
	 */
	public ExOlympiadUserInfoSpectator(L2PcInstance player, int side)
	{
		_player = player;
		_side = side;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xfe);
		writeH(0x29);
		writeC(_side);
		writeD(_player.getObjectId());
		writeS(_player.getName());
		writeD(_player.getClassId().getId());
		writeD((int) _player.getCurrentHp());
		writeD(_player.getMaxHp());
		writeD((int) _player.getCurrentCp());
		writeD(_player.getMaxCp());
	}
}