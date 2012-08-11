/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.quests;

import java.util.HashMap;
import java.util.Map;

import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.quest.Quest;
import silentium.gameserver.model.quest.QuestState;

public class Q640_TheZeroHour extends Quest
{
	private static final String qn = "Q640_TheZeroHour";

	// NPC
	private final static int KAHMAN = 31554;

	// Item
	private final static int FANG = 8085;

	private static final Map<String, int[]> REWARDS = new HashMap<>();
	{
		REWARDS.put("1", new int[] { 12, 4042, 1 });
		REWARDS.put("2", new int[] { 6, 4043, 1 });
		REWARDS.put("3", new int[] { 6, 4044, 1 });
		REWARDS.put("4", new int[] { 81, 1887, 10 });
		REWARDS.put("5", new int[] { 33, 1888, 5 });
		REWARDS.put("6", new int[] { 30, 1889, 10 });
		REWARDS.put("7", new int[] { 150, 5550, 10 });
		REWARDS.put("8", new int[] { 131, 1890, 10 });
		REWARDS.put("9", new int[] { 123, 1893, 5 });
	}

	public Q640_TheZeroHour(int questId, String name, String descr)
	{
		super(questId, name, descr);
		questItemIds = new int[] { FANG };

		addStartNpc(KAHMAN);
		addTalkId(KAHMAN);

		// All "spiked" stakatos types, except babies and cannibalistic followers.
		addKillId(22105, 22106, 22107, 22108, 22109, 22110, 22111, 22113, 22114, 22115, 22116, 22117, 22118, 22119, 22121);
	}

	public static void main(String[] args)
	{
		new Q640_TheZeroHour(640, "Q640_TheZeroHour", "The Zero Hour");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event.equalsIgnoreCase("31554-02.htm"))
		{
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("31554-05.htm"))
		{
			if (!st.hasQuestItems(FANG))
				htmltext = "31554-06.htm";
		}
		else if (event.equalsIgnoreCase("31554-08.htm"))
		{
			st.playSound(QuestState.SOUND_FINISH);
			st.exitQuest(true);
		}
		else if (REWARDS.containsKey(event))
		{
			int cost = REWARDS.get(event)[0];
			int item = REWARDS.get(event)[1];
			int amount = REWARDS.get(event)[2];

			if (st.getQuestItemsCount(FANG) >= cost)
			{
				st.takeItems(FANG, cost);
				st.rewardItems(item, amount);
				htmltext = "31554-09.htm";
			}
			else
				htmltext = "31554-06.htm";
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
				if (player.getLevel() >= 66)
				{
					QuestState st2 = player.getQuestState("Q109_InSearchOfTheNest");
					if (st2 != null && st2.isCompleted())
						htmltext = "31554-01.htm";
					else
						htmltext = "31554-10.htm";
				}
				else
				{
					htmltext = "31554-00.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				if (st.hasQuestItems(FANG))
					htmltext = "31554-04.htm";
				else
					htmltext = "31554-03.htm";
				break;
		}

		return htmltext;
	}

	@Override
	public String onKill(L2Npc npc, L2PcInstance player, boolean isPet)
	{
		L2PcInstance partyMember = getRandomPartyMemberState(player, npc, QuestState.STARTED);
		if (partyMember == null)
			return null;

		QuestState st = partyMember.getQuestState(qn);

		st.giveItems(FANG, 1);
		st.playSound(QuestState.SOUND_ITEMGET);
		return null;
	}
}