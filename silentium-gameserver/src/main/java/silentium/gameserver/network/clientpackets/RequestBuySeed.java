/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.network.clientpackets;

import static silentium.gameserver.model.actor.L2Npc.INTERACTION_DISTANCE;
import silentium.gameserver.configs.ClansConfig;
import silentium.gameserver.configs.MainConfig;
import silentium.gameserver.configs.PlayersConfig;
import silentium.gameserver.instancemanager.CastleManager;
import silentium.gameserver.instancemanager.CastleManorManager;
import silentium.gameserver.instancemanager.CastleManorManager.SeedProduction;
import silentium.gameserver.model.L2Object;
import silentium.gameserver.model.actor.instance.L2ManorManagerInstance;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.entity.Castle;
import silentium.gameserver.network.SystemMessageId;
import silentium.gameserver.network.serverpackets.ActionFailed;
import silentium.gameserver.network.serverpackets.SystemMessage;
import silentium.gameserver.tables.ItemTable;
import silentium.gameserver.templates.item.L2Item;
import silentium.gameserver.utils.Util;

/**
 * Format: cdd[dd]
 * 
 * @author l3x
 */
public class RequestBuySeed extends L2GameClientPacket
{
	private static final int BATCH_LENGTH = 8; // length of the one item

	private int _manorId;
	private Seed[] _seeds = null;

	@Override
	protected void readImpl()
	{
		_manorId = readD();

		int count = readD();
		if (count <= 0 || count > PlayersConfig.MAX_ITEM_IN_PACKET || count * BATCH_LENGTH != _buf.remaining())
			return;

		_seeds = new Seed[count];
		for (int i = 0; i < count; i++)
		{
			int itemId = readD();
			int cnt = readD();
			if (cnt < 1)
			{
				_seeds = null;
				return;
			}
			_seeds[i] = new Seed(itemId, cnt);
		}
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		if (player == null)
			return;

		if (!getClient().getFloodProtectors().getManor().tryPerformAction("buySeed"))
			return;

		if (_seeds == null)
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		L2Object manager = player.getCurrentFolkNPC();
		if (!(manager instanceof L2ManorManagerInstance))
			return;

		if (!player.isInsideRadius(manager, INTERACTION_DISTANCE, true, false))
			return;

		int totalPrice = 0;
		int slots = 0;
		int totalWeight = 0;

		Castle castle = CastleManager.getInstance().getCastleById(_manorId);

		for (Seed i : _seeds)
		{
			if (!i.setProduction(castle))
				return;

			totalPrice += i.getPrice();

			if (totalPrice > Integer.MAX_VALUE)
			{
				Util.handleIllegalPlayerAction(player, player.getName() + " of account " + player.getAccountName() + " tried to purchase over " + Integer.MAX_VALUE + " adena worth of goods.", MainConfig.DEFAULT_PUNISH);
				return;
			}

			L2Item template = ItemTable.getInstance().getTemplate(i.getSeedId());
			totalWeight += i.getCount() * template.getWeight();
			if (!template.isStackable())
				slots += i.getCount();
			else if (player.getInventory().getItemByItemId(i.getSeedId()) == null)
				slots++;
		}

		if (!player.getInventory().validateWeight(totalWeight))
		{
			sendPacket(SystemMessage.getSystemMessage(SystemMessageId.WEIGHT_LIMIT_EXCEEDED));
			return;
		}

		if (!player.getInventory().validateCapacity(slots))
		{
			sendPacket(SystemMessage.getSystemMessage(SystemMessageId.SLOTS_FULL));
			return;
		}

		// test adena
		if (totalPrice < 0 || player.getAdena() < totalPrice)
		{
			sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_NOT_ENOUGH_ADENA));
			return;
		}

		// Proceed the purchase
		for (Seed i : _seeds)
		{
			// take adena and check seed amount once again
			if (!player.reduceAdena("Buy", i.getPrice(), player, false) || !i.updateProduction(castle))
			{
				// failed buy, reduce total price
				totalPrice -= i.getPrice();
				continue;
			}

			// Add item to Inventory and adjust update packet
			player.addItem("Buy", i.getSeedId(), i.getCount(), manager, true);
		}

		// Adding to treasury for Manor Castle
		if (totalPrice > 0)
		{
			castle.addToTreasuryNoTax(totalPrice);
			SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_DISAPPEARED_ADENA);
			sm.addItemNumber(totalPrice);
			player.sendPacket(sm);
		}
	}

	private static class Seed
	{
		private final int _seedId;
		private final int _count;
		SeedProduction _seed;

		public Seed(int id, int num)
		{
			_seedId = id;
			_count = num;
		}

		public int getSeedId()
		{
			return _seedId;
		}

		public int getCount()
		{
			return _count;
		}

		public int getPrice()
		{
			return _seed.getPrice() * _count;
		}

		public boolean setProduction(Castle c)
		{
			_seed = c.getSeed(_seedId, CastleManorManager.PERIOD_CURRENT);
			// invalid price - seed disabled
			if (_seed.getPrice() <= 0)
				return false;
			// try to buy more than castle can produce
			if (_seed.getCanProduce() < _count)
				return false;
			// check for overflow
			if ((Integer.MAX_VALUE / _count) < _seed.getPrice())
				return false;

			return true;
		}

		public boolean updateProduction(Castle c)
		{
			synchronized (_seed)
			{
				int amount = _seed.getCanProduce();
				if (_count > amount)
					return false; // not enough seeds

				_seed.setCanProduce(amount - _count);
			}

			// Update Castle Seeds Amount
			if (ClansConfig.ALT_MANOR_SAVE_ALL_ACTIONS)
				c.updateSeed(_seedId, _seed.getCanProduce(), CastleManorManager.PERIOD_CURRENT);

			return true;
		}
	}
}