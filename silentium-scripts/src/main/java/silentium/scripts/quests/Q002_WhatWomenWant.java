/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.quests;

import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.quest.Quest;
import silentium.gameserver.model.quest.QuestState;

public class Q002_WhatWomenWant extends Quest
{
	private static final String qn = "Q002_WhatWomenWant";

	// NPCs
	private final static int ARUJIEN = 30223;
	private final static int MIRABEL = 30146;
	private final static int HERBIEL = 30150;
	private final static int GREENIS = 30157;

	// Items
	private final static int ARUJIENS_LETTER1 = 1092;
	private final static int ARUJIENS_LETTER2 = 1093;
	private final static int ARUJIENS_LETTER3 = 1094;
	private final static int POETRY_BOOK = 689;
	private final static int GREENIS_LETTER = 693;

	// Rewards
	private final static int ADENA = 57;
	private final static int MYSTICS_EARRING = 113;

	public Q002_WhatWomenWant(int questId, String name, String descr)
	{
		super(questId, name, descr);

		questItemIds = new int[] { ARUJIENS_LETTER1, ARUJIENS_LETTER2, ARUJIENS_LETTER3, POETRY_BOOK, GREENIS_LETTER };

		addStartNpc(ARUJIEN);
		addTalkId(ARUJIEN, MIRABEL, HERBIEL, GREENIS);
	}

	public static void main(String[] args)
	{
		new Q002_WhatWomenWant(2, "Q002_WhatWomenWant", "What Women Want");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event.equalsIgnoreCase("30223-04.htm"))
		{
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.giveItems(ARUJIENS_LETTER1, 1);
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("30223-08.htm"))
		{
			st.takeItems(ARUJIENS_LETTER3, -1);
			st.giveItems(POETRY_BOOK, 1);
			st.set("cond", "4");
			st.playSound(QuestState.SOUND_MIDDLE);
		}
		else if (event.equalsIgnoreCase("30223-09.htm"))
		{
			st.takeItems(ARUJIENS_LETTER3, -1);
			st.rewardItems(ADENA, 450);
			st.exitQuest(false);
			st.playSound(QuestState.SOUND_FINISH);
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
				if (player.getRace().ordinal() != 1 && player.getRace().ordinal() != 0)
				{
					htmltext = "30223-00.htm";
					st.exitQuest(true);
				}
				else if (player.getLevel() >= 2 && player.getLevel() <= 5)
					htmltext = "30223-02.htm";
				else
				{
					htmltext = "30223-01.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				int cond = st.getInt("cond");
				switch (npc.getNpcId())
				{
					case ARUJIEN:
						if (st.getQuestItemsCount(ARUJIENS_LETTER1) > 0)
							htmltext = "30223-05.htm";
						else if (st.getQuestItemsCount(ARUJIENS_LETTER3) > 0)
							htmltext = "30223-07.htm";
						else if (st.getQuestItemsCount(ARUJIENS_LETTER2) > 0)
							htmltext = "30223-06.htm";
						else if (st.getQuestItemsCount(POETRY_BOOK) > 0)
							htmltext = "30223-11.htm";
						else if (st.getQuestItemsCount(GREENIS_LETTER) > 0)
						{
							htmltext = "30223-10.htm";
							st.takeItems(GREENIS_LETTER, 1);
							st.giveItems(MYSTICS_EARRING, 1);
							st.exitQuest(false);
							st.playSound(QuestState.SOUND_FINISH);
						}
						break;

					case MIRABEL:
						if (cond == 1)
						{
							if (st.getQuestItemsCount(ARUJIENS_LETTER1) > 0)
							{
								htmltext = "30146-01.htm";
								st.takeItems(ARUJIENS_LETTER1, 1);
								st.giveItems(ARUJIENS_LETTER2, 1);
								st.set("cond", "2");
								st.playSound(QuestState.SOUND_MIDDLE);
							}
						}
						else if (st.getQuestItemsCount(ARUJIENS_LETTER2) > 0 || st.getQuestItemsCount(ARUJIENS_LETTER3) > 0 || st.getQuestItemsCount(POETRY_BOOK) > 0 || st.getQuestItemsCount(GREENIS_LETTER) > 0)
							htmltext = "30146-02.htm";
						break;

					case HERBIEL:
						if (cond == 2 && st.getQuestItemsCount(ARUJIENS_LETTER1) == 0)
						{
							if (st.getQuestItemsCount(ARUJIENS_LETTER2) > 0)
							{
								htmltext = "30150-01.htm";
								st.takeItems(ARUJIENS_LETTER2, 1);
								st.giveItems(ARUJIENS_LETTER3, 1);
								st.set("cond", "3");
								st.playSound(QuestState.SOUND_MIDDLE);
							}
						}
						else if (st.getQuestItemsCount(ARUJIENS_LETTER3) > 0 || st.getQuestItemsCount(POETRY_BOOK) > 0 || st.getQuestItemsCount(GREENIS_LETTER) > 0)
							htmltext = "30150-02.htm";
						break;

					case GREENIS:
						if (st.getQuestItemsCount(POETRY_BOOK) > 0 && cond == 4)
						{
							htmltext = "30157-02.htm";
							st.takeItems(POETRY_BOOK, -1);
							st.giveItems(GREENIS_LETTER, 1);
							st.set("cond", "5");
							st.playSound(QuestState.SOUND_MIDDLE);
						}
						else if (st.getQuestItemsCount(GREENIS_LETTER) > 0)
							htmltext = "30157-03.htm";
						else if (st.getQuestItemsCount(ARUJIENS_LETTER1) > 0 || st.getQuestItemsCount(ARUJIENS_LETTER2) > 0 || st.getQuestItemsCount(ARUJIENS_LETTER3) > 0)
							htmltext = "30157-01.htm";
						break;
				}
				break;

			case QuestState.COMPLETED:
				htmltext = Quest.getAlreadyCompletedMsg();
				break;
		}

		return htmltext;
	}
}