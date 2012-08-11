/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.network.clientpackets;

import silentium.gameserver.configs.PlayersConfig;
import silentium.gameserver.data.xml.SkillTreeData;
import silentium.gameserver.data.xml.SpellbookData;
import silentium.gameserver.model.L2PledgeSkillLearn;
import silentium.gameserver.model.L2ShortCut;
import silentium.gameserver.model.L2Skill;
import silentium.gameserver.model.L2SkillLearn;
import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.instance.L2FishermanInstance;
import silentium.gameserver.model.actor.instance.L2NpcInstance;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.actor.instance.L2VillageMasterInstance;
import silentium.gameserver.network.SystemMessageId;
import silentium.gameserver.network.serverpackets.ExStorageMaxCount;
import silentium.gameserver.network.serverpackets.PledgeSkillList;
import silentium.gameserver.network.serverpackets.ShortCutRegister;
import silentium.gameserver.network.serverpackets.StatusUpdate;
import silentium.gameserver.network.serverpackets.SystemMessage;
import silentium.gameserver.tables.SkillTable;

public class RequestAcquireSkill extends L2GameClientPacket
{
	private int _skillId;
	private int _skillLevel;
	private int _skillType;

	@Override
	protected void readImpl()
	{
		_skillId = readD();
		_skillLevel = readD();
		_skillType = readD();
	}

	@Override
	protected void runImpl()
	{
		// Not valid skill data, return.
		if (_skillId <= 0 || _skillLevel <= 0)
			return;

		// Incorrect player, return.
		final L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;

		// Incorrect npc, return.
		final L2Npc trainer = activeChar.getCurrentFolkNPC();
		if (trainer == null)
			return;

		// Distance check for player <-> npc.
		if (!activeChar.isInsideRadius(trainer, L2Npc.INTERACTION_DISTANCE, false, false) && !activeChar.isGM())
			return;

		// Skill doesn't exist, return.
		final L2Skill skill = SkillTable.getInstance().getInfo(_skillId, _skillLevel);
		if (skill == null)
			return;

		// Set learn class.
		activeChar.setSkillLearningClassId(activeChar.getClassId());

		boolean exists = false;

		// Types.
		switch (_skillType)
		{
		// General skills.
			case 0:
				// Player already has such skill with same or higher level.
				int skillLvl = activeChar.getSkillLevel(_skillId);
				if (skillLvl >= _skillLevel)
					return;

				// Requested skill must be 1 level higher than existing skill.
				if (Math.max(skillLvl, 0) + 1 != _skillLevel)
					return;

				int spCost = 0;

				// Find skill information.
				for (L2SkillLearn sl : SkillTreeData.getInstance().getAvailableSkills(activeChar, activeChar.getSkillLearningClassId()))
				{
					// Skill found.
					if (sl.getId() == _skillId && sl.getLevel() == _skillLevel)
					{
						exists = true;
						spCost = sl.getSpCost();
						break;
					}
				}

				// No skill found, return.
				if (!exists)
					return;

				// Not enought SP.
				if (activeChar.getSp() < spCost)
				{
					activeChar.sendPacket(SystemMessageId.NOT_ENOUGH_SP_TO_LEARN_SKILL);
					L2NpcInstance.showSkillList(activeChar, trainer, activeChar.getSkillLearningClassId());
					return;
				}

				// Get spellbook and try to consume it.
				int spbId = SpellbookData.getInstance().getBookForSkill(_skillId, _skillLevel);
				if (spbId > 0)
				{
					if (!activeChar.destroyItemByItemId("SkillLearn", spbId, 1, trainer, false))
					{
						activeChar.sendPacket(SystemMessageId.ITEM_MISSING_TO_LEARN_SKILL);
						L2NpcInstance.showSkillList(activeChar, trainer, activeChar.getSkillLearningClassId());
						return;
					}

					SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_DISAPPEARED);
					sm.addItemName(spbId);
					activeChar.sendPacket(sm);
				}

				// Consume SP.
				activeChar.setSp(activeChar.getSp() - spCost);
				StatusUpdate su = new StatusUpdate(activeChar);
				su.addAttribute(StatusUpdate.SP, activeChar.getSp());
				activeChar.sendPacket(su);

				// Add skill new skill.
				activeChar.addSkill(skill, true);
				SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.LEARNED_SKILL_S1);
				sm.addSkillName(skill);
				activeChar.sendPacket(sm);

				// Update player and return.
				updateShortCuts(activeChar);
				activeChar.sendSkillList();
				L2NpcInstance.showSkillList(activeChar, trainer, activeChar.getSkillLearningClassId());
				break;
			// Common skills.
			case 1:
				skillLvl = activeChar.getSkillLevel(_skillId);
				if (skillLvl >= _skillLevel)
					return;

				if (Math.max(skillLvl, 0) + 1 != _skillLevel)
					return;

				int costId = 0;
				int costCount = 0;

				for (L2SkillLearn sl : SkillTreeData.getInstance().getAvailableFishingDwarvenCraftSkills(activeChar))
				{
					if (sl.getId() == _skillId && sl.getLevel() == _skillLevel)
					{
						exists = true;
						costId = sl.getIdCost();
						costCount = sl.getCostCount();
						break;
					}
				}

				if (!exists)
					return;

				if (!activeChar.destroyItemByItemId("Consume", costId, costCount, trainer, false))
				{
					activeChar.sendPacket(SystemMessageId.ITEM_MISSING_TO_LEARN_SKILL);
					L2FishermanInstance.showFishSkillList(activeChar);
					return;
				}

				sm = SystemMessage.getSystemMessage(SystemMessageId.S2_S1_DISAPPEARED);
				sm.addItemName(costId);
				sm.addItemNumber(costCount);
				activeChar.sendPacket(sm);

				activeChar.addSkill(skill, true);
				sm = SystemMessage.getSystemMessage(SystemMessageId.LEARNED_SKILL_S1);
				sm.addSkillName(skill);
				activeChar.sendPacket(sm);

				if (_skillId >= 1368 && _skillId <= 1372)
					activeChar.sendPacket(new ExStorageMaxCount(activeChar));

				updateShortCuts(activeChar);
				activeChar.sendSkillList();
				L2FishermanInstance.showFishSkillList(activeChar);
				break;
			// Pledge skills.
			case 2:
				if (!activeChar.isClanLeader())
					return;

				int itemId = 0;
				int repCost = 0;

				for (L2PledgeSkillLearn psl : SkillTreeData.getInstance().getAvailablePledgeSkills(activeChar))
				{
					if (psl.getId() == _skillId && psl.getLevel() == _skillLevel)
					{
						exists = true;
						itemId = psl.getItemId();
						repCost = psl.getRepCost();
						break;
					}
				}

				if (!exists)
					return;

				if (activeChar.getClan().getReputationScore() < repCost)
				{
					activeChar.sendPacket(SystemMessageId.ACQUIRE_SKILL_FAILED_BAD_CLAN_REP_SCORE);
					L2VillageMasterInstance.showPledgeSkillList(activeChar);
					return;
				}

				if (PlayersConfig.LIFE_CRYSTAL_NEEDED)
				{
					if (!activeChar.destroyItemByItemId("Consume", itemId, 1, trainer, false))
					{
						activeChar.sendPacket(SystemMessageId.ITEM_MISSING_TO_LEARN_SKILL);
						L2VillageMasterInstance.showPledgeSkillList(activeChar);
						return;
					}

					sm = SystemMessage.getSystemMessage(SystemMessageId.S2_S1_DISAPPEARED);
					sm.addItemName(itemId);
					sm.addItemNumber(1);
					activeChar.sendPacket(sm);
				}

				activeChar.getClan().takeReputationScore(repCost);
				activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_DEDUCTED_FROM_CLAN_REP).addNumber(repCost));

				activeChar.getClan().addNewSkill(skill);
				activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.CLAN_SKILL_S1_ADDED).addSkillName(_skillId));
				activeChar.getClan().broadcastToOnlineMembers(new PledgeSkillList(activeChar.getClan()));

				for (L2PcInstance member : activeChar.getClan().getOnlineMembers(0))
					member.sendSkillList();
				L2VillageMasterInstance.showPledgeSkillList(activeChar);
				return;
		}
	}

	private void updateShortCuts(L2PcInstance player)
	{
		if (_skillLevel > 1)
		{
			for (L2ShortCut sc : player.getAllShortCuts())
			{
				if (sc.getId() == _skillId && sc.getType() == L2ShortCut.TYPE_SKILL)
				{
					L2ShortCut newsc = new L2ShortCut(sc.getSlot(), sc.getPage(), L2ShortCut.TYPE_SKILL, _skillId, _skillLevel, 1);
					player.sendPacket(new ShortCutRegister(newsc));
					player.registerShortCut(newsc);
				}
			}
		}
	}
}