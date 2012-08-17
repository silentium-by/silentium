/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.handlers.admin;

import silentium.gameserver.handler.IAdminCommandHandler;
import silentium.gameserver.model.L2Object;
import silentium.gameserver.model.L2World;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.network.SystemMessageId;
import silentium.gameserver.network.clientpackets.Say2;
import silentium.gameserver.network.serverpackets.CreatureSay;
import silentium.gameserver.tables.GmListTable;

/**
 * This class handles following admin commands:<br>
 * <br>
 * - gmchat : sends text to all online GM's<br>
 * - gmchat_menu : same as gmchat, but displays the admin panel after chat<br>
 * - snoop : spy the targeted player
 */
public class AdminGmChat implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS = { "admin_gmchat", "admin_gmchat_menu", "admin_snoop" };

	@Override
	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		if (command.startsWith("admin_gmchat"))
			handleGmChat(command, activeChar);
		else if (command.startsWith("admin_snoop"))
			snoop(command, activeChar);

		if (command.startsWith("admin_gmchat_menu"))
			AdminHelpPage.showHelpPage(activeChar, "main_menu.htm");

		return true;
	}

	/**
	 * @param command
	 * @param activeChar
	 */
	private static void snoop(String command, L2PcInstance activeChar)
	{
		L2Object target = null;
		if (command.length() > 12)
			target = L2World.getInstance().getPlayer(command.substring(12));

		if (target == null)
			target = activeChar.getTarget();

		if (target == null)
		{
			activeChar.sendPacket(SystemMessageId.SELECT_TARGET);
			return;
		}

		if (!(target instanceof L2PcInstance))
		{
			activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
			return;
		}

		L2PcInstance player = (L2PcInstance) target;
		player.addSnooper(activeChar);
		activeChar.addSnooped(player);
	}

	/**
	 * @param command
	 * @param activeChar
	 */
	private static void handleGmChat(String command, L2PcInstance activeChar)
	{
		try
		{
			int offset = 0;
			String text;

			if (command.startsWith("admin_gmchat_menu"))
				offset = 18;
			else
				offset = 13;

			text = command.substring(offset);
			CreatureSay cs = new CreatureSay(0, Say2.ALLIANCE, activeChar.getName(), text);
			GmListTable.broadcastToGMs(cs);
		}
		catch (StringIndexOutOfBoundsException e)
		{
			// empty message.. ignore
		}
	}

	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}