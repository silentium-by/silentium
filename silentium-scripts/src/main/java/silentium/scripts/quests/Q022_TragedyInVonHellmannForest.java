/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.quests;

import silentium.commons.utils.Rnd;
import silentium.gameserver.ai.CtrlIntention;
import silentium.gameserver.model.actor.L2Attackable;
import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.quest.Quest;
import silentium.gameserver.model.quest.QuestState;

public class Q022_TragedyInVonHellmannForest extends Quest
{
	private final static String qn = "Q022_TragedyInVonHellmannForest";

	// NPCs
	private final static int Well = 31527;
	private final static int Tifaren = 31334;
	private final static int Innocentin = 31328;
	private final static int GhostOfPriest = 31528;
	private final static int GhostOfAdventurer = 31529;

	// Items
	private final static int CrossOfEinhasad = 7141;
	private final static int LostSkullOfElf = 7142;
	private final static int LetterOfInnocentin = 7143;
	private final static int JewelOfAdventurerGreen = 7144;
	private final static int JewelOfAdventurerRed = 7145;
	private final static int SealedReportBox = 7146;
	private final static int ReportBox = 7147;

	// Monsters
	private final static int SoulOfWell = 27217;

	private static L2Npc GhostOfPriestInstance = null;
	private static L2Npc SoulOfWellInstance = null;

	public Q022_TragedyInVonHellmannForest(int questId, String name, String descr)
	{
		super(questId, name, descr);

		questItemIds = new int[] { LostSkullOfElf, ReportBox, SealedReportBox, LetterOfInnocentin, JewelOfAdventurerRed, JewelOfAdventurerGreen };

		addStartNpc(Tifaren, Innocentin);
		addTalkId(Innocentin, Tifaren, GhostOfPriest, GhostOfAdventurer, Well);

		addAttackId(SoulOfWell);
		addKillId(SoulOfWell, 21553, 21554, 21555, 21556, 21561);
	}

	public static void main(String[] args)
	{
		new Q022_TragedyInVonHellmannForest(22, "Q022_TragedyInVonHellmannForest", "Tragedy in von Hellmann Forest");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event.equalsIgnoreCase("31334-03.htm"))
		{
			QuestState st2 = player.getQuestState("Q021_HiddenTruth");
			if (st2 != null && st2.isCompleted() && player.getLevel() >= 63)
				htmltext = "31334-02.htm";
		}
		else if (event.equalsIgnoreCase("31334-04.htm"))
		{
			st.setState(QuestState.STARTED);
			st.set("cond", "1");
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("31334-07.htm"))
		{
			if (st.getQuestItemsCount(CrossOfEinhasad) == 0)
				st.set("cond", "2");
			else
				htmltext = "31334-06.htm";
		}
		else if (event.equalsIgnoreCase("31334-08.htm"))
		{
			if (st.getQuestItemsCount(CrossOfEinhasad) == 1)
			{
				st.set("cond", "4");
				st.playSound(QuestState.SOUND_MIDDLE);
				st.takeItems(CrossOfEinhasad, 1);
			}
			else
			{
				st.set("cond", "2");
				htmltext = "31334-07.htm";
			}
		}
		else if (event.equalsIgnoreCase("31334-13.htm"))
		{
			if (GhostOfPriestInstance != null)
			{
				st.set("cond", "6");
				htmltext = "31334-14.htm";
			}
			else
			{
				st.set("cond", "7");
				st.playSound(QuestState.SOUND_MIDDLE);
				st.takeItems(LostSkullOfElf, -1);

				GhostOfPriestInstance = st.addSpawn(GhostOfPriest, 38418, -49894, -1104, 120000);
				GhostOfPriestInstance.broadcastNpcSay("Did you call me, " + st.getPlayer().getName() + "?");
				st.startQuestTimer("ghost_cleanup", 118000);
			}
		}
		else if (event.equalsIgnoreCase("31528-08.htm"))
		{
			st.set("cond", "8");
			st.playSound(QuestState.SOUND_MIDDLE);

			// Cancel cleanup timer, as the despawn will make it.
			if (st.getQuestTimer("ghost_cleanup") != null)
				st.getQuestTimer("ghost_cleanup").cancel();

			if (GhostOfPriestInstance != null)
			{
				GhostOfPriestInstance.deleteMe();
				GhostOfPriestInstance = null;
			}
		}
		else if (event.equalsIgnoreCase("31328-10.htm"))
		{
			st.set("cond", "9");
			st.giveItems(LetterOfInnocentin, 1);
			st.playSound(QuestState.SOUND_MIDDLE);
		}
		else if (event.equalsIgnoreCase("31529-12.htm"))
		{
			st.set("cond", "10");
			st.takeItems(LetterOfInnocentin, -1);
			st.giveItems(JewelOfAdventurerGreen, 1);
			st.playSound(QuestState.SOUND_MIDDLE);
		}
		else if (event.equalsIgnoreCase("31527-02.htm"))
		{
			if (SoulOfWellInstance == null)
			{
				SoulOfWellInstance = st.addSpawn(SoulOfWell, 34860, -54542, -2048, 0);

				// Attack player.
				((L2Attackable) SoulOfWellInstance).addDamageHate(player, 0, 99999);
				SoulOfWellInstance.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, player, true);
			}
		}
		else if (event.equalsIgnoreCase("attack_timer"))
		{
			st.set("cond", "11");
			st.giveItems(JewelOfAdventurerRed, 1);
			st.takeItems(JewelOfAdventurerGreen, -1);
			st.playSound(QuestState.SOUND_ITEMGET);
		}
		else if (event.equalsIgnoreCase("31328-13.htm"))
		{
			st.set("cond", "15");
			st.takeItems(ReportBox, 1);
			st.playSound(QuestState.SOUND_MIDDLE);
		}
		else if (event.equalsIgnoreCase("31328-21.htm"))
		{
			st.set("cond", "16");
			st.playSound(QuestState.SOUND_MIDDLE);
		}
		else if (event.equalsIgnoreCase("ghost_cleanup"))
		{
			GhostOfPriestInstance.broadcastNpcSay("I'm confused! Maybe it's time to go back.");
			GhostOfPriestInstance = null;
			return null;
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
				switch (npc.getNpcId())
				{
					case Innocentin:
						QuestState st2 = player.getQuestState("Q021_HiddenTruth");
						if (st2 != null && st2.isCompleted())
						{
							if (st.getQuestItemsCount(CrossOfEinhasad) == 0)
							{
								htmltext = "31328-01.htm";
								st.giveItems(CrossOfEinhasad, 1);
								st.playSound(QuestState.SOUND_ITEMGET);
							}
							else
								htmltext = "31328-01b.htm";
						}
						break;

					case Tifaren:
						htmltext = "31334-01.htm";
						break;
				}
				break;

			case QuestState.STARTED:
				int cond = st.getInt("cond");
				switch (npc.getNpcId())
				{
					case Tifaren:
						if (cond == 1 || cond == 2 || cond == 3)
							htmltext = "31334-05.htm";
						else if (cond == 4)
							htmltext = "31334-09.htm";
						else if (cond == 5 || cond == 6)
						{
							if (st.getQuestItemsCount(LostSkullOfElf) != 0)
							{
								if (GhostOfPriestInstance != null)
									htmltext = "31334-11.htm";
								else
									htmltext = "31334-10.htm";
							}
							else
							{
								st.set("cond", "4");
								htmltext = "31334-09.htm";
							}
						}
						else if (cond == 7)
						{
							if (GhostOfPriestInstance != null)
								htmltext = "31334-15.htm";
							else
								htmltext = "31334-17.htm";
						}
						else if (cond >= 8)
							htmltext = "31334-18.htm";
						break;

					case Innocentin:
						if (cond < 3)
						{
							if (st.getQuestItemsCount(CrossOfEinhasad) == 0)
							{
								st.set("cond", "3");
								htmltext = "31328-01.htm";
								st.giveItems(CrossOfEinhasad, 1);
								st.playSound(QuestState.SOUND_ITEMGET);
							}
							else
								htmltext = "31328-01b.htm";
						}
						else if (cond == 3)
							htmltext = "31328-02.htm";
						else if (cond == 8)
							htmltext = "31328-03.htm";
						else if (cond == 9)
							htmltext = "31328-11.htm";
						else if (cond == 14)
						{
							if (st.getQuestItemsCount(ReportBox) != 0)
								htmltext = "31328-12.htm";
							else
								st.set("cond", "13");
						}
						else if (cond == 15)
							htmltext = "31328-14.htm";
						else if (cond == 16)
						{
							if (player.getLevel() < 64)
								htmltext = "31328-23.htm";
							else
								htmltext = "31328-22.htm";

							st.exitQuest(false);
							st.playSound(QuestState.SOUND_FINISH);
						}
						break;

					case GhostOfPriest:
						if (cond == 7)
							htmltext = "31528-01.htm";
						else if (cond == 8)
							htmltext = "31528-08.htm";
						break;

					case GhostOfAdventurer:
						if (cond == 9)
						{
							if (st.getQuestItemsCount(LetterOfInnocentin) != 0)
								htmltext = "31529-01.htm";
							else
							{
								st.set("cond", "8");
								htmltext = "31529-10.htm";
							}
						}
						else if (cond == 10)
							htmltext = "31529-16.htm";
						else if (cond == 11)
						{
							if (st.getQuestItemsCount(JewelOfAdventurerRed) != 0)
							{
								htmltext = "31529-17.htm";

								st.set("cond", "12");
								st.takeItems(JewelOfAdventurerRed, -1);
								st.playSound(QuestState.SOUND_MIDDLE);
							}
							else
							{
								st.set("cond", "10");
								htmltext = "31529-09.htm";
							}
						}
						else if (cond == 12)
							htmltext = "31529-17.htm";
						else if (cond == 13)
						{
							if (st.getQuestItemsCount(SealedReportBox) != 0)
							{
								htmltext = "31529-18.htm";

								st.set("cond", "14");
								st.takeItems(SealedReportBox, -1);
								st.giveItems(ReportBox, 1);
								st.playSound(QuestState.SOUND_MIDDLE);
							}
							else
							{
								st.set("cond", "12");
								htmltext = "31529-10.htm";
							}
						}
						else if (cond >= 14)
							htmltext = "31529-19.htm";
						break;

					case Well:
						if (cond == 10)
							htmltext = "31527-01.htm";
						else if (cond == 11)
							htmltext = "31527-03.htm";
						else if (cond == 12)
						{
							st.set("cond", "13");
							htmltext = "31527-04.htm";
							st.giveItems(SealedReportBox, 1);
							st.playSound(QuestState.SOUND_MIDDLE);
						}
						else if (cond >= 13)
							htmltext = "31527-05.htm";
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
	public String onAttack(L2Npc npc, L2PcInstance attacker, int damage, boolean isPet)
	{
		QuestState st = attacker.getQuestState(qn);
		if (st == null || !st.isStarted() || isPet)
			return null;

		if (st.getQuestTimer("attack_timer") != null)
			return null;

		if (st.getInt("cond") == 10)
			st.startQuestTimer("attack_timer", 20000);

		return null;
	}

	@Override
	public String onKill(L2Npc npc, L2PcInstance player, boolean isPet)
	{
		QuestState st = player.getQuestState(qn);
		if (st == null || !st.isStarted())
			return null;

		if (npc.getNpcId() != SoulOfWell)
		{
			if (st.getInt("cond") == 4 && Rnd.get(10) < 1)
			{
				st.set("cond", "5");
				st.giveItems(LostSkullOfElf, 1);
				st.playSound(QuestState.SOUND_MIDDLE);
			}
		}
		else
		{
			// Cancel current timer, if any.
			if (st.getQuestTimer("attack_timer") != null)
				st.getQuestTimer("attack_timer").cancel();

			SoulOfWellInstance = null;
		}

		return null;
	}
}