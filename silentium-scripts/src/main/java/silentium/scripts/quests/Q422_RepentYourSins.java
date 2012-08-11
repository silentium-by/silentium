/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.quests;

import silentium.commons.utils.Rnd;
import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.L2Summon;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.quest.Quest;
import silentium.gameserver.model.quest.QuestState;
import silentium.gameserver.network.serverpackets.UserInfo;

public class Q422_RepentYourSins extends Quest
{
	private final static String qn = "Q422_RepentYourSins";

	// Items
	private static final int SCAVENGER_WERERAT_SKULL = 4326;
	private static final int TUREK_WARHOUND_TAIL = 4327;
	private static final int TYRANT_KINGPIN_HEART = 4328;
	private static final int TRISALIM_TARANTULAS_VENOM_SAC = 4329;

	private static final int MANUAL_OF_MANACLES = 4331;
	private static final int PENITENTS_MANACLES = 4425;
	private static final int QITEM_PENITENTS_MANACLES = 4330;
	private static final int LEFT_PENITENTS_MANACLES = 4426;

	private static final int SILVER_NUGGET = 1873;
	private static final int ADAMANTINE_NUGGET = 1877;
	private static final int BLACKSMITHS_FRAME = 1892;
	private static final int COKES = 1879;
	private static final int STEEL = 1880;

	// NPCs
	private static final int BLACK_JUDGE = 30981;
	private static final int KATARI = 30668;
	private static final int PIOTUR = 30597;
	private static final int CASIAN = 30612;
	private static final int JOAN = 30718;
	private static final int PUSHKIN = 30300;

	public Q422_RepentYourSins(int questId, String name, String descr)
	{
		super(questId, name, descr);

		questItemIds = new int[] { SCAVENGER_WERERAT_SKULL, TUREK_WARHOUND_TAIL, TYRANT_KINGPIN_HEART, TRISALIM_TARANTULAS_VENOM_SAC, MANUAL_OF_MANACLES, PENITENTS_MANACLES, QITEM_PENITENTS_MANACLES };

		addStartNpc(BLACK_JUDGE);
		addTalkId(BLACK_JUDGE, KATARI, PIOTUR, CASIAN, JOAN, PUSHKIN);

		addKillId(20039, 20494, 20193, 20561);
	}

	public static void main(String[] args)
	{
		new Q422_RepentYourSins(422, "Q422_RepentYourSins", "Repent Your Sins");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event.equalsIgnoreCase("Start"))
		{
			st.set("cond", "1");
			if (player.getLevel() <= 20)
			{
				st.set("cond", "2");
				htmltext = "30981-03.htm";
			}
			else if (player.getLevel() >= 20 && player.getLevel() <= 30)
			{
				st.set("cond", "3");
				htmltext = "30981-04.htm";
			}
			else if (player.getLevel() >= 30 && player.getLevel() <= 40)
			{
				st.set("cond", "4");
				htmltext = "30981-05.htm";
			}
			else
			{
				st.set("cond", "5");
				htmltext = "30981-06.htm";
			}
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("30981-11.htm"))
		{
			if (st.getQuestItemsCount(PENITENTS_MANACLES) == 0)
			{
				int cond = st.getInt("cond");

				// Case you return back the qitem to Black Judge. She rewards you with the pet item.
				if (cond == 15)
				{
					st.set("cond", "16");
					st.playSound(QuestState.SOUND_ITEMGET);

					st.set("level", String.valueOf(player.getLevel()));
					st.takeItems(QITEM_PENITENTS_MANACLES, -1);
					st.giveItems(PENITENTS_MANACLES, 1);
				}
				// Case you return back to Black Judge with leftover of previous quest.
				else if (cond == 16)
				{
					st.playSound(QuestState.SOUND_ITEMGET);

					st.set("level", String.valueOf(player.getLevel()));
					st.takeItems(LEFT_PENITENTS_MANACLES, -1);
					st.giveItems(PENITENTS_MANACLES, 1);
				}
			}
		}
		else if (event.equalsIgnoreCase("30981-19.htm"))
		{
			if (st.getQuestItemsCount(LEFT_PENITENTS_MANACLES) > 0)
			{
				st.set("cond", "16");
				st.setState(QuestState.STARTED);
				st.playSound(QuestState.SOUND_ACCEPT);
			}
		}
		else if (event.equalsIgnoreCase("Pk"))
		{
			final L2Summon pet = player.getPet();

			// If Sin Eater is currently summoned, show a warning.
			if (pet != null && pet.getNpcId() == 12564)
				htmltext = "30981-16.htm";
			// If Sin Eater level is bigger than registered level, decrease PK counter by 1-10.
			else if (findSinEaterLvl(player) > st.getInt("level"))
			{
				st.takeItems(PENITENTS_MANACLES, 1);
				st.giveItems(LEFT_PENITENTS_MANACLES, 1);

				int removePkAmount = Rnd.get(10) + 1;

				// Player's PKs are lower than random amount ; finish the quest.
				if (player.getPkKills() <= removePkAmount)
				{
					htmltext = "30981-15.htm";
					st.playSound(QuestState.SOUND_FINISH);
					st.exitQuest(true);

					player.setPkKills(0);
					player.sendPacket(new UserInfo(player));
				}
				// Player's PK are bigger than random amount ; continue the quest.
				else
				{
					st.set("level", String.valueOf(player.getLevel()));
					htmltext = "30981-14.htm";
					st.playSound(QuestState.SOUND_MIDDLE);

					player.setPkKills(player.getPkKills() - removePkAmount);
					player.sendPacket(new UserInfo(player));
				}
			}
		}
		else if (event.equalsIgnoreCase("Quit"))
		{
			htmltext = "30981-20.htm";

			st.takeItems(SCAVENGER_WERERAT_SKULL, -1);
			st.takeItems(TUREK_WARHOUND_TAIL, -1);
			st.takeItems(TYRANT_KINGPIN_HEART, -1);
			st.takeItems(TRISALIM_TARANTULAS_VENOM_SAC, -1);

			st.takeItems(MANUAL_OF_MANACLES, -1);
			st.takeItems(PENITENTS_MANACLES, -1);
			st.takeItems(QITEM_PENITENTS_MANACLES, -1);

			st.playSound(QuestState.SOUND_FINISH);
			st.exitQuest(true);
		}

		return htmltext;
	}

	@Override
	public String onTalk(L2Npc npc, L2PcInstance player)
	{
		String htmltext = Quest.getAlreadyCompletedMsg();
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		switch (st.getState())
		{
			case QuestState.CREATED:
				if (player.getPkKills() >= 1)
				{
					if (st.getQuestItemsCount(LEFT_PENITENTS_MANACLES) > 0)
						htmltext = "30981-18.htm";
					else
						htmltext = "30981-02.htm";
				}
				else
				{
					htmltext = "30981-01.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				int cond = st.getInt("cond");
				switch (npc.getNpcId())
				{
					case BLACK_JUDGE:
						if (cond <= 9)
							htmltext = "30981-07.htm";
						else if (cond > 9 && cond <= 13)
						{
							st.set("cond", "14");
							htmltext = "30981-08.htm";
							st.giveItems(MANUAL_OF_MANACLES, 1);
							st.playSound(QuestState.SOUND_MIDDLE);
						}
						else if (cond == 14)
							htmltext = "30981-09.htm";
						else if (cond == 15)
							htmltext = "30981-10.htm";
						else if (cond == 16)
						{
							// If you got manacles.
							if (st.getQuestItemsCount(PENITENTS_MANACLES) > 0)
							{
								// If the Sin Eater raised level.
								if (findSinEaterLvl(player) > st.getInt("level"))
									htmltext = "30981-13.htm";
								else
									htmltext = "30981-12.htm";
							}
							else
								htmltext = "30981-18.htm";
						}
						break;

					case KATARI:
						if (cond == 2)
						{
							st.set("cond", "6");
							htmltext = "30668-01.htm";
							st.playSound(QuestState.SOUND_MIDDLE);
						}
						else if (cond == 6)
						{
							if (st.getQuestItemsCount(SCAVENGER_WERERAT_SKULL) < 10)
								htmltext = "30668-02.htm";
							else
							{
								st.set("cond", "10");
								htmltext = "30668-03.htm";
								st.takeItems(SCAVENGER_WERERAT_SKULL, -1);
								st.playSound(QuestState.SOUND_MIDDLE);
							}
						}
						else if (cond == 10)
							htmltext = "30668-04.htm";
						break;

					case PIOTUR:
						if (cond == 3)
						{
							st.set("cond", "7");
							htmltext = "30597-01.htm";
							st.playSound(QuestState.SOUND_MIDDLE);
						}
						else if (cond == 7)
						{
							if (st.getQuestItemsCount(TUREK_WARHOUND_TAIL) < 10)
								htmltext = "30597-02.htm";
							else
							{
								st.set("cond", "11");
								htmltext = "30597-03.htm";
								st.takeItems(TUREK_WARHOUND_TAIL, -1);
								st.playSound(QuestState.SOUND_MIDDLE);
							}
						}
						else if (cond == 11)
							htmltext = "30597-04.htm";
						break;

					case CASIAN:
						if (cond == 4)
						{
							st.set("cond", "8");
							htmltext = "30612-01.htm";
							st.playSound(QuestState.SOUND_MIDDLE);
						}
						else if (cond == 8)
						{
							if (st.getQuestItemsCount(TYRANT_KINGPIN_HEART) < 1)
								htmltext = "30612-02.htm";
							else
							{
								st.set("cond", "12");
								htmltext = "30612-03.htm";
								st.takeItems(TYRANT_KINGPIN_HEART, -1);
								st.playSound(QuestState.SOUND_MIDDLE);
							}
						}
						else if (cond == 12)
							htmltext = "30612-04.htm";
						break;

					case JOAN:
						if (cond == 5)
						{
							st.set("cond", "9");
							htmltext = "30718-01.htm";
							st.playSound(QuestState.SOUND_MIDDLE);
						}
						else if (cond == 9)
						{
							if (st.getQuestItemsCount(TRISALIM_TARANTULAS_VENOM_SAC) < 3)
								htmltext = "30718-02.htm";
							else
							{
								st.set("cond", "13");
								htmltext = "30718-03.htm";
								st.takeItems(TRISALIM_TARANTULAS_VENOM_SAC, -1);
								st.playSound(QuestState.SOUND_MIDDLE);
							}
						}
						else if (cond == 13)
							htmltext = "30718-04.htm";
						break;

					case PUSHKIN:
						if (cond == 14 && st.getQuestItemsCount(MANUAL_OF_MANACLES) == 1)
						{
							if (st.getQuestItemsCount(SILVER_NUGGET) < 10 || st.getQuestItemsCount(STEEL) < 5 || st.getQuestItemsCount(ADAMANTINE_NUGGET) < 2 || st.getQuestItemsCount(COKES) < 10 || st.getQuestItemsCount(BLACKSMITHS_FRAME) < 1)
								htmltext = "30300-02.htm";
							else
							{
								st.set("cond", "15");
								htmltext = "30300-01.htm";
								st.playSound(QuestState.SOUND_MIDDLE);

								st.takeItems(MANUAL_OF_MANACLES, 1);
								st.takeItems(SILVER_NUGGET, 10);
								st.takeItems(ADAMANTINE_NUGGET, 2);
								st.takeItems(COKES, 10);
								st.takeItems(STEEL, 5);
								st.takeItems(BLACKSMITHS_FRAME, 1);

								st.giveItems(QITEM_PENITENTS_MANACLES, 1);
							}
						}
						else if (st.getQuestItemsCount(QITEM_PENITENTS_MANACLES) > 0 || st.getQuestItemsCount(PENITENTS_MANACLES) > 0 || st.getQuestItemsCount(LEFT_PENITENTS_MANACLES) > 0)
							htmltext = "30300-03.htm";
						break;
				}
				break;
		}

		return htmltext;
	}

	@Override
	public String onKill(L2Npc npc, L2PcInstance player, boolean isPet)
	{
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return null;

		switch (npc.getNpcId())
		{
			case 20039:
				if (st.getInt("cond") == 6)
					st.dropAlwaysQuestItems(SCAVENGER_WERERAT_SKULL, 1, 10);
				break;

			case 20494:
				if (st.getInt("cond") == 7)
					st.dropAlwaysQuestItems(TUREK_WARHOUND_TAIL, 1, 10);
				break;

			case 20193:
				if (st.getInt("cond") == 8)
					st.dropAlwaysQuestItems(TYRANT_KINGPIN_HEART, 1, 1);
				break;

			case 20561:
				if (st.getInt("cond") == 9)
					st.dropAlwaysQuestItems(TRISALIM_TARANTULAS_VENOM_SAC, 1, 3);
				break;
		}

		return null;
	}

	private int findSinEaterLvl(L2PcInstance player)
	{
		return player.getInventory().getItemByItemId(PENITENTS_MANACLES).getEnchantLevel();
	}
}