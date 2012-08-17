/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.model.actor.instance;

import java.util.Map;
import java.util.StringTokenizer;

import javolution.util.FastMap;
import silentium.gameserver.data.html.StaticHtmPath;
import silentium.gameserver.model.L2Multisell;
import silentium.gameserver.model.quest.QuestState;
import silentium.gameserver.network.serverpackets.ActionFailed;
import silentium.gameserver.network.serverpackets.NpcHtmlMessage;
import silentium.gameserver.tables.SkillTable;
import silentium.gameserver.templates.chars.L2NpcTemplate;

/**
 * This instance leads behaviors of Golden Ram mofos, where shown htm is different according to your quest condition. Abercrombie shows you
 * multisells, Selina shows you Buffs list, when Pierce shows you "Quest" link.<br>
 * <br>
 * Kahman shows you only different htm. He's enthusiastic lazy-ass.
 * 
 * @author Tryskell
 */
public class L2GoldenRamInstance extends L2NpcInstance
{
	// Buffs
	private static Map<String, int[]> data = new FastMap<>();
	private static final int GOLDEN_RAM = 7251;

	public L2GoldenRamInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);

		// Buffs
		data.put("1", new int[] { 4404, 2, 2 }); // focus
		data.put("2", new int[] { 4405, 2, 2 }); // death whisper
		data.put("3", new int[] { 4393, 3, 3 }); // might
		data.put("4", new int[] { 4400, 2, 3 }); // acumen
		data.put("5", new int[] { 4397, 1, 3 }); // berserker
		data.put("6", new int[] { 4399, 2, 3 }); // vampiric
		data.put("7", new int[] { 4401, 1, 6 }); // empower
		data.put("8", new int[] { 4402, 2, 6 }); // haste
	}

	@Override
	public void showChatWindow(L2PcInstance player, int val)
	{
		int npcId = getNpcId();
		String filename = StaticHtmPath.DefaultHtmPath + npcId + ".htm";

		QuestState st = player.getQuestState("Q628_HuntOfTheGoldenRamMercenaryForce");
		if (st != null)
		{
			int cond = st.getInt("cond");

			switch (npcId)
			{
				case 31553:
				case 31554:
					// Captain Pierce && Kahman ; different behavior if you got at least one badge.
					if (cond >= 2)
						filename = StaticHtmPath.DefaultHtmPath + npcId + "-1.htm";
					break;

				case 31555:
				case 31556:
					// Abercrombie and Selina
					if (cond == 2)
						filename = StaticHtmPath.DefaultHtmPath + npcId + "-1.htm";
					else if (cond == 3)
						filename = StaticHtmPath.DefaultHtmPath + npcId + "-2.htm";
					break;
			}
		}

		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		player.sendPacket(html);
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		QuestState qs = player.getQuestState("Q628_HuntOfTheGoldenRamMercenaryForce");
		StringTokenizer st = new StringTokenizer(command, " ");
		String actualCommand = st.nextToken(); // Get actual command

		// if command is "buff" and quest is started and at cond 3
		if (actualCommand.contains("buff") && (qs != null && qs.getInt("cond") == 3))
		{
			// Search the next token, which is a number between 1-8 (see Map "data").
			String event = st.nextToken();

			int skill = data.get(event)[0];
			int skilllvl = data.get(event)[1];
			int coins = data.get(event)[2];
			int val = 3;

			if (qs.getQuestItemsCount(GOLDEN_RAM) >= coins)
			{
				qs.takeItems(GOLDEN_RAM, coins);
				setTarget(player);
				doCast(SkillTable.getInstance().getInfo(skill, skilllvl));
				val = 4;
			}

			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			html.setFile(StaticHtmPath.DefaultHtmPath + "31556-" + val + ".htm");
			player.sendPacket(html);
			return;
		}
		else if (command.startsWith("gmultisell"))
		{
			if (qs != null && qs.getInt("cond") == 3)
				L2Multisell.getInstance().separateAndSend(Integer.parseInt(command.substring(10).trim()), player, false, getCastle().getTaxRate());
		}
		else
			super.onBypassFeedback(player, command);
	}
}