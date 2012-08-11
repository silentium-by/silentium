/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.handlers.admin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import silentium.gameserver.handler.IAdminCommandHandler;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.olympiad.Olympiad;

public class AdminOlympiad implements IAdminCommandHandler
{
	private static Logger _log = LoggerFactory.getLogger(AdminOlympiad.class.getName());
	private static final String[] ADMIN_COMMANDS = { "admin_endoly", "admin_manualhero", "admin_saveoly", "admin_sethero", "admin_setnoble" };

	@Override
	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		if (command.startsWith("admin_endoly"))
		{
			try
			{
				Olympiad.getInstance().manualSelectHeroes();
			}
			catch (Exception e)
			{
				_log.warn("An error occured while ending olympiad: " + e);
			}
			activeChar.sendMessage("Heroes have been formed.");
		}
		else if (command.startsWith("admin_manualhero") || command.startsWith("admin_sethero"))
		{
			L2PcInstance target = null;
			if (activeChar.getTarget() instanceof L2PcInstance)
				target = (L2PcInstance) activeChar.getTarget();
			else
				target = activeChar;

			target.setHero(!target.isHero());
			target.broadcastUserInfo();
			activeChar.sendMessage("You have modified " + target.getName() + "'s hero status.");
		}
		else if (command.startsWith("admin_saveoly"))
		{
			Olympiad.getInstance().saveOlympiadStatus();
			activeChar.sendMessage("Olympiad stats have been saved.");
		}
		else if (command.startsWith("admin_setnoble"))
		{
			L2PcInstance target = null;
			if (activeChar.getTarget() instanceof L2PcInstance)
				target = (L2PcInstance) activeChar.getTarget();
			else
				target = activeChar;

			target.setNoble(!target.isNoble(), true);
			activeChar.sendMessage("You have modified " + target.getName() + "'s noble status.");
		}
		return true;
	}

	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
