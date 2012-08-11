/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.quests;

import silentium.gameserver.configs.MainConfig;
import silentium.commons.utils.Rnd;
import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.quest.Quest;
import silentium.gameserver.model.quest.QuestState;

public class Q634_InSearchOfFragmentsOfDimension extends Quest
{
	private final static String qn = "Q634_InSearchOfFragmentsOfDimension";

	// Items
	private static final int DIMENSION_FRAGMENT = 7079;

	public Q634_InSearchOfFragmentsOfDimension(int questId, String name, String descr)
	{
		super(questId, name, descr);

		// Dimensional Gate Keepers.
		for (int i = 31494; i < 31508; i++)
		{
			addStartNpc(i);
			addTalkId(i);
		}

		// All mobs.
		for (int i = 21208; i < 21256; i++)
			addKillId(i);
	}

	public static void main(String[] args)
	{
		new Q634_InSearchOfFragmentsOfDimension(634, "Q634_InSearchOfFragmentsOfDimension", "In Search of Fragments of Dimension");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event.equalsIgnoreCase("02.htm"))
		{
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("05.htm"))
		{
			st.playSound(QuestState.SOUND_FINISH);
			st.exitQuest(true);
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
				if (st.getPlayer().getLevel() >= 20)
					htmltext = "01.htm";
				else
				{
					htmltext = "01a.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				htmltext = "03.htm";
				break;
		}

		return htmltext;
	}

	@Override
	public String onKill(L2Npc npc, L2PcInstance player, boolean isPet)
	{
		L2PcInstance partyMember = getRandomPartyMemberState(player, npc, QuestState.STARTED);
		if (partyMember == null)
			return null;

		QuestState st = partyMember.getQuestState(qn);

		int itemMultiplier = (int) (80 * MainConfig.RATE_QUEST_DROP) / 1000;
		int chance = (int) (80 * MainConfig.RATE_QUEST_DROP) % 1000;

		if (Rnd.get(1000) < chance)
			itemMultiplier++;

		int numItems = (int) (itemMultiplier * (npc.getLevel() * 0.15 + 1.6));
		if (numItems > 0)
		{
			st.giveItems(DIMENSION_FRAGMENT, numItems);
			st.playSound(QuestState.SOUND_ITEMGET);
		}

		return null;
	}
}