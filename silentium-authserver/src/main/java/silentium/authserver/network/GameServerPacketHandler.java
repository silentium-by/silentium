/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.authserver.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import silentium.authserver.GameServerThread;
import silentium.authserver.network.clientpackets.ClientBasePacket;
import silentium.authserver.network.gameserverpackets.BlowFishKey;
import silentium.authserver.network.gameserverpackets.ChangeAccessLevel;
import silentium.authserver.network.gameserverpackets.GameServerAuth;
import silentium.authserver.network.gameserverpackets.PlayerAuthRequest;
import silentium.authserver.network.gameserverpackets.PlayerInGame;
import silentium.authserver.network.gameserverpackets.PlayerLogout;
import silentium.authserver.network.gameserverpackets.ServerStatus;
import silentium.authserver.network.loginserverpackets.LoginServerFail;

/**
 * @author Ashe<br>
 * Date: 27/08/2012<br>
 * Time: 02:29
 */
public class GameServerPacketHandler {
	protected static final Logger _log = LoggerFactory.getLogger(GameServerThread.class.getName());
	
	public static enum GameServerState {
		CONNECTED, BF_CONNECTED, AUTHED
	}

	public static ClientBasePacket handlePacket(final byte[] data, GameServerThread server) {
		ClientBasePacket msg = null;
		int opcode = data[0] & 0xff;
		GameServerState state = server.getLoginConnectionState();
		switch (state) {
		case CONNECTED:
			switch (opcode) {
			case 0x00:
				msg = new BlowFishKey(data, server);
				break;
			default:
				_log.warn("Unknown Opcode (" + Integer.toHexString(opcode).toUpperCase() + ") in state " + state.name() + " from GameServer, closing connection.");
				server.forceClose(LoginServerFail.NOT_AUTHED);
				break;
			}
			break;
		case BF_CONNECTED:
			switch (opcode) {
			case 0x01:
				msg = new GameServerAuth(data, server);
				break;
			default:
				_log.warn("Unknown Opcode (" + Integer.toHexString(opcode).toUpperCase() + ") in state " + state.name() + " from GameServer, closing connection.");
				server.forceClose(LoginServerFail.NOT_AUTHED);
				break;
			}
			break;
		case AUTHED:
			switch (opcode) {
			case 0x02:
				msg = new PlayerInGame(data, server);
				break;
			case 0x03:
				msg = new PlayerLogout(data, server);
				break;
			case 0x04:
				msg = new ChangeAccessLevel(data, server);
				break;
			case 0x05:
				msg = new PlayerAuthRequest(data, server);
				break;
			case 0x06:
				msg = new ServerStatus(data, server);
				break;
			default:
				_log.warn("Unknown Opcode ("+ Integer.toHexString(opcode).toUpperCase()+ ") in state " + state.name()+ " from GameServer, closing connection.");
				server.forceClose(LoginServerFail.NOT_AUTHED);
				break;
			}
			break;
		}
		return msg;
	}
}