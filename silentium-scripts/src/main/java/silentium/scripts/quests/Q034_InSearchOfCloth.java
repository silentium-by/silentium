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

public class Q034_InSearchOfCloth extends Quest implements ScriptFile {
	private static final String qn = "Q034_InSearchOfCloth";

	// NPCs
	private static final int RADIA = 30088;
	private static final int RALFORD = 30165;
	private static final int VARAN = 30294;

	// Monsters
	private static final int TRISALIM_SPIDER = 20560;
	private static final int TRISALIM_TARANTULA = 20561;

	// Items
	private static final int SPINNERET = 7528;
	private static final int SUEDE = 1866;
	private static final int THREAD = 1868;
	private static final int SPIDERSILK = 7161;

	// Rewards
	private static final int MYSTERIOUS_CLOTH = 7076;

	public Q034_InSearchOfCloth(final int scriptId, final String name, final String dname, final String path) {
		super(scriptId, name, dname, path);

		questItemIds = new int[] { SPINNERET, SPIDERSILK };

		addStartNpc(RADIA);
		addTalkId(RADIA, RALFORD, VARAN);

		addKillId(TRISALIM_SPIDER, TRISALIM_TARANTULA);
	}

	public static void onLoad() {
		new Q034_InSearchOfCloth(34, "Q034_InSearchOfCloth", "In Search Of Cloth", "quests");
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		switch (event) {
			case "30088-1.htm":
				st.set("cond", "1");
				st.setState(QuestState.STARTED);
				st.playSound(QuestState.SOUND_ACCEPT);
				break;
			case "30294-1.htm":
				st.set("cond", "2");
				st.playSound(QuestState.SOUND_MIDDLE);
				break;
			case "30088-3.htm":
				st.set("cond", "3");
				st.playSound(QuestState.SOUND_MIDDLE);
				break;
			case "30165-1.htm":
				st.set("cond", "4");
				st.playSound(QuestState.SOUND_MIDDLE);
				break;
			case "30165-3.htm":
				st.takeItems(SPINNERET, 10);
				st.giveItems(SPIDERSILK, 1);
				st.set("cond", "6");
				st.playSound(QuestState.SOUND_MIDDLE);
				break;
			case "30088-5.htm":
				if (st.getQuestItemsCount(SUEDE) >= 3000 && st.getQuestItemsCount(THREAD) >= 5000 && st.getQuestItemsCount(SPIDERSILK) == 1) {
					st.takeItems(SUEDE, 3000);
					st.takeItems(THREAD, 5000);
					st.takeItems(SPIDERSILK, 1);
					st.giveItems(MYSTERIOUS_CLOTH, 1);
					st.playSound(QuestState.SOUND_FINISH);
					st.exitQuest(false);
				} else
					htmltext = "30088-4a.htm";
				break;
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
				if (player.getLevel() >= 60) {
					final QuestState fwear = player.getQuestState("Q037_MakeFormalWear");
					if (fwear != null && fwear.getInt("cond") == 6)
						htmltext = "30088-0.htm";
					else {
						htmltext = "30088-0a.htm";
						st.exitQuest(true);
					}
				} else {
					htmltext = "30088-0b.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				final int cond = st.getInt("cond");
				switch (npc.getNpcId()) {
					case RADIA:
						if (cond == 1)
							htmltext = "30088-1a.htm";
						else if (cond == 2)
							htmltext = "30088-2.htm";
						else if (cond == 3)
							htmltext = "30088-3a.htm";
						else if (cond == 6) {
							htmltext = st.getQuestItemsCount(SUEDE) < 3000 || st.getQuestItemsCount(THREAD) < 5000 || st.getQuestItemsCount(SPIDERSILK) < 1 ? "30088-4a.htm" : "30088-4.htm";
						}
						break;

					case VARAN:
						if (cond == 1)
							htmltext = "30294-0.htm";
						else if (cond >= 2)
							htmltext = "30294-1a.htm";
						break;

					case RALFORD:
						if (cond == 3)
							htmltext = "30165-0.htm";
						else if (cond == 4 && st.getQuestItemsCount(SPINNERET) < 10)
							htmltext = "30165-1a.htm";
						else if (cond == 5)
							htmltext = "30165-2.htm";
						else if (cond >= 6)
							htmltext = "30165-3a.htm";
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
	public String onKill(final L2Npc npc, final L2PcInstance player, final boolean isPet) {
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return null;

		if (st.getInt("cond") == 4)
			if (st.dropAlwaysQuestItems(SPINNERET, 1, 10))
				st.set("cond", "5");

		return null;
	}
}