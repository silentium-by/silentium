/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.quests;

import silentium.commons.utils.Rnd;
import silentium.gameserver.ai.CtrlIntention;
import silentium.gameserver.model.L2CharPosition;
import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.quest.Quest;
import silentium.gameserver.model.quest.QuestState;

public class Q652_AnAgedExAdventurer extends Quest
{
	private static final String qn = "Q652_AnAgedExAdventurer";

	// NPCs
	private static final int TANTAN = 32012;
	private static final int SARA = 30180;

	// Item
	private static final int CSS = 1464;

	// Reward
	private static final int EAD = 956;

	// Table of possible spawns
	private static final int[][] spawns = { { 78355, -1325, -3659 }, { 79890, -6132, -2922 }, { 90012, -7217, -3085 }, { 94500, -10129, -3290 }, { 96534, -1237, -3677 } };

	// Current position
	private int _currentPosition = 0;

	public Q652_AnAgedExAdventurer(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(TANTAN);
		addTalkId(TANTAN, SARA);

		addSpawn(TANTAN, 78355, -1325, -3659, 0, false, 0);
	}

	public static void main(String[] args)
	{
		new Q652_AnAgedExAdventurer(652, "Q652_AnAgedExAdventurer", "An Aged Ex-Adventurer");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event.equalsIgnoreCase("32012-02.htm"))
		{
			if (st.getQuestItemsCount(CSS) >= 100)
			{
				st.set("cond", "1");
				st.setState(QuestState.STARTED);
				st.takeItems(CSS, 100);
				st.playSound(QuestState.SOUND_ACCEPT);

				npc.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new L2CharPosition(85326, 7869, -3620, 0));
				startQuestTimer("apparition_npc", 6000, npc, player);
			}
			else
			{
				htmltext = "32012-02a.htm";
				st.exitQuest(true);
			}
		}
		else if (event.equalsIgnoreCase("apparition_npc"))
		{
			int chance = Rnd.get(5);

			// Loop to avoid to spawn to the same place.
			while (chance == _currentPosition)
				chance = Rnd.get(5);

			// Register new position.
			_currentPosition = chance;

			npc.deleteMe();
			addSpawn(TANTAN, spawns[chance][0], spawns[chance][1], spawns[chance][2], 0, false, 0);
			return null;
		}
		return htmltext;
	}

	@Override
	public String onTalk(L2Npc npc, L2PcInstance player)
	{
		QuestState st = player.getQuestState(qn);
		String htmltext = getNoQuestMsg();
		if (st == null)
			return htmltext;

		switch (st.getState())
		{
			case QuestState.CREATED:
				if (player.getLevel() >= 46)
					htmltext = "32012-01.htm";
				else
				{
					htmltext = "32012-00.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				switch (npc.getNpcId())
				{
					case SARA:
						if (Rnd.get(100) < 50)
						{
							htmltext = "30180-01.htm";
							st.rewardItems(57, 5026);
							st.giveItems(EAD, 1);
						}
						else
						{
							htmltext = "30180-02.htm";
							st.rewardItems(57, 10000);
						}
						st.playSound(QuestState.SOUND_FINISH);
						st.exitQuest(true);
						break;

					case TANTAN:
						htmltext = "32012-04a.htm";
						break;
				}
				break;
		}
		return htmltext;
	}
}