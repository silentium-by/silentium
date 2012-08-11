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
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.quest.Quest;
import silentium.gameserver.model.quest.QuestState;

public class Q171_ActsOfEvil extends Quest
{
	private static final String qn = "Q171_ActsOfEvil";

	// Items
	private final static int BLADE_MOLD = 4239;
	private final static int TYRAS_BILL = 4240;
	private final static int RANGERS_REPORT1 = 4241;
	private final static int RANGERS_REPORT2 = 4242;
	private final static int RANGERS_REPORT3 = 4243;
	private final static int RANGERS_REPORT4 = 4244;
	private final static int WEAPON_TRADE_CONTRACT = 4245;
	private final static int ATTACK_DIRECTIVES = 4246;
	private final static int CERTIFICATE = 4247;
	private final static int CARGOBOX = 4248;
	private final static int OL_MAHUM_HEAD = 4249;

	// Reward
	private final static int ADENA = 57;

	// NPCs
	private final static int ALVAH = 30381;
	private final static int ARODIN = 30207;
	private final static int TYRA = 30420;
	private final static int ROLENTO = 30437;
	private final static int NETI = 30425;
	private final static int BURAI = 30617;

	public Q171_ActsOfEvil(int questId, String name, String descr)
	{
		super(questId, name, descr);

		questItemIds = new int[] { RANGERS_REPORT1, RANGERS_REPORT2, RANGERS_REPORT3, RANGERS_REPORT4, OL_MAHUM_HEAD, CARGOBOX, TYRAS_BILL, CERTIFICATE, BLADE_MOLD, WEAPON_TRADE_CONTRACT };

		addStartNpc(ALVAH);
		addTalkId(ALVAH, ARODIN, TYRA, ROLENTO, NETI, BURAI);

		addKillId(20496, 20497, 20498, 20499, 20062, 20066, 20438);
	}

	public static void main(String[] args)
	{
		new Q171_ActsOfEvil(171, "Q171_ActsOfEvil", "Acts of Evil");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		int cond = st.getInt("cond");
		if (event.equalsIgnoreCase("30381-02.htm"))
		{
			st.setState(QuestState.STARTED);
			st.set("cond", "1");
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("30207-02.htm") && cond == 1)
			st.set("cond", "2");
		else if (event.equalsIgnoreCase("30381-04.htm") && cond == 4)
			st.set("cond", "5");
		else if (event.equalsIgnoreCase("30381-07.htm") && cond == 6)
		{
			st.set("cond", "7");
			st.takeItems(WEAPON_TRADE_CONTRACT, 1);
			st.playSound(QuestState.SOUND_MIDDLE);
		}
		else if (event.equalsIgnoreCase("30437-03.htm") && cond == 8)
		{
			st.giveItems(CARGOBOX, 1);
			st.giveItems(CERTIFICATE, 1);
			st.set("cond", "9");
		}
		else if (event.equalsIgnoreCase("30617-04.htm") && cond == 9)
		{
			st.takeItems(CERTIFICATE, 1);
			st.takeItems(ATTACK_DIRECTIVES, 1);
			st.takeItems(CARGOBOX, 1);
			st.set("cond", "10");
		}

		return htmltext;
	}

	@Override
	public String onTalk(L2Npc npc, L2PcInstance player)
	{
		QuestState st = player.getQuestState(qn);
		String htmltext = getNoQuestMsg();
		if (st == null)
			return htmltext;

		switch (st.getState())
		{
			case QuestState.CREATED:
				if (player.getLevel() >= 27 && player.getLevel() <= 32)
					htmltext = "30381-01.htm";
				else
				{
					htmltext = "30381-01a.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				int cond = st.getInt("cond");
				switch (npc.getNpcId())
				{
					case ALVAH:
						if (cond >= 1 && cond <= 3)
							htmltext = "30381-02a.htm";
						else if (cond == 4)
							htmltext = "30381-03.htm";
						else if (cond == 5)
						{
							if (st.getQuestItemsCount(RANGERS_REPORT1) == 1 && st.getQuestItemsCount(RANGERS_REPORT2) == 1 && st.getQuestItemsCount(RANGERS_REPORT3) == 1 && st.getQuestItemsCount(RANGERS_REPORT4) == 1)
							{
								htmltext = "30381-05.htm";
								st.takeItems(RANGERS_REPORT1, 1);
								st.takeItems(RANGERS_REPORT2, 1);
								st.takeItems(RANGERS_REPORT3, 1);
								st.takeItems(RANGERS_REPORT4, 1);
								st.set("cond", "6");
							}
							else
								htmltext = "30381-04a.htm";
						}
						else if (cond == 6)
						{
							if (st.getQuestItemsCount(WEAPON_TRADE_CONTRACT) == 1 && st.getQuestItemsCount(ATTACK_DIRECTIVES) == 1)
								htmltext = "30381-06.htm";
							else
								htmltext = "30381-05a.htm";
						}
						else if (cond >= 7 && cond <= 10)
							htmltext = "30381-07a.htm";
						else if (cond == 11)
						{
							htmltext = "30381-08.htm";
							st.rewardItems(ADENA, 90000);
							st.playSound(QuestState.SOUND_FINISH);
							st.exitQuest(false);
						}
						break;

					case ARODIN:
						if (cond == 1)
							htmltext = "30207-01.htm";
						else if (cond == 2)
							htmltext = "30207-01a.htm";
						else if (cond == 3)
						{
							if (st.getQuestItemsCount(TYRAS_BILL) == 1)
							{
								st.takeItems(TYRAS_BILL, 1);
								htmltext = "30207-03.htm";
								st.set("cond", "4");
							}
							else
								htmltext = "30207-01a.htm";
						}
						else if (cond >= 4)
							htmltext = "30207-03a.htm";
						break;

					case TYRA:
						if (cond == 2)
						{
							if (st.getQuestItemsCount(BLADE_MOLD) >= 20)
							{
								st.takeItems(BLADE_MOLD, -1);
								st.giveItems(TYRAS_BILL, 1);
								htmltext = "30420-01.htm";
								st.set("cond", "3");
							}
							else
								htmltext = "30420-01b.htm";
						}
						else if (cond == 3)
							htmltext = "30420-01a.htm";
						else if (cond > 3)
							htmltext = "30420-02.htm";
						break;

					case NETI:
						if (cond == 7)
						{
							htmltext = "30425-01.htm";
							st.set("cond", "8");
						}
						else if (cond >= 8)
							htmltext = "30425-02.htm";
						break;

					case ROLENTO:
						if (cond == 8)
							htmltext = "30437-01.htm";
						else if (cond >= 9)
							htmltext = "30437-03a.htm";
						break;

					case BURAI:
						if (cond == 9 && st.getQuestItemsCount(CERTIFICATE) == 1 && st.getQuestItemsCount(CARGOBOX) == 1 && st.getQuestItemsCount(ATTACK_DIRECTIVES) == 1)
							htmltext = "30617-01.htm";
						else if (cond == 10)
						{
							if (st.getQuestItemsCount(OL_MAHUM_HEAD) >= 30)
							{
								htmltext = "30617-05.htm";
								st.giveItems(ADENA, 8000);
								st.takeItems(OL_MAHUM_HEAD, -1);
								st.set("cond", "11");
								st.playSound(QuestState.SOUND_ITEMGET);
							}
							else
								htmltext = "30617-04a.htm";
						}
						break;
				}
				break;

			case QuestState.COMPLETED:
				htmltext = Quest.getAlreadyCompletedMsg();
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

		int cond = st.getInt("cond");

		switch (npc.getNpcId())
		{
			case 20496:
			case 20497:
			case 20498:
			case 20499:
				if (cond == 2)
				{
					if (!st.dropQuestItems(BLADE_MOLD, 1, 20, 500000))
						if (Rnd.get(10) == 0)
							st.addSpawn(27190);
				}
				break;

			case 20062:
				if (cond == 5)
				{
					int chance = Rnd.get(100);
					if (!st.hasQuestItems(RANGERS_REPORT1) && chance < 100)
					{
						st.giveItems(RANGERS_REPORT1, 1);
						st.playSound(QuestState.SOUND_ITEMGET);
					}
					else if (!st.hasQuestItems(RANGERS_REPORT2) && chance < 20)
					{
						st.giveItems(RANGERS_REPORT2, 1);
						st.playSound(QuestState.SOUND_ITEMGET);
					}
					else if (!st.hasQuestItems(RANGERS_REPORT3) && chance < 20)
					{
						st.giveItems(RANGERS_REPORT3, 1);
						st.playSound(QuestState.SOUND_ITEMGET);
					}
					else if (!st.hasQuestItems(RANGERS_REPORT4) && chance < 20)
					{
						st.giveItems(RANGERS_REPORT4, 1);
						st.playSound(QuestState.SOUND_ITEMGET);
					}
				}
				break;

			case 20066:
				if (cond == 6)
				{
					int chance = Rnd.get(100);
					if (!st.hasQuestItems(WEAPON_TRADE_CONTRACT) && chance < 10)
					{
						st.giveItems(WEAPON_TRADE_CONTRACT, 1);
						st.playSound(QuestState.SOUND_ITEMGET);
					}
					else if (!st.hasQuestItems(ATTACK_DIRECTIVES) && chance < 10)
					{
						st.giveItems(ATTACK_DIRECTIVES, 1);
						st.playSound(QuestState.SOUND_ITEMGET);
					}
				}
				break;

			case 20438:
				if (cond == 10)
					st.dropQuestItems(OL_MAHUM_HEAD, 1, 30, 500000);
				break;
		}

		return null;
	}
}