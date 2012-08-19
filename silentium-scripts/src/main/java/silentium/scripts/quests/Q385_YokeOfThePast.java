/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.quests;

import gnu.trove.map.hash.TIntIntHashMap;
import silentium.commons.utils.Rnd;
import silentium.gameserver.configs.MainConfig;
import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.quest.Quest;
import silentium.gameserver.model.quest.QuestState;
import silentium.gameserver.scripting.ScriptFile;

public class Q385_YokeOfThePast extends Quest implements ScriptFile {
	private static final String qn = "Q385_YokeOfThePast";

	// NPCs
	private static final int[] GATEKEEPER_ZIGGURAT = { 31095, 31096, 31097, 31098, 31099, 31100, 31101, 31102, 31103, 31104, 31105, 31106, 31107, 31108, 31109, 31110, 31114, 31115, 31116, 31117, 31118, 31119, 31120, 31121, 31122, 31123, 31124, 31125, 31126 };

	// Item
	private static final int ANCIENT_SCROLL = 5902;

	// Reward
	private static final int BLANK_SCROLL = 5965;

	private static final TIntIntHashMap Chance = new TIntIntHashMap();

	static {
		Chance.put(21208, 7);
		Chance.put(21209, 8);
		Chance.put(21210, 11);
		Chance.put(21211, 11);
		Chance.put(21213, 14);
		Chance.put(21214, 19);
		Chance.put(21215, 19);
		Chance.put(21217, 24);
		Chance.put(21218, 30);
		Chance.put(21219, 30);
		Chance.put(21221, 37);
		Chance.put(21222, 46);
		Chance.put(21223, 45);
		Chance.put(21224, 50);
		Chance.put(21225, 54);
		Chance.put(21226, 66);
		Chance.put(21227, 64);
		Chance.put(21228, 70);
		Chance.put(21229, 75);
		Chance.put(21230, 91);
		Chance.put(21231, 86);
		Chance.put(21236, 12);
		Chance.put(21237, 14);
		Chance.put(21238, 19);
		Chance.put(21239, 19);
		Chance.put(21240, 22);
		Chance.put(21241, 24);
		Chance.put(21242, 30);
		Chance.put(21243, 30);
		Chance.put(21244, 34);
		Chance.put(21245, 37);
		Chance.put(21246, 46);
		Chance.put(21247, 45);
		Chance.put(21248, 50);
		Chance.put(21249, 54);
		Chance.put(21250, 99);
		Chance.put(21251, 64);
		Chance.put(21252, 70);
		Chance.put(21253, 75);
		Chance.put(21254, 91);
		Chance.put(21255, 86);
	}

	public Q385_YokeOfThePast(final int questId, final String name, final String descr) {
		super(questId, name, descr);

		questItemIds = new int[] { ANCIENT_SCROLL };

		for (final int ziggurat : GATEKEEPER_ZIGGURAT) {
			addStartNpc(ziggurat);
			addTalkId(ziggurat);
		}

		addKillId(21208, 21209, 21210, 21211, 21213, 21214, 21215, 21217, 21218, 21219, 21221, 21223, 21224, 21225, 21226, 21227, 21228, 21229, 21230, 21231, 21236, 21237, 21238, 21239, 21240, 21241, 21242, 21243, 21244, 21245, 21246, 21247, 21248, 21249, 21250, 21251, 21252, 21253, 21254, 21255);
	}

	public static void onLoad() {
		new Q385_YokeOfThePast(385, "Q385_YokeOfThePast", "quests");
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		final String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if ("05.htm".equalsIgnoreCase(event)) {
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		} else if ("10.htm".equalsIgnoreCase(event)) {
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
				if (player.getLevel() >= 20 && player.getLevel() <= 75)
					htmltext = "01.htm";
				else {
					htmltext = "02.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				if (st.getQuestItemsCount(ANCIENT_SCROLL) == 0)
					htmltext = "08.htm";
				else {
					htmltext = "09.htm";
					final int count = st.getQuestItemsCount(ANCIENT_SCROLL);
					st.takeItems(ANCIENT_SCROLL, -1);
					st.rewardItems(BLANK_SCROLL, count);
				}
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

		final int p = Chance.get(npc.getNpcId());
		int chance = (int) (p * MainConfig.RATE_QUEST_DROP);
		int numItems = chance / 100; // Max is 100
		chance %= 100;

		if (Rnd.get(100) < chance)
			numItems++;

		if (numItems > 0) {
			st.giveItems(ANCIENT_SCROLL, numItems);
			st.playSound(QuestState.SOUND_ITEMGET);
		}

		return null;
	}
}