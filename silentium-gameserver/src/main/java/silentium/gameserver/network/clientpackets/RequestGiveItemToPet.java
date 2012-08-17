/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.network.clientpackets;

import silentium.gameserver.configs.PlayersConfig;
import silentium.gameserver.model.L2ItemInstance;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.actor.instance.L2PetInstance;
import silentium.gameserver.network.SystemMessageId;
import silentium.gameserver.network.serverpackets.EnchantResult;

public final class RequestGiveItemToPet extends L2GameClientPacket
{
	private int _objectId;
	private int _amount;

	@Override
	protected void readImpl()
	{
		_objectId = readD();
		_amount = readD();
	}

	@Override
	protected void runImpl()
	{
		if (_amount <= 0)
			return;

		final L2PcInstance player = getClient().getActiveChar();
		if ((player == null) || !(player.getPet() instanceof L2PetInstance))
			return;

		if (player.isProcessingTransaction())
		{
			player.sendPacket(SystemMessageId.ALREADY_TRADING);
			return;
		}

		// Alt game - Karma punishment
		if (!PlayersConfig.KARMA_PLAYER_CAN_TRADE && player.getKarma() > 0)
			return;

		if (player.getPrivateStoreType() != 0)
		{
			player.sendPacket(SystemMessageId.CANNOT_PICKUP_OR_USE_ITEM_WHILE_TRADING);
			return;
		}

		final L2ItemInstance item = player.getInventory().getItemByObjectId(_objectId);
		if (item == null || item.isAugmented())
			return;

		if (item.isHeroItem() || !item.isDropable() || !item.isDestroyable() || !item.isTradable())
		{
			player.sendPacket(SystemMessageId.ITEM_NOT_FOR_PETS);
			return;
		}

		L2PetInstance pet = (L2PetInstance) player.getPet();
		if (pet.isDead())
		{
			player.sendPacket(SystemMessageId.CANNOT_GIVE_ITEMS_TO_DEAD_PET);
			return;
		}

		if (!pet.getInventory().validateCapacity(item))
		{
			player.sendPacket(SystemMessageId.YOUR_PET_CANNOT_CARRY_ANY_MORE_ITEMS);
			return;
		}

		if (!pet.getInventory().validateWeight(item, _amount))
		{
			player.sendPacket(SystemMessageId.UNABLE_TO_PLACE_ITEM_YOUR_PET_IS_TOO_ENCUMBERED);
			return;
		}

		if (player.getActiveEnchantItem() != null)
		{
			player.setActiveEnchantItem(null);
			player.sendPacket(EnchantResult.CANCELLED);
			player.sendPacket(SystemMessageId.ENCHANT_SCROLL_CANCELLED);
		}

		if (player.transferItem("Transfer", _objectId, _amount, pet.getInventory(), pet) == null)
			log.warn("Invalid item transfer request: " + pet.getName() + "(pet) --> " + player.getName());
	}
}
