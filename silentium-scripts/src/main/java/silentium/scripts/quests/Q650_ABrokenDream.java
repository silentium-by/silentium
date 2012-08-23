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

public class Q650_ABrokenDream extends Quest implements ScriptFile {
	private static final String qn = "Q650_ABrokenDream";

	// NPC
	private static final int GHOST = 32054;

	// Item
	private static final int DREAM_FRAGMENT = 8514;

	// Monsters
	private static final int CREWMAN = 22027;
	private static final int VAGABOND = 22028;

	public Q650_ABrokenDream(final int scriptId, final String name, final String dname, final String path) {
		super(scriptId, name, dname, path);

		questItemIds = new int[] { DREAM_FRAGMENT };

		addStartNpc(GHOST);
		addTalkId(GHOST);
		addKillId(CREWMAN, VAGABOND);
	}

	public static void onLoad() {
		new Q650_ABrokenDream(650, "Q650_ABrokenDream", "", "quests");
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if ("32054-01a.htm".equalsIgnoreCase(event)) {
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		} else if ("32054-03.htm".equalsIgnoreCase(event)) {
			if (!st.hasQuestItems(DREAM_FRAGMENT))
				htmltext = "32054-04.htm";
		} else if ("32054-05.htm".equalsIgnoreCase(event)) {
			st.exitQuest(true);
			st.playSound(QuestState.SOUND_GIVEUP);
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
				final QuestState st2 = player.getQuestState("Q117_TheOceanOfDistantStars");
				if (st2 != null && st2.isCompleted() && player.getLevel() >= 39)
					htmltext = "32054-01.htm";
				else {
					htmltext = "32054-00.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				htmltext = "32054-02.htm";
				break;
		}

		return htmltext;
	}

	@Override
	public String onKill(final L2Npc npc, final L2PcInstance player, final boolean isPet) {
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return null;

		if (st.isStarted() && Rnd.get(100) < 25) {
			st.giveItems(DREAM_FRAGMENT, 1);
			st.playSound(QuestState.SOUND_ITEMGET);
		}

		return null;
	}
}