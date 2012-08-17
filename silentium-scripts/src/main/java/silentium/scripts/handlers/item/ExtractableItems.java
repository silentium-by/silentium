/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.handlers.item;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import silentium.commons.utils.Rnd;
import silentium.gameserver.handler.IItemHandler;
import silentium.gameserver.model.L2ExtractableProduct;
import silentium.gameserver.model.L2ItemInstance;
import silentium.gameserver.model.actor.L2Playable;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.network.SystemMessageId;
import silentium.gameserver.tables.ItemTable;
import silentium.gameserver.templates.item.L2EtcItem;

/**
 * @author FBIagent 11/12/2006
 */
public class ExtractableItems implements IItemHandler
{
	private static Logger _log = LoggerFactory.getLogger(ItemTable.class.getName());

	@Override
	public void useItem(L2Playable playable, L2ItemInstance item, boolean forceUse)
	{
		if (!(playable instanceof L2PcInstance))
			return;

		L2PcInstance activeChar = (L2PcInstance) playable;

		L2EtcItem etcitem = (L2EtcItem) item.getItem();
		List<L2ExtractableProduct> exitem = etcitem.getExtractableItems();

		if (exitem == null)
		{
			_log.info("No extractable data defined for " + etcitem);
			return;
		}

		// destroy item
		if (!activeChar.destroyItem("Extract", item.getObjectId(), 1, activeChar, true))
			return;

		boolean created = false;

		// calculate extraction
		for (L2ExtractableProduct expi : exitem)
		{
			if (Rnd.get(100000) <= expi.getChance())
			{
				int min = expi.getMin();
				int max = expi.getMax();
				int createItemID = expi.getId();

				int createitemAmount = 0;
				if (max == min)
					createitemAmount = min;
				else
					createitemAmount = Rnd.get(max - min + 1) + min;

				activeChar.addItem("Extract", createItemID, createitemAmount, activeChar, true);
				created = true;
			}
		}

		if (!created)
			activeChar.sendPacket(SystemMessageId.NOTHING_INSIDE_THAT);
	}
}
