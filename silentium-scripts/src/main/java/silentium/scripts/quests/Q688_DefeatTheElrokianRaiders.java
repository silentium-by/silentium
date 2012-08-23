/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.quests;

import silentium.commons.utils.Rnd;
import silentium.gameserver.configs.MainConfig;
import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.quest.Quest;
import silentium.gameserver.model.quest.QuestState;
import silentium.gameserver.scripting.ScriptFile;

public class Q688_DefeatTheElrokianRaiders extends Quest implements ScriptFile {
	private static final String qn = "Q688_DefeatTheElrokianRaiders";

	// Item
	private static final int DINOSAUR_FANG_NECKLACE = 8785;

	// NPC
	private static final int DINN = 32105;

	// Monster
	private static final int ELROKI = 22214;

	public Q688_DefeatTheElrokianRaiders(final int scriptId, final String name, final String dname, final String path) {
		super(scriptId, name, dname, path);

		questItemIds = new int[] { DINOSAUR_FANG_NECKLACE };

		addStartNpc(DINN);
		addTalkId(DINN);

		addKillId(ELROKI);
	}

	public static void onLoad() {
		new Q688_DefeatTheElrokianRaiders(688, "Q688_DefeatTheElrokianRaiders", "", "quests");
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		final int count = st.getQuestItemsCount(DINOSAUR_FANG_NECKLACE);
		if ("None".equalsIgnoreCase(event))
			return null;

		if ("32105-03.htm".equalsIgnoreCase(event)) {
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		} else if ("32105-08.htm".equalsIgnoreCase(event)) {
			if (count > 0) {
				st.takeItems(DINOSAUR_FANG_NECKLACE, -1);
				st.rewardItems(57, count * 3000);
			}
			st.playSound(QuestState.SOUND_FINISH);
			st.exitQuest(true);
		} else if ("32105-06.htm".equalsIgnoreCase(event)) {
			st.takeItems(DINOSAUR_FANG_NECKLACE, -1);
			st.rewardItems(57, count * 3000);
		} else if ("32105-07.htm".equalsIgnoreCase(event)) {
			if (count >= 100) {
				st.takeItems(DINOSAUR_FANG_NECKLACE, 100);
				st.rewardItems(57, 450000);
			} else
				htmltext = "32105-04.htm";
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
				if (player.getLevel() >= 75)
					htmltext = "32105-01.htm";
				else {
					htmltext = "32105-00.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				htmltext = !st.hasQuestItems(DINOSAUR_FANG_NECKLACE) ? "32105-04.htm" : "32105-05.htm";
				break;
		}

		return htmltext;
	}

	@Override
	public String onKill(final L2Npc npc, final L2PcInstance player, final boolean isPet) {
		final L2PcInstance partyMember = getRandomPartyMemberState(player, npc, QuestState.STARTED);
		if (partyMember == null)
			return null;

		final QuestState st = partyMember.getQuestState(qn);

		int chance = (int) (50 * MainConfig.RATE_QUEST_DROP);
		int numItems = chance / 100;
		chance %= 100;

		if (Rnd.get(100) < chance)
			numItems++;

		if (numItems > 0) {
			st.giveItems(DINOSAUR_FANG_NECKLACE, numItems);
			st.playSound(QuestState.SOUND_ITEMGET);
		}

		return null;
	}
}