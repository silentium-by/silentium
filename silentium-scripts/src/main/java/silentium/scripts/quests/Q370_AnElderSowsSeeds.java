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

public class Q370_AnElderSowsSeeds extends Quest implements ScriptFile {
	private static final String qn = "Q370_AnElderSowsSeeds";

	// NPC
	private static final int CASIAN = 30612;

	// Items
	private static final int SPELLBOOK_PAGE = 5916;
	private static final int CHAPTER_OF_FIRE = 5917;
	private static final int CHAPTER_OF_WATER = 5918;
	private static final int CHAPTER_OF_WIND = 5919;
	private static final int CHAPTER_OF_EARTH = 5920;

	public Q370_AnElderSowsSeeds(final int scriptId, final String name, final String dname, final String path) {
		super(scriptId, name, dname, path);

		questItemIds = new int[] { SPELLBOOK_PAGE, CHAPTER_OF_FIRE, CHAPTER_OF_WATER, CHAPTER_OF_WIND, CHAPTER_OF_EARTH };

		addStartNpc(CASIAN);
		addTalkId(CASIAN);

		addKillId(20082, 20084, 20086, 20089, 20090);
	}

	public static void onLoad() {
		new Q370_AnElderSowsSeeds(370, "Q370_AnElderSowsSeeds", "", "quests");
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if ("30612-3.htm".equalsIgnoreCase(event)) {
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		} else if ("30612-6.htm".equalsIgnoreCase(event)) {
			if (st.getQuestItemsCount(CHAPTER_OF_FIRE) > 0 && st.getQuestItemsCount(CHAPTER_OF_WATER) > 0 && st.getQuestItemsCount(CHAPTER_OF_WIND) > 0 && st.getQuestItemsCount(CHAPTER_OF_EARTH) > 0) {
				htmltext = "30612-8.htm";
				st.takeItems(CHAPTER_OF_FIRE, 1);
				st.takeItems(CHAPTER_OF_WATER, 1);
				st.takeItems(CHAPTER_OF_WIND, 1);
				st.takeItems(CHAPTER_OF_EARTH, 1);
				st.rewardItems(57, 3600);
			}
		} else if ("30612-9.htm".equalsIgnoreCase(event)) {
			st.playSound(QuestState.SOUND_FINISH);
			st.exitQuest(true);
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
				if (player.getLevel() >= 28 && player.getLevel() <= 42)
					htmltext = "30612-0.htm";
				else {
					htmltext = "30612-0a.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				htmltext = "30612-4.htm";
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

		st.giveItems(SPELLBOOK_PAGE, 1);
		st.playSound(QuestState.SOUND_ITEMGET);

		return null;
	}
}