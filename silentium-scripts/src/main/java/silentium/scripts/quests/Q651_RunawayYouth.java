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
import silentium.gameserver.network.serverpackets.MagicSkillUse;
import silentium.gameserver.scripting.ScriptFile;

public class Q651_RunawayYouth extends Quest implements ScriptFile
{
	private static final String qn = "Q651_RunawayYouth";

	// NPCs
	private static final int IVAN = 32014;
	private static final int BATIDAE = 31989;

	// Item
	private static final int SOE = 736;

	// Table of possible spawns
	private static final int[][] spawns = { { 118600, -161235, -1119 }, { 108380, -150268, -2376 }, { 123254, -148126, -3425 } };

	// Current position
	private int _currentPosition = 0;

	public Q651_RunawayYouth(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(IVAN);
		addTalkId(IVAN, BATIDAE);

		addSpawn(IVAN, 118600, -161235, -1119, 0, false, 0);
	}

	public static void onLoad()
	{
		new Q651_RunawayYouth(651, "Q651_RunawayYouth", "Runaway Youth");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event.equalsIgnoreCase("32014-04.htm"))
		{
			if (st.hasQuestItems(SOE))
			{
				st.set("cond", "1");
				st.setState(QuestState.STARTED);
				st.takeItems(SOE, 1);
				st.playSound(QuestState.SOUND_ACCEPT);

				htmltext = "32014-03.htm";
				npc.broadcastPacket(new MagicSkillUse(npc, npc, 2013, 1, 3500, 0));
				startQuestTimer("apparition_npc", 4000, npc, player);
			}
			else
				st.exitQuest(true);
		}
		else if (event.equalsIgnoreCase("apparition_npc"))
		{
			int chance = Rnd.get(3);

			// Loop to avoid to spawn to the same place.
			while (chance == _currentPosition)
				chance = Rnd.get(3);

			// Register new position.
			_currentPosition = chance;

			npc.deleteMe();
			addSpawn(IVAN, spawns[chance][0], spawns[chance][1], spawns[chance][2], 0, false, 0);
			return null;
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
				if (player.getLevel() >= 26)
					htmltext = "32014-02.htm";
				else
				{
					htmltext = "32014-01.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				switch (npc.getNpcId())
				{
					case BATIDAE:
						htmltext = "31989-01.htm";
						st.rewardItems(57, 2883);
						st.playSound(QuestState.SOUND_FINISH);
						st.exitQuest(true);
						break;

					case IVAN:
						htmltext = "32014-04a.htm";
						break;
				}
				break;
		}

		return htmltext;
	}
}