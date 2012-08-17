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

public class Q257_TheGuardIsBusy extends Quest implements ScriptFile
{
	private final static String qn = "Q257_TheGuardIsBusy";

	// NPC
	private static final int GILBERT = 30039;

	// Items
	private static final int GLUDIO_LORDS_MARK = 1084;
	private static final int ORC_AMULET = 752;
	private static final int ORC_NECKLACE = 1085;
	private static final int WEREWOLF_FANG = 1086;

	// Newbie Items
	private static final int SPIRITSHOT_FOR_BEGINNERS = 5790;
	private static final int SOULSHOT_FOR_BEGINNERS = 5789;

	public Q257_TheGuardIsBusy(int questId, String name, String descr)
	{
		super(questId, name, descr);

		questItemIds = new int[] { ORC_AMULET, ORC_NECKLACE, WEREWOLF_FANG, GLUDIO_LORDS_MARK };

		addStartNpc(GILBERT);
		addTalkId(GILBERT);

		addKillId(20006, 20093, 20096, 20098, 20130, 20131, 20132, 20342, 20343);
	}

	public static void onLoad()
	{
		new Q257_TheGuardIsBusy(257, "Q257_TheGuardIsBusy", "quests");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event.equalsIgnoreCase("30039-03.htm"))
		{
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
			st.giveItems(GLUDIO_LORDS_MARK, 1);
		}
		else if (event.equalsIgnoreCase("30039-05.htm"))
		{
			st.takeItems(GLUDIO_LORDS_MARK, 1);
			st.exitQuest(true);
			st.playSound(QuestState.SOUND_FINISH);
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
				if (st.getPlayer().getLevel() >= 6 && st.getPlayer().getLevel() <= 16)
					htmltext = "30039-02.htm";
				else
				{
					htmltext = "30039-01.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				int orc_a = st.getQuestItemsCount(ORC_AMULET);
				int orc_n = st.getQuestItemsCount(ORC_NECKLACE);
				int fang = st.getQuestItemsCount(WEREWOLF_FANG);

				if (orc_a + orc_n + fang == 0)
					htmltext = "30039-04.htm";
				else
				{
					htmltext = "30039-07.htm";

					st.takeItems(ORC_AMULET, -1);
					st.takeItems(ORC_NECKLACE, -1);
					st.takeItems(WEREWOLF_FANG, -1);

					int reward = (10 * orc_a) + 20 * (orc_n + fang);
					if (orc_a + orc_n + fang >= 10)
						reward += 1000;

					st.rewardItems(57, reward);

					if (player.isNewbie() && st.getInt("Reward") == 0)
					{
						st.showQuestionMark(26);
						st.set("Reward", "1");

						if (player.isMageClass())
						{
							st.playTutorialVoice("tutorial_voice_027");
							st.giveItems(SPIRITSHOT_FOR_BEGINNERS, 3000);
						}
						else
						{
							st.playTutorialVoice("tutorial_voice_026");
							st.giveItems(SOULSHOT_FOR_BEGINNERS, 6000);
						}
					}
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

		if (st.isStarted() && st.getQuestItemsCount(GLUDIO_LORDS_MARK) == 1)
		{
			int chance = 5;
			int item = WEREWOLF_FANG;

			switch (npc.getNpcId())
			{
				case 20006:
				case 20130:
				case 20131:
					item = ORC_AMULET;
					break;

				case 20093:
				case 20096:
				case 20098:
					item = ORC_NECKLACE;
					break;

				case 20342:
					chance = 2;
					break;

				case 20343:
					chance = 4;
					break;
			}

			if (Rnd.get(10) < chance)
			{
				st.giveItems(item, 1);
				st.playSound(QuestState.SOUND_ITEMGET);
			}
		}

		return null;
	}
}