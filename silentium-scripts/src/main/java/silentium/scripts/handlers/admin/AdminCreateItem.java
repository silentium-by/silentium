/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.handlers.admin;

import silentium.gameserver.handler.IAdminCommandHandler;
import silentium.gameserver.model.L2World;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.tables.ItemTable;
import silentium.gameserver.templates.item.L2Item;

import java.util.Collection;
import java.util.StringTokenizer;

/**
 * This class handles following admin commands:<br>
 * <br>
 * - itemcreate = show "item creation" menu<br>
 * - create_item = creates num items with respective id, if num is not specified, assumes 1.<br>
 * - create_coin = creates currency, using the choice box or typing good IDs.<br>
 * - reward_all = reward all online players with items.
 */
public class AdminCreateItem implements IAdminCommandHandler {
	private static final String[] ADMIN_COMMANDS = { "admin_itemcreate", "admin_create_item", "admin_create_coin", "admin_reward_all" };

	@Override
	public boolean useAdminCommand(final String command, final L2PcInstance activeChar) {
		if ("admin_itemcreate".equals(command)) {
			AdminHelpPage.showHelpPage(activeChar, "itemcreation.htm");
		} else if (command.startsWith("admin_create_item")) {
			final L2PcInstance target;
			target = activeChar.getTarget() != null && activeChar.getTarget() instanceof L2PcInstance ? (L2PcInstance) activeChar.getTarget() : activeChar;

			try {
				final String val = command.substring(17);
				final StringTokenizer st = new StringTokenizer(val);
				if (st.countTokens() == 3) {
					final String id = st.nextToken();
					final int idval = Integer.parseInt(id);
					final String num = st.nextToken();
					final int numval = Integer.parseInt(num);
					final String radius = st.nextToken();
					final int radiusval = Integer.parseInt(radius);
					createItem(activeChar, target, idval, numval, radiusval);
				} else if (st.countTokens() == 2) {
					final String id = st.nextToken();
					final int idval = Integer.parseInt(id);
					final String num = st.nextToken();
					final int numval = Integer.parseInt(num);
					createItem(activeChar, target, idval, numval);
				} else if (st.countTokens() == 1) {
					final String id = st.nextToken();
					final int idval = Integer.parseInt(id);
					createItem(activeChar, target, idval, 1);
				}
			} catch (Exception e) {
				activeChar.sendMessage("Usage: //create_item <itemId> [amount] [radius]");
			}
			AdminHelpPage.showHelpPage(activeChar, "itemcreation.htm");
		} else if (command.startsWith("admin_create_coin")) {
			final L2PcInstance target;
			target = activeChar.getTarget() != null && activeChar.getTarget() instanceof L2PcInstance ? (L2PcInstance) activeChar.getTarget() : activeChar;

			try {
				final String val = command.substring(17);
				final StringTokenizer st = new StringTokenizer(val);
				if (st.countTokens() == 2) {
					final String name = st.nextToken();
					final int idval = getCoinId(name);
					if (idval > 0) {
						final String num = st.nextToken();
						final int numval = Integer.parseInt(num);
						createItem(activeChar, target, idval, numval);
					}
				} else if (st.countTokens() == 1) {
					final String name = st.nextToken();
					final int idval = getCoinId(name);
					createItem(activeChar, target, idval, 1);
				}
			} catch (Exception e) {
				activeChar.sendMessage("Usage: //create_coin <name> [amount]");
			}
			AdminHelpPage.showHelpPage(activeChar, "itemcreation.htm");
		} else if (command.startsWith("admin_reward_all")) {
			try {
				final String val = command.substring(17);
				final StringTokenizer st = new StringTokenizer(val);
				int idval = 0;
				int numval = 0;
				if (st.countTokens() == 2) {
					final String id = st.nextToken();
					idval = Integer.parseInt(id);
					final String num = st.nextToken();
					numval = Integer.parseInt(num);
				} else if (st.countTokens() == 1) {
					final String id = st.nextToken();
					idval = Integer.parseInt(id);
					numval = 1;
				}

				int counter = 0;
				final L2Item template = ItemTable.getInstance().getTemplate(idval);
				if (template == null) {
					activeChar.sendMessage("This item doesn't exist.");
					return false;
				}

				if (numval > 1 && !template.isStackable()) {
					activeChar.sendMessage("This item doesn't stack - Creation aborted.");
					return false;
				}

				final Collection<L2PcInstance> pls = L2World.getInstance().getAllPlayers().values();
				for (final L2PcInstance onlinePlayer : pls) {
					if (activeChar != onlinePlayer && onlinePlayer.isOnline() && onlinePlayer.getClient() != null && !onlinePlayer.getClient().isDetached()) {
						onlinePlayer.getInventory().addItem("Admin", idval, numval, onlinePlayer, activeChar);
						onlinePlayer.sendMessage("A GM spawned " + numval + ' ' + template.getName() + " in your inventory.");
						counter++;
					}
				}
				activeChar.sendMessage(counter + " players rewarded with " + template.getName());
			} catch (Exception e) {
				activeChar.sendMessage("Usage: //reward_all <itemId> [amount]");
			}
			AdminHelpPage.showHelpPage(activeChar, "itemcreation.htm");
		}
		return true;
	}

	private static void createItem(final L2PcInstance activeChar, final L2PcInstance target, final int id, final int num) {
		createItem(activeChar, target, id, num, 0);
	}

	private static void createItem(final L2PcInstance activeChar, final L2PcInstance target, final int id, final int num, final int radius) {
		final L2Item template = ItemTable.getInstance().getTemplate(id);
		if (template == null) {
			activeChar.sendMessage("This item doesn't exist.");
			return;
		}

		if (num > 1 && !template.isStackable()) {
			activeChar.sendMessage("This item doesn't stack - Creation aborted.");
			return;
		}

		if (radius > 0) {
			int counter = 0;

			final Collection<L2PcInstance> objs = activeChar.getKnownList().getKnownPlayersInRadius(radius);
			for (final L2PcInstance obj : objs) {
				if (!obj.equals(activeChar)) {
					obj.getInventory().addItem("Admin", id, num, obj, activeChar);
					obj.sendMessage("A GM spawned " + num + ' ' + template.getName() + " in your inventory.");
					counter++;
				}
			}
			activeChar.sendMessage(counter + " players rewarded with " + num + ' ' + template.getName() + " in a " + radius + " radius.");
		} else {
			target.getInventory().addItem("Admin", id, num, target, activeChar);
			if (activeChar != target)
				target.sendMessage("A GM spawned " + num + ' ' + template.getName() + " in your inventory.");

			activeChar.sendMessage("You have spawned " + num + ' ' + template.getName() + " in " + target.getName() + " inventory.");
		}
	}

	private static int getCoinId(final String name) {
		final int id;
		if ("adena".equalsIgnoreCase(name))
			id = 57;
		else if ("ancientadena".equalsIgnoreCase(name))
			id = 5575;
		else id = "festivaladena".equalsIgnoreCase(name) ? 6673 : 0;

		return id;
	}

	@Override
	public String[] getAdminCommandList() {
		return ADMIN_COMMANDS;
	}
}