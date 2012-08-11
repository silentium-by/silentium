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

public class Q345_MethodToRaiseTheDead extends Quest
{
	private final static String qn = "Q345_MethodToRaiseTheDead";

	// Items
	private static final int VICTIMS_ARM_BONE = 4274;
	private static final int VICTIMS_THIGH_BONE = 4275;
	private static final int VICTIMS_SKULL = 4276;
	private static final int VICTIMS_RIB_BONE = 4277;
	private static final int VICTIMS_SPINE = 4278;
	private static final int USELESS_BONE_PIECES = 4280;
	private static final int POWDER_TO_SUMMON_DEAD_SOULS = 4281;

	private static final int[] CORPSE_PARTS = { VICTIMS_ARM_BONE, VICTIMS_THIGH_BONE, VICTIMS_SKULL, VICTIMS_RIB_BONE, VICTIMS_SPINE };

	// NPCs
	private static final int Xenovia = 30912;
	private static final int Dorothy = 30970;
	private static final int Orpheus = 30971;
	private static final int Medium_Jar = 30973;

	// Rewards
	private static final int BILL_OF_IASON_HEINE = 4310;
	private static final int IMPERIAL_DIAMOND = 3456;

	public Q345_MethodToRaiseTheDead(int questId, String name, String descr)
	{
		super(questId, name, descr);

		questItemIds = new int[] { VICTIMS_ARM_BONE, VICTIMS_THIGH_BONE, VICTIMS_SKULL, VICTIMS_RIB_BONE, VICTIMS_SPINE, POWDER_TO_SUMMON_DEAD_SOULS, USELESS_BONE_PIECES };

		addStartNpc(Dorothy);
		addTalkId(Dorothy, Xenovia, Medium_Jar, Orpheus);

		addKillId(20789, 20791);
	}

	public static void main(String[] args)
	{
		new Q345_MethodToRaiseTheDead(345, "Q345_MethodToRaiseTheDead", "Method to Raise the Dead");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event.equalsIgnoreCase("30970-03.htm"))
		{
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("30970-06.htm"))
		{
			st.set("cond", "2");
			st.playSound(QuestState.SOUND_MIDDLE);
		}
		else if (event.equalsIgnoreCase("30912-04.htm"))
		{
			if (player.getAdena() >= 1000)
			{
				if (st.getQuestItemsCount(VICTIMS_ARM_BONE) + st.getQuestItemsCount(VICTIMS_THIGH_BONE) + st.getQuestItemsCount(VICTIMS_SKULL) + st.getQuestItemsCount(VICTIMS_RIB_BONE) + st.getQuestItemsCount(VICTIMS_SPINE) == 5)
				{
					st.set("cond", "3");
					st.takeItems(57, 1000);
					htmltext = "30912-03.htm";
					st.giveItems(POWDER_TO_SUMMON_DEAD_SOULS, 1);
					st.playSound(QuestState.SOUND_ITEMGET);
				}
				else
					st.set("cond", "1");
			}
		}
		else if (event.equalsIgnoreCase("30973-04.htm"))
		{
			if (st.getInt("cond") == 3)
			{
				if (st.getQuestItemsCount(POWDER_TO_SUMMON_DEAD_SOULS) + st.getQuestItemsCount(VICTIMS_ARM_BONE) + st.getQuestItemsCount(VICTIMS_THIGH_BONE) + st.getQuestItemsCount(VICTIMS_SKULL) + st.getQuestItemsCount(VICTIMS_RIB_BONE) + st.getQuestItemsCount(VICTIMS_SPINE) == 6)
				{
					int chance = Rnd.get(3);
					if (chance == 0)
					{
						st.set("cond", "6");
						htmltext = "30973-02a.htm";
					}
					else if (chance == 1)
					{
						st.set("cond", "6");
						htmltext = "30973-02b.htm";
					}
					else
					{
						st.set("cond", "7");
						htmltext = "30973-02c.htm";
					}

					st.takeItems(POWDER_TO_SUMMON_DEAD_SOULS, -1);
					st.takeItems(VICTIMS_ARM_BONE, -1);
					st.takeItems(VICTIMS_THIGH_BONE, -1);
					st.takeItems(VICTIMS_SKULL, -1);
					st.takeItems(VICTIMS_RIB_BONE, -1);
					st.takeItems(VICTIMS_SPINE, -1);

					st.playSound(QuestState.SOUND_MIDDLE);
				}
				else
				{
					st.set("cond", "1");
					st.takeItems(POWDER_TO_SUMMON_DEAD_SOULS, -1);
				}
			}
		}
		else if (event.equalsIgnoreCase("30971-02a.htm"))
		{
			if (st.getQuestItemsCount(USELESS_BONE_PIECES) > 0)
				htmltext = "30971-02.htm";
		}
		else if (event.equalsIgnoreCase("30971-03.htm"))
		{
			if (st.getQuestItemsCount(USELESS_BONE_PIECES) > 0)
			{
				int amount = st.getQuestItemsCount(USELESS_BONE_PIECES) * 104;
				st.takeItems(USELESS_BONE_PIECES, -1);
				st.rewardItems(57, amount);
			}
			else
				htmltext = "30971-02a.htm";
		}

		return htmltext;
	}

	@Override
	public String onTalk(L2Npc npc, L2PcInstance player)
	{
		String htmltext = Quest.getNoQuestMsg();
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		switch (st.getState())
		{
			case QuestState.CREATED:
				if (player.getLevel() >= 35 && player.getLevel() <= 42)
					htmltext = "30970-01.htm";
				else
				{
					htmltext = "30970-00.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				int cond = st.getInt("cond");
				switch (npc.getNpcId())
				{
					case Dorothy:
						if (cond == 1)
						{
							if (st.getQuestItemsCount(VICTIMS_ARM_BONE) + st.getQuestItemsCount(VICTIMS_THIGH_BONE) + st.getQuestItemsCount(VICTIMS_SKULL) + st.getQuestItemsCount(VICTIMS_RIB_BONE) + st.getQuestItemsCount(VICTIMS_SPINE) < 5)
								htmltext = "30970-04.htm";
							else
								htmltext = "30970-05.htm";
						}
						else if (cond == 2)
							htmltext = "30970-07.htm";
						else if (cond >= 3 && cond <= 5)
							htmltext = "30970-08.htm";
						else if (cond >= 6)
						{
							// Shared part between cond 6 and 7.
							int amount = st.getQuestItemsCount(USELESS_BONE_PIECES) * 70;
							st.takeItems(USELESS_BONE_PIECES, -1);

							// Scaried little girl
							if (cond == 7)
							{
								htmltext = "30970-10.htm";
								st.rewardItems(57, 3040 + amount);

								// Reward can be either an Imperial Diamond or bills.
								if (Rnd.get(10) < 1)
									st.giveItems(IMPERIAL_DIAMOND, 1);
								else
									st.giveItems(BILL_OF_IASON_HEINE, 5);
							}
							// Friends of Dorothy
							else
							{
								htmltext = "30970-09.htm";
								st.rewardItems(57, 5390 + amount);
								st.giveItems(BILL_OF_IASON_HEINE, 3);
							}
							st.playSound(QuestState.SOUND_FINISH);
							st.exitQuest(true);
						}
						break;

					case Xenovia:
						if (cond == 2)
							htmltext = "30912-01.htm";
						else if (cond >= 3)
							htmltext = "30912-06.htm";
						break;

					case Medium_Jar:
						htmltext = "30973-01.htm";
						break;

					case Orpheus:
						htmltext = "30971-01.htm";
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

		if (st.getInt("cond") == 1)
		{
			if (Rnd.get(100) < 66)
			{
				st.giveItems(USELESS_BONE_PIECES, 1);
				st.playSound(QuestState.SOUND_ITEMGET);
			}
			else
			{
				int randomPart = CORPSE_PARTS[Rnd.get(CORPSE_PARTS.length)];
				if (st.getQuestItemsCount(randomPart) == 0)
				{
					st.giveItems(randomPart, 1);
					st.playSound(QuestState.SOUND_MIDDLE);
				}
			}
		}

		return null;
	}
}