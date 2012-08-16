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
import silentium.gameserver.scripting.ScriptFile;

public class Q159_ProtectTheWaterSource extends Quest implements ScriptFile {
	private final static String qn = "Q159_ProtectTheWaterSource";

	// Items
	private static final int PLAGUE_DUST = 1035;
	private static final int HYACINTH_CHARM1 = 1071;
	private static final int HYACINTH_CHARM2 = 1072;

	// NPC
	private static final int ASTERIOS = 30154;

	// Mob
	private static final int PLAGUE_ZOMBIE = 27017;

	// Reward
	private static final int ADENA = 57;

	public Q159_ProtectTheWaterSource(int questId, String name, String descr) {
		super(questId, name, descr);

		questItemIds = new int[] { PLAGUE_DUST, HYACINTH_CHARM1, HYACINTH_CHARM2 };

		addStartNpc(ASTERIOS);
		addTalkId(ASTERIOS);

		addKillId(PLAGUE_ZOMBIE);
	}

	public static void onLoad() {
		new Q159_ProtectTheWaterSource(159, "Q159_ProtectTheWaterSource", "Protect the Water Source");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player) {
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event.equalsIgnoreCase("30154-04.htm")) {
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
			st.giveItems(HYACINTH_CHARM1, 1);
		}

		return htmltext;
	}

	@Override
	public String onTalk(L2Npc npc, L2PcInstance player) {
		QuestState st = player.getQuestState(qn);
		String htmltext = getNoQuestMsg();
		if (st == null)
			return htmltext;

		switch (st.getState()) {
			case QuestState.CREATED:
				if (player.getRace().ordinal() == 1) {
					if (player.getLevel() >= 12 && player.getLevel() <= 18)
						htmltext = "30154-03.htm";
					else {
						htmltext = "30154-02.htm";
						st.exitQuest(true);
					}
				} else {
					htmltext = "30154-00.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				int cond = st.getInt("cond");
				if (cond == 1)
					htmltext = "30154-05.htm";
				else if (cond == 2) {
					st.set("cond", "3");
					htmltext = "30154-06.htm";
					st.takeItems(PLAGUE_DUST, -1);
					st.takeItems(HYACINTH_CHARM1, -1);
					st.giveItems(HYACINTH_CHARM2, 1);
					st.playSound(QuestState.SOUND_MIDDLE);
				} else if (cond == 3)
					htmltext = "30154-07.htm";
				else if (cond == 4) {
					htmltext = "30154-08.htm";
					st.takeItems(PLAGUE_DUST, -1);
					st.takeItems(HYACINTH_CHARM2, -1);
					st.rewardItems(ADENA, 18250);
					st.playSound(QuestState.SOUND_FINISH);
					st.exitQuest(false);
				}
				break;

			case QuestState.COMPLETED:
				htmltext = Quest.getAlreadyCompletedMsg();
				break;
		}

		return htmltext;
	}

	@Override
	public String onKill(L2Npc npc, L2PcInstance player, boolean isPet) {
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return null;

		switch (st.getInt("cond")) {
			case 1:
				if (Rnd.get(10) < 4) {
					st.set("cond", "2");
					st.playSound(QuestState.SOUND_MIDDLE);
					st.giveItems(PLAGUE_DUST, 1);
				}
				break;

			case 3:
				if (st.dropQuestItems(PLAGUE_DUST, 1, 5, 400000))
					st.set("cond", "4");
				break;
		}

		return null;
	}
}