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

import silentium.commons.utils.Rnd;
import silentium.gameserver.configs.MainConfig;
import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.quest.Quest;
import silentium.gameserver.model.quest.QuestState;
import silentium.gameserver.scripting.ScriptFile;

public class Q645_GhostsOfBatur extends Quest implements ScriptFile
{
	private static final String qn = "Q645_GhostsOfBatur";

	// NPC
	private static final int KARUDA = 32017;

	// Item
	private static final int GRAVE_GOODS = 8089;

	// Rewards
	private static final Map<String, int[]> Rewards = new HashMap<>();

	{
		Rewards.put("BDH", new int[] { 1878, 18 });
		Rewards.put("CKS", new int[] { 1879, 7 });
		Rewards.put("STL", new int[] { 1880, 4 });
		Rewards.put("CBP", new int[] { 1881, 6 });
		Rewards.put("LTR", new int[] { 1882, 10 });
		Rewards.put("STM", new int[] { 1883, 2 });
	}

	public Q645_GhostsOfBatur(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(KARUDA);
		addTalkId(KARUDA);

		addKillId(22007, 22009, 22010, 22011, 22012, 22013, 22014, 22015, 22016);
	}

	public static void onLoad()
	{
		new Q645_GhostsOfBatur(645, "Q645_GhostsOfBatur", "quests");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event.equalsIgnoreCase("32017-03.htm"))
		{
			if (player.getLevel() >= 23 && player.getLevel() <= 36)
			{
				st.set("cond", "1");
				st.setState(QuestState.STARTED);
				st.playSound(QuestState.SOUND_ACCEPT);
			}
			else
			{
				htmltext = "32017-02.htm";
				st.exitQuest(true);
			}
		}
		else if (Rewards.containsKey(event))
		{
			if (st.getQuestItemsCount(GRAVE_GOODS) == 180)
			{
				htmltext = "32017-07.htm";
				st.takeItems(GRAVE_GOODS, -1);
				st.giveItems(Rewards.get(event)[0], Rewards.get(event)[1]);
				st.playSound(QuestState.SOUND_FINISH);
				st.exitQuest(true);
			}
			else
				htmltext = "32017-04.htm";
		}

		return htmltext;
	}

	@Override
	public String onTalk(L2Npc npc, L2PcInstance player)
	{
		String htmltext = Quest.getNoQuestMsg();
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		switch (st.getState())
		{
			case QuestState.CREATED:
				htmltext = "32017-01.htm";
				break;

			case QuestState.STARTED:
				int cond = st.getInt("cond");
				if (cond == 1)
					htmltext = "32017-04.htm";
				else if (cond == 2)
				{
					if (st.getQuestItemsCount(GRAVE_GOODS) == 180)
						htmltext = "32017-05.htm";
					else
						htmltext = "32017-01.htm";
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
		int count = st.getQuestItemsCount(GRAVE_GOODS);

		if (count < 180)
		{
			int chance = (int) (75 * MainConfig.RATE_QUEST_DROP);
			int numItems = chance / 100;
			chance = chance % 100;

			if (Rnd.get(100) < chance)
				numItems++;

			if (numItems > 0)
			{
				if (count + numItems >= 180)
				{
					numItems = 180 - count;
					st.playSound(QuestState.SOUND_MIDDLE);
					st.set("cond", "2");
				}
				else
					st.playSound(QuestState.SOUND_ITEMGET);

				st.giveItems(GRAVE_GOODS, numItems);
			}
		}

		return null;
	}
}