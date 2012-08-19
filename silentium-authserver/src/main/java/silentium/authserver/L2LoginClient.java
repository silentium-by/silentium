/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.authserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import silentium.authserver.configs.MainConfig;
import silentium.authserver.network.serverpackets.L2LoginServerPacket;
import silentium.authserver.network.serverpackets.LoginFail;
import silentium.authserver.network.serverpackets.LoginFail.LoginFailReason;
import silentium.authserver.network.serverpackets.PlayFail;
import silentium.authserver.network.serverpackets.PlayFail.PlayFailReason;
import silentium.commons.crypt.LoginCrypt;
import silentium.commons.crypt.ScrambledKeyPair;
import silentium.commons.network.mmocore.MMOClient;
import silentium.commons.network.mmocore.MMOConnection;
import silentium.commons.network.mmocore.SendablePacket;
import silentium.commons.utils.Rnd;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.security.interfaces.RSAPrivateKey;

/**
 * Represents a client connected into the LoginServer
 *
 * @author KenM
 */
public final class L2LoginClient extends MMOClient<MMOConnection<L2LoginClient>> {
	private static Logger _log = LoggerFactory.getLogger(L2LoginClient.class.getName());

	public enum LoginClientState {
		CONNECTED, AUTHED_GG, AUTHED_LOGIN
	}

	private LoginClientState _state;

	// Crypt
	private final LoginCrypt _loginCrypt;
	private final ScrambledKeyPair _scrambledPair;
	private final byte[] _blowfishKey;

	private String _account;
	private int _accessLevel;
	private int _lastServer;
	private boolean _usesInternalIP;
	private SessionKey _sessionKey;
	private final int _sessionId;
	private boolean _joinedGS;

	private final long _connectionStartTime;

	/**
	 * @param con
	 */
	public L2LoginClient(final MMOConnection<L2LoginClient> con) {
		super(con);
		_state = LoginClientState.CONNECTED;
		final String ip = getConnection().getInetAddress().getHostAddress();

		// TODO unhardcode this
		if (ip.startsWith("192.168") || ip.startsWith("10.0") || "127.0.0.1".equals(ip)) {
			_usesInternalIP = true;
		}

		_scrambledPair = LoginController.getInstance().getScrambledRSAKeyPair();
		_blowfishKey = LoginController.getInstance().getBlowfishKey();
		_sessionId = Rnd.nextInt();
		_connectionStartTime = System.currentTimeMillis();
		_loginCrypt = new LoginCrypt();
		_loginCrypt.setKey(_blowfishKey);
	}

	public boolean usesInternalIP() {
		return _usesInternalIP;
	}

	@Override
	public boolean decrypt(final ByteBuffer buf, final int size) {
		boolean ret = false;
		try {
			ret = _loginCrypt.decrypt(buf.array(), buf.position(), size);
		} catch (IOException e) {
			e.printStackTrace();
			getConnection().close((SendablePacket<L2LoginClient>) null);
			return false;
		}

		if (!ret) {
			final byte[] dump = new byte[size];
			System.arraycopy(buf.array(), buf.position(), dump, 0, size);
			_log.warn("Wrong checksum from client: " + toString());
			getConnection().close((SendablePacket<L2LoginClient>) null);
		}

		return ret;
	}

	@Override
	public boolean encrypt(final ByteBuffer buf, int size) {
		final int offset = buf.position();
		try {
			size = _loginCrypt.encrypt(buf.array(), offset, size);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

		buf.position(offset + size);
		return true;
	}

	public LoginClientState getState() {
		return _state;
	}

	public void setState(final LoginClientState state) {
		_state = state;
	}

	public byte[] getBlowfishKey() {
		return _blowfishKey;
	}

	public byte[] getScrambledModulus() {
		return _scrambledPair._scrambledModulus;
	}

	public RSAPrivateKey getRSAPrivateKey() {
		return (RSAPrivateKey) _scrambledPair._pair.getPrivate();
	}

	public String getAccount() {
		return _account;
	}

	public void setAccount(final String account) {
		_account = account;
	}

	public void setAccessLevel(final int accessLevel) {
		_accessLevel = accessLevel;
	}

	public int getAccessLevel() {
		return _accessLevel;
	}

	public void setLastServer(final int lastServer) {
		_lastServer = lastServer;
	}

	public int getLastServer() {
		return _lastServer;
	}

	public int getSessionId() {
		return _sessionId;
	}

	public boolean hasJoinedGS() {
		return _joinedGS;
	}

	public void setJoinedGS(final boolean val) {
		_joinedGS = val;
	}

	public void setSessionKey(final SessionKey sessionKey) {
		_sessionKey = sessionKey;
	}

	public SessionKey getSessionKey() {
		return _sessionKey;
	}

	public long getConnectionStartTime() {
		return _connectionStartTime;
	}

	public void sendPacket(final L2LoginServerPacket lsp) {
		getConnection().sendPacket(lsp);
	}

	public void close(final LoginFailReason reason) {
		getConnection().close(new LoginFail(reason));
	}

	public void close(final PlayFailReason reason) {
		getConnection().close(new PlayFail(reason));
	}

	public void close(final L2LoginServerPacket lsp) {
		getConnection().close(lsp);
	}

	@Override
	public void onDisconnection() {
		if (MainConfig.DEBUG)
			_log.info("DISCONNECTED: " + toString());

		if (!hasJoinedGS() || getConnectionStartTime() + LoginController.LOGIN_TIMEOUT < System.currentTimeMillis())
			LoginController.getInstance().removeAuthedLoginClient(_account);
	}

	@Override
	public String toString() {
		final InetAddress address = getConnection().getInetAddress();
		if (_state == LoginClientState.AUTHED_LOGIN) {
			return "[" + _account + " (" + (address == null ? "disconnected" : address.getHostAddress()) + ")]";
		}
		return "[" + (address == null ? "disconnected" : address.getHostAddress()) + "]";
	}

	@Override
	protected void onForcedDisconnection() {
		// Empty
	}
}
