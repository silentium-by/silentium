/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.model.actor.instance;

import silentium.commons.utils.Rnd;
import silentium.gameserver.ThreadPoolManager;
import silentium.gameserver.ai.AttackableAI;
import silentium.gameserver.ai.CtrlIntention;
import silentium.gameserver.data.html.StaticHtmPath;
import silentium.gameserver.model.L2CharPosition;
import silentium.gameserver.model.L2World;
import silentium.gameserver.model.L2WorldRegion;
import silentium.gameserver.model.actor.L2Attackable;
import silentium.gameserver.model.actor.L2Character;
import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.knownlist.GuardKnownList;
import silentium.gameserver.model.quest.Quest;
import silentium.gameserver.network.serverpackets.ActionFailed;
import silentium.gameserver.network.serverpackets.MoveToPawn;
import silentium.gameserver.network.serverpackets.MyTargetSelected;
import silentium.gameserver.network.serverpackets.ValidateLocation;
import silentium.gameserver.templates.chars.L2NpcTemplate;

/**
 * This class manages all Guards in the world.<br>
 * It inherits all methods from L2Attackable and adds some more such as:
 * <ul>
 * <li>tracking PK</li>
 * <li>aggressive L2MonsterInstance.</li>
 * </ul>
 */
public final class L2GuardInstance extends L2Attackable
{
	private static final int RETURN_INTERVAL = 60000;

	public class ReturnTask implements Runnable
	{
		@Override
		public void run()
		{
			if (getAI().getIntention() == CtrlIntention.AI_INTENTION_IDLE)
				returnHome();
		}
	}

	public L2GuardInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
		ThreadPoolManager.getInstance().scheduleAiAtFixedRate(new ReturnTask(), RETURN_INTERVAL, RETURN_INTERVAL + Rnd.nextInt(60000));
	}

	@Override
	public void initKnownList()
	{
		setKnownList(new GuardKnownList(this));
	}

	@Override
	public final GuardKnownList getKnownList()
	{
		return (GuardKnownList) super.getKnownList();
	}

	@Override
	public boolean isAutoAttackable(L2Character attacker)
	{
		return attacker instanceof L2MonsterInstance;
	}

	/**
	 * Notify the L2GuardInstance to return to its home location (AI_INTENTION_MOVE_TO) and clear its _aggroList.<BR>
	 * <BR>
	 */
	@Override
	public void returnHome()
	{
		if (!isInsideRadius(getSpawn().getLocx(), getSpawn().getLocy(), L2Npc.INTERACTION_DISTANCE, false))
		{
			clearAggroList();
			getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new L2CharPosition(getSpawn().getLocx(), getSpawn().getLocy(), getSpawn().getLocz(), 0));
		}
	}

	@Override
	public void onSpawn()
	{
		setIsNoRndWalk(true);
		super.onSpawn();

		// check the region where this mob is, do not activate the AI if region is inactive.
		L2WorldRegion region = L2World.getInstance().getRegion(getX(), getY());
		if (region != null && !region.isActive())
			((AttackableAI) getAI()).stopAITask();
	}

	@Override
	public String getHtmlPath(int npcId, int val)
	{
		String pom = "";
		if (val == 0)
			pom = "" + npcId;
		else
			pom = npcId + "-" + val;

		return StaticHtmPath.GuardHtmPath + pom + ".htm";
	}

	@Override
	public void onAction(L2PcInstance player)
	{
		if (!player.canTarget())
			return;

		// Check if the L2PcInstance already target the L2GuardInstance
		if (getObjectId() != player.getTargetId())
		{
			// Set the target of the L2PcInstance player
			player.setTarget(this);

			// Send a Server->Client packet MyTargetSelected to the L2PcInstance player
			player.sendPacket(new MyTargetSelected(getObjectId(), 0));

			// Send a Server->Client packet ValidateLocation to correct the L2Npc position and heading on the client
			player.sendPacket(new ValidateLocation(this));
		}
		else
		{
			// Check if the L2PcInstance is in the _aggroList of the L2GuardInstance
			if (containsTarget(player))
			{
				// Set the L2PcInstance Intention to AI_INTENTION_ATTACK
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
			}
			else
			{
				// Calculate the distance between the L2PcInstance and the L2Npc
				if (!canInteract(player))
				{
					// Set the L2PcInstance Intention to AI_INTENTION_INTERACT
					player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
				}
				else
				{
					// Some guards have no HTMs on retail. Bypass the chat window if such guard is met.
					switch (getNpcId())
					{
						case 31671:
						case 31672:
						case 31673:
						case 31674:
							// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait
							// another packet
							player.sendPacket(ActionFailed.STATIC_PACKET);
							return;
					}

					// Rotate the player to face the instance
					player.sendPacket(new MoveToPawn(player, this, L2Npc.INTERACTION_DISTANCE));

					if (hasRandomAnimation())
						onRandomAnimation(Rnd.get(8));

					Quest[] qlsa = getTemplate().getEventQuests(Quest.QuestEventType.QUEST_START);
					if ((qlsa != null) && qlsa.length > 0)
						player.setLastQuestNpcObject(getObjectId());
					Quest[] qlst = getTemplate().getEventQuests(Quest.QuestEventType.ON_FIRST_TALK);
					if ((qlst != null) && qlst.length == 1)
						qlst[0].notifyFirstTalk(this, player);
					else
						showChatWindow(player, 0);
				}
			}
		}
	}
}