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

public class Q329_CuriosityOfADwarf extends Quest implements ScriptFile {
	private static final String qn = "Q329_CuriosityOfADwarf";

	// NPC
	private static final int Rolento = 30437;

	// Items
	private static final int Golem_Heartstone = 1346;
	private static final int Broken_Heartstone = 1365;

	public Q329_CuriosityOfADwarf(final int scriptId, final String name, final String dname, final String path) {
		super(scriptId, name, dname, path);

		addStartNpc(Rolento);
		addTalkId(Rolento);

		addKillId(20083, 20085); // Granite golem, Puncher
	}

	public static void onLoad() {
		new Q329_CuriosityOfADwarf(329, "Q329_CuriosityOfADwarf", "Curiosity Of A Dwarf", "quests");
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		final String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if ("30437-03.htm".equalsIgnoreCase(event)) {
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		} else if ("30437-06.htm".equalsIgnoreCase(event)) {
			st.playSound(QuestState.SOUND_FINISH);
			st.exitQuest(true);
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
				if (player.getLevel() >= 33 && player.getLevel() <= 38)
					htmltext = "30437-02.htm";
				else {
					htmltext = "30437-01.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				final int golem = st.getQuestItemsCount(Golem_Heartstone);
				final int broken = st.getQuestItemsCount(Broken_Heartstone);

				if (golem + broken == 0)
					htmltext = "30437-04.htm";
				else {
					htmltext = "30437-05.htm";
					st.takeItems(Golem_Heartstone, -1);
					st.takeItems(Broken_Heartstone, -1);
					st.rewardItems(57, broken * 50 + golem * 1000 + (golem + broken > 10 ? 1183 : 0));
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
			final int chance = Rnd.get(100);
			if (chance < 15) {
				st.giveItems(Golem_Heartstone, 1);
				st.playSound(QuestState.SOUND_ITEMGET);
			} else if (chance < 65) {
				st.giveItems(Broken_Heartstone, 1);
				st.playSound(QuestState.SOUND_ITEMGET);
			}
		}

		return null;
	}
}