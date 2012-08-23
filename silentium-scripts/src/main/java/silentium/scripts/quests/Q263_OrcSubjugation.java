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

public class Q263_OrcSubjugation extends Quest implements ScriptFile {
	private static final String qn = "Q263_OrcSubjugation";

	// Items
	private static final int ORC_AMULET = 1116;
	private static final int ORC_NECKLACE = 1117;

	public Q263_OrcSubjugation(final int scriptId, final String name, final String dname, final String path) {
		super(scriptId, name, dname, path);

		questItemIds = new int[] { ORC_AMULET, ORC_NECKLACE };

		addStartNpc(30346);
		addTalkId(30346);

		addKillId(20385, 20386, 20387, 20388);
	}

	public static void onLoad() {
		new Q263_OrcSubjugation(263, "Q263_OrcSubjugation", "", "quests");
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		final String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if ("30346-03.htm".equalsIgnoreCase(event)) {
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		} else if ("30346-06.htm".equalsIgnoreCase(event)) {
			st.playSound(QuestState.SOUND_FINISH);
			st.exitQuest(true);
		}

		return htmltext;
	}

	@Override
	public String onTalk(final L2Npc npc, final L2PcInstance player) {
		String htmltext = Quest.getNoQuestMsg();
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		switch (st.getState()) {
			case QuestState.CREATED:
				if (player.getRace().ordinal() == 2) {
					if (player.getLevel() >= 8 && player.getLevel() <= 16)
						htmltext = "30346-02.htm";
					else {
						htmltext = "30346-01.htm";
						st.exitQuest(true);
					}
				} else {
					htmltext = "30346-00.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				final int amulet = st.getQuestItemsCount(ORC_AMULET);
				final int necklace = st.getQuestItemsCount(ORC_NECKLACE);

				if (amulet == 0 && necklace == 0)
					htmltext = "30346-04.htm";
				else {
					htmltext = "30346-05.htm";
					st.rewardItems(57, amulet * 20 + necklace * 30);
					st.takeItems(ORC_AMULET, -1);
					st.takeItems(ORC_NECKLACE, -1);
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

		if (st.isStarted() && Rnd.get(10) > 4) {
			st.giveItems(npc.getNpcId() == 20385 ? ORC_AMULET : ORC_NECKLACE, 1);
			st.playSound(QuestState.SOUND_ITEMGET);
		}

		return null;
	}
}