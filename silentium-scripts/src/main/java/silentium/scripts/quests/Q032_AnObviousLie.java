/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.quests;

import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.quest.Quest;
import silentium.gameserver.model.quest.QuestState;
import silentium.gameserver.scripting.ScriptFile;

public class Q032_AnObviousLie extends Quest implements ScriptFile
{
	private final static String qn = "Q032_AnObviousLie";

	// Items
	private static final int Suede = 1866;
	private static final int Thread = 1868;
	private static final int SpiritOre = 3031;
	private static final int Map = 7165;
	private static final int MedicinalHerb = 7166;

	// Rewards
	private static final int CatsEar = 6843;
	private static final int RacoonsEar = 7680;
	private static final int RabbitsEar = 7683;

	// NPCs
	private static final int Gentler = 30094;
	private static final int Maximilian = 30120;
	private static final int MikiTheCat = 31706;

	// Monster
	private static final int Alligator = 20135;

	public Q032_AnObviousLie(int questId, String name, String descr)
	{
		super(questId, name, descr);

		questItemIds = new int[] { Map, MedicinalHerb };

		addStartNpc(Maximilian);
		addTalkId(Maximilian, Gentler, MikiTheCat);

		addKillId(Alligator);
	}

	public static void onLoad()
	{
		new Q032_AnObviousLie(32, "Q032_AnObviousLie", "An Obvious Lie");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event.equalsIgnoreCase("30120-1.htm"))
		{
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("30094-1.htm"))
		{
			st.set("cond", "2");
			st.giveItems(Map, 1);
			st.playSound(QuestState.SOUND_MIDDLE);
		}
		else if (event.equalsIgnoreCase("31706-1.htm"))
		{
			st.set("cond", "3");
			st.takeItems(Map, 1);
			st.playSound(QuestState.SOUND_MIDDLE);
		}
		else if (event.equalsIgnoreCase("30094-4.htm"))
		{
			st.set("cond", "5");
			st.takeItems(MedicinalHerb, 20);
			st.playSound(QuestState.SOUND_MIDDLE);
		}
		else if (event.equalsIgnoreCase("30094-7.htm"))
		{
			if (st.getQuestItemsCount(SpiritOre) < 500)
				htmltext = "30094-5.htm";
			else
			{
				st.set("cond", "6");
				st.takeItems(SpiritOre, 500);
				st.playSound(QuestState.SOUND_MIDDLE);
			}
		}
		else if (event.equalsIgnoreCase("31706-4.htm"))
		{
			st.set("cond", "7");
			st.playSound(QuestState.SOUND_MIDDLE);
		}
		else if (event.equalsIgnoreCase("30094-10.htm"))
		{
			st.set("cond", "8");
			st.playSound(QuestState.SOUND_MIDDLE);
		}
		else if (event.equalsIgnoreCase("30094-13.htm"))
			st.playSound(QuestState.SOUND_MIDDLE);
		else if (event.equalsIgnoreCase("cat"))
		{
			if (st.getQuestItemsCount(Thread) < 1000 || st.getQuestItemsCount(Suede) < 500)
				htmltext = "30094-11.htm";
			else
			{
				htmltext = "30094-14.htm";
				st.takeItems(Suede, 500);
				st.takeItems(Thread, 1000);
				st.giveItems(CatsEar, 1);
				st.playSound(QuestState.SOUND_FINISH);
				st.exitQuest(false);
			}
		}
		else if (event.equalsIgnoreCase("racoon"))
		{
			if (st.getQuestItemsCount(Thread) < 1000 || st.getQuestItemsCount(Suede) < 500)
				htmltext = "30094-11.htm";
			else
			{
				htmltext = "30094-14.htm";
				st.takeItems(Suede, 500);
				st.takeItems(Thread, 1000);
				st.giveItems(RacoonsEar, 1);
				st.playSound(QuestState.SOUND_FINISH);
				st.exitQuest(false);
			}
		}
		else if (event.equalsIgnoreCase("rabbit"))
		{
			if (st.getQuestItemsCount(Thread) < 1000 || st.getQuestItemsCount(Suede) < 500)
				htmltext = "30094-11.htm";
			else
			{
				htmltext = "30094-14.htm";
				st.takeItems(Suede, 500);
				st.takeItems(Thread, 1000);
				st.giveItems(RabbitsEar, 1);
				st.playSound(QuestState.SOUND_FINISH);
				st.exitQuest(false);
			}
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
				if (player.getLevel() < 45)
				{
					htmltext = "30120-0a.htm";
					st.exitQuest(true);
				}
				else
					htmltext = "30120-0.htm";
				break;

			case QuestState.STARTED:
				int cond = st.getInt("cond");
				switch (npc.getNpcId())
				{
					case Maximilian:
						if (cond >= 1)
							htmltext = "30120-2.htm";
						break;

					case Gentler:
						if (cond == 1)
							htmltext = "30094-0.htm";
						else if (cond == 2 || cond == 3)
							htmltext = "30094-2.htm";
						else if (cond == 4)
							htmltext = "30094-3.htm";
						else if (cond == 5)
						{
							if (st.getQuestItemsCount(SpiritOre) < 500)
								htmltext = "30094-5.htm";
							else
								htmltext = "30094-6.htm";
						}
						else if (cond == 6)
							htmltext = "30094-8.htm";
						else if (cond == 7)
							htmltext = "30094-9.htm";
						else if (cond == 8)
						{
							if (st.getQuestItemsCount(Thread) < 1000 || st.getQuestItemsCount(Suede) < 500)
								htmltext = "30094-11.htm";
							else
								htmltext = "30094-12.htm";
						}
						break;

					case MikiTheCat:
						if (cond == 2)
							htmltext = "31706-0.htm";
						else if (cond >= 3 && cond <= 5)
							htmltext = "31706-2.htm";
						else if (cond == 6)
							htmltext = "31706-3.htm";
						else if (cond >= 7)
							htmltext = "31706-5.htm";
						break;
				}
				break;

			case QuestState.COMPLETED:
				htmltext = Quest.getAlreadyCompletedMsg();
				break;
		}

		return htmltext;
	}

	@Override
	public String onKill(L2Npc npc, L2PcInstance player, boolean isPet)
	{
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return null;

		if (st.getInt("cond") == 3)
			if (st.dropQuestItems(MedicinalHerb, 1, 20, 300000))
				st.set("cond", "4");

		return null;
	}
}