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

public class Q116_BeyondTheHillsOfWinter extends Quest implements ScriptFile {
	private static final String qn = "Q116_BeyondTheHillsOfWinter";

	// NPCs
	private static final int FILAUR = 30535;
	private static final int OBI = 32052;

	// Items
	private static final int BANDAGE = 1833;
	private static final int ENERGY_STONE = 5589;
	private static final int THIEF_KEY = 1661;
	private static final int GOODS = 8098;

	// Reward
	private static final int SSD = 1463;

	public Q116_BeyondTheHillsOfWinter(final int scriptId, final String name, final String dname, final String path) {
		super(scriptId, name, dname, path);

		questItemIds = new int[] { GOODS };

		addStartNpc(FILAUR);
		addTalkId(FILAUR, OBI);
	}

	public static void onLoad() {
		new Q116_BeyondTheHillsOfWinter(116, "Q116_BeyondTheHillsOfWinter", "Beyond The Hills Of Winter", "quests");
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if ("30535-02.htm".equalsIgnoreCase(event)) {
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		} else if ("30535-05.htm".equalsIgnoreCase(event)) {
			st.set("cond", "2");
			st.giveItems(GOODS, 1);
			st.playSound(QuestState.SOUND_MIDDLE);
		} else if ("materials".equalsIgnoreCase(event)) {
			htmltext = "32052-02.htm";
			st.takeItems(GOODS, -1);
			st.rewardItems(SSD, 1650);
			st.playSound(QuestState.SOUND_FINISH);
			st.exitQuest(false);
		} else if ("adena".equalsIgnoreCase(event)) {
			htmltext = "32052-02.htm";
			st.takeItems(GOODS, -1);
			st.giveItems(57, 16500);
			st.playSound(QuestState.SOUND_FINISH);
			st.exitQuest(false);
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
				if (player.getLevel() >= 30 && player.getRace().ordinal() == 4)
					htmltext = "30535-01.htm";
				else {
					htmltext = "30535-00.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				final int cond = st.getInt("cond");
				switch (npc.getNpcId()) {
					case FILAUR:
						if (cond == 1) {
							if (st.getQuestItemsCount(BANDAGE) >= 20 && st.getQuestItemsCount(ENERGY_STONE) >= 5 && st.getQuestItemsCount(THIEF_KEY) >= 10) {
								htmltext = "30535-03.htm";
								st.takeItems(BANDAGE, 20);
								st.takeItems(ENERGY_STONE, 5);
								st.takeItems(THIEF_KEY, 10);
							} else
								htmltext = "30535-04.htm";
						} else if (cond == 2)
							htmltext = "30535-05.htm";
						break;

					case OBI:
						if (cond == 2 && st.getQuestItemsCount(GOODS) == 1)
							htmltext = "32052-00.htm";
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