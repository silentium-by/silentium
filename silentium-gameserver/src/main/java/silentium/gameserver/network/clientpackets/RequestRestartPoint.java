/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.network.clientpackets;

import silentium.gameserver.ThreadPoolManager;
import silentium.gameserver.data.xml.MapRegionData;
import silentium.gameserver.instancemanager.CastleManager;
import silentium.gameserver.instancemanager.ClanHallManager;
import silentium.gameserver.model.L2SiegeClan;
import silentium.gameserver.model.Location;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.entity.Castle;
import silentium.gameserver.model.entity.ClanHall;
import silentium.gameserver.model.entity.Siege;
import silentium.gameserver.model.entity.TvTEvent;

public final class RequestRestartPoint extends L2GameClientPacket
{
	protected int _requestedPointType;
	protected boolean _continuation;

	@Override
	protected void readImpl()
	{
		_requestedPointType = readD();
	}

	class DeathTask implements Runnable
	{
		final L2PcInstance activeChar;

		DeathTask(L2PcInstance _activeChar)
		{
			activeChar = _activeChar;
		}

		@Override
		public void run()
		{
			Location loc = null;
			Castle castle = null;

			// force
			if (activeChar.isInJail())
				_requestedPointType = 27;
			else if (activeChar.isFestivalParticipant())
				_requestedPointType = 4;

			switch (_requestedPointType)
			{
				case 1: // to clanhall
					if (activeChar.getClan() == null || !activeChar.getClan().hasHideout())
					{
						log.warn(activeChar.getName() + " called RestartPointPacket - To Clanhall while he doesn't have clan / Clanhall.");
						return;
					}

					loc = MapRegionData.getInstance().getTeleToLocation(activeChar, MapRegionData.TeleportWhereType.ClanHall);

					if (ClanHallManager.getInstance().getClanHallByOwner(activeChar.getClan()) != null && ClanHallManager.getInstance().getClanHallByOwner(activeChar.getClan()).getFunction(ClanHall.FUNC_RESTORE_EXP) != null)
					{
						activeChar.restoreExp(ClanHallManager.getInstance().getClanHallByOwner(activeChar.getClan()).getFunction(ClanHall.FUNC_RESTORE_EXP).getLvl());
					}
					break;

				case 2: // to castle
					castle = CastleManager.getInstance().getCastle(activeChar);

					if (castle != null && castle.getSiege().getIsInProgress())
					{
						// Siege in progress
						if (castle.getSiege().checkIsDefender(activeChar.getClan()))
							loc = MapRegionData.getInstance().getTeleToLocation(activeChar, MapRegionData.TeleportWhereType.Castle);
						// Just in case you lost castle while being dead.. Port to nearest Town.
						else if (castle.getSiege().checkIsAttacker(activeChar.getClan()))
							loc = MapRegionData.getInstance().getTeleToLocation(activeChar, MapRegionData.TeleportWhereType.Town);
						else
						{
							log.warn(activeChar.getName() + " called RestartPointPacket - To Castle while he doesn't have Castle.");
							return;
						}
					}
					else
					{
						if (activeChar.getClan() == null || !activeChar.getClan().hasCastle())
							return;

						loc = MapRegionData.getInstance().getTeleToLocation(activeChar, MapRegionData.TeleportWhereType.Castle);
					}
					break;

				case 3: // to siege HQ
					L2SiegeClan siegeClan = null;
					castle = CastleManager.getInstance().getCastle(activeChar);

					if (castle != null && castle.getSiege().getIsInProgress())
						siegeClan = castle.getSiege().getAttackerClan(activeChar.getClan());

					// Not a normal scenario.
					if (siegeClan == null)
					{
						log.warn(activeChar.getName() + " called RestartPointPacket - To Siege HQ while he doesn't have Siege HQ.");
						return;
					}

					// If a player was waiting with flag option and then the flag dies before the
					// player pushes the button, he is send back to closest/second closest town.
					if (siegeClan.getFlag().isEmpty())
						loc = MapRegionData.getInstance().getTeleToLocation(activeChar, MapRegionData.TeleportWhereType.Town);
					else
						loc = MapRegionData.getInstance().getTeleToLocation(activeChar, MapRegionData.TeleportWhereType.SiegeFlag);
					break;

				case 4: // Fixed or player is a festival participant
					if (!activeChar.isGM() && !activeChar.isFestivalParticipant())
					{
						log.warn(activeChar.getName() + " called RestartPointPacket - Fixed while he isn't festival participant!");
						return;
					}
					loc = new Location(activeChar.getX(), activeChar.getY(), activeChar.getZ()); // spawn them where they died
					break;

				case 27: // to jail
					if (!activeChar.isInJail())
						return;
					loc = new Location(-114356, -249645, -2984);
					break;

				default:
					loc = MapRegionData.getInstance().getTeleToLocation(activeChar, MapRegionData.TeleportWhereType.Town);
					break;
			}

			// Teleport and revive
			activeChar.setIsIn7sDungeon(false);
			activeChar.setIsPendingRevive(true);
			activeChar.teleToLocation(loc, true);
		}
	}

	@Override
	protected void runImpl()
	{
		final L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;

		if (TvTEvent.isStarted() && TvTEvent.isPlayerParticipant(activeChar.getObjectId()))
			return;

		if (activeChar.isFakeDeath())
		{
			activeChar.stopFakeDeath(true);
			return;
		}

		if (!activeChar.isDead())
		{
			log.warn("Living player [" + activeChar.getName() + "] called RequestRestartPoint packet.");
			return;
		}

		Castle castle = CastleManager.getInstance().getCastle(activeChar.getX(), activeChar.getY(), activeChar.getZ());
		if (castle != null && castle.getSiege().getIsInProgress())
		{
			if (activeChar.getClan() != null && castle.getSiege().checkIsAttacker(activeChar.getClan()))
			{
				// Schedule respawn delay for attacker
				ThreadPoolManager.getInstance().scheduleGeneral(new DeathTask(activeChar), Siege.getAttackerRespawnDelay());

				if (Siege.getAttackerRespawnDelay() > 0)
					activeChar.sendMessage("You will be teleported in " + Siege.getAttackerRespawnDelay() / 1000 + " seconds.");

				return;
			}
		}

		// run immediately (no need to schedule)
		new DeathTask(activeChar).run();
	}
}
