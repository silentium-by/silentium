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

public class Q300_HuntingLetoLizardman extends Quest
{
	private static final String qn = "Q300_HuntingLetoLizardman";

	// NPC
	private static final int RATH = 30126;

	// Item
	private static final int BRACELET = 7139;

	public Q300_HuntingLetoLizardman(int questId, String name, String descr)
	{
		super(questId, name, descr);

		questItemIds = new int[] { BRACELET };

		addStartNpc(RATH);
		addTalkId(RATH);

		addKillId(20577, 20578, 20579, 20580, 20582);
	}

	public static void main(String[] args)
	{
		new Q300_HuntingLetoLizardman(300, "Q300_HuntingLetoLizardman", "Hunting Leto Lizardman");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event.equalsIgnoreCase("30126-03.htm"))
		{
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("30126-05.htm"))
		{
			if (st.getQuestItemsCount(BRACELET) >= 60)
			{
				htmltext = "30126-06.htm";

				int luck = Rnd.get(3);

				st.takeItems(BRACELET, -1);
				if (luck == 0)
					st.rewardItems(57, 30000);
				else if (luck == 1)
					st.rewardItems(1867, 50);
				else if (luck == 2)
					st.rewardItems(1872, 50);

				st.playSound(QuestState.SOUND_FINISH);
				st.exitQuest(true);
			}
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
				if (player.getLevel() >= 34 && player.getLevel() <= 39)
					htmltext = "30126-02.htm";
				else
				{
					htmltext = "30126-01.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				if (st.getQuestItemsCount(BRACELET) >= 60)
					htmltext = "30126-04.htm";
				else
					htmltext = "30126-04a.htm";
				break;
		}

		return htmltext;
	}

	@Override
	public String onKill(L2Npc npc, L2PcInstance player, boolean isPet)
	{
		L2PcInstance partyMember = getRandomPartyMember(player, npc, "1");
		if (partyMember != null)
			partyMember.getQuestState(qn).dropQuestItems(BRACELET, 1, 60, 330000);

		return null;
	}
}