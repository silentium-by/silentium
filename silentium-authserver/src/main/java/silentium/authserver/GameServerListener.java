/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.authserver;

import javolution.util.FastList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import silentium.authserver.configs.MainConfig;

import java.io.IOException;
import java.net.Socket;
import java.util.List;

/**
 * @author KenM
 */
public class GameServerListener extends FloodProtectedListener {
	private static final Logger _log = LoggerFactory.getLogger(GameServerListener.class.getName());
	private static final List<GameServerThread> _gameServers = new FastList<>();

	public GameServerListener() throws IOException {
		super(MainConfig.GAME_SERVER_LOGIN_HOST, MainConfig.GAME_SERVER_LOGIN_PORT);
	}

	/**
	 * @see FloodProtectedListener#addClient(java.net.Socket)
	 */
	@Override
	public void addClient(final Socket s) {
		if (MainConfig.DEBUG) {
			_log.info("Received gameserver connection from: " + s.getInetAddress().getHostAddress());
		}
		final GameServerThread gst = new GameServerThread(s);
		_gameServers.add(gst);
	}

	public void removeGameServer(final GameServerThread gst) {
		_gameServers.remove(gst);
	}
}
