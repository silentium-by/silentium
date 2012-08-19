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

public class Q379_FantasyWine extends Quest implements ScriptFile {
	private static final String qn = "Q379_FantasyWine";

	// NPCs
	private static final int HARLAN = 30074;

	// Monsters
	private static final int ENKU_CHAMPION = 20291;
	private static final int ENKU_SHAMAN = 20292;

	// Items
	private static final int LEAF = 5893;
	private static final int STONE = 5894;

	public Q379_FantasyWine(final int questId, final String name, final String descr) {
		super(questId, name, descr);

		questItemIds = new int[] { LEAF, STONE };

		addStartNpc(HARLAN);
		addTalkId(HARLAN);

		addKillId(ENKU_CHAMPION, ENKU_SHAMAN);
	}

	public static void onLoad() {
		new Q379_FantasyWine(379, "Q379_FantasyWine", "quests");
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		final int leaf = st.getQuestItemsCount(LEAF);
		final int stone = st.getQuestItemsCount(STONE);

		if ("30074-3.htm".equalsIgnoreCase(event)) {
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		} else if ("30074-6.htm".equalsIgnoreCase(event)) {
			if (leaf == 80 && stone == 100) {
				st.takeItems(LEAF, 80);
				st.takeItems(STONE, 100);
				final int rand = Rnd.get(100);

				if (rand < 25) {
					st.giveItems(5956, 1);
					htmltext = "30074-6.htm";
				} else if (rand < 50) {
					st.giveItems(5957, 1);
					htmltext = "30074-7.htm";
				} else {
					st.giveItems(5958, 1);
					htmltext = "30074-8.htm";
				}

				st.playSound(QuestState.SOUND_FINISH);
				st.exitQuest(true);
			} else
				htmltext = "30074-4.htm";
		} else if ("30074-2a.htm".equalsIgnoreCase(event))
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
				if (player.getLevel() >= 20 && player.getLevel() <= 25)
					htmltext = "30074-0.htm";
				else {
					htmltext = "30074-0a.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				final int cond = st.getInt("cond");
				final int leaf = st.getQuestItemsCount(LEAF);
				final int stone = st.getQuestItemsCount(STONE);

				if (cond == 1) {
					if (leaf < 80 && stone < 100)
						htmltext = "30074-4.htm";
					else if (leaf == 80 && stone < 100)
						htmltext = "30074-4a.htm";
					else if (leaf < 80 & stone == 100)
						htmltext = "30074-4b.htm";
				} else if (cond == 2 && leaf == 80 && stone == 100)
					htmltext = "30074-5.htm";
				break;
		}

		return htmltext;
	}

	@Override
	public String onKill(final L2Npc npc, final L2PcInstance player, final boolean isPet) {
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return null;

		final int npcId = npc.getNpcId();
		if (st.isStarted()) {
			if (npcId == ENKU_CHAMPION && st.getQuestItemsCount(LEAF) < 80)
				st.giveItems(LEAF, 1);
			else if (npcId == ENKU_SHAMAN && st.getQuestItemsCount(STONE) < 100)
				st.giveItems(STONE, 1);

			if (st.getQuestItemsCount(LEAF) >= 80 && st.getQuestItemsCount(STONE) >= 100) {
				st.playSound(QuestState.SOUND_MIDDLE);
				st.set("cond", "2");
			} else
				st.playSound(QuestState.SOUND_ITEMGET);
		}
		return null;
	}
}