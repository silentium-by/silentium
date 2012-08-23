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

public class Q300_HuntingLetoLizardman extends Quest implements ScriptFile {
	private static final String qn = "Q300_HuntingLetoLizardman";

	// NPC
	private static final int RATH = 30126;

	// Item
	private static final int BRACELET = 7139;

	public Q300_HuntingLetoLizardman(final int scriptId, final String name, final String dname, final String path) {
		super(scriptId, name, dname, path);

		questItemIds = new int[] { BRACELET };

		addStartNpc(RATH);
		addTalkId(RATH);

		addKillId(20577, 20578, 20579, 20580, 20582);
	}

	public static void onLoad() {
		new Q300_HuntingLetoLizardman(300, "Q300_HuntingLetoLizardman", "", "quests");
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if ("30126-03.htm".equalsIgnoreCase(event)) {
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		} else if ("30126-05.htm".equalsIgnoreCase(event)) {
			if (st.getQuestItemsCount(BRACELET) >= 60) {
				htmltext = "30126-06.htm";

				final int luck = Rnd.get(3);

				st.takeItems(BRACELET, -1);
				if (luck == 0)
					st.rewardItems(57, 30000);
				else if (luck == 1)
					st.rewardItems(1867, 50);
				else if (luck == 2)
					st.rewardItems(1872, 50);

				st.playSound(QuestState.SOUND_FINISH);
				st.exitQuest(true);
			}
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
				if (player.getLevel() >= 34 && player.getLevel() <= 39)
					htmltext = "30126-02.htm";
				else {
					htmltext = "30126-01.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				htmltext = st.getQuestItemsCount(BRACELET) >= 60 ? "30126-04.htm" : "30126-04a.htm";
				break;
		}

		return htmltext;
	}

	@Override
	public String onKill(final L2Npc npc, final L2PcInstance player, final boolean isPet) {
		final L2PcInstance partyMember = getRandomPartyMember(player, npc, "1");
		if (partyMember != null)
			partyMember.getQuestState(qn).dropQuestItems(BRACELET, 1, 60, 330000);

		return null;
	}
}