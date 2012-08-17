/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.authserver;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import silentium.authserver.configs.MainConfig;
import silentium.commons.ServerType;
import silentium.commons.database.DatabaseFactory;
import silentium.commons.database.DatabaseTuning;
import silentium.commons.network.mmocore.SelectorConfig;
import silentium.commons.network.mmocore.SelectorThread;
import silentium.commons.utils.Util;

/**
 * @author KenM
 */
public class L2LoginServer
{
	public static final int PROTOCOL_REV = 0x0102;

	private static L2LoginServer loginServer;
	private final Logger _log = LoggerFactory.getLogger(L2LoginServer.class.getName());
	private GameServerListener _gameServerListener;
	private SelectorThread<L2LoginClient> _selectorThread;

	public static void main(String[] args) throws Exception
	{
		loginServer = new L2LoginServer();
	}

	public static L2LoginServer getInstance()
	{
		return loginServer;
	}

	public L2LoginServer() throws Exception
	{
		ServerType.serverMode = ServerType.MODE_LOGINSERVER;

		// Create log folder
		final File logFolder = new File("./log");
		logFolder.mkdir();

		Util.printSection("L2J");

		// Initialize config
		MainConfig.load();

		Util.printSection("Database");
		DatabaseFactory.init();
		DatabaseTuning.start();

		Util.printSection("LoginController");
		LoginController.load();
		GameServerTable.getInstance();

		Util.printSection("Ban List");
		loadBanFile();

		Util.printSection("IP, Ports & Socket infos");
		InetAddress bindAddress = null;
		if (!MainConfig.LOGIN_BIND_ADDRESS.equals("*"))
		{
			try
			{
				bindAddress = InetAddress.getByName(MainConfig.LOGIN_BIND_ADDRESS);
			}
			catch (UnknownHostException e1)
			{
				_log.error("WARNING: The LoginServer bind address is invalid, using all available IPs. Reason: " + e1.getMessage());
				if (MainConfig.DEVELOPER)
					e1.printStackTrace();
			}
		}

		final SelectorConfig sc = new SelectorConfig();
		sc.MAX_READ_PER_PASS = MainConfig.MMO_MAX_READ_PER_PASS;
		sc.MAX_SEND_PER_PASS = MainConfig.MMO_MAX_SEND_PER_PASS;
		sc.SLEEP_TIME = MainConfig.MMO_SELECTOR_SLEEP_TIME;
		sc.HELPER_BUFFER_COUNT = MainConfig.MMO_HELPER_BUFFER_COUNT;

		final L2LoginPacketHandler lph = new L2LoginPacketHandler();
		final SelectorHelper sh = new SelectorHelper();
		try
		{
			_selectorThread = new SelectorThread<>(sc, sh, lph, sh, sh);
		}
		catch (IOException e)
		{
			_log.error("FATAL: Failed to open selector. Reason: " + e.getMessage());
			if (MainConfig.DEVELOPER)
				e.printStackTrace();

			System.exit(1);
		}

		try
		{
			_gameServerListener = new GameServerListener();
			_gameServerListener.start();
			_log.info("Listening for gameservers on " + MainConfig.GAME_SERVER_LOGIN_HOST + ":" + MainConfig.GAME_SERVER_LOGIN_PORT);
		}
		catch (IOException e)
		{
			_log.error("FATAL: Failed to start the gameserver listener. Reason: " + e.getMessage());
			if (MainConfig.DEVELOPER)
				e.printStackTrace();

			System.exit(1);
		}

		try
		{
			_selectorThread.openServerSocket(bindAddress, MainConfig.PORT_LOGIN);
		}
		catch (IOException e)
		{
			_log.error("FATAL: Failed to open server socket. Reason: " + e.getMessage());
			if (MainConfig.DEVELOPER)
				e.printStackTrace();

			System.exit(1);
		}
		_selectorThread.start();
		_log.info("Loginserver ready on " + (bindAddress == null ? "*" : bindAddress.getHostAddress()) + ":" + MainConfig.PORT_LOGIN);

		Util.printSection("Waiting for gameserver answer");
	}

	public GameServerListener getGameServerListener()
	{
		return _gameServerListener;
	}

	private void loadBanFile()
	{
		File banFile = new File("config/banned_ip.cfg");
		if (banFile.exists() && banFile.isFile())
		{
			LineNumberReader reader = null;
			try
			{
				String line;
				String[] parts;
				reader = new LineNumberReader(new FileReader(banFile));

				while ((line = reader.readLine()) != null)
				{
					line = line.trim();
					// check if this line isnt a comment line
					if (line.length() > 0 && line.charAt(0) != '#')
					{
						// split comments if any
						parts = line.split("#");

						// discard comments in the line, if any
						line = parts[0];
						parts = line.split(" ");

						String address = parts[0];
						long duration = 0;

						if (parts.length > 1)
						{
							try
							{
								duration = Long.parseLong(parts[1]);
							}
							catch (NumberFormatException e)
							{
								_log.warn("Skipped: Incorrect ban duration (" + parts[1] + ") on banned_ip.cfg. Line: " + reader.getLineNumber());
								continue;
							}
						}

						try
						{
							LoginController.getInstance().addBanForAddress(address, duration);
						}
						catch (UnknownHostException e)
						{
							_log.warn("Skipped: Invalid address (" + parts[0] + ") on banned_ip.cfg. Line: " + reader.getLineNumber());
						}
					}
				}
			}
			catch (IOException e)
			{
				_log.warn("Error while reading banned_ip.cfg. Details: " + e.getMessage());
				if (MainConfig.DEVELOPER)
					e.printStackTrace();
			}
			_log.info("Loaded " + LoginController.getInstance().getBannedIps().size() + " IP(s) from banned_ip.cfg.");
		}
		else
			_log.warn("banned_ip.cfg is missing. Ban listing is skipped.");
	}

	public void shutdown(boolean restart)
	{
		Runtime.getRuntime().exit(restart ? 2 : 0);
	}
}
