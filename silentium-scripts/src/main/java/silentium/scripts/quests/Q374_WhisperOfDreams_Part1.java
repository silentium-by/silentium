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

public class Q374_WhisperOfDreams_Part1 extends Quest implements ScriptFile {
	private static final String qn = "Q374_WhisperOfDreams_Part1";

	private static final String condition = "condStone";

	// NPCs
	private static final int MANAKIA = 30515;
	private static final int TORAI = 30557;

	// Monsters
	private static final int CAVE_BEAST = 20620;
	private static final int DEATH_WAVE = 20621;

	// Items
	private static final int CAVE_BEAST_TOOTH = 5884;
	private static final int DEATH_WAVE_LIGHT = 5885;
	private static final int SEALED_MYSTERIOUS_STONE = 5886;
	private static final int MYSTERIOUS_STONE = 5887;

	// Rewards
	private static final int[][] REWARDS =
			{
					{ 5486, 3, 2950 }, // Dark Crystal, 3x, 2950 adena
					{ 5487, 2, 18050 }, // Nightmare, 2x, 18050 adena
					{ 5488, 2, 18050 }, // Majestic, 2x, 18050 adena
					{ 5485, 4, 10450 }, // Tallum Tunic, 4, 10450 adena
					{ 5489, 6, 15550 } // Tallum Stockings, 6, 15550 adena
			};

	public Q374_WhisperOfDreams_Part1(final int scriptId, final String name, final String dname, final String path) {
		super(scriptId, name, dname, path);

		questItemIds = new int[] { DEATH_WAVE_LIGHT, CAVE_BEAST_TOOTH, SEALED_MYSTERIOUS_STONE, MYSTERIOUS_STONE };

		addStartNpc(MANAKIA);
		addTalkId(MANAKIA, TORAI);

		addKillId(CAVE_BEAST, DEATH_WAVE);
	}

	public static void onLoad() {
		new Q374_WhisperOfDreams_Part1(374, "Q374_WhisperOfDreams_Part1", "Whisper Of Dreams 1", "quests");
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		// Manakia
		if ("30515-03.htm".equalsIgnoreCase(event)) {
			st.set("cond", "1");
			st.set(condition, "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		} else if (event.startsWith("30515-06-")) {
			if (st.getQuestItemsCount(CAVE_BEAST_TOOTH) >= 65 && st.getQuestItemsCount(DEATH_WAVE_LIGHT) >= 65) {
				htmltext = "30515-06.htm";
				final int[] reward = REWARDS[Integer.parseInt(event.substring(9, 10))];

				st.takeItems(CAVE_BEAST_TOOTH, -1);
				st.takeItems(DEATH_WAVE_LIGHT, -1);

				st.rewardItems(57, reward[2]);
				st.giveItems(reward[0], reward[1]);

				st.playSound(QuestState.SOUND_MIDDLE);
			} else
				htmltext = "30515-07.htm";
		} else if ("30515-08.htm".equalsIgnoreCase(event)) {
			st.playSound(QuestState.SOUND_FINISH);
			st.exitQuest(true);
		}
		// Torai
		else if ("30557-02.htm".equalsIgnoreCase(event)) {
			if (st.getInt("cond") == 2 && st.hasQuestItems(SEALED_MYSTERIOUS_STONE)) {
				st.set("cond", "3");
				st.takeItems(SEALED_MYSTERIOUS_STONE, -1);
				st.giveItems(MYSTERIOUS_STONE, 1);
				st.playSound(QuestState.SOUND_MIDDLE);
			} else
				htmltext = "30557-03.htm";
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
				if (player.getLevel() < 56 || player.getLevel() > 66) {
					st.exitQuest(true);
					htmltext = "30515-01.htm";
				} else
					htmltext = "30515-02.htm";
				break;

			case QuestState.STARTED:
				final int cond = st.getInt("cond");
				switch (npc.getNpcId()) {
					case MANAKIA:
						if (!st.hasQuestItems(SEALED_MYSTERIOUS_STONE)) {
							htmltext = st.getQuestItemsCount(CAVE_BEAST_TOOTH) >= 65 && st.getQuestItemsCount(DEATH_WAVE_LIGHT) >= 65 ? "30515-05.htm" : "30515-04.htm";
						} else {
							if (cond == 1) {
								st.set("cond", "2");
								st.playSound(QuestState.SOUND_MIDDLE);
								htmltext = "30515-09.htm";
							} else
								htmltext = "30515-10.htm";
						}
						break;

					case TORAI:
						if (cond == 2 && st.hasQuestItems(SEALED_MYSTERIOUS_STONE))
							htmltext = "30557-01.htm";
						break;
				}
				break;
		}

		return htmltext;
	}

	@Override
	public String onKill(final L2Npc npc, final L2PcInstance player, final boolean isPet) {
		// Drop tooth or light to anyone.
		L2PcInstance partyMember = getRandomPartyMemberState(player, npc, QuestState.STARTED);
		if (partyMember == null)
			return null;

		QuestState st = partyMember.getQuestState(qn);

		switch (npc.getNpcId()) {
			case CAVE_BEAST:
				st.dropQuestItems(CAVE_BEAST_TOOTH, 1, 65, 200000);
				break;

			case DEATH_WAVE:
				st.dropQuestItems(DEATH_WAVE_LIGHT, 1, 65, 200000);
				break;
		}

		// Drop sealed mysterious stone to party member who still need it.
		partyMember = getRandomPartyMember(player, npc, condition, "1");
		if (partyMember == null)
			return null;

		st = partyMember.getQuestState(qn);

		if (Rnd.get(1000000) < 4000) {
			st.unset(condition);
			st.giveItems(SEALED_MYSTERIOUS_STONE, 1);
			st.playSound(QuestState.SOUND_ITEMGET);
		}

		return null;
	}
}