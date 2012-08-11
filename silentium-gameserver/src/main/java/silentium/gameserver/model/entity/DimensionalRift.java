/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.model.entity;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Future;

import javolution.util.FastList;
import silentium.gameserver.configs.EventsConfig;
import silentium.commons.utils.Rnd;
import silentium.gameserver.ThreadPoolManager;
import silentium.gameserver.data.html.StaticHtmPath;
import silentium.gameserver.instancemanager.DimensionalRiftManager;
import silentium.gameserver.model.L2Party;
import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.network.serverpackets.Earthquake;

/**
 * Thanks to L2Fortress and balancer.ru - kombat
 */
public class DimensionalRift
{
	protected byte _type;
	protected L2Party _party;
	protected FastList<Byte> _completedRooms = new FastList<>();
	private static final long seconds_5 = 5000L;
	protected byte _currentJumps = 0;

	private Timer teleporterTimer;
	private TimerTask teleporterTimerTask;
	private Timer spawnTimer;
	private TimerTask spawnTimerTask;

	private Future<?> earthQuakeTask;

	protected byte _choosenRoom = -1;
	private boolean _hasJumped = false;
	protected FastList<L2PcInstance> deadPlayers = new FastList<>();
	protected FastList<L2PcInstance> revivedInWaitingRoom = new FastList<>();

	private boolean isBossRoom = false;

	public DimensionalRift(L2Party party, byte type, byte room)
	{
		DimensionalRiftManager.getInstance().getRoom(type, room).setPartyInside(true);

		_type = type;
		_party = party;
		_choosenRoom = room;

		int[] coords = getRoomCoord(room);
		party.setDimensionalRift(this);

		for (L2PcInstance p : party.getPartyMembers())
			p.teleToLocation(coords[0], coords[1], coords[2]);

		createSpawnTimer(_choosenRoom);
		createTeleporterTimer(true);
	}

	public byte getType()
	{
		return _type;
	}

	public byte getCurrentRoom()
	{
		return _choosenRoom;
	}

	protected void createTeleporterTimer(final boolean reasonTP)
	{
		if (teleporterTimerTask != null)
		{
			teleporterTimerTask.cancel();
			teleporterTimerTask = null;
		}

		if (teleporterTimer != null)
		{
			teleporterTimer.cancel();
			teleporterTimer = null;
		}

		if (earthQuakeTask != null)
		{
			earthQuakeTask.cancel(false);
			earthQuakeTask = null;
		}

		teleporterTimer = new Timer();
		teleporterTimerTask = new TimerTask()
		{
			@Override
			public void run()
			{
				if (_choosenRoom > -1)
					DimensionalRiftManager.getInstance().getRoom(_type, _choosenRoom).unspawn().setPartyInside(false);

				if (reasonTP && _currentJumps < getMaxJumps() && _party.getMemberCount() > deadPlayers.size())
				{
					_currentJumps++;

					_completedRooms.add(_choosenRoom);
					_choosenRoom = -1;

					for (L2PcInstance p : _party.getPartyMembers())
						if (!revivedInWaitingRoom.contains(p))
							teleportToNextRoom(p, false);

					createTeleporterTimer(true);
					createSpawnTimer(_choosenRoom);
				}
				else
				{
					for (L2PcInstance p : _party.getPartyMembers())
						if (!revivedInWaitingRoom.contains(p))
							teleportToWaitingRoom(p);

					killRift();
					cancel();
				}
			}
		};

		if (reasonTP)
		{
			long jumpTime = calcTimeToNextJump();
			teleporterTimer.schedule(teleporterTimerTask, jumpTime); // Teleporter task, 8-10 minutes

			earthQuakeTask = ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
			{
				@Override
				public void run()
				{
					for (L2PcInstance p : _party.getPartyMembers())
						if (!revivedInWaitingRoom.contains(p))
							p.sendPacket(new Earthquake(p.getX(), p.getY(), p.getZ(), 65, 9));
				}
			}, jumpTime - 7000);
		}
		else
			teleporterTimer.schedule(teleporterTimerTask, seconds_5); // incorrect party member invited.
	}

	public void createSpawnTimer(final byte room)
	{
		if (spawnTimerTask != null)
		{
			spawnTimerTask.cancel();
			spawnTimerTask = null;
		}

		if (spawnTimer != null)
		{
			spawnTimer.cancel();
			spawnTimer = null;
		}

		spawnTimer = new Timer();
		spawnTimerTask = new TimerTask()
		{
			@Override
			public void run()
			{
				DimensionalRiftManager.getInstance().getRoom(_type, room).spawn();
			}
		};

		spawnTimer.schedule(spawnTimerTask, EventsConfig.RIFT_SPAWN_DELAY);
	}

	public void partyMemberInvited()
	{
		createTeleporterTimer(false);
	}

	public void partyMemberExited(L2PcInstance player)
	{
		if (deadPlayers.contains(player))
			deadPlayers.remove(player);

		if (revivedInWaitingRoom.contains(player))
			revivedInWaitingRoom.remove(player);

		if (_party.getMemberCount() < EventsConfig.RIFT_MIN_PARTY_SIZE || _party.getMemberCount() == 1)
		{
			for (L2PcInstance p : _party.getPartyMembers())
				teleportToWaitingRoom(p);

			killRift();
		}
	}

	public void manualTeleport(L2PcInstance player, L2Npc npc)
	{
		if (!player.isInParty() || !player.getParty().isInDimensionalRift())
			return;

		if (player.getObjectId() != player.getParty().getPartyLeaderOID())
		{
			DimensionalRiftManager.getInstance().showHtmlFile(player, StaticHtmPath.SevenSignsHtmPath + "rift/NotPartyLeader.htm", npc);
			return;
		}

		if (_currentJumps == EventsConfig.RIFT_MAX_JUMPS)
		{
			DimensionalRiftManager.getInstance().showHtmlFile(player, StaticHtmPath.SevenSignsHtmPath + "rift/UsedAllJumps.htm", npc);
			return;
		}

		if (_hasJumped)
		{
			DimensionalRiftManager.getInstance().showHtmlFile(player, StaticHtmPath.SevenSignsHtmPath + "rift/AlreadyTeleported.htm", npc);
			return;
		}
		_hasJumped = true;

		DimensionalRiftManager.getInstance().getRoom(_type, _choosenRoom).unspawn();
		_completedRooms.add(_choosenRoom);
		_choosenRoom = -1;

		for (L2PcInstance p : _party.getPartyMembers())
			teleportToNextRoom(p, true);

		DimensionalRiftManager.getInstance().getRoom(_type, _choosenRoom).setPartyInside(true);

		createSpawnTimer(_choosenRoom);
		createTeleporterTimer(true);
	}

	public void manualExitRift(L2PcInstance player, L2Npc npc)
	{
		if (!player.isInParty() || !player.getParty().isInDimensionalRift())
			return;

		if (player.getObjectId() != player.getParty().getPartyLeaderOID())
		{
			DimensionalRiftManager.getInstance().showHtmlFile(player, StaticHtmPath.SevenSignsHtmPath + "rift/NotPartyLeader.htm", npc);
			return;
		}

		for (L2PcInstance p : player.getParty().getPartyMembers())
			teleportToWaitingRoom(p);

		killRift();
	}

	/**
	 * This method allows to jump from one room to another. It calculates the next roomId.
	 *
	 * @param player
	 *            to teleport
	 * @param cantJumpToBossRoom
	 *            if true, Anakazel room can't be choosen (case of manual teleport).
	 */
	protected void teleportToNextRoom(L2PcInstance player, boolean cantJumpToBossRoom)
	{
		if (_choosenRoom == -1)
		{
			FastList<Byte> emptyRooms;

			do
			{
				emptyRooms = DimensionalRiftManager.getInstance().getFreeRooms(_type);

				// Do not tp in the same room a second time
				emptyRooms.removeAll(_completedRooms);

				// If no room left, find any empty
				if (emptyRooms.isEmpty())
					emptyRooms = DimensionalRiftManager.getInstance().getFreeRooms(_type);

				// Pickup a random room
				_choosenRoom = emptyRooms.get(Rnd.get(1, emptyRooms.size()) - 1);

				// This code handles Anakazel's room special behavior.
				if (cantJumpToBossRoom)
				{
					while (_choosenRoom == 9)
					{
						_choosenRoom = emptyRooms.get(Rnd.get(1, emptyRooms.size()) - 1);
					}
				}
			}
			while (DimensionalRiftManager.getInstance().getRoom(_type, _choosenRoom).ispartyInside());
		}

		DimensionalRiftManager.getInstance().getRoom(_type, _choosenRoom).setPartyInside(true);
		int[] coords = getRoomCoord(_choosenRoom);
		player.teleToLocation(coords[0], coords[1], coords[2]);
	}

	protected void teleportToWaitingRoom(L2PcInstance player)
	{
		DimensionalRiftManager.getInstance().teleportToWaitingRoom(player);
	}

	public void killRift()
	{
		_completedRooms = null;

		if (_party != null)
			_party.setDimensionalRift(null);

		_party = null;
		revivedInWaitingRoom = null;
		deadPlayers = null;

		if (earthQuakeTask != null)
		{
			earthQuakeTask.cancel(false);
			earthQuakeTask = null;
		}

		DimensionalRiftManager.getInstance().getRoom(_type, _choosenRoom).unspawn().setPartyInside(false);
		DimensionalRiftManager.getInstance().killRift(this);
	}

	public Timer getTeleportTimer()
	{
		return teleporterTimer;
	}

	public TimerTask getTeleportTimerTask()
	{
		return teleporterTimerTask;
	}

	public Timer getSpawnTimer()
	{
		return spawnTimer;
	}

	public TimerTask getSpawnTimerTask()
	{
		return spawnTimerTask;
	}

	public void setTeleportTimer(Timer t)
	{
		teleporterTimer = t;
	}

	public void setTeleportTimerTask(TimerTask tt)
	{
		teleporterTimerTask = tt;
	}

	public void setSpawnTimer(Timer t)
	{
		spawnTimer = t;
	}

	public void setSpawnTimerTask(TimerTask st)
	{
		spawnTimerTask = st;
	}

	private long calcTimeToNextJump()
	{
		int time = Rnd.get(EventsConfig.RIFT_AUTO_JUMPS_TIME_MIN, EventsConfig.RIFT_AUTO_JUMPS_TIME_MAX) * 1000;

		if (isBossRoom)
			return (long) (time * EventsConfig.RIFT_BOSS_ROOM_TIME_MUTIPLY);

		return time;
	}

	public void memberDead(L2PcInstance player)
	{
		if (!deadPlayers.contains(player))
			deadPlayers.add(player);
	}

	public void memberRessurected(L2PcInstance player)
	{
		if (deadPlayers.contains(player))
			deadPlayers.remove(player);
	}

	public void usedTeleport(L2PcInstance player)
	{
		if (!revivedInWaitingRoom.contains(player))
			revivedInWaitingRoom.add(player);

		if (!deadPlayers.contains(player))
			deadPlayers.add(player);

		if (_party.getMemberCount() - revivedInWaitingRoom.size() < EventsConfig.RIFT_MIN_PARTY_SIZE)
		{
			for (L2PcInstance p : _party.getPartyMembers())
				if (p != null && !revivedInWaitingRoom.contains(p))
					teleportToWaitingRoom(p);

			killRift();
		}
	}

	public FastList<L2PcInstance> getDeadMemberList()
	{
		return deadPlayers;
	}

	public FastList<L2PcInstance> getRevivedAtWaitingRoom()
	{
		return revivedInWaitingRoom;
	}

	public void checkBossRoom(byte room)
	{
		isBossRoom = DimensionalRiftManager.getInstance().getRoom(_type, room).isBossRoom();
	}

	public int[] getRoomCoord(byte room)
	{
		return DimensionalRiftManager.getInstance().getRoom(_type, room).getTeleportCoords();
	}

	public byte getMaxJumps()
	{
		if (EventsConfig.RIFT_MAX_JUMPS <= 8 && EventsConfig.RIFT_MAX_JUMPS >= 1)
			return (byte) EventsConfig.RIFT_MAX_JUMPS;

		return 4;
	}
}