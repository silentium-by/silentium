/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.quests;

import silentium.commons.utils.Rnd;
import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.quest.Quest;
import silentium.gameserver.model.quest.QuestState;
import silentium.gameserver.scripting.ScriptFile;

public class Q275_DarkWingedSpies extends Quest implements ScriptFile {
	private static final String qn = "Q275_DarkWingedSpies";

	// NPC
	private static final int TANTUS = 30567;

	// Monsters
	private static final int DARKWING_BAT = 20316;
	private static final int VARANGKA_TRACKER = 27043;

	// Items
	private static final int DARKWING_BAT_FANG = 1478;
	private static final int VARANGKAS_PARASITE = 1479;

	// Reward
	private static final int ADENA = 57;

	public Q275_DarkWingedSpies(final int scriptId, final String name, final String dname, final String path) {
		super(scriptId, name, dname, path);

		questItemIds = new int[] { DARKWING_BAT_FANG, VARANGKAS_PARASITE };

		addStartNpc(TANTUS);
		addTalkId(TANTUS);

		addKillId(DARKWING_BAT, VARANGKA_TRACKER);
	}

	public static void onLoad() {
		new Q275_DarkWingedSpies(275, "Q275_DarkWingedSpies", "Dark Winged Spies", "quests");
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		final String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if ("30567-03.htm".equalsIgnoreCase(event)) {
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		}

		return htmltext;
	}

	@Override
	public String onTalk(final L2Npc npc, final L2PcInstance player) {
		final QuestState st = player.getQuestState(qn);
		String htmltext = getNoQuestMsg();
		if (st == null)
			return htmltext;

		switch (st.getState()) {
			case QuestState.CREATED:
				if (player.getRace().ordinal() == 3) {
					if (player.getLevel() >= 11 && player.getLevel() <= 15)
						htmltext = "30567-02.htm";
					else {
						htmltext = "30567-01.htm";
						st.exitQuest(true);
					}
				} else {
					htmltext = "30567-00.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				if (st.getQuestItemsCount(DARKWING_BAT_FANG) < 70)
					htmltext = "30567-04.htm";
				else {
					htmltext = "30567-05.htm";
					st.takeItems(DARKWING_BAT_FANG, -1);
					st.takeItems(VARANGKAS_PARASITE, -1);
					st.rewardItems(ADENA, 4220);
					st.playSound(QuestState.SOUND_FINISH);
					st.exitQuest(true);
				}
				break;
		}

		return htmltext;
	}

	@Override
	public String onKill(final L2Npc npc, final L2PcInstance player, final boolean isPet) {
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return null;

		if (st.isStarted()) {
			switch (npc.getNpcId()) {
				case DARKWING_BAT:
					if (st.getQuestItemsCount(DARKWING_BAT_FANG) < 70) {
						st.giveItems(DARKWING_BAT_FANG, 1);

						if (st.getQuestItemsCount(DARKWING_BAT_FANG) == 70) {
							st.playSound(QuestState.SOUND_MIDDLE);
							st.set("cond", "2");
						} else
							st.playSound(QuestState.SOUND_ITEMGET);

						// Spawn of Varangka Tracker on the npc position.
						if (st.getQuestItemsCount(DARKWING_BAT_FANG) < 66 && Rnd.get(100) < 10) {
							st.addSpawn(VARANGKA_TRACKER, npc);
							st.giveItems(VARANGKAS_PARASITE, 1);
						}
					}
					break;

				case VARANGKA_TRACKER:
					if (st.getQuestItemsCount(DARKWING_BAT_FANG) < 66 && st.getQuestItemsCount(VARANGKAS_PARASITE) == 1) {
						st.takeItems(VARANGKAS_PARASITE, -1);
						st.giveItems(DARKWING_BAT_FANG, 5);

						if (st.getQuestItemsCount(DARKWING_BAT_FANG) == 70) {
							st.playSound(QuestState.SOUND_MIDDLE);
							st.set("cond", "2");
						} else
							st.playSound(QuestState.SOUND_ITEMGET);
					}
					break;
			}
		}
		return null;
	}
}