/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.quests;

import silentium.gameserver.ai.CtrlIntention;
import silentium.gameserver.model.L2CharPosition;
import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.quest.Quest;
import silentium.gameserver.model.quest.QuestState;

public class Q021_HiddenTruth extends Quest
{
	private final static String qn = "Q021_HiddenTruth";

	// NPCs
	private final static int MYSTERIOUS_WIZARD = 31522;
	private final static int TOMBSTONE = 31523;
	private final static int VON_HELLMAN = 31524;
	private final static int VON_HELLMAN_PAGE = 31525;
	private final static int BROKEN_BOOKSHELF = 31526;
	private final static int AGRIPEL = 31348;
	private final static int DOMINIC = 31350;
	private final static int BENEDICT = 31349;
	private final static int INNOCENTIN = 31328;

	// Items
	private final static int CROSS_OF_EINHASAD = 7140;
	private final static int CROSS_OF_EINHASAD_NEXT_QUEST = 7141;

	private L2Npc VonHellmannPage;
	private L2Npc VonHellmann;

	private void spawnVonHellmann(QuestState st)
	{
		if (VonHellmann == null)
		{
			VonHellmann = st.addSpawn(VON_HELLMAN, 51432, -54570, -3136, 0);
			VonHellmann.broadcastNpcSay("Who awoke me?");
		}
	}

	public Q021_HiddenTruth(int questId, String name, String descr)
	{
		super(questId, name, descr);

		questItemIds = new int[] { CROSS_OF_EINHASAD };

		addStartNpc(MYSTERIOUS_WIZARD);
		addTalkId(MYSTERIOUS_WIZARD, TOMBSTONE, VON_HELLMAN, VON_HELLMAN_PAGE, BROKEN_BOOKSHELF, AGRIPEL, DOMINIC, BENEDICT, INNOCENTIN);
	}

	public static void main(String[] args)
	{
		new Q021_HiddenTruth(21, "Q021_HiddenTruth", "quests");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event.equalsIgnoreCase("31522-02.htm"))
		{
			st.setState(QuestState.STARTED);
			st.set("cond", "1");
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("31523-03.htm"))
		{
			st.set("cond", "2");
			st.playSound(QuestState.SOUND_MIDDLE);
			spawnVonHellmann(st);
		}
		else if (event.equalsIgnoreCase("31524-06.htm"))
		{
			st.set("cond", "3");
			st.playSound(QuestState.SOUND_MIDDLE);

			// Spawn the page.
			if (VonHellmannPage == null)
			{
				VonHellmannPage = st.addSpawn(VON_HELLMAN_PAGE, 51462, -54539, -3176, 90000);
				VonHellmannPage.broadcastNpcSay("My master has instructed me to be your guide, " + st.getPlayer().getName() + ".");

				// Make it move.
				startQuestTimer("1", 4000, VonHellmannPage, player);
				startQuestTimer("pageDespawn", 88000, VonHellmannPage, player);
			}
		}
		else if (event.equalsIgnoreCase("31526-08.htm"))
		{
			st.set("cond", "5");
			st.playSound(QuestState.SOUND_MIDDLE);
		}
		else if (event.equalsIgnoreCase("31526-14.htm"))
		{
			st.set("cond", "6");
			st.giveItems(CROSS_OF_EINHASAD, 1);
			st.playSound(QuestState.SOUND_MIDDLE);
		}
		else if (event.equalsIgnoreCase("1"))
		{
			VonHellmannPage.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new L2CharPosition(52373, -54296, -3136, 0));
			VonHellmannPage.broadcastNpcSay("Follow me...");
			st.startQuestTimer("2", 5000, VonHellmannPage);
			return null;
		}
		else if (event.equalsIgnoreCase("2"))
		{
			VonHellmannPage.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new L2CharPosition(52279, -53064, -3161, 0));
			st.startQuestTimer("3", 12000, VonHellmannPage);
			return null;
		}
		else if (event.equalsIgnoreCase("3"))
		{
			VonHellmannPage.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new L2CharPosition(51909, -51725, -3125, 0));
			st.startQuestTimer("4", 15000, VonHellmannPage);
			return null;
		}
		else if (event.equalsIgnoreCase("4"))
		{
			VonHellmannPage.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new L2CharPosition(52438, -51240, -3097, 0));
			VonHellmannPage.broadcastNpcSay("This where that here...");
			st.startQuestTimer("5", 5000, VonHellmannPage);
			return null;
		}
		else if (event.equalsIgnoreCase("5"))
		{
			VonHellmannPage.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new L2CharPosition(52143, -51418, -3085, 0));
			VonHellmannPage.broadcastNpcSay("I want to speak to you...");
			return null;
		}
		else if (event.equalsIgnoreCase("31328-05.htm"))
		{
			if (st.getQuestItemsCount(CROSS_OF_EINHASAD) != 0)
			{
				st.takeItems(CROSS_OF_EINHASAD, 1);
				st.giveItems(CROSS_OF_EINHASAD_NEXT_QUEST, 1);
				st.playSound(QuestState.SOUND_FINISH);
				st.exitQuest(false);
			}
		}
		else if (event.equalsIgnoreCase("pageDespawn"))
			VonHellmannPage = null;

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
				if (st.getPlayer().getLevel() >= 63)
					htmltext = "31522-01.htm";
				else
				{
					htmltext = "31522-03.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				int cond = st.getInt("cond");
				switch (npc.getNpcId())
				{
					case MYSTERIOUS_WIZARD:
						htmltext = "31522-05.htm";
						break;

					case TOMBSTONE:
						if (cond == 1)
							htmltext = "31523-01.htm";
						else if (cond == 2 || cond == 3)
						{
							htmltext = "31523-04.htm";
							spawnVonHellmann(st);
						}
						else if (cond >= 4)
							htmltext = "31523-04.htm";
						break;

					case VON_HELLMAN:
						if (cond == 2)
							htmltext = "31524-01.htm";
						else if (cond == 3)
							htmltext = "31524-07.htm";
						else if (cond >= 4)
							htmltext = "31524-07a.htm";
						break;

					case VON_HELLMAN_PAGE:
						if (cond == 3 || cond == 4)
						{
							htmltext = "31525-01.htm";
							if (!VonHellmannPage.isMoving())
							{
								htmltext = "31525-02.htm";
								if (cond == 3)
								{
									st.set("cond", "4");
									st.playSound(QuestState.SOUND_MIDDLE);
								}
							}
						}
						break;

					case BROKEN_BOOKSHELF:
						if (cond == 3 || cond == 4)
						{
							htmltext = "31526-01.htm";

							if (!VonHellmannPage.isMoving())
							{
								st.set("cond", "5");
								st.playSound(QuestState.SOUND_MIDDLE);

								if (VonHellmannPage != null)
								{
									VonHellmannPage.deleteMe();
									VonHellmannPage = null;

									// Cancel current timer, if any.
									if (st.getQuestTimer("pageDespawn") != null)
										st.getQuestTimer("pageDespawn").cancel();
								}

								if (VonHellmann != null)
								{
									VonHellmann.deleteMe();
									VonHellmann = null;
								}
							}
						}
						else if (cond == 5)
							htmltext = "31526-10.htm";
						else if (cond >= 6)
							htmltext = "31526-15.htm";
						break;

					case AGRIPEL:
					case BENEDICT:
					case DOMINIC:
						if ((cond == 6 || cond == 7) && st.getQuestItemsCount(CROSS_OF_EINHASAD) >= 1)
						{
							int npcId = npc.getNpcId();

							// For cond 6, make checks until cond 7 is activated.
							if (cond == 6)
							{
								int npcId1 = 0, npcId2 = 0;
								if (npcId == AGRIPEL)
								{
									npcId1 = BENEDICT;
									npcId2 = DOMINIC;
								}
								else if (npcId == BENEDICT)
								{
									npcId1 = AGRIPEL;
									npcId2 = DOMINIC;
								}
								else if (npcId == DOMINIC)
								{
									npcId1 = AGRIPEL;
									npcId2 = BENEDICT;
								}

								if (st.getInt(String.valueOf(npcId1)) == 1 && st.getInt(String.valueOf(npcId2)) == 1)
								{
									st.set("cond", "7");
									st.playSound(QuestState.SOUND_MIDDLE);
								}
								else
									st.set(String.valueOf(npcId), "1");
							}

							htmltext = npcId + "-01.htm";
						}
						break;

					case INNOCENTIN:
						if (cond == 7 && st.getQuestItemsCount(CROSS_OF_EINHASAD) != 0)
							htmltext = "31328-01.htm";
						break;
				}
				break;

			case QuestState.COMPLETED:
				if (npc.getNpcId() == INNOCENTIN)
					htmltext = "31328-06.htm";
				else
					htmltext = Quest.getAlreadyCompletedMsg();
				break;
		}

		return htmltext;
	}
}