/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.authserver.network.gameserverpackets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import silentium.authserver.GameServerThread;
import silentium.authserver.LoginController;
import silentium.authserver.SessionKey;
import silentium.authserver.configs.MainConfig;
import silentium.authserver.network.clientpackets.ClientBasePacket;
import silentium.authserver.network.loginserverpackets.PlayerAuthResponse;

/**
 * @author -Wooden-
 * @rework Ashe
 */
public class PlayerAuthRequest extends ClientBasePacket {
	protected static final Logger _log = LoggerFactory.getLogger(PlayerAuthRequest.class.getName());
	
	/**
	 * @param decrypt
	 * @param server
	 */
	public PlayerAuthRequest(final byte[] decrypt, GameServerThread server) {
		super(decrypt);
		String account = readS();
		final int playKey1 = readD();
		final int playKey2 = readD();
		final int loginKey1 = readD();
		final int loginKey2 = readD();
		final SessionKey sessionKey = new SessionKey(loginKey1, loginKey2, playKey1, playKey2);

		final PlayerAuthResponse authResponse;
		if (MainConfig.PACKET_HANDLER_DEBUG) {
			_log.info("auth request received for Player " + account);
		}
		
		final SessionKey key = LoginController.getInstance().getKeyForAccount(account);
		if ((key != null) && key.equals(sessionKey)) {
			if (MainConfig.PACKET_HANDLER_DEBUG) {
				_log.info("auth request: OK");
			}
			LoginController.getInstance().removeAuthedLoginClient(account);
			authResponse = new PlayerAuthResponse(account, true);
		} else {
			if (MainConfig.PACKET_HANDLER_DEBUG) {
				_log.info("auth request: NO");
				_log.info("session key from self: " + key);
				_log.info("session key sent: " + sessionKey);
			}
			authResponse = new PlayerAuthResponse(account, false);
		}
		server.sendPacket(authResponse);
	}
}