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
import silentium.gameserver.model.base.Race;
import silentium.gameserver.model.quest.Quest;
import silentium.gameserver.model.quest.QuestState;
import silentium.gameserver.scripting.ScriptFile;

public class Q266_PleasOfPixies extends Quest implements ScriptFile {
	private static final String qn = "Q266_PleasOfPixies";

	// Items
	private static final int PREDATORS_FANG = 1334;

	// Rewards
	private static final int GLASS_SHARD = 1336;
	private static final int EMERALD = 1337;
	private static final int BLUE_ONYX = 1338;
	private static final int ONYX = 1339;

	public Q266_PleasOfPixies(final int questId, final String name, final String descr) {
		super(questId, name, descr);

		questItemIds = new int[] { PREDATORS_FANG };

		addStartNpc(31852);
		addTalkId(31852);

		addKillId(20525, 20530, 20534, 20537);
	}

	public static void onLoad() {
		new Q266_PleasOfPixies(266, "Q266_PleasOfPixies", "quests");
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		final String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if ("31852-03.htm".equalsIgnoreCase(event)) {
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
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
				if (player.getRace() != Race.Elf) {
					htmltext = "31852-00.htm";
					st.exitQuest(true);
				} else if (player.getLevel() >= 3 && player.getLevel() <= 8)
					htmltext = "31852-02.htm";
				else {
					htmltext = "31852-01.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				if (st.getQuestItemsCount(PREDATORS_FANG) < 100)
					htmltext = "31852-04.htm";
				else {
					htmltext = "31852-05.htm";
					st.takeItems(PREDATORS_FANG, -1);

					final int n = Rnd.get(100);
					if (n < 10) {
						st.rewardItems(EMERALD, 1);
						st.playSound(QuestState.SOUND_JACKPOT);
					} else if (n < 30)
						st.rewardItems(BLUE_ONYX, 1);
					else if (n < 60)
						st.rewardItems(ONYX, 1);
					else
						st.rewardItems(GLASS_SHARD, 1);

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
			if (st.dropAlwaysQuestItems(PREDATORS_FANG, 1, 3, 100))
				st.set("cond", "2");

		return null;
	}
}