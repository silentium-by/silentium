/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import silentium.commons.database.DatabaseFactory;
import silentium.commons.utils.Point3D;
import silentium.commons.utils.Rnd;
import silentium.gameserver.ThreadPoolManager;
import silentium.gameserver.configs.MainConfig;
import silentium.gameserver.instancemanager.CursedWeaponsManager;
import silentium.gameserver.model.actor.L2Attackable;
import silentium.gameserver.model.actor.L2Character;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.network.SystemMessageId;
import silentium.gameserver.network.serverpackets.*;
import silentium.gameserver.tables.SkillTable;
import silentium.gameserver.tables.SkillTable.FrequentSkill;
import silentium.gameserver.templates.item.L2Item;
import silentium.gameserver.utils.Broadcast;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.ScheduledFuture;

public class CursedWeapon
{
	private static final Logger _log = LoggerFactory.getLogger(CursedWeapon.class.getName());

	private final String _name;
	private final int _itemId;
	private final int _skillId;
	private final int _skillMaxLevel;
	private int _dropRate;
	private int _duration;
	private int _durationLost;
	private int _disapearChance;
	private int _stageKills;

	private boolean _isDropped = false;
	private boolean _isActivated = false;
	private ScheduledFuture<?> _removeTask;

	private int _nbKills = 0;
	private long _endTime = 0;

	private int _playerId = 0;
	private L2PcInstance _player = null;
	private L2ItemInstance _item = null;
	private int _playerKarma = 0;
	private int _playerPkKills = 0;

	public CursedWeapon(int itemId, int skillId, String name)
	{
		_name = name;
		_itemId = itemId;
		_skillId = skillId;
		_skillMaxLevel = SkillTable.getInstance().getMaxLevel(_skillId);
	}

	public void endOfLife()
	{
		if (_isActivated)
		{
			// Player is online ; unequip weapon && destroy it.
			if (_player != null && _player.isOnline())
			{
				_log.info(_name + " being removed online.");

				_player.abortAttack();

				_player.setKarma(_playerKarma);
				_player.setPkKills(_playerPkKills);
				_player.setCursedWeaponEquippedId(0);
				removeSkill();

				// Remove
				_player.getInventory().unEquipItemInBodySlot(L2Item.SLOT_LR_HAND);
				_player.store();

				// Destroy
				L2ItemInstance removedItem = _player.getInventory().destroyItemByItemId("", _itemId, 1, _player, null);
				if (!MainConfig.FORCE_INVENTORY_UPDATE)
				{
					InventoryUpdate iu = new InventoryUpdate();
					if (removedItem.getCount() == 0)
						iu.addRemovedItem(removedItem);
					else
						iu.addModifiedItem(removedItem);

					_player.sendPacket(iu);
				}
				else
					_player.sendPacket(new ItemList(_player, true));

				_player.broadcastUserInfo();
			}
			// Player is offline ; make only SQL operations.
			else
			{
				_log.info(_name + " being removed offline.");

				try (Connection con = DatabaseFactory.getConnection())
				{
					// Delete the item
					PreparedStatement statement = con.prepareStatement("DELETE FROM items WHERE owner_id=? AND item_id=?");
					statement.setInt(1, _playerId);
					statement.setInt(2, _itemId);
					if (statement.executeUpdate() != 1)
						_log.warn("Error while deleting itemId " + _itemId + " from userId " + _playerId);

					statement.close();

					// Restore the karma and PK kills.
					statement = con.prepareStatement("UPDATE characters SET karma=?, pkkills=? WHERE obj_id=?");
					statement.setInt(1, _playerKarma);
					statement.setInt(2, _playerPkKills);
					statement.setInt(3, _playerId);
					if (statement.executeUpdate() != 1)
						_log.warn("Error while updating karma & pkkills for userId " + _playerId);

					statement.close();
				}
				catch (Exception e)
				{
					_log.warn("Could not delete : " + e.getMessage(), e);
				}
			}
		}
		// This CW is in the inventory of someone who has another CW equipped, OR this CW is on the ground.
		else
		{
			if ((_player != null) && (_player.getInventory().getItemByItemId(_itemId) != null))
			{
				// Destroy
				L2ItemInstance removedItem = _player.getInventory().destroyItemByItemId("", _itemId, 1, _player, null);
				if (!MainConfig.FORCE_INVENTORY_UPDATE)
				{
					InventoryUpdate iu = new InventoryUpdate();
					if (removedItem.getCount() == 0)
						iu.addRemovedItem(removedItem);
					else
						iu.addModifiedItem(removedItem);

					_player.sendPacket(iu);
				}
				else
					_player.sendPacket(new ItemList(_player, true));

				_player.broadcastUserInfo();
			}
			else if (_item != null)
			{
				_item.decayMe();
				L2World.getInstance().removeObject(_item);
				_log.info(_name + " item has been removed from world.");
			}
		}

		// Delete infos from table if any
		CursedWeaponsManager.removeFromDb(_itemId);

		SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_HAS_DISAPPEARED);
		sm.addItemName(_itemId);
		Broadcast.toAllOnlinePlayers(sm);

		// Reset state
		cancelTask();
		_isActivated = false;
		_isDropped = false;
		_endTime = 0;
		_player = null;
		_playerId = 0;
		_playerKarma = 0;
		_playerPkKills = 0;
		_item = null;
		_nbKills = 0;
	}

	private void cancelTask()
	{
		if (_removeTask != null)
		{
			_removeTask.cancel(true);
			_removeTask = null;
		}
	}

	private class RemoveTask implements Runnable
	{
		protected RemoveTask()
		{
		}

		@Override
		public void run()
		{
			if (System.currentTimeMillis() >= getEndTime())
				endOfLife();
		}
	}

	private void dropIt(L2Attackable attackable, L2PcInstance player)
	{
		dropIt(attackable, player, null, true);
	}

	private void dropIt(L2Attackable attackable, L2PcInstance player, L2Character killer, boolean fromMonster)
	{
		_isActivated = false;

		if (fromMonster)
		{
			_item = attackable.dropItem(player, _itemId, 1);
			_item.setDropTime(0); // Prevent item from being removed by ItemsAutoDestroy

			// RedSky and Earthquake
			Broadcast.toAllOnlinePlayers(new ExRedSky(10));
			Broadcast.toAllOnlinePlayers(new Earthquake(player.getX(), player.getY(), player.getZ(), 14, 3));
		}
		else
		{
			_player.dropItem("DieDrop", _item, killer, true);
			_player.setKarma(_playerKarma);
			_player.setPkKills(_playerPkKills);
			_player.setCursedWeaponEquippedId(0);
			removeSkill();
			_player.abortAttack();
		}

		_isDropped = true;
		SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S2_WAS_DROPPED_IN_THE_S1_REGION);
		if (player != null)
			sm.addZoneName(player.getX(), player.getY(), player.getZ()); // Region Name
		else if (_player != null)
			sm.addZoneName(_player.getX(), _player.getY(), _player.getZ()); // Region Name
		else
			sm.addZoneName(killer.getX(), killer.getY(), killer.getZ()); // Region Name
		sm.addItemName(_itemId);
		Broadcast.toAllOnlinePlayers(sm); // in the Hot Spring region
	}

	public void cursedOnLogin()
	{
		SystemMessage msg = SystemMessage.getSystemMessage(SystemMessageId.S2_OWNER_HAS_LOGGED_INTO_THE_S1_REGION);
		msg.addZoneName(_player.getX(), _player.getY(), _player.getZ());
		msg.addItemName(_player.getCursedWeaponEquippedId());
		Broadcast.toAllOnlinePlayers(msg);

		CursedWeapon cw = CursedWeaponsManager.getInstance().getCursedWeapon(_player.getCursedWeaponEquippedId());
		SystemMessage msg2 = SystemMessage.getSystemMessage(SystemMessageId.S2_MINUTE_OF_USAGE_TIME_ARE_LEFT_FOR_S1);
		int timeLeft = (int) (cw.getTimeLeft() / 60000);
		msg2.addItemName(_player.getCursedWeaponEquippedId());
		msg2.addNumber(timeLeft);
		_player.sendPacket(msg2);
	}

	/**
	 * Rebind the passive skill belonging to the CursedWeapon. Invoke this method if the weapon owner switches to a subclass.
	 */
	public void giveSkill()
	{
		int level = 1 + (_nbKills / _stageKills);
		if (level > _skillMaxLevel)
			level = _skillMaxLevel;

		L2Skill skill = SkillTable.getInstance().getInfo(_skillId, level);
		_player.addSkill(skill, false);

		// Void Burst, Void Flow
		skill = FrequentSkill.VOID_BURST.getSkill();
		_player.addSkill(skill, false);
		skill = FrequentSkill.VOID_FLOW.getSkill();
		_player.addSkill(skill, false);

		_log.debug(_player.getName() + " has been awarded with skill " + skill);

		_player.sendSkillList();
	}

	public void removeSkill()
	{
		_player.removeSkill(_skillId);
		_player.removeSkill(FrequentSkill.VOID_BURST.getSkill().getId());
		_player.removeSkill(FrequentSkill.VOID_FLOW.getSkill().getId());
		_player.sendSkillList();
	}

	public void reActivate(boolean fromZero)
	{
		if (fromZero)
		{
			_isActivated = true;
			if (_endTime - System.currentTimeMillis() <= 0)
				endOfLife();
			else
				_removeTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new RemoveTask(), _durationLost * 12000L, _durationLost * 12000L);
		}
		else
			_removeTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new RemoveTask(), _durationLost * 12000L, _durationLost * 12000L);
	}

	public boolean checkDrop(L2Attackable attackable, L2PcInstance player)
	{
		if (Rnd.get(100000) < _dropRate)
		{
			// Drop the item
			dropIt(attackable, player);

			// Start the Life Task
			_endTime = System.currentTimeMillis() + _duration * 60000L;
			_removeTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new RemoveTask(), _durationLost * 12000L, _durationLost * 12000L);

			return true;
		}
		return false;
	}

	public void activate(L2PcInstance player, L2ItemInstance item)
	{
		// if the player is mounted, attempt to unmount first and pick it if successful.
		if (player.isMounted() && !player.dismount())
		{
			player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.FAILED_TO_PICKUP_S1).addItemName(item.getItemId()));
			player.dropItem("InvDrop", item, null, true);
			return;
		}

		// if the player wears a Formal Wear, unequip it.
		final L2ItemInstance chestArmor = player.getChestArmorInstance();
		if (chestArmor != null && chestArmor.getItemId() == 6408)
			player.useEquippableItem(chestArmor, false);

		_isActivated = true;

		// Player holding it data
		_player = player;
		_playerId = _player.getObjectId();
		_playerKarma = _player.getKarma();
		_playerPkKills = _player.getPkKills();
		saveData();

		// Change player stats
		_player.setCursedWeaponEquippedId(_itemId);
		_player.setKarma(9999999);
		_player.setPkKills(0);

		if (_player.isInParty())
			_player.getParty().removePartyMember(_player);

		// Disable active toggles
		for (L2Effect effect : _player.getAllEffects())
		{
			if (effect.getSkill().isToggle())
				effect.exit();
		}

		// Add CW skills
		giveSkill();

		// Equip with the weapon
		_item = item;
		_player.getInventory().equipItem(_item);
		SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_EQUIPPED);
		sm.addItemName(_item.getItemId());
		_player.sendPacket(sm);

		// Fully heal player
		_player.setCurrentHpMp(_player.getMaxHp(), _player.getMaxMp());
		_player.setCurrentCp(_player.getMaxCp());

		// Refresh inventory
		if (!MainConfig.FORCE_INVENTORY_UPDATE)
		{
			InventoryUpdate iu = new InventoryUpdate();
			iu.addItem(_item);
			_player.sendPacket(iu);
		}
		else
			_player.sendPacket(new ItemList(_player, false));

		// Refresh player stats
		_player.broadcastUserInfo();

		_player.broadcastPacket(new SocialAction(_player, 17));
		Broadcast.toAllOnlinePlayers(SystemMessage.getSystemMessage(SystemMessageId.THE_OWNER_OF_S2_HAS_APPEARED_IN_THE_S1_REGION).addZoneName(_player.getX(), _player.getY(), _player.getZ()).addItemName(_item.getItemId()));
	}

	public void saveData()
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			// Delete previous datas
			PreparedStatement statement = con.prepareStatement("DELETE FROM cursed_weapons WHERE itemId = ?");
			statement.setInt(1, _itemId);
			statement.executeUpdate();

			if (_isActivated)
			{
				statement = con.prepareStatement("INSERT INTO cursed_weapons (itemId, playerId, playerKarma, playerPkKills, nbKills, endTime) VALUES (?, ?, ?, ?, ?, ?)");
				statement.setInt(1, _itemId);
				statement.setInt(2, _playerId);
				statement.setInt(3, _playerKarma);
				statement.setInt(4, _playerPkKills);
				statement.setInt(5, _nbKills);
				statement.setLong(6, _endTime);
				statement.executeUpdate();
				statement.close();
			}
		}
		catch (SQLException e)
		{
			_log.error("CursedWeapon: Failed to save data.", e);
		}
	}

	public void dropIt(L2Character killer)
	{
		// Remove it
		if (Rnd.get(100) <= _disapearChance)
			endOfLife();
		else
		{
			// Unequip & Drop
			dropIt(null, null, killer, false);

			// Reset player stats
			_player.setKarma(_playerKarma);
			_player.setPkKills(_playerPkKills);
			_player.setCursedWeaponEquippedId(0);
			removeSkill();

			_player.abortAttack();
			_player.broadcastUserInfo();
		}
	}

	public void increaseKills()
	{
		_nbKills++;

		if (_player != null && _player.isOnline())
		{
			_player.setPkKills(_nbKills);
			_player.sendPacket(new UserInfo(_player));

			if (_nbKills % _stageKills == 0 && _nbKills <= _stageKills * (_skillMaxLevel - 1))
				giveSkill();
		}

		// Reduce time-to-live
		_endTime -= _durationLost * 60000L;
		saveData();
	}

	public void setDisapearChance(int disapearChance)
	{
		_disapearChance = disapearChance;
	}

	public void setDropRate(int dropRate)
	{
		_dropRate = dropRate;
	}

	public void setDuration(int duration)
	{
		_duration = duration;
	}

	public void setDurationLost(int durationLost)
	{
		_durationLost = durationLost;
	}

	public void setStageKills(int stageKills)
	{
		_stageKills = stageKills;
	}

	public void setNbKills(int nbKills)
	{
		_nbKills = nbKills;
	}

	public void setPlayerId(int playerId)
	{
		_playerId = playerId;
	}

	public void setPlayerKarma(int playerKarma)
	{
		_playerKarma = playerKarma;
	}

	public void setPlayerPkKills(int playerPkKills)
	{
		_playerPkKills = playerPkKills;
	}

	public void setActivated(boolean isActivated)
	{
		_isActivated = isActivated;
	}

	public void setDropped(boolean isDropped)
	{
		_isDropped = isDropped;
	}

	public void setEndTime(long endTime)
	{
		_endTime = endTime;
	}

	public void setPlayer(L2PcInstance player)
	{
		_player = player;
	}

	public void setItem(L2ItemInstance item)
	{
		_item = item;
	}

	public boolean isActivated()
	{
		return _isActivated;
	}

	public boolean isDropped()
	{
		return _isDropped;
	}

	public long getEndTime()
	{
		return _endTime;
	}

	public long getDuration()
	{
		return _duration;
	}

	public String getName()
	{
		return _name;
	}

	public int getItemId()
	{
		return _itemId;
	}

	public int getSkillId()
	{
		return _skillId;
	}

	public int getPlayerId()
	{
		return _playerId;
	}

	public L2PcInstance getPlayer()
	{
		return _player;
	}

	public int getPlayerKarma()
	{
		return _playerKarma;
	}

	public int getPlayerPkKills()
	{
		return _playerPkKills;
	}

	public int getNbKills()
	{
		return _nbKills;
	}

	public int getStageKills()
	{
		return _stageKills;
	}

	public boolean isActive()
	{
		return _isActivated || _isDropped;
	}

	public int getLevel()
	{
		if (_nbKills > _stageKills * _skillMaxLevel)
			return _skillMaxLevel;

		return (_nbKills / _stageKills);
	}

	public long getTimeLeft()
	{
		return _endTime - System.currentTimeMillis();
	}

	public void goTo(L2PcInstance player)
	{
		if (player == null)
			return;

		// Go to player holding the weapon
		if (_isActivated)
			player.teleToLocation(_player.getX(), _player.getY(), _player.getZ() + 20, true);
		// Go to item on the ground
		else if (_isDropped)
			player.teleToLocation(_item.getX(), _item.getY(), _item.getZ() + 20, true);
		else
			player.sendMessage(_name + " isn't in the world.");
	}

	public Point3D getWorldPosition()
	{
		if (_isActivated && _player != null)
			return _player.getPosition().getWorldPosition();

		if (_isDropped && _item != null)
			return _item.getPosition().getWorldPosition();

		return null;
	}
}
