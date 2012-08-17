/*
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation; either version 2, or (at your option) any later version. This program is distributed in the hope that it will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details. You should have received a copy of the GNU General Public License along with this program; if not,
 * write to the Free Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA. http://www.gnu.org/copyleft/gpl.html
 */
package silentium.scripts.handlers.voiced;

import silentium.gameserver.handler.IVoicedCommandHandler;
import silentium.gameserver.model.L2World;
import silentium.gameserver.model.actor.instance.L2PcInstance;

/**
 * @author horr1f1k
 */
public class Online implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS = { "online" };

	@Override
	public boolean useVoicedCommand(String command, L2PcInstance player, String target)
	{
		if (command.equalsIgnoreCase("online"))
		{
			player.sendMessage("Сейчас в игре " + L2World.getInstance().getAllPlayers().size() + " человек.");
		}
		return true;

	}

	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
}