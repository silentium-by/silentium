/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.network.clientpackets;

import silentium.gameserver.model.L2ItemInstance;
import silentium.gameserver.model.L2Skill;
import silentium.gameserver.model.L2World;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.itemcontainer.PcInventory;
import silentium.gameserver.network.SystemMessageId;
import silentium.gameserver.network.serverpackets.ActionFailed;
import silentium.gameserver.network.serverpackets.InventoryUpdate;
import silentium.gameserver.network.serverpackets.SystemMessage;
import silentium.gameserver.templates.item.L2Item;
import silentium.gameserver.utils.IllegalPlayerAction;
import silentium.gameserver.utils.Util;

public final class RequestCrystallizeItem extends L2GameClientPacket
{
	private int _objectId;
	private int _count;

	@Override
	protected void readImpl()
	{
		_objectId = readD();
		_count = readD();
	}

	@Override
	protected void runImpl()
	{
		final L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;

		if (_count <= 0)
		{
			Util.handleIllegalPlayerAction(activeChar, "[RequestCrystallizeItem] " + activeChar.getName() + "tried to crystallize an object but count was inferior to 0", IllegalPlayerAction.PUNISH_KICK);
			return;
		}

		if (activeChar.getPrivateStoreType() != 0 || activeChar.isInCrystallize())
		{
			activeChar.sendPacket(SystemMessageId.CANNOT_TRADE_DISCARD_DROP_ITEM_WHILE_IN_SHOPMODE);
			return;
		}

		int skillLevel = activeChar.getSkillLevel(L2Skill.SKILL_CRYSTALLIZE);
		if (skillLevel <= 0)
		{
			activeChar.sendPacket(SystemMessageId.CRYSTALLIZE_LEVEL_TOO_LOW);
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		final PcInventory inventory = activeChar.getInventory();
		if (inventory != null)
		{
			final L2ItemInstance item = inventory.getItemByObjectId(_objectId);
			if (item == null)
			{
				activeChar.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}

			if (item.isHeroItem())
				return;

			if (_count > item.getCount())
				_count = activeChar.getInventory().getItemByObjectId(_objectId).getCount();
		}

		L2ItemInstance itemToRemove = activeChar.getInventory().getItemByObjectId(_objectId);
		if (itemToRemove == null || itemToRemove.isShadowItem() /* || itemToRemove.isTimeLimitedItem() */)
			return;

		if (!itemToRemove.getItem().isCrystallizable() || (itemToRemove.getItem().getCrystalCount() <= 0) || (itemToRemove.getItem().getCrystalType() == L2Item.CRYSTAL_NONE))
		{
			log.warn(activeChar.getName() + " tried to crystallize " + itemToRemove.getItem().getItemId());
			return;
		}

		if (!activeChar.getInventory().canManipulateWithItemId(itemToRemove.getItemId()))
			return;

		// Check if the char can crystallize items and return if false;
		boolean canCrystallize = true;

		switch (itemToRemove.getItem().getCrystalType())
		{
			case L2Item.CRYSTAL_C:
				if (skillLevel <= 1)
					canCrystallize = false;
				break;

			case L2Item.CRYSTAL_B:
				if (skillLevel <= 2)
					canCrystallize = false;
				break;

			case L2Item.CRYSTAL_A:
				if (skillLevel <= 3)
					canCrystallize = false;
				break;

			case L2Item.CRYSTAL_S:
				if (skillLevel <= 4)
					canCrystallize = false;
				break;
		}

		if (!canCrystallize)
		{
			activeChar.sendPacket(SystemMessageId.CRYSTALLIZE_LEVEL_TOO_LOW);
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		activeChar.setInCrystallize(true);

		// unequip if needed
		if (itemToRemove.isEquipped())
		{
			L2ItemInstance[] unequipped = activeChar.getInventory().unEquipItemInSlotAndRecord(itemToRemove.getLocationSlot());
			InventoryUpdate iu = new InventoryUpdate();
			for (L2ItemInstance item : unequipped)
				iu.addModifiedItem(item);

			activeChar.sendPacket(iu);

			SystemMessage msg;
			if (itemToRemove.getEnchantLevel() > 0)
			{
				msg = SystemMessage.getSystemMessage(SystemMessageId.EQUIPMENT_S1_S2_REMOVED);
				msg.addNumber(itemToRemove.getEnchantLevel());
				msg.addItemName(itemToRemove.getItemId());
			}
			else
			{
				msg = SystemMessage.getSystemMessage(SystemMessageId.S1_DISARMED);
				msg.addItemName(itemToRemove.getItemId());
			}
			activeChar.sendPacket(msg);
		}

		// remove from inventory
		L2ItemInstance removedItem = activeChar.getInventory().destroyItem("Crystalize", _objectId, _count, activeChar, null);

		InventoryUpdate iu = new InventoryUpdate();
		iu.addRemovedItem(removedItem);
		activeChar.sendPacket(iu);

		// add crystals
		int crystalId = itemToRemove.getItem().getCrystalItemId();
		int crystalAmount = itemToRemove.getCrystalCount();
		L2ItemInstance createditem = activeChar.getInventory().addItem("Crystalize", crystalId, crystalAmount, activeChar, activeChar);

		activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_CRYSTALLIZED).addItemName(removedItem.getItemId()));
		activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.EARNED_S2_S1_S).addItemName(createditem.getItemId()).addItemNumber(crystalAmount));

		activeChar.broadcastUserInfo();
		L2World.getInstance().removeObject(removedItem);
		activeChar.setInCrystallize(false);
	}
}
