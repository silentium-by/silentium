/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.network.clientpackets;

import silentium.gameserver.configs.CustomConfig;
import silentium.gameserver.configs.MainConfig;
import silentium.gameserver.network.serverpackets.KeyPacket;
import silentium.gameserver.network.serverpackets.L2GameServerPacket;
import silentium.gameserver.network.serverpackets.SendStatus;

public final class ProtocolVersion extends L2GameClientPacket
{
	private int _version;

	@Override
	protected void readImpl()
	{
		_version = readD();
	}

	@Override
	protected void runImpl()
	{
		if (_version == 65534 || _version == -2)
			getClient().close((L2GameServerPacket) null);
		else if (_version == 65533 || _version == -3) // RWHO
		{
			if (CustomConfig.RWHO_LOG)
				log.info(getClient().toString() + " RWHO received");
			getClient().getConnection().close(new SendStatus());
		}
		else if (_version < MainConfig.MIN_PROTOCOL_REVISION || _version > MainConfig.MAX_PROTOCOL_REVISION)
		{
			log.warn("Client: " + getClient().toString() + " -> Protocol Revision: " + _version + " is invalid. Minimum and maximum allowed are: " + MainConfig.MIN_PROTOCOL_REVISION + " and " + MainConfig.MAX_PROTOCOL_REVISION + ". Closing connection.");
			getClient().close((L2GameServerPacket) null);
		}
		else
			getClient().sendPacket(new KeyPacket(getClient().enableCrypt()));
	}
}
