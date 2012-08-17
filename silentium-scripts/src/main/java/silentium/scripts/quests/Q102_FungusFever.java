/*
 * This program is free software: you can redistribute it &&/or modify it under the terms of the GNU General Public License as published by the
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

/**
 * @author Demon
 */

public class Q102_FungusFever extends Quest implements ScriptFile
{
	private final static String qn = "Q102_FungusFever";

	private static final int ALBERRYUS_LETTER_ID = 964;
	private static final int EVERGREEN_AMULET_ID = 965;
	private static final int DRYAD_TEARS_ID = 966;
	private static final int ALBERRYUS_LIST_ID = 746;
	private static final int COBS_MEDICINE1_ID = 1130;
	private static final int COBS_MEDICINE2_ID = 1131;
	private static final int COBS_MEDICINE3_ID = 1132;
	private static final int COBS_MEDICINE4_ID = 1133;
	private static final int COBS_MEDICINE5_ID = 1134;
	private static final int SWORD_OF_SENTINEL_ID = 743;
	private static final int STAFF_OF_SENTINEL_ID = 744;

	public Q102_FungusFever(int questId, String name, String descr)
	{
		super(questId, name, descr);
		addStartNpc(30284);
		addTalkId(30284, 30156, 30217, 30219, 30221, 30285);
		addKillId(20013, 20019);
		questItemIds = new int[] { ALBERRYUS_LETTER_ID, EVERGREEN_AMULET_ID, DRYAD_TEARS_ID, COBS_MEDICINE1_ID, COBS_MEDICINE2_ID, COBS_MEDICINE3_ID, COBS_MEDICINE4_ID, COBS_MEDICINE5_ID, ALBERRYUS_LIST_ID };
	}

	public static void onLoad()
	{
		new Q102_FungusFever(102, "Q102_FungusFever", "Fungus Fever");
	}

	private void check(QuestState st)
	{
		if (st.getQuestItemsCount(COBS_MEDICINE1_ID) == 0 || st.getQuestItemsCount(COBS_MEDICINE2_ID) == 0 || st.getQuestItemsCount(COBS_MEDICINE3_ID) == 0 || st.getQuestItemsCount(COBS_MEDICINE4_ID) == 0 || st.getQuestItemsCount(COBS_MEDICINE5_ID) == 0)
		{
			st.set("cond", "6");
			st.playSound("ItemSound.quest_middle");
		}
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event == "1")
		{
			htmltext = "30284-02.htm";
			st.giveItems(ALBERRYUS_LETTER_ID, 1);
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound("ItemSound.quest_accept");
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

		int npcId = npc.getNpcId();
		int id = st.getState();
		int cid = player.getClassId().getId();
		if (id == QuestState.CREATED)
		{
			st.set("cond", "0");
			st.set("onlyone", "0");
		}
		if (npcId == 30284 && st.getInt("cond") == 0 && st.getInt("onlyone") == 0)
		{
			if (player.getRace().ordinal() != 1)
			{
				htmltext = "30284-00.htm";
				st.exitQuest(true);
			}
			else if (player.getLevel() >= 12)
			{
				htmltext = "30284-07.htm";
				return htmltext;
			}
			else
			{
				htmltext = "30284-08.htm";
				st.exitQuest(true);
			}
		}
		else if (id == QuestState.STARTED)
		{
			if (npcId == 30284 && st.getInt("cond") == 1 && st.getQuestItemsCount(ALBERRYUS_LETTER_ID) == 1)
				htmltext = "30284-03.htm";
			else if (npcId == 30284 && st.getInt("cond") == 1 && st.getQuestItemsCount(EVERGREEN_AMULET_ID) == 1)
				htmltext = "30284-09.htm";
			else if (npcId == 30156 && st.getInt("cond") == 1 && st.getQuestItemsCount(ALBERRYUS_LETTER_ID) == 1)
			{
				st.giveItems(EVERGREEN_AMULET_ID, 1);
				st.takeItems(ALBERRYUS_LETTER_ID, 1);
				st.set("cond", "2");
				htmltext = "30156-03.htm";
			}
			else if (npcId == 30156 && st.getInt("cond") == 2 && st.getQuestItemsCount(EVERGREEN_AMULET_ID) > 0 && st.getQuestItemsCount(DRYAD_TEARS_ID) < 10)
				htmltext = "30156-04.htm";
			else if (npcId == 30156 && st.getInt("cond") == 5 && st.getQuestItemsCount(ALBERRYUS_LIST_ID) > 0)
				htmltext = "30156-07.htm";
			else if (npcId == 30156 && st.getInt("cond") == 3 && st.getQuestItemsCount(EVERGREEN_AMULET_ID) > 0 && st.getQuestItemsCount(DRYAD_TEARS_ID) >= 10)
			{
				st.takeItems(EVERGREEN_AMULET_ID, 1);
				st.takeItems(DRYAD_TEARS_ID, -1);
				st.giveItems(COBS_MEDICINE1_ID, 1);
				st.giveItems(COBS_MEDICINE2_ID, 1);
				st.giveItems(COBS_MEDICINE3_ID, 1);
				st.giveItems(COBS_MEDICINE4_ID, 1);
				st.giveItems(COBS_MEDICINE5_ID, 1);
				st.set("cond", "4");
				htmltext = "30156-05.htm";
			}
			else if (npcId == 30156 && st.getInt("cond") == 4 && st.getQuestItemsCount(ALBERRYUS_LIST_ID) == 0 && (st.getQuestItemsCount(COBS_MEDICINE1_ID) == 1 || st.getQuestItemsCount(COBS_MEDICINE2_ID) == 1 || st.getQuestItemsCount(COBS_MEDICINE3_ID) == 1 || st.getQuestItemsCount(COBS_MEDICINE4_ID) == 1 || st.getQuestItemsCount(COBS_MEDICINE5_ID) == 1))
				htmltext = "30156-06.htm";
			else if (npcId == 30284 && st.getInt("cond") == 4 && st.getQuestItemsCount(ALBERRYUS_LIST_ID) == 0 && st.getQuestItemsCount(COBS_MEDICINE1_ID) == 1)
			{
				st.takeItems(COBS_MEDICINE1_ID, 1);
				st.giveItems(ALBERRYUS_LIST_ID, 1);
				st.set("cond", "5");
				htmltext = "30284-04.htm";
			}
			else if (npcId == 30284 && st.getInt("cond") == 5 && st.getQuestItemsCount(ALBERRYUS_LIST_ID) == 1 && (st.getQuestItemsCount(COBS_MEDICINE1_ID) == 1 || st.getQuestItemsCount(COBS_MEDICINE2_ID) == 1 || st.getQuestItemsCount(COBS_MEDICINE3_ID) == 1 || st.getQuestItemsCount(COBS_MEDICINE4_ID) == 1 || st.getQuestItemsCount(COBS_MEDICINE5_ID) == 1))
				htmltext = "30284-05.htm";
			else if (npcId == 30217 && st.getInt("cond") == 5 && st.getQuestItemsCount(ALBERRYUS_LIST_ID) == 1 && st.getQuestItemsCount(COBS_MEDICINE2_ID) == 1)
			{
				st.takeItems(COBS_MEDICINE2_ID, 1);
				check(st);
				htmltext = "30217-01.htm";
			}
			else if (npcId == 30219 && st.getInt("cond") == 5 && st.getQuestItemsCount(ALBERRYUS_LIST_ID) == 1 && st.getQuestItemsCount(COBS_MEDICINE3_ID) == 1)
			{
				st.takeItems(COBS_MEDICINE3_ID, 1);
				check(st);
				htmltext = "30219-01.htm";
			}
			else if (npcId == 30221 && st.getInt("cond") == 5 && st.getQuestItemsCount(ALBERRYUS_LIST_ID) == 1 && st.getQuestItemsCount(COBS_MEDICINE4_ID) == 1)
			{
				st.takeItems(COBS_MEDICINE4_ID, 1);
				check(st);
				htmltext = "30221-01.htm";
			}
			else if (npcId == 30285 && st.getInt("cond") == 5 && st.getQuestItemsCount(ALBERRYUS_LIST_ID) == 1 && st.getQuestItemsCount(COBS_MEDICINE5_ID) == 1)
			{
				st.takeItems(COBS_MEDICINE5_ID, 1);
				check(st);
				htmltext = "30285-01.htm";
			}
			else if (npcId == 30284 && st.getInt("cond") == 6 && st.getQuestItemsCount(ALBERRYUS_LIST_ID) == 1)
			{
				st.takeItems(ALBERRYUS_LIST_ID, 1);
				st.set("cond", "0");
				st.setState(QuestState.COMPLETED);
				st.playSound("ItemSound.quest_finish");
				htmltext = "30284-06.htm";
				st.set("onlyone", "1");
				if (cid > 17 && cid < 26)
				{
					st.giveItems(SWORD_OF_SENTINEL_ID, 1);
					st.giveItems(1835, 1000);
				}
				else
				{
					st.giveItems(STAFF_OF_SENTINEL_ID, 1);
					st.giveItems(2509, 1000);
				}
				st.giveItems(1060, 100);
			}
		}
		else if (npcId == 30284 && st.getInt("cond") == 0 && st.getInt("onlyone") == 1)
			htmltext = "<html><body>This quest has already been completed.</body></html>";

		return htmltext;
	}

	@Override
	public String onKill(L2Npc npc, L2PcInstance player, boolean isPet)
	{
		QuestState st = player.getQuestState(qn);
		int npcId = npc.getNpcId();
		if (st == null)
			return null;

		if (st.getState() == QuestState.STARTED)
		{
			if (npcId == 20013 || npcId == 20019)
			{
				if (st.getQuestItemsCount(EVERGREEN_AMULET_ID) > 0 && st.getQuestItemsCount(DRYAD_TEARS_ID) < 10)
				{
					if (st.getRandom(10) < 3)
					{
						st.giveItems(DRYAD_TEARS_ID, 1);
						if (st.getQuestItemsCount(DRYAD_TEARS_ID) == 10)
						{
							st.playSound("ItemSound.quest_middle");
							st.set("cond", "3");
						}
						else
							st.playSound("ItemSound.quest_itemget");
					}
				}
			}
		}

		return null;
	}
}