/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.network.clientpackets;

import silentium.gameserver.data.crest.CrestCache;
import silentium.gameserver.data.crest.CrestCache.CrestType;
import silentium.gameserver.network.serverpackets.AllyCrest;

public final class RequestAllyCrest extends L2GameClientPacket
{
	private int _crestId;

	@Override
	protected void readImpl()
	{
		_crestId = readD();
	}

	@Override
	protected void runImpl()
	{
		log.debug("Allycrestid: " + _crestId + " requested.");

		byte[] data = CrestCache.getCrest(CrestType.ALLY, _crestId);
		if (data != null)
			sendPacket(new AllyCrest(_crestId, data));
		else
		{
			log.debug("Allycrest is missing: " + _crestId + ".");
		}
	}
}