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

public class Q264_KeenClaws extends Quest implements ScriptFile {
	private static final String qn = "Q264_KeenClaws";

	// Item
	private static final int WOLF_CLAW = 1367;

	// NPC
	private static final int PAYNE = 30136;

	// Mobs
	private static final int GOBLIN = 20003;
	private static final int WOLF = 20456;

	// Rewards
	private static final int LeatherSandals = 36;
	private static final int WoodenHelmet = 43;
	private static final int Stockings = 462;
	private static final int HealingPotion = 1061;
	private static final int ShortGloves = 48;
	private static final int ClothShoes = 35;

	public Q264_KeenClaws(final int scriptId, final String name, final String dname, final String path) {
		super(scriptId, name, dname, path);

		questItemIds = new int[] { WOLF_CLAW };

		addStartNpc(PAYNE);
		addTalkId(PAYNE);

		addKillId(GOBLIN, WOLF);
	}

	public static void onLoad() {
		new Q264_KeenClaws(264, "Q264_KeenClaws", "", "quests");
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		final String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if ("30136-03.htm".equalsIgnoreCase(event)) {
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
				if (player.getLevel() >= 3 && player.getLevel() <= 9)
					htmltext = "30136-02.htm";
				else {
					htmltext = "30136-01.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				final int count = st.getQuestItemsCount(WOLF_CLAW);

				if (count < 50)
					htmltext = "30136-04.htm";
				else {
					st.takeItems(WOLF_CLAW, -1);

					final int n = Rnd.get(17);
					if (n == 0) {
						st.giveItems(WoodenHelmet, 1);
						st.playSound(QuestState.SOUND_JACKPOT);
					} else if (n < 2)
						st.giveItems(57, 1000);
					else if (n < 5)
						st.giveItems(LeatherSandals, 1);
					else if (n < 8) {
						st.giveItems(Stockings, 1);
						st.giveItems(57, 50);
					} else if (n < 11)
						st.giveItems(HealingPotion, 1);
					else if (n < 14)
						st.giveItems(ShortGloves, 1);
					else
						st.giveItems(ClothShoes, 1);

					htmltext = "30136-05.htm";
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
			if (st.dropQuestItems(WOLF_CLAW, 1, 8, 50, 800000))
				st.set("cond", "2");

		return null;
	}
}