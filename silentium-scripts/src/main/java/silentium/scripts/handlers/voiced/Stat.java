/*
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2, or (at your option) any later version. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU
 * General Public License along with this program; if not, write to the Free Software Foundation, Inc., 59 Temple Place - Suite
 * 330, Boston, MA 02111-1307, USA. http://www.gnu.org/copyleft/gpl.html
 */
package silentium.scripts.handlers.voiced;

import silentium.gameserver.handler.IVoicedCommandHandler;
import silentium.gameserver.model.actor.instance.L2PcInstance;

/**
 * @author Kirito
 */
public class Stat implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS = { "stat" };

	@Override
	public boolean useVoicedCommand(String command, L2PcInstance activeChar, String target)
	{
		if (command.equalsIgnoreCase("stat"))
		{
			if (activeChar.getTarget() == null)
			{
				activeChar.sendMessage("Select target.");
				return false;
			}
			if (!(activeChar.getTarget() instanceof L2PcInstance))
			{
				activeChar.sendMessage("You can get information only about the player.");

				return false;
			}

			L2PcInstance targetp = (L2PcInstance) activeChar.getTarget();
			activeChar.sendMessage("Name: " + targetp.getName());
			activeChar.sendMessage("Level: " + targetp.getLevel());
			activeChar.sendMessage("Adena: " + targetp.getAdena());
			activeChar.sendMessage("PvP Kills: " + targetp.getPvpKills());
			activeChar.sendMessage("PvP Flags: " + targetp.getPvpFlag());
			activeChar.sendMessage("PK Kills: " + targetp.getPkKills());
			activeChar.sendMessage("CP, HP, MP: " + targetp.getMaxCp() + ", " + targetp.getMaxHp() + ", " + targetp.getMaxMp());
		}
		return true;
	}

	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
}