/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.quests;

import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.quest.Quest;
import silentium.gameserver.model.quest.QuestState;

public class Q153_DeliverGoods extends Quest
{
	private static final String qn = "Q153_DeliverGoods";

	// NPCs
	private static final int Jackson = 30002;
	private static final int Silvia = 30003;
	private static final int Arnold = 30041;
	private static final int Rant = 30054;

	// Items
	private static final int DeliveryList = 1012;
	private static final int HeavyWoodBox = 1013;
	private static final int ClothBundle = 1014;
	private static final int ClayPot = 1015;
	private static final int JacksonsReceipt = 1016;
	private static final int SilviasReceipt = 1017;
	private static final int RantsReceipt = 1018;

	// Rewards
	private static final int SoulshotNoGrade = 1835;
	private static final int RingofKnowledge = 875;

	public Q153_DeliverGoods(int questId, String name, String descr)
	{
		super(questId, name, descr);

		questItemIds = new int[] { DeliveryList, HeavyWoodBox, ClothBundle, ClayPot, JacksonsReceipt, SilviasReceipt, RantsReceipt };

		addStartNpc(Arnold);
		addTalkId(Jackson, Silvia, Arnold, Rant);
	}

	public static void main(String[] args)
	{
		new Q153_DeliverGoods(153, "Q153_DeliverGoods", "Deliver Goods");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event.equalsIgnoreCase("30041-02.htm"))
		{
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
			st.giveItems(DeliveryList, 1);
			st.giveItems(HeavyWoodBox, 1);
			st.giveItems(ClothBundle, 1);
			st.giveItems(ClayPot, 1);
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
				if (player.getLevel() >= 2 && player.getLevel() <= 5)
					htmltext = "30041-01.htm";
				else
					htmltext = "30041-00.htm";
				break;

			case QuestState.STARTED:
				switch (npc.getNpcId())
				{
					case Arnold:
						if (st.getInt("cond") == 1)
							htmltext = "30041-03.htm";
						else if (st.getInt("cond") == 2)
						{
							htmltext = "30041-04.htm";
							st.takeItems(DeliveryList, 1);
							st.takeItems(JacksonsReceipt, 1);
							st.takeItems(SilviasReceipt, 1);
							st.takeItems(RantsReceipt, 1);
							st.giveItems(RingofKnowledge, 1);
							st.giveItems(RingofKnowledge, 1);
							st.addExpAndSp(600, 0);
							st.exitQuest(false);
							st.playSound(QuestState.SOUND_FINISH);
						}
						break;

					case Jackson:
						if (st.getQuestItemsCount(HeavyWoodBox) > 0)
						{
							htmltext = "30002-01.htm";
							st.takeItems(HeavyWoodBox, 1);
							st.giveItems(JacksonsReceipt, 1);
						}
						else
							htmltext = "30002-02.htm";
						break;

					case Silvia:
						if (st.getQuestItemsCount(ClothBundle) > 0)
						{
							htmltext = "30003-01.htm";
							st.takeItems(ClothBundle, 1);
							st.giveItems(SilviasReceipt, 1);
							st.giveItems(SoulshotNoGrade, 3);
						}
						else
							htmltext = "30003-02.htm";
						break;

					case Rant:
						if (st.getQuestItemsCount(ClayPot) > 0)
						{
							htmltext = "30054-01.htm";
							st.takeItems(ClayPot, 1);
							st.giveItems(RantsReceipt, 1);
						}
						else
							htmltext = "30054-02.htm";
						break;
				}

				if (st.getInt("cond") == 1 && st.getQuestItemsCount(JacksonsReceipt) > 0 && st.getQuestItemsCount(SilviasReceipt) > 0 && st.getQuestItemsCount(RantsReceipt) > 0)
				{
					st.set("cond", "2");
					st.playSound(QuestState.SOUND_MIDDLE);
				}
				break;

			case QuestState.COMPLETED:
				htmltext = Quest.getAlreadyCompletedMsg();
				break;
		}

		return htmltext;
	}
}