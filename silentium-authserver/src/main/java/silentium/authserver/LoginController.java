/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.authserver;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.RSAKeyGenParameterSpec;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;

import javax.crypto.Cipher;

import javolution.util.FastMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import silentium.authserver.GameServerTable.GameServerInfo;
import silentium.authserver.configs.MainConfig;
import silentium.authserver.network.gameserverpackets.ServerStatus;
import silentium.authserver.network.serverpackets.LoginFail.LoginFailReason;
import silentium.commons.crypt.Base64;
import silentium.commons.crypt.ScrambledKeyPair;
import silentium.commons.database.DatabaseFactory;
import silentium.commons.utils.Rnd;

public class LoginController
{
	protected static final Logger _log = LoggerFactory.getLogger(LoginController.class.getName());
	private static final Logger authLog = LoggerFactory.getLogger(LoginController.class);

	private static LoginController _instance;

	/** Time before kicking the client if he didnt logged yet */
	public static final int LOGIN_TIMEOUT = 60 * 1000;

	protected FastMap<String, L2LoginClient> _loginServerClients = new FastMap<String, L2LoginClient>().shared();
	private final Map<InetAddress, BanInfo> _bannedIps = new FastMap<InetAddress, BanInfo>().shared();
	private final Map<InetAddress, FailedLoginAttempt> _hackProtection;

	protected ScrambledKeyPair[] _keyPairs;

	private final Thread _purge;

	protected byte[][] _blowfishKeys;
	private static final int BLOWFISH_KEYS = 20;

	public static void load() throws GeneralSecurityException
	{
		if (_instance == null)
		{
			_instance = new LoginController();
		}
		else
		{
			throw new IllegalStateException("LoginController can only be loaded a single time.");
		}
	}

	public static LoginController getInstance()
	{
		return _instance;
	}

	private LoginController() throws GeneralSecurityException
	{
		_log.info("Loading LoginController...");

		_hackProtection = new FastMap<>();

		_keyPairs = new ScrambledKeyPair[10];

		KeyPairGenerator keygen = null;

		keygen = KeyPairGenerator.getInstance("RSA");
		RSAKeyGenParameterSpec spec = new RSAKeyGenParameterSpec(1024, RSAKeyGenParameterSpec.F4);
		keygen.initialize(spec);

		// generate the initial set of keys
		for (int i = 0; i < 10; i++)
			_keyPairs[i] = new ScrambledKeyPair(keygen.generateKeyPair());

		_log.info("Cached 10 KeyPairs for RSA communication.");

		testCipher((RSAPrivateKey) _keyPairs[0]._pair.getPrivate());

		// Store keys for blowfish communication
		generateBlowFishKeys();

		_purge = new PurgeThread();
		_purge.setDaemon(true);
		_purge.start();
	}

	/**
	 * This is mostly to force the initialization of the Crypto Implementation, avoiding it being done on runtime when its first needed.<BR>
	 * In short it avoids the worst-case execution time on runtime by doing it on loading.
	 * 
	 * @param key
	 *            Any private RSA Key just for testing purposes.
	 * @throws java.security.GeneralSecurityException
	 *             if a underlying exception was thrown by the Cipher
	 */
	private static void testCipher(RSAPrivateKey key) throws GeneralSecurityException
	{
		// avoid worst-case execution, KenM
		Cipher rsaCipher = Cipher.getInstance("RSA/ECB/nopadding");
		rsaCipher.init(Cipher.DECRYPT_MODE, key);
	}

	private void generateBlowFishKeys()
	{
		_blowfishKeys = new byte[BLOWFISH_KEYS][16];

		for (int i = 0; i < BLOWFISH_KEYS; i++)
		{
			for (int j = 0; j < _blowfishKeys[i].length; j++)
				_blowfishKeys[i][j] = (byte) (Rnd.nextInt(255) + 1);
		}
		_log.info("Stored " + _blowfishKeys.length + " keys for Blowfish communication.");
	}

	/**
	 * @return Returns a random key
	 */
	public byte[] getBlowfishKey()
	{
		return _blowfishKeys[(int) (Math.random() * BLOWFISH_KEYS)];
	}

	public SessionKey assignSessionKeyToClient(String account, L2LoginClient client)
	{
		SessionKey key;

		key = new SessionKey(Rnd.nextInt(), Rnd.nextInt(), Rnd.nextInt(), Rnd.nextInt());
		_loginServerClients.put(account, client);
		return key;
	}

	public void removeAuthedLoginClient(String account)
	{
		if (account == null)
			return;

		_loginServerClients.remove(account);
	}

	public L2LoginClient getAuthedClient(String account)
	{
		return _loginServerClients.get(account);
	}

	public static enum AuthLoginResult
	{
		INVALID_PASSWORD, ACCOUNT_BANNED, ALREADY_ON_LS, ALREADY_ON_GS, AUTH_SUCCESS
	};

	public AuthLoginResult tryAuthLogin(String account, String password, L2LoginClient client)
	{
		AuthLoginResult ret = AuthLoginResult.INVALID_PASSWORD;
		// check auth
		if (loginValid(account, password, client))
		{
			// login was successful, verify presence on Gameservers
			ret = AuthLoginResult.ALREADY_ON_GS;
			if (!isAccountInAnyGameServer(account))
			{
				// account isnt on any GS verify LS itself
				ret = AuthLoginResult.ALREADY_ON_LS;

				if (_loginServerClients.putIfAbsent(account, client) == null)
					ret = AuthLoginResult.AUTH_SUCCESS;
			}
		}
		else
		{
			if (client.getAccessLevel() < 0)
				ret = AuthLoginResult.ACCOUNT_BANNED;
		}
		return ret;
	}

	/**
	 * Adds the address to the ban list of the login server, with the given duration.
	 * 
	 * @param address
	 *            The Address to be banned.
	 * @param expiration
	 *            Timestamp in miliseconds when this ban expires
	 * @throws java.net.UnknownHostException
	 *             if the address is invalid.
	 */
	public void addBanForAddress(String address, long expiration) throws UnknownHostException
	{
		InetAddress netAddress = InetAddress.getByName(address);
		if (!_bannedIps.containsKey(netAddress))
			_bannedIps.put(netAddress, new BanInfo(netAddress, expiration));
	}

	/**
	 * Adds the address to the ban list of the login server, with the given duration.
	 * 
	 * @param address
	 *            The Address to be banned.
	 * @param duration
	 *            is miliseconds
	 */
	public void addBanForAddress(InetAddress address, long duration)
	{
		if (!_bannedIps.containsKey(address))
			_bannedIps.put(address, new BanInfo(address, System.currentTimeMillis() + duration));
	}

	public boolean isBannedAddress(InetAddress address)
	{
		BanInfo bi = _bannedIps.get(address);
		if (bi != null)
		{
			if (bi.hasExpired())
			{
				_bannedIps.remove(address);
				return false;
			}
			return true;
		}
		return false;
	}

	public Map<InetAddress, BanInfo> getBannedIps()
	{
		return _bannedIps;
	}

	/**
	 * Remove the specified address from the ban list
	 * 
	 * @param address
	 *            The address to be removed from the ban list
	 * @return true if the ban was removed, false if there was no ban for this ip
	 */
	public boolean removeBanForAddress(InetAddress address)
	{
		return _bannedIps.remove(address) != null;
	}

	/**
	 * Remove the specified address from the ban list
	 * 
	 * @param address
	 *            The address to be removed from the ban list
	 * @return true if the ban was removed, false if there was no ban for this ip or the address was invalid.
	 */
	public boolean removeBanForAddress(String address)
	{
		try
		{
			return this.removeBanForAddress(InetAddress.getByName(address));
		}
		catch (UnknownHostException e)
		{
			return false;
		}
	}

	public SessionKey getKeyForAccount(String account)
	{
		L2LoginClient client = _loginServerClients.get(account);
		if (client != null)
		{
			return client.getSessionKey();
		}
		return null;
	}

	public boolean isAccountInAnyGameServer(String account)
	{
		Collection<GameServerInfo> serverList = GameServerTable.getInstance().getRegisteredGameServers().values();
		for (GameServerInfo gsi : serverList)
		{
			GameServerThread gst = gsi.getGameServerThread();
			if (gst != null && gst.hasAccountOnGameServer(account))
				return true;
		}
		return false;
	}

	public GameServerInfo getAccountOnGameServer(String account)
	{
		Collection<GameServerInfo> serverList = GameServerTable.getInstance().getRegisteredGameServers().values();
		for (GameServerInfo gsi : serverList)
		{
			GameServerThread gst = gsi.getGameServerThread();
			if (gst != null && gst.hasAccountOnGameServer(account))
				return gsi;
		}
		return null;
	}

	public boolean isLoginPossible(L2LoginClient client, int serverId)
	{
		GameServerInfo gsi = GameServerTable.getInstance().getRegisteredGameServerById(serverId);
		int access = client.getAccessLevel();
		if (gsi != null && gsi.isAuthed())
		{
			boolean loginOk = (gsi.getCurrentPlayerCount() < gsi.getMaxPlayers() && gsi.getStatus() != ServerStatus.STATUS_GM_ONLY) || access > 0;

			if (loginOk && client.getLastServer() != serverId)
			{
				try (Connection con = DatabaseFactory.getConnection(); PreparedStatement statement = con.prepareStatement("UPDATE accounts SET lastServer = ? WHERE login = ?"))
				{
					statement.setInt(1, serverId);
					statement.setString(2, client.getAccount());
					statement.executeUpdate();
				}
				catch (SQLException e)
				{
					_log.warn("Could not set lastServer: " + e.getMessage(), e);
				}
			}
			return loginOk;
		}
		return false;
	}

	public void setAccountAccessLevel(String account, int banLevel)
	{
		try (Connection con = DatabaseFactory.getConnection(); PreparedStatement statement = con.prepareStatement("UPDATE accounts SET access_level=? WHERE login=?"))
		{
			statement.setInt(1, banLevel);
			statement.setString(2, account);
			statement.executeUpdate();
		}
		catch (Exception e)
		{
			_log.warn("Could not set accessLevel: " + e.getMessage(), e);
		}
	}

	/**
	 * <p>
	 * This method returns one of the cached {@link silentium.commons.crypt.ScrambledKeyPair ScrambledKeyPairs} for communication with Login
	 * Clients.
	 * </p>
	 * 
	 * @return a scrambled keypair
	 */
	public ScrambledKeyPair getScrambledRSAKeyPair()
	{
		return _keyPairs[Rnd.nextInt(10)];
	}

	/**
	 * user name is not case sensitive any more
	 * 
	 * @param user
	 * @param password
	 * @param client
	 * @return true if successful
	 */
	public boolean loginValid(String user, String password, L2LoginClient client)
	{
		boolean ok = false;
		InetAddress address = client.getConnection().getInetAddress();

		// player disconnected meanwhile
		if (address == null || user == null)
			return false;

		try
		{
			MessageDigest md = MessageDigest.getInstance("SHA");
			byte[] raw = password.getBytes("UTF-8");
			byte[] hash = md.digest(raw);

			byte[] expected = null;
			int access = 0;
			int lastServer = 1;

			try (Connection con = DatabaseFactory.getConnection(); PreparedStatement statement = con.prepareStatement("SELECT password, access_level, lastServer FROM accounts WHERE login=?"))
			{
				statement.setString(1, user);

				try (ResultSet rset = statement.executeQuery())
				{
					if (rset.next())
					{
						expected = Base64.decode(rset.getString("password"));
						access = rset.getInt("access_level");
						lastServer = Math.max(rset.getInt("lastServer"), 1);
					}
				}
			}

			// if account doesnt exists
			if (expected == null)
			{
				if (MainConfig.AUTO_CREATE_ACCOUNTS)
				{
					if ((user.length() >= 2) && (user.length() <= 14))
					{
						try (Connection con = DatabaseFactory.getConnection(); PreparedStatement statement = con.prepareStatement("INSERT INTO accounts (login,password,lastactive,access_level,newbie) values(?,?,?,?,?)"))
						{
							statement.setString(1, user);
							statement.setString(2, Base64.encodeBytes(hash));
							statement.setLong(3, System.currentTimeMillis());
							statement.setInt(4, 0);
							statement.setInt(5, 0);
							statement.execute();
						}

						if (MainConfig.LOG_LOGIN_CONTROLLER)
							authLog.info("'" + user + "' " + address.getHostAddress() + " - OK : AccountCreate", "loginlog");

						_log.info("New account has been created for " + user + ".");
						return true;
					}

					if (MainConfig.LOG_LOGIN_CONTROLLER)
						authLog.info("'" + user + "' " + address.getHostAddress() + " - ERR : ErrCreatingACC", "loginlog");

					_log.warn("Invalid username creation/use attempt: " + user + ".");
					return false;
				}

				if (MainConfig.LOG_LOGIN_CONTROLLER)
					authLog.info("'" + user + "' " + address.getHostAddress() + " - ERR : AccountMissing", "loginlog");

				_log.warn("Account missing for user " + user);

				FailedLoginAttempt failedAttempt = _hackProtection.get(address);
				int failedCount;

				if (failedAttempt == null)
				{
					_hackProtection.put(address, new FailedLoginAttempt(address, password));
					failedCount = 1;
				}
				else
				{
					failedAttempt.increaseCounter();
					failedCount = failedAttempt.getCount();
				}

				if (failedCount >= MainConfig.LOGIN_TRY_BEFORE_BAN)
				{
					_log.info("Banning '" + address.getHostAddress() + "' for " + MainConfig.LOGIN_BLOCK_AFTER_BAN + " seconds due to " + failedCount + " invalid user name attempts");
					this.addBanForAddress(address, MainConfig.LOGIN_BLOCK_AFTER_BAN * 1000);
				}
				return false;
			}

			// is this account banned?
			if (access < 0)
			{
				if (MainConfig.LOG_LOGIN_CONTROLLER)
					authLog.info("'" + user + "' " + address.getHostAddress() + " - ERR : AccountBanned", "loginlog");

				client.setAccessLevel(access);
				return false;
			}

			// check password hash
			ok = true;
			for (int i = 0; i < expected.length; i++)
			{
				if (hash[i] != expected[i])
				{
					ok = false;
					break;
				}
			}

			if (ok)
			{
				client.setAccessLevel(access);
				client.setLastServer(lastServer);

				try (Connection con = DatabaseFactory.getConnection(); PreparedStatement statement = con.prepareStatement("UPDATE accounts SET lastactive=? WHERE login=?"))
				{
					statement.setLong(1, System.currentTimeMillis());
					statement.setString(2, user);
					statement.execute();
				}
			}
		}
		catch (Exception e)
		{
			_log.warn("Could not check password:" + e.getMessage(), e);
			ok = false;
		}

		if (!ok)
		{
			if (MainConfig.LOG_LOGIN_CONTROLLER)
				authLog.info("'" + user + "' " + address.getHostAddress() + " - ERR : LoginFailed", "loginlog");

			FailedLoginAttempt failedAttempt = _hackProtection.get(address);
			int failedCount;
			if (failedAttempt == null)
			{
				_hackProtection.put(address, new FailedLoginAttempt(address, password));
				failedCount = 1;
			}
			else
			{
				failedAttempt.increaseCounter(password);
				failedCount = failedAttempt.getCount();
			}

			if (failedCount >= MainConfig.LOGIN_TRY_BEFORE_BAN)
			{
				_log.info("Banning '" + address.getHostAddress() + "' for " + MainConfig.LOGIN_BLOCK_AFTER_BAN + " seconds due to " + failedCount + " invalid user/pass attempts");
				this.addBanForAddress(address, MainConfig.LOGIN_BLOCK_AFTER_BAN * 1000);
			}
		}
		else
		{
			_hackProtection.remove(address);
			if (MainConfig.LOG_LOGIN_CONTROLLER)
				authLog.info("'" + user + "' " + address.getHostAddress() + " - OK : LoginOk", "loginlog");
		}
		return ok;
	}

	class FailedLoginAttempt
	{
		private int _count;
		private long _lastAttempTime;
		private String _lastPassword;

		public FailedLoginAttempt(InetAddress address, String lastPassword)
		{
			_count = 1;
			_lastAttempTime = System.currentTimeMillis();
			_lastPassword = lastPassword;
		}

		public void increaseCounter(String password)
		{
			if (!_lastPassword.equals(password))
			{
				// check if theres a long time since last wrong try
				if (System.currentTimeMillis() - _lastAttempTime < 300 * 1000)
					_count++;
				// restart the status
				else
					_count = 1;

				_lastPassword = password;
				_lastAttempTime = System.currentTimeMillis();
			}
			else
				// trying the same password is not brute force
				_lastAttempTime = System.currentTimeMillis();
		}

		public int getCount()
		{
			return _count;
		}

		public void increaseCounter()
		{
			_count++;
		}
	}

	class BanInfo
	{
		private final InetAddress _ipAddress;
		private final long _expiration;

		public BanInfo(InetAddress ipAddress, long expiration)
		{
			_ipAddress = ipAddress;
			_expiration = expiration;
		}

		public InetAddress getAddress()
		{
			return _ipAddress;
		}

		public boolean hasExpired()
		{
			return System.currentTimeMillis() > _expiration && _expiration > 0;
		}
	}

	class PurgeThread extends Thread
	{
		public PurgeThread()
		{
			setName("PurgeThread");
		}

		@Override
		public void run()
		{
			while (!isInterrupted())
			{
				for (L2LoginClient client : _loginServerClients.values())
				{
					if (client == null)
						continue;

					if ((client.getConnectionStartTime() + LOGIN_TIMEOUT) < System.currentTimeMillis())
						client.close(LoginFailReason.REASON_ACCESS_FAILED);
				}

				try
				{
					Thread.sleep(LOGIN_TIMEOUT / 2);
				}
				catch (InterruptedException e)
				{
					return;
				}
			}
		}
	}
}
