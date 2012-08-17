/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.quests;

import java.util.HashMap;
import java.util.Map;

import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.quest.Quest;
import silentium.gameserver.model.quest.QuestState;
import silentium.gameserver.scripting.ScriptFile;

public class Q631_DeliciousTopChoiceMeat extends Quest implements ScriptFile
{
	private static final String qn = "Q631_DeliciousTopChoiceMeat";

	// NPC
	private static final int TUNATUN = 31537;

	// Item
	private static final int TOP_QUALITY_MEAT = 7546;

	// Rewards
	private static final Map<String, int[]> Rewards = new HashMap<>();

	{
		Rewards.put("1", new int[] { 4039, 15 });
		Rewards.put("2", new int[] { 4040, 15 });
		Rewards.put("3", new int[] { 4041, 15 });
		Rewards.put("4", new int[] { 4042, 10 });
		Rewards.put("5", new int[] { 4043, 10 });
		Rewards.put("6", new int[] { 4044, 5 });
	}

	public Q631_DeliciousTopChoiceMeat(int questId, String name, String descr)
	{
		super(questId, name, descr);

		questItemIds = new int[] { TOP_QUALITY_MEAT };

		addStartNpc(TUNATUN);
		addTalkId(TUNATUN);

		for (int num1 = 21460; num1 <= 21468; num1++)
			addKillId(num1);

		for (int num2 = 21479; num2 <= 21487; num2++)
			addKillId(num2);

		for (int num3 = 21498; num3 <= 21506; num3++)
			addKillId(num3);
	}

	public static void onLoad()
	{
		new Q631_DeliciousTopChoiceMeat(631, "Q631_DeliciousTopChoiceMeat", "quests");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event.equalsIgnoreCase("31537-03.htm"))
		{
			if (player.getLevel() >= 65 && player.getLevel() <= 73)
			{
				st.setState(QuestState.STARTED);
				st.set("cond", "1");
				st.playSound(QuestState.SOUND_ACCEPT);
			}
			else
			{
				htmltext = "31537-02.htm";
				st.exitQuest(true);
			}
		}
		else if (Rewards.containsKey(event))
		{
			if (st.getQuestItemsCount(TOP_QUALITY_MEAT) >= 120)
			{
				htmltext = "31537-06.htm";
				st.takeItems(TOP_QUALITY_MEAT, -1);
				st.rewardItems(Rewards.get(event)[0], Rewards.get(event)[1]);
				st.playSound(QuestState.SOUND_FINISH);
				st.exitQuest(true);
			}
			else
			{
				st.set("cond", "1");
				htmltext = "31537-07.htm";
			}
		}

		return htmltext;
	}

	@Override
	public String onTalk(L2Npc npc, L2PcInstance player)
	{
		String htmltext = getNoQuestMsg();
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		switch (st.getState())
		{
			case QuestState.CREATED:
				htmltext = "31537-01.htm";
				break;

			case QuestState.STARTED:
				int cond = st.getInt("cond");
				if (cond == 1)
					htmltext = "31537-03a.htm";
				else if (cond == 2)
				{
					if (st.getQuestItemsCount(TOP_QUALITY_MEAT) >= 120)
						htmltext = "31537-04.htm";
					else
					{
						st.set("cond", "1");
						htmltext = "31537-03a.htm";
					}
				}
				break;
		}

		return htmltext;
	}

	@Override
	public String onKill(L2Npc npc, L2PcInstance player, boolean isPet)
	{
		L2PcInstance partyMember = getRandomPartyMember(player, npc, "1");
		if (partyMember == null)
			return null;

		QuestState st = partyMember.getQuestState(qn);

		if (st.dropAlwaysQuestItems(TOP_QUALITY_MEAT, 1, 120))
			st.set("cond", "2");

		return null;
	}
}