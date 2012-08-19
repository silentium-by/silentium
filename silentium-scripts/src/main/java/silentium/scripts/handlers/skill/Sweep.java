/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.handlers.skill;

import silentium.gameserver.configs.MainConfig;
import silentium.gameserver.handler.ISkillHandler;
import silentium.gameserver.model.L2ItemInstance;
import silentium.gameserver.model.L2Object;
import silentium.gameserver.model.L2Skill;
import silentium.gameserver.model.actor.L2Attackable;
import silentium.gameserver.model.actor.L2Character;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.network.SystemMessageId;
import silentium.gameserver.network.serverpackets.InventoryUpdate;
import silentium.gameserver.network.serverpackets.ItemList;
import silentium.gameserver.network.serverpackets.SystemMessage;
import silentium.gameserver.templates.skills.L2SkillType;

/**
 * @author _drunk_
 */
public class Sweep implements ISkillHandler {
	private static final L2SkillType[] SKILL_IDS = { L2SkillType.SWEEP };

	@Override
	public void useSkill(final L2Character activeChar, final L2Skill skill, final L2Object... targets) {
		if (!(activeChar instanceof L2PcInstance))
			return;

		final L2PcInstance player = (L2PcInstance) activeChar;
		final InventoryUpdate iu = MainConfig.FORCE_INVENTORY_UPDATE ? null : new InventoryUpdate();
		boolean send = false;

		for (final L2Object target1 : targets) {
			if (!(target1 instanceof L2Attackable))
				continue;
			final L2Attackable target = (L2Attackable) target1;
			L2Attackable.RewardItem[] items = null;
			boolean isSweeping = false;
			synchronized (target) {
				if (target.isSweepActive()) {
					items = target.takeSweep();
					isSweeping = true;
				}
			}
			if (isSweeping) {
				if (items == null || items.length == 0)
					continue;
				for (final L2Attackable.RewardItem ritem : items) {
					if (player.isInParty())
						player.getParty().distributeItem(player, ritem, true, target);
					else {
						final L2ItemInstance item = player.getInventory().addItem("Sweep", ritem.getItemId(), ritem.getCount(), player, target);
						if (iu != null)
							iu.addItem(item);
						send = true;

						SystemMessage smsg;
						if (ritem.getCount() > 1) {
							smsg = SystemMessage.getSystemMessage(SystemMessageId.EARNED_S2_S1_S); // earned $s2$s1
							smsg.addItemName(ritem.getItemId());
							smsg.addNumber(ritem.getCount());
						} else {
							smsg = SystemMessage.getSystemMessage(SystemMessageId.EARNED_ITEM_S1); // earned $s1
							smsg.addItemName(ritem.getItemId());
						}
						player.sendPacket(smsg);
						smsg = null;
					}
				}
			}
			target.endDecayTask();

			if (send) {
				if (iu != null)
					player.sendPacket(iu);
				else
					player.sendPacket(new ItemList(player, false));
			}
		}
	}

	@Override
	public L2SkillType[] getSkillIds() {
		return SKILL_IDS;
	}
}