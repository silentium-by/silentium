/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.authserver;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.RSAKeyGenParameterSpec;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Map.Entry;

import javolution.io.UTF8StreamReader;
import javolution.util.FastMap;
import javolution.xml.stream.XMLStreamConstants;
import javolution.xml.stream.XMLStreamReaderImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import silentium.authserver.network.gameserverpackets.ServerStatus;
import silentium.commons.database.DatabaseFactory;
import silentium.commons.utils.Rnd;

/**
 * @author KenM
 */
public class GameServerTable {
	private static final Logger _log = LoggerFactory.getLogger(GameServerTable.class.getName());

	// Server Names Config
	private static final Map<Integer, String> _serverNames = new FastMap<>();

	// Game Server Table
	private final Map<Integer, GameServerInfo> _gameServerTable = new FastMap<Integer, GameServerInfo>().shared();

	// RSA Config
	private static final int KEYS_SIZE = 10;
	private KeyPair[] _keyPairs;

	protected GameServerTable() {
		loadServerNames();
		_log.info("Loaded " + _serverNames.size() + " server names.");

		loadRegisteredGameServers();
		_log.info("Loaded " + _gameServerTable.size() + " registered gameserver(s).");

		initRSAKeys();
		_log.info("Cached " + _keyPairs.length + " RSA keys for gameserver communication.");
	}

	private void initRSAKeys() {
		try {
			final KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
			keyGen.initialize(new RSAKeyGenParameterSpec(512, RSAKeyGenParameterSpec.F4));

			_keyPairs = new KeyPair[KEYS_SIZE];
			for (int i = 0; i < KEYS_SIZE; i++)
				_keyPairs[i] = keyGen.genKeyPair();
		} catch (Exception e) {
			_log.error(getClass().getSimpleName() + ": Error loading RSA keys for Game Server communication!", e);
		}
	}
	
	private static void loadServerNames() {
		try (InputStream in = new FileInputStream("./config/servername.xml");
				InputStream bis = new BufferedInputStream(in);
				UTF8StreamReader utf8 = new UTF8StreamReader()) {
			final XMLStreamReaderImpl xpp = new XMLStreamReaderImpl();
			xpp.setInput(utf8.setInput(bis));
			for (int e = xpp.getEventType(); e != XMLStreamConstants.END_DOCUMENT; e = xpp.next()) {
				if (e == XMLStreamConstants.START_ELEMENT) {
					if ("server".equals(xpp.getLocalName().toString())) {
						final int id = Integer.parseInt(xpp.getAttributeValue(null, "id").toString());
						final String name = xpp.getAttributeValue(null, "name").toString();
						_serverNames.put(id, name);
					}
				}
			}
			xpp.close();
		} catch (Exception e) {
			_log.error("servername.xml could not be loaded.");
		}
	}

	private void loadRegisteredGameServers() {
		try (Connection con = DatabaseFactory.getConnection();
				final Statement statement = con.createStatement();
				final ResultSet rset = statement.executeQuery("SELECT * FROM gameservers")) {

			int id;
			while (rset.next()) {
				id = rset.getInt("server_id");
				_gameServerTable.put(id, new GameServerInfo(id, stringToHex(rset.getString("hexid"))));
			}
		} catch (Exception e) {
			_log.error(getClass().getSimpleName() + ": Error loading registered game servers!", e);
		}
	}

	public Map<Integer, GameServerInfo> getRegisteredGameServers() {
		return _gameServerTable;
	}

	public GameServerInfo getRegisteredGameServerById(final int id) {
		return _gameServerTable.get(id);
	}

	public boolean hasRegisteredGameServerOnId(final int id) {
		return _gameServerTable.containsKey(id);
	}

	public boolean registerWithFirstAvaliableId(final GameServerInfo gsi) {
		// avoid two servers registering with the same "free" id
		synchronized (_gameServerTable) {
			for (final Entry<Integer, String> entry : _serverNames.entrySet()) {
				if (!_gameServerTable.containsKey(entry.getKey())) {
					_gameServerTable.put(entry.getKey(), gsi);
					gsi.setId(entry.getKey());
					return true;
				}
			}
		}
		return false;
	}

	public boolean register(final int id, final GameServerInfo gsi) {
		// avoid two servers registering with the same id
		synchronized (_gameServerTable) {
			if (!_gameServerTable.containsKey(id)) {
				_gameServerTable.put(id, gsi);
				gsi.setId(id);
				return true;
			}
		}
		return false;
	}

	public void registerServerOnDB(final GameServerInfo gsi) {
		registerServerOnDB(gsi.getHexId(), gsi.getId(), gsi.getExternalHost());
	}

	public void registerServerOnDB(final byte[] hexId, final int id, final String externalHost) {
		try (Connection con = DatabaseFactory.getConnection();
				final PreparedStatement statement = con.prepareStatement("INSERT INTO gameservers (hexid,server_id,host) values (?,?,?)");) {
			statement.setString(1, hexToString(hexId));
			statement.setInt(2, id);
			statement.setString(3, externalHost);
			statement.executeUpdate();
		} catch (SQLException e) {
			_log.warn("SQL error while saving gameserver: " + e);
		}
	}

	public String getServerNameById(final int id) {
		return _serverNames.get(id);
	}

	public Map<Integer, String> getServerNames() {
		return _serverNames;
	}

	public KeyPair getKeyPair() {
		return _keyPairs[Rnd.nextInt(10)];
	}

	private static byte[] stringToHex(final String string) {
		return new BigInteger(string, 16).toByteArray();
	}

	private static String hexToString(final byte... hex) {
		if (hex == null)
			return "null";

		return new BigInteger(hex).toString(16);
	}

	public static class GameServerInfo {
		// auth
		private int _id;
		private final byte[] _hexId;
		private boolean _isAuthed;

		// status
		private GameServerThread _gst;
		private int _status;

		// network
		private String _internalIp;
		private String _externalIp;
		private String _externalHost;
		private int _port;

		// config
		private final boolean _isPvp = true;
		private boolean _isTestServer;
		private boolean _isShowingClock;
		private boolean _isShowingBrackets;
		private int _maxPlayers;

		public GameServerInfo(final int id, final byte[] hexId, final GameServerThread gst) {
			_id = id;
			_hexId = hexId;
			_gst = gst;
			_status = ServerStatus.STATUS_DOWN;
		}

		public GameServerInfo(final int id, final byte... hexId) {
			this(id, hexId, null);
		}

		public void setId(final int id) {
			_id = id;
		}

		public int getId() {
			return _id;
		}

		public byte[] getHexId() {
			return _hexId;
		}

		public void setAuthed(final boolean isAuthed) {
			_isAuthed = isAuthed;
		}

		public boolean isAuthed() {
			return _isAuthed;
		}

		public void setGameServerThread(final GameServerThread gst) {
			_gst = gst;
		}

		public GameServerThread getGameServerThread() {
			return _gst;
		}

		public void setStatus(final int status) {
			_status = status;
		}

		public int getStatus() {
			return _status;
		}

		public int getCurrentPlayerCount() {
			if (_gst == null)
				return 0;
			return _gst.getPlayerCount();
		}

		public void setInternalIp(final String internalIp) {
			_internalIp = internalIp;
		}

		public String getInternalHost() {
			return _internalIp;
		}

		public void setExternalIp(final String externalIp) {
			_externalIp = externalIp;
		}

		public String getExternalIp() {
			return _externalIp;
		}

		public void setExternalHost(final String externalHost) {
			_externalHost = externalHost;
		}

		public String getExternalHost() {
			return _externalHost;
		}

		public int getPort() {
			return _port;
		}

		public void setPort(final int port) {
			_port = port;
		}

		public void setMaxPlayers(final int maxPlayers) {
			_maxPlayers = maxPlayers;
		}

		public int getMaxPlayers() {
			return _maxPlayers;
		}

		public boolean isPvp() {
			return _isPvp;
		}

		public void setTestServer(final boolean val) {
			_isTestServer = val;
		}

		public boolean isTestServer() {
			return _isTestServer;
		}

		public void setShowingClock(final boolean clock) {
			_isShowingClock = clock;
		}

		public boolean isShowingClock() {
			return _isShowingClock;
		}

		public void setShowingBrackets(final boolean val) {
			_isShowingBrackets = val;
		}

		public boolean isShowingBrackets() {
			return _isShowingBrackets;
		}

		public void setDown() {
			_isAuthed = false;
			_port = 0;
			_gst = null;
			_status = ServerStatus.STATUS_DOWN;
		}
	}

	/**
	 * Gets the single instance of GameServerTable.
	 *
	 * @return single instance of GameServerTable
	 */
	public static GameServerTable getInstance() {
		return SingletonHolder._instance;
	}

	/**
	 * The Class SingletonHolder.
	 */
	private static class SingletonHolder {
		protected static final GameServerTable _instance = new GameServerTable();
	}
}