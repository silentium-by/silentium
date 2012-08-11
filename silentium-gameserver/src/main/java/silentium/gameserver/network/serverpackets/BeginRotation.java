/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.network.serverpackets;

import silentium.gameserver.model.actor.instance.L2PcInstance;

public class BeginRotation extends L2GameServerPacket
{
	private final int _charObjId;
	private final int _degree;
	private final int _side;

	public BeginRotation(L2PcInstance player, int degree, int side)
	{
		_charObjId = player.getObjectId();
		_degree = degree;
		_side = side;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x62);
		writeD(_charObjId);
		writeD(_degree);
		writeD(_side);
	}
}