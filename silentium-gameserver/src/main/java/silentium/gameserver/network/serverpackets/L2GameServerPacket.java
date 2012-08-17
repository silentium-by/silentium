/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.network.serverpackets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import silentium.commons.network.mmocore.SendablePacket;
import silentium.gameserver.network.L2GameClient;

/**
 * @author KenM
 */
public abstract class L2GameServerPacket extends SendablePacket<L2GameClient>
{
	protected static final Logger _log = LoggerFactory.getLogger(L2GameServerPacket.class.getName());

	@Override
	protected void write()
	{
		_log.debug(getType());

		try
		{
			writeImpl();
		}
		catch (Throwable t)
		{
			_log.error("Client: " + getClient().toString() + " - Failed writing: " + getType());
			t.printStackTrace();
		}
	}

	public void runImpl()
	{
	}

	protected abstract void writeImpl();

	public String getType()
	{
		return "[S] " + getClass().getSimpleName();
	}
}
