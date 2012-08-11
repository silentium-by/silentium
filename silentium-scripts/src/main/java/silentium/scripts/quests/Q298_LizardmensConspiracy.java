/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.quests;

import silentium.commons.utils.Rnd;
import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.quest.Quest;
import silentium.gameserver.model.quest.QuestState;

public class Q298_LizardmensConspiracy extends Quest
{
	private static final String qn = "Q298_LizardmensConspiracy";

	// NPCs
	private static final int PRAGA = 30333;
	private static final int ROHMER = 30344;

	// Items
	private static final int PATROL_REPORT = 7182;
	private static final int WHITE_GEM = 7183;
	private static final int RED_GEM = 7184;

	public Q298_LizardmensConspiracy(int questId, String name, String descr)
	{
		super(questId, name, descr);

		questItemIds = new int[] { PATROL_REPORT, WHITE_GEM, RED_GEM };

		addStartNpc(PRAGA);
		addTalkId(PRAGA, ROHMER);

		addKillId(20926, 20927, 20922, 20923, 20924);
	}

	public static void main(String[] args)
	{
		new Q298_LizardmensConspiracy(298, "Q298_LizardmensConspiracy", "Lizardmen's Conspiracy");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event.equalsIgnoreCase("30333-1.htm"))
		{
			st.set("cond", "1");
			st.giveItems(PATROL_REPORT, 1);
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("30344-1.htm"))
		{
			st.takeItems(PATROL_REPORT, 1);
			st.set("cond", "2");
			st.playSound(QuestState.SOUND_MIDDLE);
		}
		else if (event.equalsIgnoreCase("30344-3.htm"))
		{
			if (st.getQuestItemsCount(WHITE_GEM) >= 50 && st.getQuestItemsCount(RED_GEM) >= 50)
			{
				st.takeItems(WHITE_GEM, -1);
				st.takeItems(RED_GEM, -1);
				st.addExpAndSp(0, 42000);
				st.playSound(QuestState.SOUND_FINISH);
				st.exitQuest(true);
			}
			else
				htmltext = "30344-4.htm";
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
				if (player.getLevel() >= 25 && player.getLevel() <= 34)
					htmltext = "30333-0a.htm";
				else
				{
					htmltext = "30333-0b.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				int cond = st.getInt("cond");
				switch (npc.getNpcId())
				{
					case PRAGA:
						htmltext = "30333-2.htm";
						break;

					case ROHMER:
						if (cond == 1)
						{
							if (st.getQuestItemsCount(PATROL_REPORT) == 1)
								htmltext = "30344-0.htm";
							else
								htmltext = "30344-0a.htm";
						}
						else if (cond == 2 || cond == 3)
							htmltext = "30344-2.htm";
						break;
				}
				break;
		}

		return htmltext;
	}

	@Override
	public String onKill(L2Npc npc, L2PcInstance player, boolean isPet)
	{
		L2PcInstance partyMember = getRandomPartyMember(player, npc, "2");
		if (partyMember == null)
			return null;

		QuestState st = partyMember.getQuestState(qn);

		if (Rnd.get(100) < 62)
		{
			switch (npc.getNpcId())
			{
				case 20926:
				case 20927:
					if (st.getQuestItemsCount(RED_GEM) < 50)
					{
						st.giveItems(RED_GEM, 1);
						if (st.getQuestItemsCount(WHITE_GEM) >= 50 && st.getQuestItemsCount(RED_GEM) >= 50)
						{
							st.set("cond", "3");
							st.playSound(QuestState.SOUND_MIDDLE);
						}
						else
							st.playSound(QuestState.SOUND_ITEMGET);
					}
					break;

				case 20922:
				case 20923:
				case 20924:
					if (st.getQuestItemsCount(WHITE_GEM) < 50)
					{
						st.giveItems(WHITE_GEM, 1);
						if (st.getQuestItemsCount(RED_GEM) >= 50 && st.getQuestItemsCount(WHITE_GEM) >= 50)
						{
							st.set("cond", "3");
							st.playSound(QuestState.SOUND_MIDDLE);
						}
						else
							st.playSound(QuestState.SOUND_ITEMGET);
					}
					break;
			}
		}

		return null;
	}
}