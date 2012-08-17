/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.board.Manager;

import silentium.gameserver.model.actor.instance.L2PcInstance;

public class RegionBBSManager extends BaseBBSManager
{
	protected RegionBBSManager()
	{
	}

	public static RegionBBSManager getInstance()
	{
		return SingletonHolder._instance;
	}

	@Override
	public void parseCmd(String command, L2PcInstance activeChar)
	{
		if (command.equals("_bbsloc"))
		{
			// showOldCommunity(activeChar, 1);
		}
		else
			separateAndSend("<html><body><br><br><center>The command: " + command + " isn't implemented.</center></body></html>", activeChar);
	}

	@Override
	public void parseWrite(String ar1, String ar2, String ar3, String ar4, String ar5, L2PcInstance activeChar)
	{
		separateAndSend("<html><body><br><br><center>The command: " + ar1 + " isn't implemented yet</center></body></html>", activeChar);
	}

	private static class SingletonHolder
	{
		protected static final RegionBBSManager _instance = new RegionBBSManager();
	}
}