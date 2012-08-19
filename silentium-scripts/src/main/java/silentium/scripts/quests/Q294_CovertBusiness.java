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

public class Q294_CovertBusiness extends Quest implements ScriptFile {
	private static final String qn = "Q294_CovertBusiness";

	// Item
	private static final int BatFang = 1491;

	// Reward
	private static final int RingOfRaccoon = 1508;

	// Mobs
	private static final int Barded = 20370;
	private static final int Blade = 20480;

	// NPCs
	private static final int Keef = 30534;

	public Q294_CovertBusiness(final int questId, final String name, final String descr) {
		super(questId, name, descr);

		questItemIds = new int[] { BatFang };

		addStartNpc(Keef);
		addTalkId(Keef);

		addKillId(Barded, Blade);
	}

	public static void onLoad() {
		new Q294_CovertBusiness(294, "Q294_CovertBusiness", "quests");
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		final String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if ("30534-03.htm".equalsIgnoreCase(event)) {
			st.setState(QuestState.STARTED);
			st.set("cond", "1");
			st.playSound(QuestState.SOUND_ACCEPT);
		}

		return htmltext;
	}

	@Override
	public String onTalk(final L2Npc npc, final L2PcInstance player) {
		String htmltext = getNoQuestMsg();
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		switch (st.getState()) {
			case QuestState.CREATED:
				if (player.getRace().ordinal() == 4 && player.getLevel() >= 10 && player.getLevel() <= 16)
					htmltext = "30534-02.htm";
				else {
					htmltext = "30534-01.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				final int cond = st.getInt("cond");
				if (cond == 1)
					htmltext = "30534-04.htm";
				else if (cond == 2) {
					htmltext = "30534-05.htm";
					st.takeItems(BatFang, -1);
					st.giveItems(RingOfRaccoon, 1);
					st.addExpAndSp(0, 600);
					st.exitQuest(true);
					st.playSound(QuestState.SOUND_FINISH);
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

		if (st.getInt("cond") == 1)
			if (st.dropAlwaysQuestItems(BatFang, 1, 4, 100))
				st.set("cond", "2");

		return null;
	}
}