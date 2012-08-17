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
import silentium.gameserver.model.itemcontainer.Inventory;
import silentium.gameserver.model.quest.Quest;
import silentium.gameserver.model.quest.QuestState;
import silentium.gameserver.scripting.ScriptFile;

public class Q104_SpiritOfMirrors extends Quest implements ScriptFile
{
	private final static String qn = "Q104_SpiritOfMirrors";

	// Items
	private static final int GALLINS_OAK_WAND = 748;
	private static final int WAND_SPIRITBOUND1 = 1135;
	private static final int WAND_SPIRITBOUND2 = 1136;
	private static final int WAND_SPIRITBOUND3 = 1137;

	// Normal rewards
	private static final int SPIRITSHOT_NO_GRADE = 2509;
	private static final int SOULSHOT_NO_GRADE = 1835;
	private static final int WAND_OF_ADEPT = 747;

	// Newbie system
	private static final int SPIRITSHOT_NO_GRADE_FOR_BEGINNERS = 5790;
	private static final int SOULSHOT_FOR_BEGINNERS = 5789;
	private static final int crystals[] = { 4412, 4413, 4414, 4415, 4416 };

	// NPCs
	private static final int GALLINT = 30017;
	private static final int ARNOLD = 30041;
	private static final int JOHNSTONE = 30043;
	private static final int KENYOS = 30045;

	public Q104_SpiritOfMirrors(int questId, String name, String descr)
	{
		super(questId, name, descr);

		questItemIds = new int[] { GALLINS_OAK_WAND, WAND_SPIRITBOUND1, WAND_SPIRITBOUND2, WAND_SPIRITBOUND3 };

		addStartNpc(GALLINT);
		addTalkId(GALLINT, ARNOLD, JOHNSTONE, KENYOS);

		addKillId(27003, 27004, 27005);
	}

	public static void onLoad()
	{
		new Q104_SpiritOfMirrors(104, "Q104_SpiritOfMirrors", "quests");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event.equalsIgnoreCase("30017-03.htm"))
		{
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
			st.giveItems(GALLINS_OAK_WAND, 1);
			st.giveItems(GALLINS_OAK_WAND, 1);
			st.giveItems(GALLINS_OAK_WAND, 1);
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
				if (player.getRace().ordinal() == 0)
				{
					if (player.getLevel() >= 10 && player.getLevel() <= 15)
						htmltext = "30017-02.htm";
					else
					{
						htmltext = "30017-01.htm";
						st.exitQuest(true);
					}
				}
				else
				{
					htmltext = "30017-00.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				int cond = st.getInt("cond");
				switch (npc.getNpcId())
				{
					case GALLINT:
						if (cond == 1 || cond == 2)
							htmltext = "30017-04.htm";
						else if (cond == 3)
						{
							htmltext = "30017-05.htm";

							st.takeItems(WAND_SPIRITBOUND1, -1);
							st.takeItems(WAND_SPIRITBOUND2, -1);
							st.takeItems(WAND_SPIRITBOUND3, -1);

							st.giveItems(WAND_OF_ADEPT, 1);
							st.rewardItems(1060, 100);

							if (player.isMageClass())
								st.giveItems(SPIRITSHOT_NO_GRADE, 500);
							else
								st.giveItems(SOULSHOT_NO_GRADE, 1000);

							if (player.isNewbie())
							{
								st.showQuestionMark(26);
								if (player.isMageClass())
								{
									st.playTutorialVoice("tutorial_voice_027");
									st.giveItems(SPIRITSHOT_NO_GRADE_FOR_BEGINNERS, 3000);
								}
								else
								{
									st.playTutorialVoice("tutorial_voice_026");
									st.giveItems(SOULSHOT_FOR_BEGINNERS, 7000);
								}

								for (int item : crystals)
									st.rewardItems(item, 10);
							}
							st.exitQuest(false);
							st.playSound(QuestState.SOUND_FINISH);
						}
						break;

					case KENYOS:
						htmltext = "30045-01.htm";
						if (cond == 1)
						{
							st.set("cond", "2");
							st.playSound(QuestState.SOUND_MIDDLE);
						}
						break;

					case JOHNSTONE:
						htmltext = "30043-01.htm";
						if (cond == 1)
						{
							st.set("cond", "2");
							st.playSound(QuestState.SOUND_MIDDLE);
						}
						break;

					case ARNOLD:
						htmltext = "30041-01.htm";
						if (cond == 1)
						{
							st.set("cond", "2");
							st.playSound(QuestState.SOUND_MIDDLE);
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

		if (st.isStarted() && st.getItemEquipped(Inventory.PAPERDOLL_RHAND) == GALLINS_OAK_WAND)
		{
			switch (npc.getNpcId())
			{
				case 27003:
					if (!st.hasQuestItems(WAND_SPIRITBOUND1))
					{
						st.takeItems(GALLINS_OAK_WAND, 1);
						st.giveItems(WAND_SPIRITBOUND1, 1);

						if (st.hasQuestItems(WAND_SPIRITBOUND2) && st.hasQuestItems(WAND_SPIRITBOUND3))
						{
							st.set("cond", "3");
							st.playSound(QuestState.SOUND_MIDDLE);
						}
						else
							st.playSound(QuestState.SOUND_ITEMGET);
					}
					break;

				case 27004:
					if (!st.hasQuestItems(WAND_SPIRITBOUND2))
					{
						st.takeItems(GALLINS_OAK_WAND, 1);
						st.giveItems(WAND_SPIRITBOUND2, 1);

						if (st.hasQuestItems(WAND_SPIRITBOUND1) && st.hasQuestItems(WAND_SPIRITBOUND3))
						{
							st.set("cond", "3");
							st.playSound(QuestState.SOUND_MIDDLE);
						}
						else
							st.playSound(QuestState.SOUND_ITEMGET);
					}
					break;

				case 27005:
					if (!st.hasQuestItems(WAND_SPIRITBOUND3))
					{
						st.takeItems(GALLINS_OAK_WAND, 1);
						st.giveItems(WAND_SPIRITBOUND3, 1);

						if (st.hasQuestItems(WAND_SPIRITBOUND1) && st.hasQuestItems(WAND_SPIRITBOUND2))
						{
							st.set("cond", "3");
							st.playSound(QuestState.SOUND_MIDDLE);
						}
						else
							st.playSound(QuestState.SOUND_ITEMGET);
					}
					break;
			}
		}

		return null;
	}
}