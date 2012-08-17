/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.quests;

import silentium.commons.utils.Rnd;
import silentium.gameserver.configs.MainConfig;
import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.quest.Quest;
import silentium.gameserver.model.quest.QuestState;
import silentium.gameserver.scripting.ScriptFile;

public class Q619_RelicsOfTheOldEmpire extends Quest implements ScriptFile
{
	private static final String qn = "Q619_RelicsOfTheOldEmpire";

	// NPC
	private static int GHOST_OF_ADVENTURER = 31538;

	// Items
	private static int RELICS = 7254;
	private static int ENTRANCE = 7075;

	// Rewards ; all S grade weapons recipe (60%)
	private static int[] RCP_REWARDS = new int[] { 6881, 6883, 6885, 6887, 6891, 6893, 6895, 6897, 6899, 7580 };

	public Q619_RelicsOfTheOldEmpire(int questId, String name, String descr)
	{
		super(questId, name, descr);

		questItemIds = new int[] { RELICS };

		addStartNpc(GHOST_OF_ADVENTURER);
		addTalkId(GHOST_OF_ADVENTURER);

		for (int id = 21396; id <= 21434; id++)
			// IT monsters
			addKillId(id);

		// monsters at IT entrance
		addKillId(21798, 21799, 21800);

		for (int id = 18120; id <= 18256; id++)
			// Sepulchers monsters
			addKillId(id);
	}

	public static void onLoad()
	{
		new Q619_RelicsOfTheOldEmpire(619, "Q619_RelicsOfTheOldEmpire", "Relics of the Old Empire");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event.equalsIgnoreCase("31538-03.htm"))
		{
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("31538-09.htm"))
		{
			if (st.getQuestItemsCount(RELICS) >= 1000)
			{
				htmltext = "31538-09.htm";
				st.takeItems(RELICS, 1000);
				st.giveItems(RCP_REWARDS[Rnd.get(RCP_REWARDS.length)], 1);
			}
			else
				htmltext = "31538-06.htm";
		}
		else if (event.equalsIgnoreCase("31538-10.htm"))
		{
			st.playSound(QuestState.SOUND_FINISH);
			st.exitQuest(true);
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
				if (player.getLevel() >= 74)
					htmltext = "31538-01.htm";
				else
				{
					htmltext = "31538-02.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				int cond = st.getInt("cond");
				int relics = st.getQuestItemsCount(RELICS);
				int entrance = st.getQuestItemsCount(ENTRANCE);

				if (cond == 1 && relics >= 1000)
					htmltext = "31538-04.htm";
				else if (entrance >= 1)
					htmltext = "31538-06.htm";
				else
					htmltext = "31538-07.htm";
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
		if (st.isStarted())
		{
			int numItems = (int) (100 * MainConfig.RATE_QUEST_DROP / 100);
			int chance = (int) (100 * MainConfig.RATE_QUEST_DROP % 100);

			if (Rnd.get(100) < chance)
				numItems++;

			if (numItems > 0)
			{
				st.giveItems(RELICS, numItems);
				st.playSound(QuestState.SOUND_ITEMGET);
			}

			if (Rnd.get(100) < (5 * MainConfig.RATE_QUEST_DROP))
			{
				st.giveItems(ENTRANCE, 1);
				st.playSound(QuestState.SOUND_MIDDLE);
			}
		}
		return null;
	}
}