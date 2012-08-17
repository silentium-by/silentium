/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.handlers.skill;

import silentium.commons.utils.Rnd;
import silentium.gameserver.configs.MainConfig;
import silentium.gameserver.handler.ISkillHandler;
import silentium.gameserver.model.L2ItemInstance;
import silentium.gameserver.model.L2Object;
import silentium.gameserver.model.L2Skill;
import silentium.gameserver.model.actor.L2Attackable;
import silentium.gameserver.model.actor.L2Character;
import silentium.gameserver.model.actor.instance.L2MonsterInstance;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.network.SystemMessageId;
import silentium.gameserver.network.serverpackets.InventoryUpdate;
import silentium.gameserver.network.serverpackets.ItemList;
import silentium.gameserver.network.serverpackets.SystemMessage;
import silentium.gameserver.templates.skills.L2SkillType;

/**
 * @author l3x
 */
public class Harvest implements ISkillHandler
{
	private static final L2SkillType[] SKILL_IDS = { L2SkillType.HARVEST };

	@Override
	public void useSkill(L2Character activeChar, L2Skill skill, L2Object[] targets)
	{
		if (!(activeChar instanceof L2PcInstance))
			return;

		final L2Object[] targetList = skill.getTargetList(activeChar);
		if (targetList == null)
			return;

		L2PcInstance player = (L2PcInstance) activeChar;
		L2MonsterInstance target;
		InventoryUpdate iu = MainConfig.FORCE_INVENTORY_UPDATE ? null : new InventoryUpdate();

		for (L2Object tgt : targetList)
		{
			if (!(tgt instanceof L2MonsterInstance))
				continue;

			target = (L2MonsterInstance) tgt;

			if (player.getObjectId() != target.getSeederId())
			{
				player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_HARVEST);
				continue;
			}

			boolean send = false;
			int total = 0;
			int cropId = 0;

			// TODO: check items and amount of items player harvest
			if (target.isSeeded())
			{
				if (calcSuccess(player, target))
				{
					L2Attackable.RewardItem[] items = target.takeHarvest();
					if (items != null && items.length > 0)
					{
						for (L2Attackable.RewardItem ritem : items)
						{
							cropId = ritem.getItemId(); // always got 1 type of crop as reward
							if (player.isInParty())
								player.getParty().distributeItem(player, ritem, true, target);
							else
							{
								L2ItemInstance item = player.getInventory().addItem("Manor", ritem.getItemId(), ritem.getCount(), player, target);
								if (iu != null)
									iu.addItem(item);

								send = true;
								total += ritem.getCount();
							}
						}

						if (send)
						{
							SystemMessage smsg = SystemMessage.getSystemMessage(SystemMessageId.YOU_PICKED_UP_S1_S2);
							smsg.addNumber(total);
							smsg.addItemName(cropId);
							player.sendPacket(smsg);

							if (player.isInParty())
							{
								smsg = SystemMessage.getSystemMessage(SystemMessageId.S1_HARVESTED_S3_S2S);
								smsg.addPcName(player);
								smsg.addNumber(total);
								smsg.addItemName(cropId);
								player.getParty().broadcastToPartyMembers(player, smsg);
							}

							if (iu != null)
								player.sendPacket(iu);
							else
								player.sendPacket(new ItemList(player, false));

							smsg = null;
						}
					}
				}
				else
					player.sendPacket(SystemMessageId.THE_HARVEST_HAS_FAILED);
			}
			else
				player.sendPacket(SystemMessageId.THE_HARVEST_FAILED_BECAUSE_THE_SEED_WAS_NOT_SOWN);
		}

	}

	private static boolean calcSuccess(L2Character activeChar, L2Character target)
	{
		int basicSuccess = 100;
		final int levelPlayer = activeChar.getLevel();
		final int levelTarget = target.getLevel();

		int diff = (levelPlayer - levelTarget);
		if (diff < 0)
			diff = -diff;

		// apply penalty, target <=> player levels, 5% penalty for each level
		if (diff > 5)
			basicSuccess -= (diff - 5) * 5;

		// success rate cant be less than 1%
		if (basicSuccess < 1)
			basicSuccess = 1;

		return Rnd.nextInt(99) < basicSuccess;
	}

	@Override
	public L2SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}