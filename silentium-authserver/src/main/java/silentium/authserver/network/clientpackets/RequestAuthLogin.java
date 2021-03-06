/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.authserver.network.clientpackets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import silentium.authserver.GameServerTable.GameServerInfo;
import silentium.authserver.L2LoginClient;
import silentium.authserver.L2LoginClient.LoginClientState;
import silentium.authserver.LoginController;
import silentium.authserver.LoginController.AuthLoginResult;
import silentium.authserver.configs.MainConfig;
import silentium.authserver.network.serverpackets.AccountKicked;
import silentium.authserver.network.serverpackets.AccountKicked.AccountKickedReason;
import silentium.authserver.network.serverpackets.LoginFail.LoginFailReason;
import silentium.authserver.network.serverpackets.LoginOk;
import silentium.authserver.network.serverpackets.ServerList;

import javax.crypto.Cipher;
import java.security.GeneralSecurityException;

/**
 * Format: x 0 (a leading null) x: the rsa encrypted block with the login an password
 */
public class RequestAuthLogin extends L2LoginClientPacket {
	private static Logger _log = LoggerFactory.getLogger(RequestAuthLogin.class.getName());

	private final byte[] _raw = new byte[128];

	private String _user;
	private String _password;
	private int _ncotp;

	public String getPassword() {
		return _password;
	}

	public String getUser() {
		return _user;
	}

	public int getOneTimePassword() {
		return _ncotp;
	}

	@Override
	public boolean readImpl() {
		if (_buf.remaining() >= 128) {
			readB(_raw);
			return true;
		}
		return false;
	}

	@Override
	public void run() {
		byte[] decrypted = null;
		final L2LoginClient client = getClient();
		try {
			final Cipher rsaCipher = Cipher.getInstance("RSA/ECB/nopadding");
			rsaCipher.init(Cipher.DECRYPT_MODE, getClient().getRSAPrivateKey());
			decrypted = rsaCipher.doFinal(_raw, 0x00, 0x80);
		} catch (GeneralSecurityException e) {
			_log.error(e.getLocalizedMessage(), e);
			return;
		}

		try {
			_user = new String(decrypted, 0x5E, 14).trim().toLowerCase();
			_password = new String(decrypted, 0x6C, 16).trim();
			_ncotp = decrypted[0x7c];
			_ncotp |= decrypted[0x7d] << 8;
			_ncotp |= decrypted[0x7e] << 16;
			_ncotp |= decrypted[0x7f] << 24;
		} catch (Exception e) {
			_log.error(e.getLocalizedMessage(), e);
			return;
		}

		final LoginController lc = LoginController.getInstance();
		final AuthLoginResult result = lc.tryAuthLogin(_user, _password, client);
		switch (result) {
			case AUTH_SUCCESS:
				client.setAccount(_user);
				client.setState(LoginClientState.AUTHED_LOGIN);
				client.setSessionKey(lc.assignSessionKeyToClient(_user, client));
				if (MainConfig.SHOW_LICENCE)
					client.sendPacket(new LoginOk(getClient().getSessionKey()));
				else
					getClient().sendPacket(new ServerList(getClient()));
				break;

			case INVALID_PASSWORD:
				client.close(LoginFailReason.REASON_USER_OR_PASS_WRONG);
				break;

			case ACCOUNT_BANNED:
				client.close(new AccountKicked(AccountKickedReason.REASON_PERMANENTLY_BANNED));
				break;

			case ALREADY_ON_LS:
				final L2LoginClient oldClient;
				if ((oldClient = lc.getAuthedClient(_user)) != null) {
					// kick the other client
					oldClient.close(LoginFailReason.REASON_ACCOUNT_IN_USE);
					lc.removeAuthedLoginClient(_user);
				}
				// kick also current client
				client.close(LoginFailReason.REASON_ACCOUNT_IN_USE);
				break;

			case ALREADY_ON_GS:
				final GameServerInfo gsi;
				if ((gsi = lc.getAccountOnGameServer(_user)) != null) {
					client.close(LoginFailReason.REASON_ACCOUNT_IN_USE);

					// kick from there
					if (gsi.isAuthed())
						gsi.getGameServerThread().kickPlayer(_user);
				}
				break;
		}
	}
}
