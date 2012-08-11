/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.quests;

import java.util.HashMap;
import java.util.Map;

import silentium.commons.utils.Rnd;
import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.quest.Quest;
import silentium.gameserver.model.quest.QuestState;

public class Q369_CollectorOfJewels extends Quest
{
	private final static String qn = "Q369_CollectorOfJewels";

	// NPC
	private static final int NELL = 30376;

	// Items
	private static final int FLARE_SHARD = 5882;
	private static final int FREEZING_SHARD = 5883;

	// Reward
	private static final int ADENA = 57;

	// Droplists
	private static final Map<Integer, Integer> DROPLIST_FREEZE = new HashMap<>();
	{
		DROPLIST_FREEZE.put(20747, 85);
		DROPLIST_FREEZE.put(20619, 73);
		DROPLIST_FREEZE.put(20616, 60);
	}

	private static final Map<Integer, Integer> DROPLIST_FLARE = new HashMap<>();
	{
		DROPLIST_FLARE.put(20612, 77);
		DROPLIST_FLARE.put(20609, 77);
		DROPLIST_FLARE.put(20749, 85);
	}

	public Q369_CollectorOfJewels(int questId, String name, String descr)
	{
		super(questId, name, descr);

		questItemIds = new int[] { FLARE_SHARD, FREEZING_SHARD };

		addStartNpc(NELL);
		addTalkId(NELL);

		for (int mob : DROPLIST_FREEZE.keySet())
			addKillId(mob);

		for (int mob : DROPLIST_FLARE.keySet())
			addKillId(mob);
	}

	public static void main(String[] args)
	{
		new Q369_CollectorOfJewels(369, "Q369_CollectorOfJewels", "Collector of Jewels");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event.equalsIgnoreCase("30376-03.htm"))
		{
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
			st.set("awaitsFreezing", "1");
			st.set("awaitsFlare", "1");
		}
		else if (event.equalsIgnoreCase("30376-07.htm"))
			st.playSound(QuestState.SOUND_ITEMGET);
		else if (event.equalsIgnoreCase("30376-08.htm"))
		{
			st.exitQuest(true);
			st.playSound(QuestState.SOUND_FINISH);
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
				if (player.getLevel() >= 25 && player.getLevel() <= 37)
					htmltext = "30376-02.htm";
				else
				{
					htmltext = "30376-01.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				int cond = st.getInt("cond");
				int flare = st.getQuestItemsCount(FLARE_SHARD);
				int freezing = st.getQuestItemsCount(FREEZING_SHARD);

				if (cond == 1)
					htmltext = "30376-04.htm";
				else if (cond == 2 && flare >= 50 && freezing >= 50)
				{
					htmltext = "30376-05.htm";
					st.set("cond", "3");
					st.rewardItems(ADENA, 12500);
					st.takeItems(FLARE_SHARD, -1);
					st.takeItems(FREEZING_SHARD, -1);
					st.set("awaitsFreezing", "1");
					st.set("awaitsFlare", "1");
					st.playSound(QuestState.SOUND_MIDDLE);
				}
				else if (cond == 3)
					htmltext = "30376-09.htm";
				else if (cond == 4 && flare >= 200 && freezing >= 200)
				{
					htmltext = "30376-10.htm";
					st.playSound(QuestState.SOUND_FINISH);
					st.rewardItems(ADENA, 63500);
					st.takeItems(FLARE_SHARD, -1);
					st.takeItems(FREEZING_SHARD, -1);
					st.exitQuest(true);
				}
				break;
		}

		return htmltext;
	}

	@Override
	public String onKill(L2Npc npc, L2PcInstance player, boolean isPet)
	{
		int npcId = npc.getNpcId();
		L2PcInstance partymember = null;
		int item = 0, chance = 0;

		if (DROPLIST_FREEZE.containsKey(npcId))
		{
			partymember = getRandomPartyMember(player, npc, "awaitsFreezing", "1");

			item = FREEZING_SHARD;
			chance = DROPLIST_FREEZE.get(npcId);
		}
		else if (DROPLIST_FLARE.containsKey(npcId))
		{
			partymember = getRandomPartyMember(player, npc, "awaitsFlare", "1");

			item = FLARE_SHARD;
			chance = DROPLIST_FLARE.get(npcId);
		}

		if (partymember == null)
			return null;

		QuestState st = partymember.getQuestState(qn);
		int cond = st.getInt("cond");

		if (cond >= 1 && cond <= 3)
		{
			int max = 0;

			if (cond == 1)
				max = 50;
			else if (cond == 3)
				max = 200;

			if (Rnd.get(100) < chance && st.getQuestItemsCount(item) <= max)
			{
				st.giveItems(item, 1);

				if (st.getQuestItemsCount(FREEZING_SHARD) == max)
					st.unset("awaitsFreezing");
				else if (st.getQuestItemsCount(FLARE_SHARD) == max)
					st.unset("awaitsFlare");

				if (st.getQuestItemsCount(FLARE_SHARD) == max && st.getQuestItemsCount(FREEZING_SHARD) == max)
				{
					st.set("cond", String.valueOf(cond + 1));
					st.playSound(QuestState.SOUND_MIDDLE);
				}
				else
					st.playSound(QuestState.SOUND_ITEMGET);
			}
		}
		return null;
	}
}