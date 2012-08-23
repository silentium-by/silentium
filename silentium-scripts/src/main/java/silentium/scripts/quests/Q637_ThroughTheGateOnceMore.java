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

public class Q637_ThroughTheGateOnceMore extends Quest implements ScriptFile {
	private static final String qn = "Q637_ThroughTheGateOnceMore";

	// NPC
	private static final int FLAURON = 32010;

	// Items
	private static final int FADEDMARK = 8065;
	private static final int NECRO_HEART = 8066;

	// Reward
	private static final int MARK = 8067;

	public Q637_ThroughTheGateOnceMore(final int scriptId, final String name, final String dname, final String path) {
		super(scriptId, name, dname, path);

		questItemIds = new int[] { NECRO_HEART };

		addStartNpc(FLAURON);
		addTalkId(FLAURON);

		addKillId(21565, 21566, 21567);
	}

	public static void onLoad() {
		new Q637_ThroughTheGateOnceMore(637, "Q637_ThroughTheGateOnceMore", "", "quests");
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		final String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if ("32010-04.htm".equalsIgnoreCase(event)) {
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		} else if ("32010-10.htm".equalsIgnoreCase(event))
			st.exitQuest(true);

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

				if (player.getLevel() >= 73) {
					if (st.hasQuestItems(MARK)) {
						htmltext = "32010-00.htm";
						st.exitQuest(true);
					} else if (st.hasQuestItems(FADEDMARK))
						htmltext = "32010-01.htm";
					else {
						htmltext = "32010-01a.htm";
						st.exitQuest(true);
					}
				} else {
					htmltext = "32010-01a.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				if (st.getInt("cond") == 2) {
					if (st.getQuestItemsCount(NECRO_HEART) == 10) {
						htmltext = "32010-06.htm";
						st.takeItems(FADEDMARK, 1);
						st.takeItems(NECRO_HEART, -1);
						st.giveItems(MARK, 1);
						st.giveItems(8273, 10);
						st.playSound(QuestState.SOUND_FINISH);
						st.exitQuest(true);
					} else
						st.set("cond", "1");
				} else
					htmltext = "32010-05.htm";
				break;
		}

		return htmltext;
	}

	@Override
	public String onKill(final L2Npc npc, final L2PcInstance player, final boolean isPet) {
		final L2PcInstance partyMember = getRandomPartyMember(player, npc, "1");
		if (partyMember == null)
			return null;

		final QuestState st = partyMember.getQuestState(qn);

		if (st.dropQuestItems(NECRO_HEART, 1, 10, 400000))
			st.set("cond", "2");

		return null;
	}
}