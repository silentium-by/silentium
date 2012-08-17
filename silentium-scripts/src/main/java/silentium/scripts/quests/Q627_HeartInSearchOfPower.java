/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.quests;

import java.util.HashMap;
import java.util.Map;

import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.quest.Quest;
import silentium.gameserver.model.quest.QuestState;
import silentium.gameserver.scripting.ScriptFile;

public class Q627_HeartInSearchOfPower extends Quest implements ScriptFile
{
	private final static String qn = "Q627_HeartInSearchOfPower";

	// NPCs
	private static final int NECROMANCER = 31518;
	private static final int ENFEUX = 31519;

	// Items
	private static final int SEAL_OF_LIGHT = 7170;
	private static final int BEAD_OF_OBEDIENCE = 7171;
	private static final int GEM_OF_SAINTS = 7172;

	// Rewards
	private static final Map<String, int[]> Rewards = new HashMap<>();

	{
		Rewards.put("adena", new int[] { 0, 0, 100000 });
		Rewards.put("asofe", new int[] { 4043, 13, 6400 });
		Rewards.put("thon", new int[] { 4044, 13, 6400 });
		Rewards.put("enria", new int[] { 4042, 6, 13600 });
		Rewards.put("mold", new int[] { 4041, 3, 17200 });
	}

	public Q627_HeartInSearchOfPower(int questId, String name, String descr)
	{
		super(questId, name, descr);

		questItemIds = new int[] { BEAD_OF_OBEDIENCE };

		addStartNpc(NECROMANCER);
		addTalkId(NECROMANCER, ENFEUX);

		addKillId(21520, 21521, 21522, 21523, 21524, 21525, 21526, 21527, 21528, 21529, 21530, 21531, 21532, 21533, 21534, 21535, 21536, 21537, 21538, 21539, 21540);
	}

	public static void onLoad()
	{
		new Q627_HeartInSearchOfPower(627, "Q627_HeartInSearchOfPower", "Heart in Search of Power");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event.equalsIgnoreCase("31518-01.htm"))
		{
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("31518-03.htm"))
		{
			if (st.getQuestItemsCount(BEAD_OF_OBEDIENCE) == 300)
			{
				st.set("cond", "3");
				st.takeItems(BEAD_OF_OBEDIENCE, -1);
				st.giveItems(SEAL_OF_LIGHT, 1);
				st.playSound(QuestState.SOUND_MIDDLE);
			}
			else
			{
				st.set("cond", "1");
				htmltext = "31518-03a.htm";
				st.takeItems(BEAD_OF_OBEDIENCE, -1);
			}
		}
		else if (event.equalsIgnoreCase("31519-01.htm"))
		{
			if (st.getQuestItemsCount(SEAL_OF_LIGHT) == 1)
			{
				st.set("cond", "4");
				st.takeItems(SEAL_OF_LIGHT, 1);
				st.giveItems(GEM_OF_SAINTS, 1);
				st.playSound(QuestState.SOUND_MIDDLE);
			}
		}
		else if (Rewards.containsKey(event))
		{
			if (st.getQuestItemsCount(GEM_OF_SAINTS) == 1)
			{
				htmltext = "31518-07.htm";
				st.takeItems(GEM_OF_SAINTS, 1);
				st.playSound(QuestState.SOUND_FINISH);

				if (Rewards.get(event)[0] > 0)
					st.giveItems(Rewards.get(event)[0], Rewards.get(event)[1]);
				st.rewardItems(57, Rewards.get(event)[2]);

				st.exitQuest(true);
			}
			else
				htmltext = "31518-7.htm";
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
				if (player.getLevel() >= 60 && player.getLevel() <= 71)
					htmltext = "31518-00.htm";
				else
				{
					htmltext = "31518-00a.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				int cond = st.getInt("cond");
				switch (npc.getNpcId())
				{
					case NECROMANCER:
						if (cond == 1)
							htmltext = "31518-01a.htm";
						else if (cond == 2)
							htmltext = "31518-02.htm";
						else if (cond == 3)
							htmltext = "31518-04.htm";
						else if (cond == 4)
							htmltext = "31518-05.htm";
						break;

					case ENFEUX:
						if (cond == 3)
							htmltext = "31519-00.htm";
						else if (cond == 4)
							htmltext = "31519-02.htm";
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
			if (st.dropQuestItems(BEAD_OF_OBEDIENCE, 1, 300, 900000))
				st.set("cond", "2");

		return null;
	}
}