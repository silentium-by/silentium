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
import silentium.authserver.network.clientpackets.ClientBasePacket;

/**
 * @author -Wooden-
 * @rework Ashe
 */
public class ChangeAccessLevel extends ClientBasePacket {
	protected static final Logger _log = LoggerFactory.getLogger(ChangeAccessLevel.class.getName());

	/**
	 * @param decrypt
	 * @param server
	 */
	public ChangeAccessLevel(final byte[] decrypt, GameServerThread server)
	{
		super(decrypt);
		int level = readD();
		String account = readS();
		LoginController.getInstance().setAccountAccessLevel(account, level);
		_log.info("Changed " + account + " access level to " + level);
	}
}