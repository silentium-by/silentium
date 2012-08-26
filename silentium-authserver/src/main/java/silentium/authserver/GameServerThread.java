/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.authserver;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Set;

import javolution.util.FastSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import silentium.authserver.GameServerTable.GameServerInfo;
import silentium.authserver.configs.MainConfig;
import silentium.authserver.network.GameServerPacketHandler;
import silentium.authserver.network.GameServerPacketHandler.GameServerState;
import silentium.authserver.network.loginserverpackets.InitLS;
import silentium.authserver.network.loginserverpackets.KickPlayer;
import silentium.authserver.network.loginserverpackets.LoginServerFail;
import silentium.authserver.network.serverpackets.ServerBasePacket;
import silentium.commons.crypt.NewCrypt;
import silentium.commons.utils.Util;

/**
 * @author -Wooden-
 * @author KenM
 * 
 * @rework Ashe<br>
 * Date: 26/08/2012<br>
 * Time: 00:03
 */

public class GameServerThread extends Thread {
	protected static final Logger _log = LoggerFactory.getLogger(GameServerThread.class.getName());

	private final Socket _connection;
	private InputStream _in;
	private OutputStream _out;
	private final RSAPublicKey _publicKey;
	private final RSAPrivateKey _privateKey;
	private NewCrypt _blowfish;
	private GameServerState _loginConnectionState = GameServerState.CONNECTED;
	private final String _connectionIp;
	private GameServerInfo _gsi;

	/**
	 * Authed Clients on a GameServer
	 */
	private final Set<String> _accountsOnGameServer = new FastSet<>();

	private String _connectionIPAddress;

	@Override
	public void run() {
		_connectionIPAddress = _connection.getInetAddress().getHostAddress();
		if (isBannedGameserverIP(_connectionIPAddress)) {
			_log.info("GameServerRegistration: IP Address " + _connectionIPAddress + " is on Banned IP list.");
			forceClose(LoginServerFail.REASON_IP_BANNED);
			// ensure no further processing for this connection
			return;
		}

		final InitLS startPacket = new InitLS(_publicKey.getModulus().toByteArray());
		try {
			sendPacket(startPacket);

			int lengthHi = 0;
			int lengthLo = 0;
			int length = 0;
			boolean checksumOk = false;
			for (;;) {
				lengthLo = _in.read();
				lengthHi = _in.read();
				length = lengthHi * 256 + lengthLo;

				if (lengthHi < 0 || _connection.isClosed()) {
					_log.debug("LoginServerThread: Login terminated the connection.");
					break;
				}

				byte[] data = new byte[length - 2];

				int receivedBytes = 0;
				int newBytes = 0;
				while (newBytes != -1 && receivedBytes < length - 2) {
					newBytes = _in.read(data, 0, length - 2);
					receivedBytes = receivedBytes + newBytes;
				}

				if (receivedBytes != length - 2) {
					_log.warn("Incomplete Packet is sent to the server, closing connection.(LS)");
					break;
				}

				// decrypt if we have a key
				data = _blowfish.decrypt(data);
				checksumOk = NewCrypt.verifyChecksum(data);
				if (!checksumOk) {
					_log.warn("Incorrect packet checksum, closing connection (LS)");
					return;
				}

				if (MainConfig.PACKET_HANDLER_DEBUG) {
					_log.debug("[C]\n" + Util.printData(data));
				}

				GameServerPacketHandler.handlePacket(data, this);
			}
		} catch (IOException e) {
			final String serverName = getServerId() != -1 ? "[" + getServerId() + "] " + GameServerTable.getInstance().getServerNameById(getServerId()) : "(" + _connectionIPAddress + ")";
			final String msg = "GameServer " + serverName + ": Connection lost: " + e.getMessage();
			_log.info(msg);
		} finally {
			if (isAuthed()) {
				_gsi.setDown();
				_log.info("Server [" + getServerId() + "] " + GameServerTable.getInstance().getServerNameById(getServerId()) + " is now set as disconnected");
			}
			L2LoginServer.getInstance().getGameServerListener().removeGameServer(this);
			L2LoginServer.getInstance().getGameServerListener().removeFloodProtection(_connectionIp);
		}
	}

	/**
	 * @return Returns the connectionIpAddress.
	 */
	public String getConnectionIpAddress() {
		return _connectionIPAddress;
	}

	/**
	 * @return Returns the isAuthed.
	 */
	public boolean isAuthed() {
		if (_gsi == null) {
			return false;
		}
		return _gsi.isAuthed();
	}

	/**
	 * @param gsi
	 */
	public void setGameServerInfo(final GameServerInfo gsi) {
		_gsi = gsi;
	}

	/**
	 * @return
	 */
	public GameServerInfo getGameServerInfo() {
		return _gsi;
	}

	/**
	 * @param ipAddress
	 * @return
	 */
	public static boolean isBannedGameserverIP(final String ipAddress) {
		return false;
	}

	/**
	 * @param con
	 */
	public GameServerThread(final Socket con) {
		_connection = con;
		_connectionIp = con.getInetAddress().getHostAddress();
		try {
			_in = new BufferedInputStream(_connection.getInputStream());
			_out = new BufferedOutputStream(_connection.getOutputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
		final KeyPair pair = GameServerTable.getInstance().getKeyPair();
		_privateKey = (RSAPrivateKey) pair.getPrivate();
		_publicKey = (RSAPublicKey) pair.getPublic();
		_blowfish = new NewCrypt("_;v.]05-31!|+-%xT!^[$\00");
		start();
	}

	/**
	 * 
	 * @param account
	 * @return
	 */
	public boolean hasAccountOnGameServer(final String account) {
		return _accountsOnGameServer.contains(account);
	}

	/**
	 * Count accounts on game server
	 * 
	 * @return
	 */
	public int getPlayerCount() {
		return _accountsOnGameServer.size();
	}

	/**
	 * Attachs a GameServerInfo to this Thread<br>
	 * <ul>
	 * <li>Updates the GameServerInfo values based on GameServerAuth packet</li>
	 * <li><b>Sets the GameServerInfo as Authed</b></li>
	 * </ul>
	 * 
	 * @param gsi The GameServerInfo to be attached.
	 * @param port
	 * @param hosts
	 * @param maxPlayers
	 */
	public void attachGameServerInfo(GameServerInfo gsi, int port, final String gameExternalHost, final String gameInternalHost, int maxPlayers) {
		setGameServerInfo(gsi);
		gsi.setGameServerThread(this);
		gsi.setPort(port);
		setGameHosts(gameExternalHost, gameInternalHost);
		gsi.setMaxPlayers(maxPlayers);
		gsi.setAuthed(true);
	}

	/**
	 * @param gameExternalHost
	 * @param gameInternalHost
	 */
	public void setGameHosts(final String gameExternalHost, final String gameInternalHost) {
		final String oldInternal = _gsi.getInternalHost();
		final String oldExternal = _gsi.getExternalHost();

		_gsi.setExternalHost(gameExternalHost);
		_gsi.setInternalIp(gameInternalHost);

		if (!"*".equals(gameExternalHost)) {
			try {
				_gsi.setExternalIp(InetAddress.getByName(gameExternalHost).getHostAddress());
			} catch (UnknownHostException e) {
				_log.warn("Couldn't resolve hostname \"" + gameExternalHost + "\"");
			}
		} else {
			_gsi.setExternalIp(_connectionIp);
		}
		if (!"*".equals(gameInternalHost)) {
			try {
				_gsi.setInternalIp(InetAddress.getByName(gameInternalHost).getHostAddress());
			} catch (UnknownHostException e) {
				_log.warn("Couldn't resolve hostname \"" + gameInternalHost + "\"");
			}
		} else {
			_gsi.setInternalIp(_connectionIp);
		}

		String internalIP = "not found", externalIP = "not found";
		if (oldInternal == null|| !oldInternal.equalsIgnoreCase(gameInternalHost)) {
			internalIP = gameInternalHost;
		}
		
		if (oldExternal == null|| !oldExternal.equalsIgnoreCase(gameExternalHost)) {
			externalIP = gameExternalHost;
		}
		
		_log.info("Hooked gameserver: [" + getServerId() + "] " + GameServerTable.getInstance().getServerNameById(getServerId()));
		_log.info("Internal/External IP(s): " + internalIP + "/" + externalIP);
	}

	/**
	 * @param account
	 */
	public void kickPlayer(final String account) {
		sendPacket(new KickPlayer(account));
	}

	/**
	 * @param reason
	 */
	public void forceClose(final int reason) {
		sendPacket(new LoginServerFail(reason));
		try {
			_connection.close();
		} catch (IOException e) {
			_log.info("GameServerThread: Failed disconnecting banned server, server already disconnected.");
		}
	}

	/**
	 * @param sl
	 */
	public void sendPacket(final ServerBasePacket sl) {
		try {
			byte[] data = sl.getContent();
			NewCrypt.appendChecksum(data);
			if (MainConfig.PACKET_HANDLER_DEBUG) {
				_log.info("[S] " + sl.getClass().getSimpleName() + ":\n" + Util.printData(data));
			}
			_blowfish.crypt(data, 0, data.length);

			int len = data.length + 2;
			synchronized (_out) {
				_out.write(len & 0xff);
				_out.write((len >> 8) & 0xff);
				_out.write(data);
				_out.flush();
			}
		} catch (IOException e) {
			_log.error("IOException while sending packet " + sl.getClass().getSimpleName());
		}
	}

	/**
	 * Return server ID
	 * 
	 * @return int
	 */
	public int getServerId() {
		if (getGameServerInfo() != null) {
			return getGameServerInfo().getId();
		}
		return -1;
	}

	/**
	 * @return _privateKey
	 */
	public RSAPrivateKey getPrivateKey() {
		return _privateKey;
	}

	/**
	 * @param blowfish
	 */
	public void SetBlowFish(NewCrypt blowfish) {
		_blowfish = blowfish;
	}

	/**
	 * @return _loginConnectionState
	 */
	public GameServerState getLoginConnectionState() {
		return _loginConnectionState;
	}

	/**
	 * @param state
	 */
	public void setLoginConnectionState(GameServerState state) {
		_loginConnectionState = state;
	}

	/**
	 * @param account
	 */
	public void addAccountOnGameServer(String account) {
		_accountsOnGameServer.add(account);
	}

	/**
	 * @param account
	 */
	public void removeAccountOnGameServer(String account) {
		_accountsOnGameServer.remove(account);
	}
}