/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.handlers.skill;

import silentium.commons.utils.Rnd;
import silentium.gameserver.ai.CtrlIntention;
import silentium.gameserver.handler.ISkillHandler;
import silentium.gameserver.model.L2Manor;
import silentium.gameserver.model.L2Object;
import silentium.gameserver.model.L2Skill;
import silentium.gameserver.model.actor.L2Character;
import silentium.gameserver.model.actor.instance.L2MonsterInstance;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.quest.QuestState;
import silentium.gameserver.network.SystemMessageId;
import silentium.gameserver.network.serverpackets.ActionFailed;
import silentium.gameserver.network.serverpackets.PlaySound;
import silentium.gameserver.network.serverpackets.SystemMessage;
import silentium.gameserver.templates.skills.L2SkillType;

/**
 * @author l3x
 */
public class Sow implements ISkillHandler
{
	private static final L2SkillType[] SKILL_IDS = { L2SkillType.SOW };

	@Override
	public void useSkill(L2Character activeChar, L2Skill skill, L2Object[] targets)
	{
		if (!(activeChar instanceof L2PcInstance))
			return;

		final L2Object[] targetList = skill.getTargetList(activeChar);
		if (targetList == null || targetList.length == 0)
			return;

		L2PcInstance player = (L2PcInstance) activeChar;
		L2MonsterInstance target;

		for (L2Object tgt : targetList)
		{
			if (!(tgt instanceof L2MonsterInstance))
				continue;

			target = (L2MonsterInstance) tgt;
			if (target.isDead() || target.isSeeded() || target.getSeederId() != activeChar.getObjectId())
			{
				activeChar.sendPacket(ActionFailed.STATIC_PACKET);
				continue;
			}

			final int seedId = target.getSeedType();
			if (seedId == 0)
			{
				activeChar.sendPacket(ActionFailed.STATIC_PACKET);
				continue;
			}

			// Consuming used seed
			if (!activeChar.destroyItemByItemId("Consume", seedId, 1, target, false))
			{
				activeChar.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}

			SystemMessage sm;
			if (calcSuccess(activeChar, target, seedId))
			{
				player.sendPacket(new PlaySound(QuestState.SOUND_ITEMGET));
				target.setSeeded((L2PcInstance) activeChar);
				sm = SystemMessage.getSystemMessage(SystemMessageId.THE_SEED_WAS_SUCCESSFULLY_SOWN);
			}
			else
				sm = SystemMessage.getSystemMessage(SystemMessageId.THE_SEED_WAS_NOT_SOWN);

			if (!player.isInParty())
				player.sendPacket(sm);
			else
				player.getParty().broadcastToPartyMembers(sm);

			target.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		}

	}

	private static boolean calcSuccess(L2Character activeChar, L2Character target, int seedId)
	{
		int basicSuccess = (L2Manor.getInstance().isAlternative(seedId) ? 20 : 90);
		final int minlevelSeed = L2Manor.getInstance().getSeedMinLevel(seedId);
		final int maxlevelSeed = L2Manor.getInstance().getSeedMaxLevel(seedId);
		final int levelPlayer = activeChar.getLevel(); // Attacker Level
		final int levelTarget = target.getLevel(); // target Level

		// Seed level
		if (levelTarget < minlevelSeed)
			basicSuccess -= 5 * (minlevelSeed - levelTarget);
		if (levelTarget > maxlevelSeed)
			basicSuccess -= 5 * (levelTarget - maxlevelSeed);

		// 5% decrease in chance if player level is more than +/- 5 levels to _target's_ level
		int diff = (levelPlayer - levelTarget);
		if (diff < 0)
			diff = -diff;
		if (diff > 5)
			basicSuccess -= 5 * (diff - 5);

		// Chance can't be less than 1%
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