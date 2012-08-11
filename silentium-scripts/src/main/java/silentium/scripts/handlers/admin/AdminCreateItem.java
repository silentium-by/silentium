/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.handlers.admin;

import java.util.Collection;
import java.util.StringTokenizer;

import silentium.gameserver.handler.IAdminCommandHandler;
import silentium.gameserver.model.L2World;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.tables.ItemTable;
import silentium.gameserver.templates.item.L2Item;

/**
 * This class handles following admin commands:<br>
 * <br>
 * - itemcreate = show "item creation" menu<br>
 * - create_item = creates num items with respective id, if num is not specified, assumes 1.<br>
 * - create_coin = creates currency, using the choice box or typing good IDs.<br>
 * - reward_all = reward all online players with items.
 */
public class AdminCreateItem implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS = { "admin_itemcreate", "admin_create_item", "admin_create_coin", "admin_reward_all" };

	@Override
	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		if (command.equals("admin_itemcreate"))
		{
			AdminHelpPage.showHelpPage(activeChar, "itemcreation.htm");
		}
		else if (command.startsWith("admin_create_item"))
		{
			L2PcInstance target;
			if (activeChar.getTarget() != null && activeChar.getTarget() instanceof L2PcInstance)
				target = (L2PcInstance) activeChar.getTarget();
			else
				target = activeChar;

			try
			{
				String val = command.substring(17);
				StringTokenizer st = new StringTokenizer(val);
				if (st.countTokens() == 3)
				{
					String id = st.nextToken();
					int idval = Integer.parseInt(id);
					String num = st.nextToken();
					int numval = Integer.parseInt(num);
					String radius = st.nextToken();
					int radiusval = Integer.parseInt(radius);
					createItem(activeChar, target, idval, numval, radiusval);
				}
				else if (st.countTokens() == 2)
				{
					String id = st.nextToken();
					int idval = Integer.parseInt(id);
					String num = st.nextToken();
					int numval = Integer.parseInt(num);
					createItem(activeChar, target, idval, numval);
				}
				else if (st.countTokens() == 1)
				{
					String id = st.nextToken();
					int idval = Integer.parseInt(id);
					createItem(activeChar, target, idval, 1);
				}
			}
			catch (Exception e)
			{
				activeChar.sendMessage("Usage: //create_item <itemId> [amount] [radius]");
			}
			AdminHelpPage.showHelpPage(activeChar, "itemcreation.htm");
		}
		else if (command.startsWith("admin_create_coin"))
		{
			L2PcInstance target;
			if (activeChar.getTarget() != null && activeChar.getTarget() instanceof L2PcInstance)
				target = (L2PcInstance) activeChar.getTarget();
			else
				target = activeChar;

			try
			{
				String val = command.substring(17);
				StringTokenizer st = new StringTokenizer(val);
				if (st.countTokens() == 2)
				{
					String name = st.nextToken();
					int idval = getCoinId(name);
					if (idval > 0)
					{
						String num = st.nextToken();
						int numval = Integer.parseInt(num);
						createItem(activeChar, target, idval, numval);
					}
				}
				else if (st.countTokens() == 1)
				{
					String name = st.nextToken();
					int idval = getCoinId(name);
					createItem(activeChar, target, idval, 1);
				}
			}
			catch (Exception e)
			{
				activeChar.sendMessage("Usage: //create_coin <name> [amount]");
			}
			AdminHelpPage.showHelpPage(activeChar, "itemcreation.htm");
		}
		else if (command.startsWith("admin_reward_all"))
		{
			try
			{
				String val = command.substring(17);
				StringTokenizer st = new StringTokenizer(val);
				int idval = 0;
				int numval = 0;
				if (st.countTokens() == 2)
				{
					String id = st.nextToken();
					idval = Integer.parseInt(id);
					String num = st.nextToken();
					numval = Integer.parseInt(num);
				}
				else if (st.countTokens() == 1)
				{
					String id = st.nextToken();
					idval = Integer.parseInt(id);
					numval = 1;
				}

				int counter = 0;
				L2Item template = ItemTable.getInstance().getTemplate(idval);
				if (template == null)
				{
					activeChar.sendMessage("This item doesn't exist.");
					return false;
				}

				if (numval > 1 && !template.isStackable())
				{
					activeChar.sendMessage("This item doesn't stack - Creation aborted.");
					return false;
				}

				Collection<L2PcInstance> pls = L2World.getInstance().getAllPlayers().values();
				for (L2PcInstance onlinePlayer : pls)
				{
					if (activeChar != onlinePlayer && onlinePlayer.isOnline() && (onlinePlayer.getClient() != null && !onlinePlayer.getClient().isDetached()))
					{
						onlinePlayer.getInventory().addItem("Admin", idval, numval, onlinePlayer, activeChar);
						onlinePlayer.sendMessage("A GM spawned " + numval + " " + template.getName() + " in your inventory.");
						counter++;
					}
				}
				activeChar.sendMessage(counter + " players rewarded with " + template.getName());
			}
			catch (Exception e)
			{
				activeChar.sendMessage("Usage: //reward_all <itemId> [amount]");
			}
			AdminHelpPage.showHelpPage(activeChar, "itemcreation.htm");
		}
		return true;
	}

	private static void createItem(L2PcInstance activeChar, L2PcInstance target, int id, int num)
	{
		createItem(activeChar, target, id, num, 0);
	}

	private static void createItem(L2PcInstance activeChar, L2PcInstance target, int id, int num, int radius)
	{
		L2Item template = ItemTable.getInstance().getTemplate(id);
		if (template == null)
		{
			activeChar.sendMessage("This item doesn't exist.");
			return;
		}

		if (num > 1 && !template.isStackable())
		{
			activeChar.sendMessage("This item doesn't stack - Creation aborted.");
			return;
		}

		if (radius > 0)
		{
			int counter = 0;

			final Collection<L2PcInstance> objs = activeChar.getKnownList().getKnownPlayersInRadius(radius);
			for (L2PcInstance obj : objs)
			{
				if (!(obj.equals(activeChar)))
				{
					obj.getInventory().addItem("Admin", id, num, obj, activeChar);
					obj.sendMessage("A GM spawned " + num + " " + template.getName() + " in your inventory.");
					counter++;
				}
			}
			activeChar.sendMessage(counter + " players rewarded with " + num + " " + template.getName() + " in a " + radius + " radius.");
		}
		else
		{
			target.getInventory().addItem("Admin", id, num, target, activeChar);
			if (activeChar != target)
				target.sendMessage("A GM spawned " + num + " " + template.getName() + " in your inventory.");

			activeChar.sendMessage("You have spawned " + num + " " + template.getName() + " in " + target.getName() + " inventory.");
		}
	}

	private static int getCoinId(String name)
	{
		int id;
		if (name.equalsIgnoreCase("adena"))
			id = 57;
		else if (name.equalsIgnoreCase("ancientadena"))
			id = 5575;
		else if (name.equalsIgnoreCase("festivaladena"))
			id = 6673;
		else
			id = 0;

		return id;
	}

	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}