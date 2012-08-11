/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.handlers.admin;

import silentium.commons.database.DatabaseFactory;
import silentium.gameserver.Announcements;
import silentium.gameserver.LoginServerThread;
import silentium.gameserver.configs.CustomConfig;
import silentium.gameserver.configs.MainConfig;
import silentium.gameserver.handler.IAdminCommandHandler;
import silentium.gameserver.model.L2World;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.network.SystemMessageId;
import silentium.gameserver.utils.GMAudit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.StringTokenizer;

/**
 * This class handles following admin commands: - ban_acc <account_name> = changes account access level to -100 and logs him off.
 * If no account is specified target's account is used. - ban_char <char_name> = changes a characters access level to -100 and
 * logs him off. If no character is specified target is used. - ban_chat <char_name> <duration> = chat bans a character for the
 * specified duration. If no name is specified the target is chat banned indefinitely. - unban_acc <account_name> = changes
 * account access level to 0. - unban_char <char_name> = changes specified characters access level to 0. - unban_chat <char_name>
 * = lifts chat ban from specified player. If no player name is specified current target is used. - jail charname [penalty_time] =
 * jails character. Time specified in minutes. For ever if no time is specified. - unjail charname = Unjails player, teleport him
 * to Floran.
 */
public class AdminBan implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS = { "admin_ban", "admin_ban_acc", "admin_ban_char", "admin_ban_chat", "admin_unban", "admin_unban_acc", "admin_unban_char", "admin_unban_chat", "admin_jail", "admin_unjail" };

	@Override
	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		StringTokenizer st = new StringTokenizer(command);
		st.nextToken();
		String player = "";
		int duration = -1;
		L2PcInstance targetPlayer = null;

		// One parameter, player name
		if (st.hasMoreTokens())
		{
			player = st.nextToken();
			targetPlayer = L2World.getInstance().getPlayer(player);

			// Second parameter, duration
			if (st.hasMoreTokens())
			{
				try
				{
					duration = Integer.parseInt(st.nextToken());
				}
				catch (NumberFormatException nfe)
				{
					activeChar.sendMessage("Invalid number format used: " + nfe);
					return false;
				}
			}
		}
		else
		{
			// If there is no name, select target
			if (activeChar.getTarget() != null && activeChar.getTarget() instanceof L2PcInstance)
				targetPlayer = (L2PcInstance) activeChar.getTarget();
		}

		// Can't ban yourself
		if (targetPlayer != null && targetPlayer.equals(activeChar))
		{
			activeChar.sendPacket(SystemMessageId.CANNOT_USE_ON_YOURSELF);
			return false;
		}

		if (command.startsWith("admin_ban ") || command.equalsIgnoreCase("admin_ban"))
		{
			activeChar.sendMessage("Available ban commands: //ban_acc, //ban_char, //ban_chat");
			return false;
		}
		else if (command.startsWith("admin_ban_acc"))
		{
			if (targetPlayer == null && player.equals(""))
			{
				activeChar.sendMessage("Usage: //ban_acc <account_name> (if none, target char's account gets banned).");
				return false;
			}
			else if (targetPlayer == null)
			{
				LoginServerThread.getInstance().sendAccessLevel(player, -100);
				if (CustomConfig.ANNOUNCE_BAN_ACC)
					Announcements.announceToAll("Ban request sent for account " + player + ".");
				else
					activeChar.sendMessage("Ban request sent for account " + player + ".");
				auditAction(command, activeChar, player);
			}
			else
			{
				targetPlayer.setPunishLevel(L2PcInstance.PunishLevel.ACC, 0);
				if (CustomConfig.ANNOUNCE_BAN_ACC)
					Announcements.announceToAll(targetPlayer.getAccountName() + " account is now banned.");
				else
					activeChar.sendMessage(targetPlayer.getAccountName() + " account is now banned.");
				auditAction(command, activeChar, targetPlayer.getAccountName());
			}
		}
		else if (command.startsWith("admin_ban_char"))
		{
			if (targetPlayer == null && player.equals(""))
			{
				activeChar.sendMessage("Usage: //ban_char <char_name> (if none, target char is banned)");
				return false;
			}

			auditAction(command, activeChar, (targetPlayer == null ? player : targetPlayer.getName()));
			return changeCharAccessLevel(targetPlayer, player, activeChar, -100);
		}
		else if (command.startsWith("admin_ban_chat"))
		{
			if (targetPlayer == null && player.equals(""))
			{
				activeChar.sendMessage("Usage: //ban_chat <char_name> [penalty_minutes]");
				return false;
			}

			if (targetPlayer != null)
			{
				if (targetPlayer.getPunishLevel().value() > 0)
				{
					activeChar.sendMessage(targetPlayer.getName() + " is already jailed or banned.");
					return false;
				}

				String banLengthStr = "";
				targetPlayer.setPunishLevel(L2PcInstance.PunishLevel.CHAT, duration);

				if (duration > 0)
					banLengthStr = " for " + duration + " minutes";

				if (CustomConfig.ANNOUNCE_BAN_CHAT)
					Announcements.announceToAll(targetPlayer.getName() + " is now chat banned" + banLengthStr + ".");
				else
					activeChar.sendMessage(targetPlayer.getName() + " is now chat banned" + banLengthStr + ".");
				auditAction(command, activeChar, targetPlayer.getName());
			}
			else
			{
				banChatOfflinePlayer(activeChar, player, duration, true);
				auditAction(command, activeChar, player);
			}
		}
		else if (command.startsWith("admin_unban ") || command.equalsIgnoreCase("admin_unban"))
		{
			activeChar.sendMessage("Available unban commands: //unban_acc, //unban_char, //unban_chat");
			return false;
		}
		else if (command.startsWith("admin_unban_acc"))
		{
			if (targetPlayer != null)
			{
				activeChar.sendMessage(targetPlayer.getName() + " is currently online so mustn't be banned.");
				return false;
			}
			else if (!player.equals(""))
			{
				LoginServerThread.getInstance().sendAccessLevel(player, 0);
				if (CustomConfig.ANNOUNCE_UNBAN_ACC)
					Announcements.announceToAll("Unban request sent for account " + player + ".");
				else
					activeChar.sendMessage("Unban request sent for account " + player + ".");
				auditAction(command, activeChar, player);
			}
			else
			{
				activeChar.sendMessage("Usage: //unban_acc <account_name>");
				return false;
			}
		}
		else if (command.startsWith("admin_unban_char"))
		{
			if (targetPlayer == null && player.equals(""))
			{
				activeChar.sendMessage("Usage: //unban_char <char_name>");
				return false;
			}
			else if (targetPlayer != null)
			{
				activeChar.sendMessage(targetPlayer.getName() + " is currently online so mustn't be banned.");
				return false;
			}
			else
			{
				auditAction(command, activeChar, player);
				return changeCharAccessLevel(null, player, activeChar, 0);
			}
		}
		else if (command.startsWith("admin_unban_chat"))
		{
			if (targetPlayer == null && player.equals(""))
			{
				activeChar.sendMessage("Usage: //unban_chat <char_name>");
				return false;
			}

			if (targetPlayer != null)
			{
				if (targetPlayer.isChatBanned())
				{
					targetPlayer.setPunishLevel(L2PcInstance.PunishLevel.NONE, 0);
					if (CustomConfig.ANNOUNCE_UNBAN_CHAT)
						Announcements.announceToAll(targetPlayer.getName() + "'s chat ban has been lifted.");
					else
						activeChar.sendMessage(targetPlayer.getName() + "'s chat ban has been lifted.");
					auditAction(command, activeChar, targetPlayer.getName());
				}
				else
					activeChar.sendMessage(targetPlayer.getName() + " isn't currently chat banned.");
			}
			else
			{
				banChatOfflinePlayer(activeChar, player, 0, false);
				auditAction(command, activeChar, player);
			}
		}
		else if (command.startsWith("admin_jail"))
		{
			if (targetPlayer == null && player.equals(""))
			{
				activeChar.sendMessage("Usage: //jail <charname> [penalty_minutes] (if no name is given, selected target is jailed forever).");
				return false;
			}

			if (targetPlayer != null)
			{
				targetPlayer.setPunishLevel(L2PcInstance.PunishLevel.JAIL, duration);
				if (CustomConfig.ANNOUNCE_JAIL)
					Announcements.announceToAll(targetPlayer.getName() + " have been jailed for " + (duration > 0 ? duration + " minutes." : "ever !"));
				else
					activeChar.sendMessage(targetPlayer.getName() + " have been jailed for " + (duration > 0 ? duration + " minutes." : "ever !"));
				auditAction(command, activeChar, targetPlayer.getName());
			}
			else
			{
				jailOfflinePlayer(activeChar, player, duration);
				auditAction(command, activeChar, player);
			}
		}
		else if (command.startsWith("admin_unjail"))
		{
			if (targetPlayer == null && player.equals(""))
			{
				activeChar.sendMessage("Usage: //unjail <charname> (If no name is given target is used).");
				return false;
			}
			else if (targetPlayer != null)
			{
				targetPlayer.setPunishLevel(L2PcInstance.PunishLevel.NONE, 0);
				if (CustomConfig.ANNOUNCE_UNJAIL)
					Announcements.announceToAll(targetPlayer.getName() + " have been unjailed.");
				else
					activeChar.sendMessage(targetPlayer.getName() + " have been unjailed.");
				auditAction(command, activeChar, targetPlayer.getName());
			}
			else
			{
				unjailOfflinePlayer(activeChar, player);
				auditAction(command, activeChar, player);
			}
		}
		return true;
	}

	private static void auditAction(String fullCommand, L2PcInstance activeChar, String target)
	{
		if (!MainConfig.GMAUDIT)
			return;

		String[] command = fullCommand.split(" ");

		GMAudit.auditGMAction(activeChar.getName() + " [" + activeChar.getObjectId() + "]", command[0], (target.equals("") ? "no-target" : target), (command.length > 2 ? command[2] : ""));
	}

	private static void banChatOfflinePlayer(L2PcInstance activeChar, String name, int delay, boolean ban)
	{
		int level = 0;
		long value = 0;

		if (ban)
		{
			level = L2PcInstance.PunishLevel.CHAT.value();
			value = (delay > 0 ? delay * 60000L : 60000);
		}
		else
		{
			level = L2PcInstance.PunishLevel.NONE.value();
			value = 0;
		}

		try (Connection con = DatabaseFactory.getConnection())
		{
			PreparedStatement statement = con.prepareStatement("UPDATE characters SET punish_level=?, punish_timer=? WHERE char_name=?");
			statement.setInt(1, level);
			statement.setLong(2, value);
			statement.setString(3, name);

			statement.execute();
			int count = statement.getUpdateCount();
			statement.close();

			if (count == 0)
				activeChar.sendMessage("Character isn't found.");
			else if (ban)
				activeChar.sendMessage(name + " is chat banned for " + (delay > 0 ? delay + " minutes." : "ever !"));
			else
				activeChar.sendMessage(name + "'s chat ban have been lifted.");
		}
		catch (SQLException se)
		{
			activeChar.sendMessage("SQLException while chat-banning player");

			se.printStackTrace();
		}
	}

	private static void jailOfflinePlayer(L2PcInstance activeChar, String name, int delay)
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			PreparedStatement statement = con.prepareStatement("UPDATE characters SET x=?, y=?, z=?, punish_level=?, punish_timer=? WHERE char_name=?");
			statement.setInt(1, -114356);
			statement.setInt(2, -249645);
			statement.setInt(3, -2984);
			statement.setInt(4, L2PcInstance.PunishLevel.JAIL.value());
			statement.setLong(5, (delay > 0 ? delay * 60000L : 0));
			statement.setString(6, name);

			statement.execute();
			int count = statement.getUpdateCount();
			statement.close();

			if (count == 0)
				activeChar.sendMessage("Character not found!");
			else
				activeChar.sendMessage(name + " have been jailed for " + (delay > 0 ? delay + " minutes." : "ever!"));
		}
		catch (SQLException se)
		{
			activeChar.sendMessage("SQLException while jailing player");

			se.printStackTrace();
		}
	}

	private static void unjailOfflinePlayer(L2PcInstance activeChar, String name)
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			PreparedStatement statement = con.prepareStatement("UPDATE characters SET x=?, y=?, z=?, punish_level=?, punish_timer=? WHERE char_name=?");
			statement.setInt(1, 17836);
			statement.setInt(2, 170178);
			statement.setInt(3, -3507);
			statement.setInt(4, 0);
			statement.setLong(5, 0);
			statement.setString(6, name);
			statement.execute();
			int count = statement.getUpdateCount();
			statement.close();
			if (count == 0)
				activeChar.sendMessage("Character isn't found.");
			else
				activeChar.sendMessage(name + " have been unjailed.");
		}
		catch (SQLException se)
		{
			activeChar.sendMessage("SQLException while jailing player");

			se.printStackTrace();
		}
	}

	private static boolean changeCharAccessLevel(L2PcInstance targetPlayer, String player, L2PcInstance activeChar, int lvl)
	{
		if (targetPlayer != null)
		{
			targetPlayer.setAccessLevel(lvl);
			targetPlayer.logout();
			activeChar.sendMessage(targetPlayer.getName() + " has been banned.");
		}
		else
		{
			try (Connection con = DatabaseFactory.getConnection())
			{
				PreparedStatement statement = con.prepareStatement("UPDATE characters SET accesslevel=? WHERE char_name=?");
				statement.setInt(1, lvl);
				statement.setString(2, player);
				statement.execute();
				int count = statement.getUpdateCount();
				statement.close();

				if (count == 0)
				{
					activeChar.sendMessage("Character not found or access level unaltered.");
					return false;
				}

				activeChar.sendMessage(player + " now has an access level of " + lvl + ".");
			}
			catch (SQLException se)
			{
				activeChar.sendMessage("SQLException while changing character's access level");

				se.printStackTrace();

				return false;
			}
		}
		return true;
	}

	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}