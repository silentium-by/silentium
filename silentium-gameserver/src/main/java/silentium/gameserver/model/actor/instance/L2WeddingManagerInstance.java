/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.model.actor.instance;

import java.util.StringTokenizer;

import silentium.gameserver.configs.EventsConfig;
import silentium.gameserver.Announcements;
import silentium.gameserver.ai.CtrlIntention;
import silentium.gameserver.data.html.StaticHtmPath;
import silentium.gameserver.instancemanager.CastleManager;
import silentium.gameserver.instancemanager.CoupleManager;
import silentium.gameserver.model.L2ItemInstance;
import silentium.gameserver.model.L2Skill;
import silentium.gameserver.model.L2World;
import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.entity.Couple;
import silentium.gameserver.network.serverpackets.ActionFailed;
import silentium.gameserver.network.serverpackets.ConfirmDlg;
import silentium.gameserver.network.serverpackets.MagicSkillUse;
import silentium.gameserver.network.serverpackets.MoveToPawn;
import silentium.gameserver.network.serverpackets.MyTargetSelected;
import silentium.gameserver.network.serverpackets.NpcHtmlMessage;
import silentium.gameserver.network.serverpackets.ValidateLocation;
import silentium.gameserver.tables.SkillTable.FrequentSkill;
import silentium.gameserver.templates.chars.L2NpcTemplate;

/**
 * @author evill33t & squeezed, rework Tryskell
 */
public class L2WeddingManagerInstance extends L2NpcInstance
{
	public L2WeddingManagerInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public void onAction(L2PcInstance player)
	{
		if (!player.canTarget())
			return;

		// Check if the L2PcInstance already target the L2Npc
		if (this != player.getTarget())
		{
			// Set the target of the L2PcInstance player
			player.setTarget(this);

			// Send a Server->Client packet MyTargetSelected to the L2PcInstance player
			player.sendPacket(new MyTargetSelected(getObjectId(), 0));

			// Send a Server->Client packet ValidateLocation to correct the L2Npc position and heading on the client
			player.sendPacket(new ValidateLocation(this));
		}
		else
		{
			// Calculate the distance between the L2PcInstance and the L2Npc
			if (!canInteract(player))
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
			else
			{
				// Rotate the player to face the instance
				player.sendPacket(new MoveToPawn(player, this, L2Npc.INTERACTION_DISTANCE));

				// Send ActionFailed to the player in order to avoid he stucks
				player.sendPacket(ActionFailed.STATIC_PACKET);

				// Shouldn't be able to see wedding content if the mod isn't activated on configs
				if (!EventsConfig.ALLOW_WEDDING)
					sendHtmlMessage(player, StaticHtmPath.ModsHtmPath + "Wedding_disabled.htm");
				else
				{
					// Married people got access to another menu
					if (player.isMarried())
						sendHtmlMessage(player, StaticHtmPath.ModsHtmPath + "Wedding_start2.htm");
					// "Under marriage acceptance" people go to this one
					else if (player.isUnderMarryRequest())
						sendHtmlMessage(player, StaticHtmPath.ModsHtmPath + "Wedding_waitforpartner.htm");
					// And normal players go here :)
					else
						sendHtmlMessage(player, StaticHtmPath.ModsHtmPath + "Wedding_start.htm");
				}
			}
		}
	}

	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		if (command.startsWith("AskWedding"))
		{
			StringTokenizer st = new StringTokenizer(command);
			st.nextToken();

			String token = st.nextToken();
			if (token.isEmpty())
				return;

			L2PcInstance ptarget = L2World.getInstance().getPlayer(token);
			if (ptarget == null)
			{
				sendHtmlMessage(player, StaticHtmPath.ModsHtmPath + "Wedding_notfound.htm");
				return;
			}

			// check conditions
			if (!weddingConditions(player, ptarget))
				return;

			// block the wedding manager until an answer is given.
			player.setUnderMarryRequest(true);
			ptarget.setUnderMarryRequest(true);

			// memorize the requesterId for future use, and send a popup to the target
			ptarget.setRequesterId(player.getObjectId());
			ptarget.sendPacket(new ConfirmDlg(1983).addString(player.getName() + " asked you to marry. Do you want to start a new relationship ?"));
			return;
		}
		else if (command.startsWith("Divorce"))
		{
			player.sendMessage("You are now divorced.");

			// Find the partner using the couple information
			L2PcInstance partner = L2World.getInstance().getPlayer(Couple.getPartnerId(player.getObjectId()));
			if (partner != null)
				partner.sendMessage("Your beloved has decided to divorce.");

			int _coupleId = player.getCoupleId();
			CoupleManager.getInstance().deleteCouple(_coupleId);
			return;
		}
		else if (command.startsWith("GoToLove"))
		{
			// Find the partner using the couple information
			L2PcInstance partner = L2World.getInstance().getPlayer(Couple.getPartnerId(player.getObjectId()));
			if (partner == null)
			{
				player.sendMessage("Your partner is not online.");
				return;
			}

			// Simple checks to avoid exploits
			if (partner.isInJail() || partner.isInOlympiadMode() || partner.isInDuel() || partner.isFestivalParticipant() || (partner.isInParty() && partner.getParty().isInDimensionalRift()) || partner.inObserverMode())
			{
				player.sendMessage("Due to the current partner's status, the teleportation failed.");
				return;
			}

			if (partner.getClan() != null && CastleManager.getInstance().getCastleByOwner(partner.getClan()) != null && CastleManager.getInstance().getCastleByOwner(partner.getClan()).getSiege().getIsInProgress())
			{
				player.sendMessage("As your partner is in siege, you can't go to him/her.");
				return;
			}

			// If all checks are successfully passed, teleport the player to the partner
			player.teleToLocation(partner.getX() + 20, partner.getY() + 20, partner.getZ());
			return;
		}
	}

	/**
	 * Are both partners wearing formal wear ? If Formal Wear check is disabled, returns True in any case.<BR>
	 *
	 * @param p1
	 *            L2PcInstance
	 * @param p2
	 *            L2PcInstance
	 * @return boolean
	 */
	private static boolean wearsFormalWear(L2PcInstance p1, L2PcInstance p2)
	{
		L2ItemInstance fw1 = p1.getChestArmorInstance();
		if (fw1 == null || fw1.getItemId() != 6408)
			return false;

		L2ItemInstance fw2 = p2.getChestArmorInstance();
		if (fw2 == null || fw2.getItemId() != 6408)
			return false;

		return true;
	}

	private boolean weddingConditions(L2PcInstance player, L2PcInstance ptarget)
	{
		// Check if player target himself
		if (ptarget.getObjectId() == player.getObjectId())
		{
			sendHtmlMessage(player, StaticHtmPath.ModsHtmPath + "Wedding_error_wrongtarget.htm");
			return false;
		}

		// Sex check
		if (ptarget.getAppearance().getSex() == player.getAppearance().getSex() && !EventsConfig.WEDDING_SAMESEX)
		{
			sendHtmlMessage(player, StaticHtmPath.ModsHtmPath + "Wedding_error_sex.htm");
			return false;
		}

		// Check if player has the target on friendlist
		if (!player.getFriendList().contains(ptarget.getObjectId()))
		{
			sendHtmlMessage(player, StaticHtmPath.ModsHtmPath + "Wedding_error_friendlist.htm");
			return false;
		}

		// Target mustn't be already married
		if (ptarget.isMarried())
		{
			sendHtmlMessage(player, StaticHtmPath.ModsHtmPath + "Wedding_error_alreadymarried.htm");
			return false;
		}

		// Check for Formal Wear
		if (EventsConfig.WEDDING_FORMALWEAR)
			if (!wearsFormalWear(player, ptarget))
			{
				sendHtmlMessage(player, StaticHtmPath.ModsHtmPath + "Wedding_error_noformal.htm");
				return false;
			}

		// Check and reduce wedding price
		if (player.getAdena() < EventsConfig.WEDDING_PRICE || ptarget.getAdena() < EventsConfig.WEDDING_PRICE)
		{
			sendHtmlMessage(player, StaticHtmPath.ModsHtmPath + "Wedding_error_adena.htm");
			return false;
		}

		return true;
	}

	public static void justMarried(L2PcInstance player, L2PcInstance ptarget)
	{
		// Unlock the wedding manager for both users, and set them as married
		player.setUnderMarryRequest(false);
		ptarget.setUnderMarryRequest(false);

		player.setMarried(true);
		ptarget.setMarried(true);

		// reduce adenas amount according to configs
		player.reduceAdena("Wedding", EventsConfig.WEDDING_PRICE, player.getCurrentFolkNPC(), true);
		ptarget.reduceAdena("Wedding", EventsConfig.WEDDING_PRICE, player.getCurrentFolkNPC(), true);

		// Flag players as married
		Couple couple = CoupleManager.getInstance().getCouple(player.getCoupleId());
		couple.marry();

		// Messages to the couple
		player.sendMessage("Congratulations, you are now married with " + ptarget.getName() + " !");
		ptarget.sendMessage("Congratulations, you are now married with " + player.getName() + " !");

		// Wedding march
		player.broadcastPacket(new MagicSkillUse(player, player, 2230, 1, 1, 0));
		ptarget.broadcastPacket(new MagicSkillUse(ptarget, ptarget, 2230, 1, 1, 0));

		// Fireworks
		L2Skill skill = FrequentSkill.LARGE_FIREWORK.getSkill();
		player.doCast(skill);
		ptarget.doCast(skill);

		Announcements.announceToAll("Congratulations to " + player.getName() + " and " + ptarget.getName() + "! They have been married.");
	}

	private void sendHtmlMessage(L2PcInstance player, String file)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(1);
		html.setFile(file);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%adenasCost%", String.valueOf(EventsConfig.WEDDING_PRICE));
		html.replace("%needOrNot%", EventsConfig.WEDDING_FORMALWEAR ? "will" : "won't");
		player.sendPacket(html);
	}
}