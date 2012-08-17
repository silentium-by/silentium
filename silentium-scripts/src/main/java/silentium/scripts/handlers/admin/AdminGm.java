/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.handlers.admin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import silentium.gameserver.data.xml.AccessLevelsData;
import silentium.gameserver.handler.IAdminCommandHandler;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.tables.GmListTable;

/**
 * This class handles following admin commands: - gm = turns gm mode on/off
 */
public class AdminGm implements IAdminCommandHandler
{
	private static Logger _log = LoggerFactory.getLogger(AdminGm.class.getName());
	private static final String[] ADMIN_COMMANDS = { "admin_gm" };

	@Override
	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		if (command.equals("admin_gm"))
			handleGm(activeChar);

		return true;
	}

	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}

	private static void handleGm(L2PcInstance activeChar)
	{
		if (activeChar.isGM())
		{
			GmListTable.getInstance().deleteGm(activeChar);
			activeChar.setAccessLevel(AccessLevelsData._userAccessLevelNum);
			activeChar.sendMessage("You no longer have GM status.");

			_log.info("GM: " + activeChar.getName() + " (" + activeChar.getObjectId() + ") turned his GM status off" + ".");
		}
	}
}
