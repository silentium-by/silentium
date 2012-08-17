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
import silentium.gameserver.scripting.ScriptFile;

public class Q031_SecretBuriedInTheSwamp extends Quest implements ScriptFile
{
	private final static String qn = "Q031_SecretBuriedInTheSwamp";

	// Item
	private static final int KrorinsJournal = 7252;

	// Reward
	private static final int Adena = 57;

	// NPCs
	private static final int Abercrombie = 31555;
	private static final int ForgottenMonument1 = 31661;
	private static final int ForgottenMonument2 = 31662;
	private static final int ForgottenMonument3 = 31663;
	private static final int ForgottenMonument4 = 31664;
	private static final int CorpseOfDwarf = 31665;

	public Q031_SecretBuriedInTheSwamp(int questId, String name, String descr)
	{
		super(questId, name, descr);

		questItemIds = new int[] { KrorinsJournal };

		addStartNpc(Abercrombie);
		addTalkId(Abercrombie, CorpseOfDwarf, ForgottenMonument1, ForgottenMonument2, ForgottenMonument3, ForgottenMonument4);
	}

	public static void onLoad()
	{
		new Q031_SecretBuriedInTheSwamp(31, "Q031_SecretBuriedInTheSwamp", "quests");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event.equalsIgnoreCase("31555-01.htm"))
		{
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("31665-01.htm"))
		{
			st.set("cond", "2");
			st.giveItems(KrorinsJournal, 1);
			st.playSound(QuestState.SOUND_MIDDLE);
		}
		else if (event.equalsIgnoreCase("31555-04.htm"))
		{
			st.set("cond", "3");
			st.playSound(QuestState.SOUND_MIDDLE);
		}
		else if (event.equalsIgnoreCase("31661-01.htm"))
		{
			st.set("cond", "4");
			st.playSound(QuestState.SOUND_MIDDLE);
		}
		else if (event.equalsIgnoreCase("31662-01.htm"))
		{
			st.set("cond", "5");
			st.playSound(QuestState.SOUND_MIDDLE);
		}
		else if (event.equalsIgnoreCase("31663-01.htm"))
		{
			st.set("cond", "6");
			st.playSound(QuestState.SOUND_MIDDLE);
		}
		else if (event.equalsIgnoreCase("31664-01.htm"))
		{
			st.set("cond", "7");
			st.playSound(QuestState.SOUND_MIDDLE);
		}
		else if (event.equalsIgnoreCase("31555-07.htm"))
		{
			st.takeItems(KrorinsJournal, 1);
			st.rewardItems(Adena, 40000);
			st.addExpAndSp(130000, 0);
			st.playSound(QuestState.SOUND_FINISH);
			st.exitQuest(false);
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
				if (player.getLevel() >= 66 && player.getLevel() <= 76)
					htmltext = "31555-00.htm";
				else
					htmltext = "31555-00a.htm";
				break;

			case QuestState.STARTED:
				int cond = st.getInt("cond");
				switch (npc.getNpcId())
				{
					case Abercrombie:
						if (cond == 1)
							htmltext = "31555-02.htm";
						else if (cond == 2)
							htmltext = "31555-03.htm";
						else if (cond >= 3 && cond <= 6)
							htmltext = "31555-05.htm";
						else if (cond == 7)
							htmltext = "31555-06.htm";
						break;

					case CorpseOfDwarf:
						if (cond == 1)
							htmltext = "31665-00.htm";
						else if (cond >= 2)
							htmltext = "31665-02.htm";
						break;

					case ForgottenMonument1:
						if (cond == 3)
							htmltext = "31661-00.htm";
						else if (cond >= 4)
							htmltext = "31661-02.htm";
						break;

					case ForgottenMonument2:
						if (cond == 4)
							htmltext = "31662-00.htm";
						else if (cond >= 5)
							htmltext = "31662-02.htm";
						break;

					case ForgottenMonument3:
						if (cond == 5)
							htmltext = "31663-00.htm";
						else if (cond >= 6)
							htmltext = "31663-02.htm";
						break;

					case ForgottenMonument4:
						if (cond == 6)
							htmltext = "31664-00.htm";
						else if (cond >= 7)
							htmltext = "31664-02.htm";
						break;
				}
				break;

			case QuestState.COMPLETED:
				htmltext = Quest.getAlreadyCompletedMsg();
				break;
		}

		return htmltext;
	}
}