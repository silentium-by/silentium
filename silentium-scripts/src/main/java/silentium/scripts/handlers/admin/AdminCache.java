/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.handlers.admin;

import java.io.File;

import silentium.gameserver.configs.MainConfig;
import silentium.gameserver.data.crest.CrestCache;
import silentium.gameserver.data.html.HtmCache;
import silentium.gameserver.handler.IAdminCommandHandler;
import silentium.gameserver.model.actor.instance.L2PcInstance;

/**
 * @author Layanere
 */
public class AdminCache implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS = { "admin_reload_cache_path", "admin_reload_cache_file", "admin_fix_cache_crest" };

	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}

	@Override
	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		if (command.startsWith("admin_reload_cache_path "))
		{
			try
			{
				String path = command.split(" ")[1];
				HtmCache.getInstance().reloadPath(new File(MainConfig.DATAPACK_ROOT, path));
				activeChar.sendMessage("HTM paths' cache have been reloaded.");
			}
			catch (Exception e)
			{
				activeChar.sendMessage("Usage: //reload_cache_path <path>");
			}
		}
		else if (command.startsWith("admin_reload_cache_file "))
		{
			try
			{
				String path = command.split(" ")[1];
				if (HtmCache.getInstance().loadFile(new File(MainConfig.DATAPACK_ROOT, path)) != null)
					activeChar.sendMessage("Cache[HTML]: requested file was loaded.");
				else
					activeChar.sendMessage("Cache[HTML]: requested file couldn't be loaded.");
			}
			catch (Exception e)
			{
				activeChar.sendMessage("Usage: //reload_cache_file <relative_path/file>");
			}
		}
		else if (command.startsWith("admin_fix_cache_crest"))
		{
			CrestCache.convertOldPledgeFiles();
			activeChar.sendMessage("Cache[Crest]: crests have been fixed.");
		}
		return true;
	}
}