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

import silentium.gameserver.configs.MainConfig;
import silentium.gameserver.handler.IAdminCommandHandler;
import silentium.gameserver.model.L2ItemInstance;
import silentium.gameserver.model.L2Object;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.itemcontainer.Inventory;
import silentium.gameserver.network.SystemMessageId;
import silentium.gameserver.network.serverpackets.ItemList;

/**
 * This class handles following admin commands: - enchant_armor
 */
public class AdminEnchant implements IAdminCommandHandler
{
	private static Logger _log = LoggerFactory.getLogger(AdminEnchant.class.getName());

	private static final String[] ADMIN_COMMANDS = { "admin_seteh",// 6
			"admin_setec",// 10
			"admin_seteg",// 9
			"admin_setel",// 11
			"admin_seteb",// 12
			"admin_setew",// 7
			"admin_setes",// 8
			"admin_setle",// 1
			"admin_setre",// 2
			"admin_setlf",// 4
			"admin_setrf",// 5
			"admin_seten",// 3
			"admin_setun",// 0
			"admin_setba",// 13
			"admin_enchant" };

	@Override
	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		if (command.equals("admin_enchant"))
			showMainPage(activeChar);
		else
		{
			int armorType = -1;

			if (command.startsWith("admin_seteh"))
				armorType = Inventory.PAPERDOLL_HEAD;
			else if (command.startsWith("admin_setec"))
				armorType = Inventory.PAPERDOLL_CHEST;
			else if (command.startsWith("admin_seteg"))
				armorType = Inventory.PAPERDOLL_GLOVES;
			else if (command.startsWith("admin_seteb"))
				armorType = Inventory.PAPERDOLL_FEET;
			else if (command.startsWith("admin_setel"))
				armorType = Inventory.PAPERDOLL_LEGS;
			else if (command.startsWith("admin_setew"))
				armorType = Inventory.PAPERDOLL_RHAND;
			else if (command.startsWith("admin_setes"))
				armorType = Inventory.PAPERDOLL_LHAND;
			else if (command.startsWith("admin_setle"))
				armorType = Inventory.PAPERDOLL_LEAR;
			else if (command.startsWith("admin_setre"))
				armorType = Inventory.PAPERDOLL_REAR;
			else if (command.startsWith("admin_setlf"))
				armorType = Inventory.PAPERDOLL_LFINGER;
			else if (command.startsWith("admin_setrf"))
				armorType = Inventory.PAPERDOLL_RFINGER;
			else if (command.startsWith("admin_seten"))
				armorType = Inventory.PAPERDOLL_NECK;
			else if (command.startsWith("admin_setun"))
				armorType = Inventory.PAPERDOLL_UNDER;
			else if (command.startsWith("admin_setba"))
				armorType = Inventory.PAPERDOLL_BACK;

			if (armorType != -1)
			{
				try
				{
					int ench = Integer.parseInt(command.substring(12));

					// check value
					if (ench < 0 || ench > 65535)
						activeChar.sendMessage("You must set the enchant level to be between 0-65535.");
					else
						setEnchant(activeChar, ench, armorType);
				}
				catch (StringIndexOutOfBoundsException e)
				{
					if (MainConfig.DEVELOPER)
						_log.warn("Set enchant error: " + e);
					activeChar.sendMessage("Please specify a new enchant value.");
				}
				catch (NumberFormatException e)
				{
					if (MainConfig.DEVELOPER)
						_log.warn("Set enchant error: " + e);
					activeChar.sendMessage("Please specify a valid new enchant value.");
				}
			}

			// show the enchant menu after an action
			showMainPage(activeChar);
		}

		return true;
	}

	private static void setEnchant(L2PcInstance activeChar, int ench, int armorType)
	{
		// get the target
		L2Object target = activeChar.getTarget();
		if (target == null)
			target = activeChar;

		L2PcInstance player = null;
		if (target instanceof L2PcInstance)
			player = (L2PcInstance) target;
		else
		{
			activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
			return;
		}

		// now we need to find the equipped weapon of the targeted character...
		int curEnchant = 0; // display purposes only
		L2ItemInstance itemInstance = null;

		// only attempt to enchant if there is a weapon equipped
		L2ItemInstance parmorInstance = player.getInventory().getPaperdollItem(armorType);
		if (parmorInstance != null && parmorInstance.getLocationSlot() == armorType)
			itemInstance = parmorInstance;

		if (itemInstance != null)
		{
			curEnchant = itemInstance.getEnchantLevel();

			// set enchant value
			itemInstance.setEnchantLevel(ench);
			itemInstance.updateDatabase();

			// send packets
			player.sendPacket(new ItemList(player, false));
			player.broadcastUserInfo();

			// informations
			activeChar.sendMessage("Changed enchantment of " + player.getName() + "'s " + itemInstance.getItem().getName() + " from " + curEnchant + " to " + ench + ".");
			if (player != activeChar)
				player.sendMessage("A GM has changed the enchantment of your " + itemInstance.getItem().getName() + " from " + curEnchant + " to " + ench + ".");
		}
	}

	private static void showMainPage(L2PcInstance activeChar)
	{
		AdminHelpPage.showHelpPage(activeChar, "enchant.htm");
	}

	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
