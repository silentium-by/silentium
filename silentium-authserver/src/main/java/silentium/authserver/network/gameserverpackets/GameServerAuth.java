/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.authserver.network.gameserverpackets;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import silentium.authserver.GameServerTable;
import silentium.authserver.GameServerTable.GameServerInfo;
import silentium.authserver.GameServerThread;
import silentium.authserver.configs.MainConfig;
import silentium.authserver.network.GameServerPacketHandler.GameServerState;
import silentium.authserver.network.clientpackets.ClientBasePacket;
import silentium.authserver.network.loginserverpackets.AuthResponse;
import silentium.authserver.network.loginserverpackets.LoginServerFail;

/**
 * Format: cccddb<br>
 * c desired ID<br>
 * c accept alternative ID<br>
 * c reserve Host<br>
 * s ExternalHostName<br>
 * s InetranlHostName<br>
 * d max players<br>
 * d hexid size<br>
 * b hexid </pre>
 * 
 * @author -Wooden-
 * @rework Ashe <br>
 *         Date: 27/08/2012<br>
 *         Time: 00:33
 */
public class GameServerAuth extends ClientBasePacket {
	protected static Logger _log = LoggerFactory.getLogger(GameServerAuth.class.getName());

	GameServerThread _server;
	private final byte[] _hexId;
	private final int _desiredId;
	@SuppressWarnings("unused")
	private final boolean _hostReserved;
	private final boolean _acceptAlternativeId;
	private final int _maxPlayers;
	private final int _port;
	private final String _externalHost;
	private final String _internalHost;

	/**
	 * @param decrypt
	 */
	public GameServerAuth(final byte[] decrypt, GameServerThread server) {
		super(decrypt);
		_server = server;
		_desiredId = readC();
		_acceptAlternativeId = (readC() == 0 ? false : true);
		_hostReserved = (readC() == 0 ? false : true);
		_externalHost = readS();
		_internalHost = readS();
		_port = readH();
		_maxPlayers = readD();
		int size = readD();
		_hexId = readB(size);

		if (MainConfig.PACKET_HANDLER_DEBUG) {
			_log.info("GameServerAuth: Auth request received.");
		}

		if (handleRegProcess()) {
			AuthResponse ar = new AuthResponse(server.getGameServerInfo().getId());
			server.sendPacket(ar);
			if (MainConfig.PACKET_HANDLER_DEBUG) {
				_log.info("Authed: id: " + server.getGameServerInfo().getId());
			}
			server.setLoginConnectionState(GameServerState.AUTHED);
		}
	}

	private boolean handleRegProcess() {
		GameServerTable gameServerTable = GameServerTable.getInstance();

		int id = _desiredId;
		byte[] hexId = _hexId;

		GameServerInfo gsi = gameServerTable.getRegisteredGameServerById(id);
		// is there a gameserver registered with this id?
		if (gsi != null) {
			// does the hex id match?
			if (Arrays.equals(gsi.getHexId(), hexId)) {
				// check to see if this GS is already connected
				synchronized (gsi) {
					if (gsi.isAuthed()) {
						_server.forceClose(LoginServerFail.REASON_ALREADY_LOGGED8IN);
						return false;
					}
					_server.attachGameServerInfo(gsi, _port, _externalHost, _internalHost, _maxPlayers);
				}
			} else {
				// there is already a server registered with the desired id and
				// different hex id
				// try to register this one with an alternative id
				if (MainConfig.ACCEPT_NEW_GAMESERVER && _acceptAlternativeId) {
					gsi = new GameServerInfo(id, hexId, _server);
					if (gameServerTable.registerWithFirstAvaliableId(gsi)) {
						_server.attachGameServerInfo(gsi, _port, _externalHost, _internalHost, _maxPlayers);
						gameServerTable.registerServerOnDB(gsi);
					} else {
						_server.forceClose(LoginServerFail.REASON_NO_FREE_ID);
						return false;
					}
				} else {
					// server id is already taken, and we cant get a new one for
					// you
					_server.forceClose(LoginServerFail.REASON_WRONG_HEXID);
					return false;
				}
			}
		} else {
			// can we register on this id?
			if (MainConfig.ACCEPT_NEW_GAMESERVER) {
				gsi = new GameServerInfo(id, hexId, _server);
				if (gameServerTable.register(id, gsi)) {
					_server.attachGameServerInfo(gsi, _port, _externalHost, _internalHost, _maxPlayers);
					gameServerTable.registerServerOnDB(gsi);
				} else {
					// some one took this ID meanwhile
					_server.forceClose(LoginServerFail.REASON_ID_RESERVED);
					return false;
				}
			} else {
				_server.forceClose(LoginServerFail.REASON_WRONG_HEXID);
				return false;
			}
		}
		return true;
	}
}