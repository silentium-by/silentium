/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.network.clientpackets;

import silentium.gameserver.GameTimeController;
import silentium.gameserver.ThreadPoolManager;
import silentium.gameserver.configs.PlayersConfig;
import silentium.gameserver.handler.IItemHandler;
import silentium.gameserver.handler.ItemHandler;
import silentium.gameserver.model.L2ItemInstance;
import silentium.gameserver.model.actor.L2Summon;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.actor.instance.L2PetInstance;
import silentium.gameserver.model.itemcontainer.Inventory;
import silentium.gameserver.network.SystemMessageId;
import silentium.gameserver.network.serverpackets.ItemList;
import silentium.gameserver.network.serverpackets.PetItemList;
import silentium.gameserver.network.serverpackets.ShowCalculator;
import silentium.gameserver.network.serverpackets.SystemMessage;
import silentium.gameserver.templates.item.L2Item;
import silentium.gameserver.templates.item.L2Weapon;
import silentium.gameserver.templates.item.L2WeaponType;

public final class UseItem extends L2GameClientPacket
{
	private int _objectId;
	private boolean _ctrlPressed;
	private int _itemId;

	/** Weapon Equip Task */
	public static class WeaponEquipTask implements Runnable
	{
		L2ItemInstance item;
		L2PcInstance activeChar;

		public WeaponEquipTask(L2ItemInstance it, L2PcInstance character)
		{
			item = it;
			activeChar = character;
		}

		@Override
		public void run()
		{
			// If character is still engaged in strike we should not change weapon
			if (activeChar.isAttackingNow())
				return;

			// Equip or unEquip
			activeChar.useEquippableItem(item, false);
		}
	}

	@Override
	protected void readImpl()
	{
		_objectId = readD();
		_ctrlPressed = readD() != 0;
	}

	@Override
	protected void runImpl()
	{
		final L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;

		if (activeChar.getPrivateStoreType() != 0)
		{
			activeChar.sendPacket(SystemMessageId.CANNOT_TRADE_DISCARD_DROP_ITEM_WHILE_IN_SHOPMODE);
			return;
		}

		if (activeChar.getActiveTradeList() != null)
		{
			activeChar.sendPacket(SystemMessageId.CANNOT_PICKUP_OR_USE_ITEM_WHILE_TRADING);
			return;
		}

		L2ItemInstance item = activeChar.getInventory().getItemByObjectId(_objectId);
		if (item == null)
			return;

		_itemId = item.getItemId();

		// The player can't use an item in those special conditions
		if (activeChar.isAlikeDead() || activeChar.isStunned() || activeChar.isSleeping() || activeChar.isParalyzed() || activeChar.isAlikeDead() || activeChar.isAfraid() || (activeChar.isCastingNow() && !(item.isPotion() || item.isElixir())))
		{
			activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED).addItemName(_itemId));
			return;
		}

		if (item.getItem().getType2() == L2Item.TYPE2_QUEST)
		{
			activeChar.sendPacket(SystemMessageId.CANNOT_USE_QUEST_ITEMS);
			return;
		}

		if (!PlayersConfig.KARMA_PLAYER_CAN_TELEPORT && activeChar.getKarma() > 0)
		{
			switch (_itemId)
			{
				case 736:
				case 1538:
				case 1829:
				case 1830:
				case 3958:
				case 5858:
				case 5859:
				case 6663:
				case 6664:
				case 7618:
				case 7619:
					return;
			}

			if ((_itemId >= 7117 && _itemId <= 7135) || (_itemId >= 7554 && _itemId <= 7559))
				return;
		}

		if (activeChar.isFishing() && (_itemId < 6535 || _itemId > 6540))
		{
			// You cannot do anything else while fishing
			activeChar.sendPacket(SystemMessageId.CANNOT_DO_WHILE_FISHING_3);
			return;
		}

		/*
		 * The player can't use pet items if no pet is currently summoned. If a pet is summoned and player uses the item directly, it will be
		 * used by the pet.
		 */
		if (item.isPetItem())
		{
			L2Summon summon = activeChar.getPet();

			// If no summon, cancels the use
			if (summon == null || !(summon instanceof L2PetInstance))
			{
				activeChar.sendPacket(SystemMessageId.CANNOT_EQUIP_PET_ITEM);
				return;
			}

			L2PetInstance pet = ((L2PetInstance) summon);

			if (!(pet.canWear(item.getItem())))
			{
				activeChar.sendPacket(SystemMessageId.PET_CANNOT_USE_ITEM);
				return;
			}

			// Transfer the item from owner to pet inventory.
			if (pet.isDead())
			{
				activeChar.sendPacket(SystemMessageId.CANNOT_GIVE_ITEMS_TO_DEAD_PET);
				return;
			}

			if (!pet.getInventory().validateCapacity(item))
			{
				activeChar.sendPacket(SystemMessageId.YOUR_PET_CANNOT_CARRY_ANY_MORE_ITEMS);
				return;
			}

			if (!pet.getInventory().validateWeight(item, 1))
			{
				activeChar.sendPacket(SystemMessageId.UNABLE_TO_PLACE_ITEM_YOUR_PET_IS_TOO_ENCUMBERED);
				return;
			}

			activeChar.transferItem("Transfer", _objectId, 1, pet.getInventory(), pet);

			// Equip it, removing first the previous item.
			if (item.isEquipped())
				pet.getInventory().unEquipItemInSlot(item.getLocationSlot());
			else
				pet.getInventory().equipPetItem(item);

			activeChar.sendPacket(new PetItemList(pet));
			pet.updateAndBroadcastStatus(1);
			return;
		}

		if (!activeChar.getInventory().canManipulateWithItemId(item.getItemId()))
			return;

		log.debug(activeChar.getName() + ": use item " + _objectId);

		if (!item.isEquipped())
		{
			if (!item.getItem().checkCondition(activeChar, activeChar, true))
				return;
		}

		if (item.isEquipable())
		{
			switch (item.getItem().getBodyPart())
			{
				case L2Item.SLOT_LR_HAND:
				case L2Item.SLOT_L_HAND:
				case L2Item.SLOT_R_HAND:
				{
					// Prevent player to remove the weapon on special conditions
					if (activeChar.isCastingNow() || activeChar.isCastingSimultaneouslyNow())
					{
						activeChar.sendPacket(SystemMessageId.CANNOT_USE_ITEM_WHILE_USING_MAGIC);
						return;
					}

					if (activeChar.isMounted())
					{
						activeChar.sendPacket(SystemMessageId.CANNOT_EQUIP_ITEM_DUE_TO_BAD_CONDITION);
						return;
					}

					// Don't allow weapon/shield equipment if a cursed weapon is equipped
					if (activeChar.isCursedWeaponEquipped())
						return;

					break;
				}
			}

			if (activeChar.isCursedWeaponEquipped() && _itemId == 6408) // Don't allow to put formal wear
				return;

			if (activeChar.isAttackingNow())
			{
				ThreadPoolManager.getInstance().scheduleGeneral(new WeaponEquipTask(item, activeChar), (activeChar.getAttackEndTime() - GameTimeController.getGameTicks()) * GameTimeController.MILLIS_IN_TICK);
				return;
			}

			// Equip or unEquip
			activeChar.useEquippableItem(item, true);
		}
		else
		{
			L2Weapon weaponItem = activeChar.getActiveWeaponItem();
			int itemid = item.getItemId();

			if (itemid == 4393)
				activeChar.sendPacket(new ShowCalculator(4393));
			else if ((weaponItem != null && weaponItem.getItemType() == L2WeaponType.FISHINGROD) && ((itemid >= 6519 && itemid <= 6527) || (itemid >= 7610 && itemid <= 7613) || (itemid >= 7807 && itemid <= 7809) || (itemid >= 8484 && itemid <= 8486) || (itemid >= 8505 && itemid <= 8513)))
			{
				activeChar.getInventory().setPaperdollItem(Inventory.PAPERDOLL_LHAND, item);
				activeChar.broadcastUserInfo();

				sendPacket(new ItemList(activeChar, false));
				return;
			}
			else
			{
				IItemHandler handler = ItemHandler.getInstance().getItemHandler(item.getEtcItem());
				if (handler != null)
					handler.useItem(activeChar, item, _ctrlPressed);
			}
		}
	}
}