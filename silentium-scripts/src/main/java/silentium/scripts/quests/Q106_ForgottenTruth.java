/*
 * This program is free software: you can redistribute it &&/or modify it under the terms of the GNU General Public License as
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
import silentium.gameserver.scripting.ScriptFile;

/**
 * @author Demon
 */

public class Q106_ForgottenTruth extends Quest implements ScriptFile {
	private final static String qn = "Q106_ForgottenTruth";

	public Q106_ForgottenTruth(int questId, String name, String descr) {
		super(questId, name, descr);
		addStartNpc(30358);
		addTalkId(30358, 30133);
		addKillId(27070);
		questItemIds = new int[] { ONYX_TALISMAN1, ONYX_TALISMAN2, ANCIENT_SCROLL, ANCIENT_CLAY_TABLET, KARTAS_TRANSLATION };
	}

	private static final int ONYX_TALISMAN1 = 984;
	private static final int ONYX_TALISMAN2 = 985;
	private static final int ANCIENT_SCROLL = 986;
	private static final int ANCIENT_CLAY_TABLET = 987;
	private static final int KARTAS_TRANSLATION = 988;
	private static final int ELDRITCH_DAGGER = 989;

	public static void onLoad() {
		new Q106_ForgottenTruth(-1, "Q106_ForgottenTruth", "Forgotten Truth");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player) {
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event == "30358-05.htm") {
			st.giveItems(ONYX_TALISMAN1, 1);
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound("ItemSound.quest_accept");
		}

		return htmltext;
	}

	@Override
	public String onTalk(L2Npc npc, L2PcInstance player) {
		String htmltext = getNoQuestMsg();
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		int id = st.getState();
		int npcId = npc.getNpcId();
		if (id == QuestState.CREATED) {
			st.set("cond", "0");
			if (player.getRace().ordinal() == 2) {
				if (player.getLevel() >= 10)
					htmltext = "30358-03.htm";
				else {
					htmltext = "30358-02.htm";
					st.exitQuest(true);
				}
			} else {
				htmltext = "30358-00.htm";
				st.exitQuest(true);
			}
		} else if (id == QuestState.COMPLETED)
			htmltext = "<html><body>This quest has already been completed.</body></html>";
		else {
			if (st.getInt("cond") == 1) {
				if (npcId == 30358)
					htmltext = "30358-06.htm";
				else if (npcId == 30133 && st.getQuestItemsCount(ONYX_TALISMAN1) > 0 && id == QuestState.STARTED) {
					htmltext = "30133-01.htm";
					st.takeItems(ONYX_TALISMAN1, 1);
					st.giveItems(ONYX_TALISMAN2, 1);
					st.set("cond", "2");
				}
			} else if (st.getInt("cond") == 2) {
				if (npcId == 30358)
					htmltext = "30358-06.htm";
				else if (npcId == 30133)
					htmltext = "30133-02.htm";
			} else if (st.getInt("cond") == 3) {
				if (npcId == 30358)
					htmltext = "30358-06.htm";
				else if (npcId == 30133 && st.getQuestItemsCount(ANCIENT_SCROLL) > 0 && st.getQuestItemsCount(ANCIENT_CLAY_TABLET) > 0 && id == QuestState.STARTED) {
					htmltext = "30133-03.htm";
					st.takeItems(ONYX_TALISMAN2, 1);
					st.takeItems(ANCIENT_SCROLL, 1);
					st.takeItems(ANCIENT_CLAY_TABLET, 1);
					st.giveItems(KARTAS_TRANSLATION, 1);
					st.set("cond", "4");
				}
			} else if (st.getInt("cond") == 4) {
				if (npcId == 30358 && st.getQuestItemsCount(KARTAS_TRANSLATION) > 0) {
					htmltext = "30358-07.htm";
					st.takeItems(KARTAS_TRANSLATION, 1);
					st.giveItems(ELDRITCH_DAGGER, 1);
					for (int item = 4412; item <= 4417; item++)
						st.giveItems(item, 10);
					st.giveItems(1060, 100);
					if (player.getClassId().isMage() && st.getInt("onlyone") == 0) {
						st.giveItems(2509, 500);
						if (player.getLevel() < 25 && player.isNewbie())
							st.giveItems(5790, 3000);
					} else if (st.getInt("onlyone") == 0)
						st.giveItems(1835, 1000);
					st.unset("cond");
					st.setState(QuestState.COMPLETED);
					st.playSound("ItemSound.quest_finish");
				} else if (npcId == 30133 && id == QuestState.STARTED)
					htmltext = "30133-04.htm";
			}
		}
		return htmltext;
	}

	@Override
	public String onKill(L2Npc npc, L2PcInstance player, boolean isPet) {
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return null;

		if (st.getInt("cond") == 2) {
			if (st.getRandom(100) < 20) {
				if (st.getQuestItemsCount(ANCIENT_SCROLL) == 0) {
					st.giveItems(ANCIENT_SCROLL, 1);
					st.playSound("Itemsound.quest_itemget");
				} else if (st.getQuestItemsCount(ANCIENT_CLAY_TABLET) == 0) {
					st.giveItems(ANCIENT_CLAY_TABLET, 1);
					st.playSound("ItemSound.quest_middle");
					st.set("cond", "3");
				}
			}
		}
		return null;
	}
}