/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.quests;

import silentium.commons.utils.Rnd;
import silentium.gameserver.instancemanager.FourSepulchersManager;
import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.quest.Quest;
import silentium.gameserver.model.quest.QuestState;
import silentium.gameserver.scripting.ScriptFile;
import silentium.gameserver.utils.Util;

import java.util.Arrays;

public class Q620_FourGoblets extends Quest implements ScriptFile {
	private static final String qn = "Q620_FourGoblets";

	// NPCs
	private static final int NAMELESS_SPIRIT = 31453;

	private static final int GHOST_OF_WIGOTH_1 = 31452;
	private static final int GHOST_OF_WIGOTH_2 = 31454;

	private static final int CONQ_SM = 31921;
	private static final int EMPER_SM = 31922;
	private static final int SAGES_SM = 31923;
	private static final int JUDGE_SM = 31924;

	private static final int GHOST_CHAMBERLAIN_1 = 31919;
	private static final int GHOST_CHAMBERLAIN_2 = 31920;

	// Items
	private static final int GRAVE_PASS = 7261;
	private static final int[] GOBLETS = { 7256, 7257, 7258, 7259 };
	private static final int RELIC = 7254;
	public static final int SEALED_BOX = 7255;

	// Rewards
	private static final int ANTIQUE_BROOCH = 7262;
	private static final int[] RCP_REWARDS = { 6881, 6883, 6885, 6887, 6891, 6893, 6895, 6897, 6899, 7580 };

	public Q620_FourGoblets(final int scriptId, final String name, final String dname, final String path) {
		super(scriptId, name, dname, path);

		questItemIds = new int[] { SEALED_BOX, GRAVE_PASS, 7256, 7257, 7258, 7259 };

		addStartNpc(NAMELESS_SPIRIT);
		addStartNpc(CONQ_SM);
		addStartNpc(EMPER_SM);
		addStartNpc(SAGES_SM);
		addStartNpc(JUDGE_SM);
		addStartNpc(GHOST_CHAMBERLAIN_1);
		addStartNpc(GHOST_CHAMBERLAIN_2);

		addTalkId(NAMELESS_SPIRIT);
		addTalkId(CONQ_SM);
		addTalkId(EMPER_SM);
		addTalkId(SAGES_SM);
		addTalkId(JUDGE_SM);
		addTalkId(GHOST_CHAMBERLAIN_1);
		addTalkId(GHOST_CHAMBERLAIN_2);
		addTalkId(GHOST_OF_WIGOTH_1);
		addTalkId(GHOST_OF_WIGOTH_2);

		for (int id = 18120; id <= 18256; id++)
			addKillId(id);
	}

	public static void onLoad() {
		new Q620_FourGoblets(620, "Q620_FourGoblets", "", "quests");
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		final int cond = st.getInt("cond");
		if ("Enter".equalsIgnoreCase(event)) {
			FourSepulchersManager.getInstance().tryEntry(npc, player);
			return null;
		} else if ("accept".equalsIgnoreCase(event)) {
			if (cond == 0) {
				if (st.getPlayer().getLevel() >= 74) {
					st.setState(QuestState.STARTED);
					st.playSound(QuestState.SOUND_ACCEPT);
					htmltext = "31453-13.htm";
					st.set("cond", "1");
				} else {
					htmltext = "31453-12.htm";
					st.exitQuest(true);
				}
			}
		} else if ("11".equalsIgnoreCase(event)) {
			if (st.getQuestItemsCount(SEALED_BOX) >= 1) {
				htmltext = "31454-13.htm";
				st.takeItems(SEALED_BOX, 1);

				if (!calculateBoxReward(st)) {
					htmltext = Rnd.get(2) == 0 ? "31454-14.htm" : "31454-15.htm";
				}
			}
		} else if ("12".equalsIgnoreCase(event)) {
			if (st.getQuestItemsCount(GOBLETS[0]) >= 1 && st.getQuestItemsCount(GOBLETS[1]) >= 1 && st.getQuestItemsCount(GOBLETS[2]) >= 1 && st.getQuestItemsCount(GOBLETS[3]) >= 1) {
				st.takeItems(GOBLETS[0], -1);
				st.takeItems(GOBLETS[1], -1);
				st.takeItems(GOBLETS[2], -1);
				st.takeItems(GOBLETS[3], -1);
				st.giveItems(ANTIQUE_BROOCH, 1);
				st.set("cond", "2");
				st.playSound(QuestState.SOUND_FINISH);
				htmltext = "31453-16.htm";
			} else
				htmltext = "31453-14.htm";
		} else if ("31453-18.htm".equalsIgnoreCase(event)) {
			st.playSound(QuestState.SOUND_FINISH);
			st.exitQuest(true);
		} else if ("14".equalsIgnoreCase(event)) {
			htmltext = "31453-13.htm";

			if (cond == 2)
				htmltext = "31453-19.htm";
		}
		// Ghost Chamberlain of Elmoreden: Teleport to 4th sepulcher
		else if ("15".equalsIgnoreCase(event)) {
			if (st.getQuestItemsCount(ANTIQUE_BROOCH) >= 1) {
				st.getPlayer().teleToLocation(178298, -84574, -7216);
				htmltext = null;
			} else if (st.getQuestItemsCount(GRAVE_PASS) >= 1) {
				st.takeItems(GRAVE_PASS, 1);
				st.getPlayer().teleToLocation(178298, -84574, -7216);
				htmltext = null;
			} else
				htmltext = npc.getNpcId() + "-0.htm";
		}
		// Ghost Chamberlain of Elmoreden: Teleport to Imperial Tomb entrance
		else if ("16".equalsIgnoreCase(event)) {
			if (st.getQuestItemsCount(ANTIQUE_BROOCH) >= 1) {
				st.getPlayer().teleToLocation(186942, -75602, -2834);
				htmltext = null;
			} else if (st.getQuestItemsCount(GRAVE_PASS) >= 1) {
				st.takeItems(GRAVE_PASS, 1);
				st.getPlayer().teleToLocation(186942, -75602, -2834);
				htmltext = null;
			} else
				htmltext = npc.getNpcId() + "-0.htm";
		}
		// Teleport to Pilgrims Temple
		else if ("17".equalsIgnoreCase(event)) {
			if (st.getQuestItemsCount(ANTIQUE_BROOCH) >= 1)
				st.getPlayer().teleToLocation(169590, -90218, -2914);
			else {
				st.takeItems(GRAVE_PASS, 1);
				st.getPlayer().teleToLocation(169590, -90218, -2914);
			}
			htmltext = "31452-6.htm";
		} else if ("18".equalsIgnoreCase(event)) {
			if (st.getQuestItemsCount(GOBLETS[0]) + st.getQuestItemsCount(GOBLETS[1]) + st.getQuestItemsCount(GOBLETS[2]) + st.getQuestItemsCount(GOBLETS[3]) < 3)
				htmltext = "31452-3.htm";
			else if (st.getQuestItemsCount(GOBLETS[0]) + st.getQuestItemsCount(GOBLETS[1]) + st.getQuestItemsCount(GOBLETS[2]) + st.getQuestItemsCount(GOBLETS[3]) == 3)
				htmltext = "31452-4.htm";
			else if (st.getQuestItemsCount(GOBLETS[0]) + st.getQuestItemsCount(GOBLETS[1]) + st.getQuestItemsCount(GOBLETS[2]) + st.getQuestItemsCount(GOBLETS[3]) >= 4)
				htmltext = "31452-5.htm";
		} else if ("19".equalsIgnoreCase(event)) {
			if (st.getQuestItemsCount(SEALED_BOX) >= 1) {
				htmltext = "31919-3.htm";
				st.takeItems(SEALED_BOX, 1);

				if (!calculateBoxReward(st)) {
					htmltext = Rnd.get(2) == 0 ? "31919-4.htm" : "31919-5.htm";
				}
			} else
				htmltext = "31919-6.htm";
		}
		// If event is a simple digit, parse it to get an integer form, then test the reward list
		else if (Util.isDigit(event)) {
			final int id = Integer.parseInt(event);
			Arrays.sort(RCP_REWARDS);

			if (Arrays.binarySearch(RCP_REWARDS, id) > 0) {
				st.takeItems(RELIC, 1000);
				st.giveItems(id, 1);
				return "31454-17.htm";
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

		final int npcId = npc.getNpcId();
		final int id = st.getState();
		final int cond = st.getInt("cond");

		if (id == QuestState.CREATED)
			st.set("cond", "0");

		if (npcId == NAMELESS_SPIRIT) {
			if (cond == 0) {
				if (st.getPlayer().getLevel() >= 74)
					htmltext = "31453-1.htm";
				else {
					htmltext = "31453-12.htm";
					st.exitQuest(true);
				}
			} else if (cond == 1) {
				htmltext = st.getQuestItemsCount(GOBLETS[0]) >= 1 && st.getQuestItemsCount(GOBLETS[1]) >= 1 && st.getQuestItemsCount(GOBLETS[2]) >= 1 && st.getQuestItemsCount(GOBLETS[3]) >= 1 ? "31453-15.htm" : "31453-14.htm";
			} else if (cond == 2)
				htmltext = "31453-17.htm";
		} else if (npcId == GHOST_OF_WIGOTH_1) {
			if (cond == 2)
				htmltext = "31452-2.htm";
			else if (cond == 1)
				if (st.getQuestItemsCount(GOBLETS[0]) + st.getQuestItemsCount(GOBLETS[1]) + st.getQuestItemsCount(GOBLETS[2]) + st.getQuestItemsCount(GOBLETS[3]) == 1)
					htmltext = "31452-1.htm";
				else if (st.getQuestItemsCount(GOBLETS[0]) + st.getQuestItemsCount(GOBLETS[1]) + st.getQuestItemsCount(GOBLETS[2]) + st.getQuestItemsCount(GOBLETS[3]) > 1)
					htmltext = "31452-2.htm";
		} else if (npcId == GHOST_OF_WIGOTH_2) {
			if (st.getQuestItemsCount(RELIC) >= 1000)
				if (st.getQuestItemsCount(SEALED_BOX) >= 1)
					if (st.getQuestItemsCount(GOBLETS[0]) >= 1 && st.getQuestItemsCount(GOBLETS[1]) >= 1 && st.getQuestItemsCount(GOBLETS[2]) >= 1 && st.getQuestItemsCount(GOBLETS[3]) >= 1)
						htmltext = "31454-4.htm";
					else
						htmltext = st.getQuestItemsCount(GOBLETS[0]) + st.getQuestItemsCount(GOBLETS[1]) + st.getQuestItemsCount(GOBLETS[2]) + st.getQuestItemsCount(GOBLETS[3]) > 1 ? "31454-8.htm" : "31454-12.htm";
				else if (st.getQuestItemsCount(GOBLETS[0]) >= 1 && st.getQuestItemsCount(GOBLETS[1]) >= 1 && st.getQuestItemsCount(GOBLETS[2]) >= 1 && st.getQuestItemsCount(GOBLETS[3]) >= 1)
					htmltext = "31454-3.htm";
				else
					htmltext = st.getQuestItemsCount(GOBLETS[0]) + st.getQuestItemsCount(GOBLETS[1]) + st.getQuestItemsCount(GOBLETS[2]) + st.getQuestItemsCount(GOBLETS[3]) > 1 ? "31454-7.htm" : "31454-11.htm";
			else if (st.getQuestItemsCount(SEALED_BOX) >= 1)
				if (st.getQuestItemsCount(GOBLETS[0]) >= 1 && st.getQuestItemsCount(GOBLETS[1]) >= 1 && st.getQuestItemsCount(GOBLETS[2]) >= 1 && st.getQuestItemsCount(GOBLETS[3]) >= 1)
					htmltext = "31454-2.htm";
				else
					htmltext = st.getQuestItemsCount(GOBLETS[0]) + st.getQuestItemsCount(GOBLETS[1]) + st.getQuestItemsCount(GOBLETS[2]) + st.getQuestItemsCount(GOBLETS[3]) > 1 ? "31454-6.htm" : "31454-10.htm";
			else if (st.getQuestItemsCount(GOBLETS[0]) >= 1 && st.getQuestItemsCount(GOBLETS[1]) >= 1 && st.getQuestItemsCount(GOBLETS[2]) >= 1 && st.getQuestItemsCount(GOBLETS[3]) >= 1)
				htmltext = "31454-1.htm";
			else
				htmltext = st.getQuestItemsCount(GOBLETS[0]) + st.getQuestItemsCount(GOBLETS[1]) + st.getQuestItemsCount(GOBLETS[2]) + st.getQuestItemsCount(GOBLETS[3]) > 1 ? "31454-5.htm" : "31454-9.htm";
		} else if (npcId == CONQ_SM)
			htmltext = "31921-E.htm";
		else if (npcId == EMPER_SM)
			htmltext = "31922-E.htm";
		else if (npcId == SAGES_SM)
			htmltext = "31923-E.htm";
		else if (npcId == JUDGE_SM)
			htmltext = "31924-E.htm";
		else if (npcId == GHOST_CHAMBERLAIN_1)
			htmltext = "31919-1.htm";

		return htmltext;
	}

	@Override
	public String onKill(final L2Npc npc, final L2PcInstance player, final boolean isPet) {
		final L2PcInstance partyMember = getRandomPartyMemberState(player, npc, QuestState.STARTED);
		if (partyMember == null)
			return null;

		if (Rnd.get(100) < 30) {
			final QuestState st = partyMember.getQuestState(qn);

			st.giveItems(SEALED_BOX, 1);
			st.playSound(QuestState.SOUND_ITEMGET);
		}

		return null;
	}

	/**
	 * Calculate boxes rewards, then return if there was a reward.
	 *
	 * @param st the QuestState of the player, used to reward him.
	 * @return true if there was a reward, false if not (used to call a "no-reward" html)
	 */
	private boolean calculateBoxReward(final QuestState st) {
		boolean reward = false;
		final int rnd = Rnd.get(5);

		if (rnd == 0) {
			st.giveItems(57, 10000);
			reward = true;
		} else if (rnd == 1) {
			if (Rnd.get(1000) < 848) {
				reward = true;
				final int i = Rnd.get(1000);

				if (i < 43)
					st.giveItems(1884, 42);
				else if (i < 66)
					st.giveItems(1895, 36);
				else if (i < 184)
					st.giveItems(1876, 4);
				else if (i < 250)
					st.giveItems(1881, 6);
				else if (i < 287)
					st.giveItems(5549, 8);
				else if (i < 484)
					st.giveItems(1874, 1);
				else if (i < 681)
					st.giveItems(1889, 1);
				else if (i < 799)
					st.giveItems(1877, 1);
				else if (i < 902)
					st.giveItems(1894, 1);
				else
					st.giveItems(4043, 1);
			}

			if (Rnd.get(1000) < 323) {
				reward = true;
				final int i = Rnd.get(1000);

				if (i < 335)
					st.giveItems(1888, 1);
				else if (i < 556)
					st.giveItems(4040, 1);
				else if (i < 725)
					st.giveItems(1890, 1);
				else if (i < 872)
					st.giveItems(5550, 1);
				else if (i < 962)
					st.giveItems(1893, 1);
				else if (i < 986)
					st.giveItems(4046, 1);
				else
					st.giveItems(4048, 1);
			}
		} else if (rnd == 2) {
			if (Rnd.get(1000) < 847) {
				reward = true;
				final int i = Rnd.get(1000);

				if (i < 148)
					st.giveItems(1878, 8);
				else if (i < 175)
					st.giveItems(1882, 24);
				else if (i < 273)
					st.giveItems(1879, 4);
				else if (i < 322)
					st.giveItems(1880, 6);
				else if (i < 357)
					st.giveItems(1885, 6);
				else if (i < 554)
					st.giveItems(1875, 1);
				else if (i < 685)
					st.giveItems(1883, 1);
				else if (i < 803)
					st.giveItems(5220, 1);
				else if (i < 901)
					st.giveItems(4039, 1);
				else
					st.giveItems(4044, 1);
			}

			if (Rnd.get(1000) < 251) {
				reward = true;
				final int i = Rnd.get(1000);

				if (i < 350)
					st.giveItems(1887, 1);
				else if (i < 587)
					st.giveItems(4042, 1);
				else if (i < 798)
					st.giveItems(1886, 1);
				else if (i < 922)
					st.giveItems(4041, 1);
				else if (i < 966)
					st.giveItems(1892, 1);
				else if (i < 996)
					st.giveItems(1891, 1);
				else
					st.giveItems(4047, 1);
			}
		} else if (rnd == 3) {
			if (Rnd.get(1000) < 31) {
				reward = true;
				final int i = Rnd.get(1000);

				if (i < 223)
					st.giveItems(730, 1);
				else if (i < 893)
					st.giveItems(948, 1);
				else
					st.giveItems(960, 1);
			}

			if (Rnd.get(1000) < 5) {
				reward = true;
				final int i = Rnd.get(1000);

				if (i < 202)
					st.giveItems(729, 1);
				else if (i < 928)
					st.giveItems(947, 1);
				else
					st.giveItems(959, 1);
			}
		} else if (rnd == 4) {
			if (Rnd.get(1000) < 329) {
				reward = true;
				final int i = Rnd.get(1000);

				if (i < 88)
					st.giveItems(6698, 1);
				else if (i < 185)
					st.giveItems(6699, 1);
				else if (i < 238)
					st.giveItems(6700, 1);
				else if (i < 262)
					st.giveItems(6701, 1);
				else if (i < 292)
					st.giveItems(6702, 1);
				else if (i < 356)
					st.giveItems(6703, 1);
				else if (i < 420)
					st.giveItems(6704, 1);
				else if (i < 482)
					st.giveItems(6705, 1);
				else if (i < 554)
					st.giveItems(6706, 1);
				else if (i < 576)
					st.giveItems(6707, 1);
				else if (i < 640)
					st.giveItems(6708, 1);
				else if (i < 704)
					st.giveItems(6709, 1);
				else if (i < 777)
					st.giveItems(6710, 1);
				else if (i < 799)
					st.giveItems(6711, 1);
				else if (i < 863)
					st.giveItems(6712, 1);
				else if (i < 927)
					st.giveItems(6713, 1);
				else
					st.giveItems(6714, 1);
			}

			if (Rnd.get(1000) < 54) {
				reward = true;
				final int i = Rnd.get(1000);

				if (i < 100)
					st.giveItems(6688, 1);
				else if (i < 198)
					st.giveItems(6689, 1);
				else if (i < 298)
					st.giveItems(6690, 1);
				else if (i < 398)
					st.giveItems(6691, 1);
				else if (i < 499)
					st.giveItems(7579, 1);
				else if (i < 601)
					st.giveItems(6693, 1);
				else if (i < 703)
					st.giveItems(6694, 1);
				else if (i < 801)
					st.giveItems(6695, 1);
				else if (i < 902)
					st.giveItems(6696, 1);
				else
					st.giveItems(6697, 1);
			}
		}

		return reward;
	}
}