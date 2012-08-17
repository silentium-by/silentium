/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.handlers.skill;

import silentium.commons.utils.Rnd;
import silentium.gameserver.handler.ISkillHandler;
import silentium.gameserver.model.L2ExtractableProductItem;
import silentium.gameserver.model.L2ExtractableSkill;
import silentium.gameserver.model.L2Object;
import silentium.gameserver.model.L2Skill;
import silentium.gameserver.model.actor.L2Character;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.network.SystemMessageId;
import silentium.gameserver.network.serverpackets.SystemMessage;
import silentium.gameserver.tables.ItemTable;
import silentium.gameserver.templates.skills.L2SkillType;

public class Extractable implements ISkillHandler
{
	private static final L2SkillType[] SKILL_IDS = { L2SkillType.EXTRACTABLE, L2SkillType.EXTRACTABLE_FISH };

	@Override
	public void useSkill(L2Character activeChar, L2Skill skill, L2Object[] targets)
	{
		if (!(activeChar instanceof L2PcInstance))
			return;

		L2ExtractableSkill exItem = skill.getExtractableSkill();
		if (exItem == null)
			return;

		if (exItem.getProductItemsArray().isEmpty())
		{
			_log.warn("Extractable Item Skill with no data, probably wrong/empty table with Skill Id: " + skill.getId());
			return;
		}

		final double rndNum = 100 * Rnd.nextDouble();
		double chance = 0;
		double chanceFrom = 0;
		int[] createItemID = new int[20];
		int[] createAmount = new int[20];

		// calculate extraction
		for (L2ExtractableProductItem expi : exItem.getProductItemsArray())
		{
			chance = expi.getChance();
			if ((rndNum >= chanceFrom) && (rndNum <= (chance + chanceFrom)))
			{
				for (int i = 0; i < expi.getId().length; i++)
				{
					createItemID[i] = expi.getId()[i];
					createAmount[i] = expi.getAmount()[i];
				}
				break;
			}
			chanceFrom += chance;
		}

		L2PcInstance player = (L2PcInstance) activeChar;
		if (player.isSubClassActive() && skill.getReuseDelay() > 0)
		{
			player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED).addSkillName(skill));
			return;
		}

		if (createItemID[0] <= 0 || createItemID.length == 0)
		{
			player.sendPacket(SystemMessageId.NOTHING_INSIDE_THAT);
			return;
		}

		for (int i = 0; i < createItemID.length; i++)
		{
			if (createItemID[i] <= 0)
				return;

			if (ItemTable.getInstance().createDummyItem(createItemID[i]) == null)
			{
				_log.warn("createItemID " + createItemID[i] + " doesn't have template!");
				player.sendPacket(SystemMessageId.NOTHING_INSIDE_THAT);
				return;
			}

			if (ItemTable.getInstance().createDummyItem(createItemID[i]).isStackable())
				player.addItem("Extract", createItemID[i], createAmount[i], targets[0], false);
			else
			{
				for (int j = 0; j < createAmount[i]; j++)
					player.addItem("Extract", createItemID[i], 1, targets[0], false);
			}

			SystemMessage sm;
			if (createItemID[i] == 57)
			{
				sm = SystemMessage.getSystemMessage(SystemMessageId.EARNED_S1_ADENA);
				sm.addNumber(createAmount[i]);
			}
			else
			{
				sm = SystemMessage.getSystemMessage(SystemMessageId.EARNED_S2_S1_S);
				sm.addItemName(createItemID[i]);
				if (createAmount[i] > 1)
					sm.addNumber(createAmount[i]);
			}
			player.sendPacket(sm);
		}
	}

	@Override
	public L2SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}
