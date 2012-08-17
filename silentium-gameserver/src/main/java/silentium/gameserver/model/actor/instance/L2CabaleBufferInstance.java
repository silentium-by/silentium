/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.model.actor.instance;

import java.util.Collection;
import java.util.concurrent.ScheduledFuture;

import silentium.gameserver.ThreadPoolManager;
import silentium.gameserver.ai.CtrlIntention;
import silentium.gameserver.model.L2Object;
import silentium.gameserver.model.L2Skill;
import silentium.gameserver.model.actor.L2Character;
import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.entity.sevensigns.SevenSigns;
import silentium.gameserver.network.SystemMessageId;
import silentium.gameserver.network.serverpackets.ActionFailed;
import silentium.gameserver.network.serverpackets.MagicSkillUse;
import silentium.gameserver.network.serverpackets.MoveToPawn;
import silentium.gameserver.network.serverpackets.MyTargetSelected;
import silentium.gameserver.network.serverpackets.SystemMessage;
import silentium.gameserver.network.serverpackets.ValidateLocation;
import silentium.gameserver.tables.SkillTable;
import silentium.gameserver.templates.chars.L2NpcTemplate;

/**
 * @author Layane
 */
public class L2CabaleBufferInstance extends L2NpcInstance
{
	@Override
	public void onAction(L2PcInstance player)
	{
		if (!player.canTarget())
			return;

		if (this != player.getTarget())
		{
			// Set the target of the L2PcInstance player
			player.setTarget(this);

			// Send a Server->Client packet MyTargetSelected to the L2PcInstance player
			player.sendPacket(new MyTargetSelected(getObjectId(), 0));

			// Send a Server->Client packet ValidateLocation to correct the L2ArtefactInstance position and heading on the client
			player.sendPacket(new ValidateLocation(this));
		}
		else
		{
			// Calculate the distance between the L2PcInstance and the L2Npc
			if (!canInteract(player))
			{
				// Notify the L2PcInstance AI with AI_INTENTION_INTERACT
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
			}
			else
			{
				// Rotate the player to face the instance
				player.sendPacket(new MoveToPawn(player, this, L2Npc.INTERACTION_DISTANCE));

				// Send ActionFailed to the player in order to avoid he stucks
				player.sendPacket(ActionFailed.STATIC_PACKET);
			}
		}
	}

	private ScheduledFuture<?> _aiTask;

	/**
	 * For each known player in range, cast either the positive or negative buff. <BR>
	 * The stats affected depend on the player type, either a fighter or a mystic. <BR>
	 * <BR>
	 * Curse of Destruction (Loser)<BR>
	 * - Fighters: -25% Accuracy, -25% Effect Resistance<BR>
	 * - Mystics: -25% Casting Speed, -25% Effect Resistance<BR>
	 * <BR>
	 * Blessing of Prophecy (Winner)<BR>
	 * - Fighters: +25% Max Load, +25% Effect Resistance<BR>
	 * - Mystics: +25% Magic Cancel Resist, +25% Effect Resistance<BR>
	 */
	private class CabaleAI implements Runnable
	{
		private final L2CabaleBufferInstance _caster;

		protected CabaleAI(L2CabaleBufferInstance caster)
		{
			_caster = caster;
		}

		@Override
		public void run()
		{
			boolean isBuffAWinner = false;
			boolean isBuffALoser = false;

			final int winningCabal = SevenSigns.getInstance().getCabalHighestScore();
			int losingCabal = SevenSigns.CABAL_NULL;

			// Defines which cabal is the loser.
			if (winningCabal == SevenSigns.CABAL_DAWN)
				losingCabal = SevenSigns.CABAL_DUSK;
			else if (winningCabal == SevenSigns.CABAL_DUSK)
				losingCabal = SevenSigns.CABAL_DAWN;

			final Collection<L2PcInstance> plrs = getKnownList().getKnownPlayers().values();
			for (L2PcInstance player : plrs)
			{
				// Don't go further if player is not online, dead, not visible or too far.
				if (player == null || player.isDead() || !player.isVisible() || !isInsideRadius(player, getDistanceToWatchObject(player), false, false))
					continue;

				final int playerCabal = SevenSigns.getInstance().getPlayerCabal(player.getObjectId());

				// Don't go further if player isn't from Dawn or Dusk sides.
				if (playerCabal != SevenSigns.CABAL_NULL)
				{
					if (!isBuffAWinner && playerCabal == winningCabal && _caster.getNpcId() == SevenSigns.ORATOR_NPC_ID)
					{
						isBuffAWinner = true;
						handleCast(player, (!player.isMageClass() ? 4364 : 4365));
					}
					else if (!isBuffALoser && playerCabal == losingCabal && _caster.getNpcId() == SevenSigns.PREACHER_NPC_ID)
					{
						isBuffALoser = true;
						handleCast(player, (!player.isMageClass() ? 4361 : 4362));
					}

					// Buff / debuff only 1 ppl per round.
					if (isBuffAWinner && isBuffALoser)
						break;
				}
			}
		}

		private void handleCast(L2PcInstance player, int skillId)
		{
			int skillLevel = (player.getLevel() > 40) ? 1 : 2;

			final L2Skill skill = SkillTable.getInstance().getInfo(skillId, skillLevel);
			if (player.getFirstEffect(skill) == null)
			{
				skill.getEffects(_caster, player);
				broadcastPacket(new MagicSkillUse(_caster, player, skill.getId(), skillLevel, skill.getHitTime(), 0));
				player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT).addSkillName(skillId));
			}
		}
	}

	public L2CabaleBufferInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);

		if (_aiTask != null)
			_aiTask.cancel(true);

		_aiTask = ThreadPoolManager.getInstance().scheduleAiAtFixedRate(new CabaleAI(this), 3000, 3000);
	}

	@Override
	public void deleteMe()
	{
		if (_aiTask != null)
		{
			_aiTask.cancel(true);
			_aiTask = null;
		}

		super.deleteMe();
	}

	@Override
	public int getDistanceToWatchObject(L2Object object)
	{
		return 900;
	}

	@Override
	public boolean isAutoAttackable(L2Character attacker)
	{
		return false;
	}
}