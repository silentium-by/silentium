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
import silentium.gameserver.network.serverpackets.SocialAction;
import silentium.gameserver.scripting.ScriptFile;

public class Q406_PathToAnElvenKnight extends Quest implements ScriptFile
{
	private static final String qn = "Q406_PathToAnElvenKnight";

	// Items
	private static final int SoriusLetter = 1202;
	private static final int KlutoBox = 1203;
	private static final int ElvenKnightBrooch = 1204;
	private static final int TopazPiece = 1205;
	private static final int EmeraldPiece = 1206;
	private static final int KlutosMemo = 1276;

	// NPCs
	private static final int Sorius = 30327;
	private static final int Kluto = 30317;

	public Q406_PathToAnElvenKnight(int questId, String name, String descr)
	{
		super(questId, name, descr);

		questItemIds = new int[] { SoriusLetter, KlutoBox, TopazPiece, EmeraldPiece, KlutosMemo };

		addStartNpc(Sorius);
		addTalkId(Sorius);
		addTalkId(Kluto);

		addKillId(20035, 20042, 20045, 20051, 20054, 20060, 20782);
	}

	public static void onLoad()
	{
		new Q406_PathToAnElvenKnight(406, "Q406_PathToAnElvenKnight", "quests");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event.equalsIgnoreCase("30327-05.htm"))
		{
			if (player.getClassId().getId() == 0x12)
			{
				if (player.getLevel() >= 19)
				{
					if (st.getQuestItemsCount(ElvenKnightBrooch) == 1)
					{
						htmltext = "30327-04.htm";
						st.exitQuest(true);
					}
				}
				else
				{
					htmltext = "30327-03.htm";
					st.exitQuest(true);
				}
			}
			else if (player.getClassId().getId() == 0x13)
			{
				htmltext = "30327-02a.htm";
				st.exitQuest(true);
			}
			else
			{
				htmltext = "30327-02.htm";
				st.exitQuest(true);
			}
		}
		else if (event.equalsIgnoreCase("30327-06.htm"))
		{
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("30317-02.htm"))
		{
			st.set("cond", "4");
			st.playSound(QuestState.SOUND_MIDDLE);
			st.takeItems(SoriusLetter, 1);
			st.giveItems(KlutosMemo, 1);
		}

		return htmltext;
	}

	@Override
	public String onTalk(L2Npc npc, L2PcInstance player)
	{
		String htmltext = getNoQuestMsg();
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		switch (st.getState())
		{
			case QuestState.CREATED:
				htmltext = "30327-01.htm";
				break;

			case QuestState.STARTED:
				int cond = st.getInt("cond");
				switch (npc.getNpcId())
				{
					case Sorius:
						if (cond == 1)
						{
							if (st.getQuestItemsCount(TopazPiece) > 1 && st.getQuestItemsCount(TopazPiece) < 20)
								htmltext = "30327-08.htm";
							else
								htmltext = "30327-07.htm";
						}
						else if (cond == 2)
						{
							st.set("cond", "3");
							st.giveItems(SoriusLetter, 1);
							st.playSound(QuestState.SOUND_MIDDLE);
							htmltext = "30327-09.htm";
						}
						else if (cond >= 3 && cond <= 5)
							htmltext = "30327-11.htm";
						else if (cond == 6)
						{
							htmltext = "30327-10.htm";
							st.takeItems(KlutoBox, 1);
							st.takeItems(KlutosMemo, 1);
							st.giveItems(ElvenKnightBrooch, 1);
							st.addExpAndSp(3200, 2280);
							player.broadcastPacket(new SocialAction(player, 3));

							st.playSound(QuestState.SOUND_FINISH);
							st.exitQuest(true);
							st.saveGlobalQuestVar("1ClassQuestFinished", "1");
						}
						break;

					case Kluto:
						if (cond == 3)
							htmltext = "30317-01.htm";
						else if (cond == 4)
						{
							if (st.getQuestItemsCount(EmeraldPiece) > 1 && st.getQuestItemsCount(EmeraldPiece) < 20)
								htmltext = "30317-04.htm";
							else
								htmltext = "30317-03.htm";
						}
						else if (cond == 5)
						{
							st.set("cond", "6");
							st.takeItems(TopazPiece, -1);
							st.takeItems(EmeraldPiece, -1);
							st.giveItems(KlutoBox, 1);
							st.playSound(QuestState.SOUND_MIDDLE);
							htmltext = "30317-05.htm";
						}
						else if (cond == 6)
							htmltext = "30317-06.htm";
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
		if (st == null || !st.isStarted())
			return null;

		switch (npc.getNpcId())
		{
			case 20035:
			case 20042:
			case 20045:
			case 20051:
			case 20054:
			case 20060:
				if (st.getInt("cond") == 1)
					if (st.dropQuestItems(TopazPiece, 1, 20, 700000))
						st.set("cond", "2");
				break;

			case 20782:
				if (st.getInt("cond") == 4)
					if (st.dropQuestItems(EmeraldPiece, 1, 20, 500000))
						st.set("cond", "5");
				break;
		}

		return null;
	}
}