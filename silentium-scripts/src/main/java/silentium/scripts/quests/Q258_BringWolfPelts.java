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

public class Q258_BringWolfPelts extends Quest implements ScriptFile {
	private static final String qn = "Q258_BringWolfPelts";

	// NPC
	private static final int LECTOR = 30001;

	// Monsters
	private static final int WOLF = 20120;
	private static final int ELDER_WOLF = 20442;

	// Item
	private static final int WOLF_PELT = 702;

	// Rewards
	private static final int Cotton_Shirt = 390;
	private static final int Leather_Pants = 29;
	private static final int Leather_Shirt = 22;
	private static final int Short_Leather_Gloves = 1119;
	private static final int Tunic = 426;

	public Q258_BringWolfPelts(final int scriptId, final String name, final String dname, final String path) {
		super(scriptId, name, dname, path);

		questItemIds = new int[] { WOLF_PELT };

		addStartNpc(LECTOR);
		addTalkId(LECTOR);

		addKillId(WOLF, ELDER_WOLF);
	}

	public static void onLoad() {
		new Q258_BringWolfPelts(258, "Q258_BringWolfPelts", "Bring Wolf Pelts", "quests");
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		final String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if ("30001-03.htm".equalsIgnoreCase(event)) {
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
				if (player.getLevel() >= 3 && player.getLevel() <= 9)
					htmltext = "30001-02.htm";
				else {
					htmltext = "30001-01.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				if (st.getQuestItemsCount(WOLF_PELT) < 40)
					htmltext = "30001-05.htm";
				else {
					st.takeItems(WOLF_PELT, 40);
					final int randomNumber = Rnd.get(16);

					// Reward is based on a random number (1D16).
					if (randomNumber == 0)
						st.giveItems(Cotton_Shirt, 1);
					else if (randomNumber < 6)
						st.giveItems(Leather_Pants, 1);
					else if (randomNumber < 9)
						st.giveItems(Leather_Shirt, 1);
					else if (randomNumber < 13)
						st.giveItems(Short_Leather_Gloves, 1);
					else
						st.giveItems(Tunic, 1);

					htmltext = "30001-06.htm";

					if (randomNumber == 0)
						st.playSound(QuestState.SOUND_JACKPOT);
					else
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

		if (st.getInt("cond") == 1)
			if (st.dropQuestItems(WOLF_PELT, 1, 40, 400000))
				st.set("cond", "2");

		return null;
	}
}