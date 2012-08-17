/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.model;

import silentium.gameserver.idfactory.IdFactory;
import silentium.gameserver.model.actor.L2Character;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.actor.knownlist.ObjectKnownList;
import silentium.gameserver.model.actor.poly.ObjectPoly;
import silentium.gameserver.model.actor.position.ObjectPosition;
import silentium.gameserver.network.L2GameClient;
import silentium.gameserver.network.serverpackets.ActionFailed;

/**
 * Mother class of all interactive objects in the world (PC, NPC, Item...)
 */
public abstract class L2Object
{
	private boolean _isVisible; // Object visibility
	private ObjectKnownList _knownList;
	private String _name;
	private int _objectId; // Object identifier
	private ObjectPoly _poly;
	private ObjectPosition _position;

	public L2Object(int objectId)
	{
		_objectId = objectId;
		initKnownList();
		initPosition();
	}

	public void onAction(L2PcInstance player)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	public void onActionShift(L2GameClient client)
	{
		client.getActiveChar().sendPacket(ActionFailed.STATIC_PACKET);
	}

	public void onForcedAttack(L2PcInstance player)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	/**
	 * Do Nothing.<BR>
	 * <BR>
	 * <B><U> Overriden in </U> :</B><BR>
	 * <BR>
	 * <li>L2GuardInstance : Set the home location of its L2GuardInstance</li> <li>L2Attackable : Reset the Spoiled flag</li><BR>
	 * <BR>
	 */
	public void onSpawn()
	{
	}

	public final void setXYZ(int x, int y, int z)
	{
		getPosition().setXYZ(x, y, z);
	}

	public final void setXYZInvisible(int x, int y, int z)
	{
		getPosition().setXYZInvisible(x, y, z);
	}

	public final int getX()
	{
		assert getPosition().getWorldRegion() != null || _isVisible;
		return getPosition().getX();
	}

	public final int getY()
	{
		assert getPosition().getWorldRegion() != null || _isVisible;
		return getPosition().getY();
	}

	public final int getZ()
	{
		assert getPosition().getWorldRegion() != null || _isVisible;
		return getPosition().getZ();
	}

	/**
	 * Remove a L2Object from the world.
	 */
	public void decayMe()
	{
		assert getPosition().getWorldRegion() != null;

		L2WorldRegion reg = getPosition().getWorldRegion();

		synchronized (this)
		{
			_isVisible = false;
			getPosition().setWorldRegion(null);
		}

		// Remove the L2Object from the world -- Out of synchronized to avoid deadlocks
		L2World.getInstance().removeVisibleObject(this, reg);
		L2World.getInstance().removeObject(this);
	}

	public void refreshID()
	{
		L2World.getInstance().removeObject(this);
		IdFactory.getInstance().releaseId(getObjectId());
		_objectId = IdFactory.getInstance().getNextId();
	}

	/**
	 * Init the position of a L2Object spawn and add it in the world as a visible object.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Set the x,y,z position of the L2Object spawn and update its _worldregion</li> <li>Add the L2Object spawn in the _allobjects of L2World
	 * </li> <li>Add the L2Object spawn to _visibleObjects of its L2WorldRegion</li> <li>Add the L2Object spawn in the world as a <B>visible</B>
	 * object</li><BR>
	 * <BR>
	 * <B><U> Assert </U> :</B><BR>
	 * <BR>
	 * <li>_worldRegion == null <I>(L2Object is invisible at the beginning)</I></li><BR>
	 * <BR>
	 * <B><U> Example of use </U> :</B><BR>
	 * <BR>
	 * <li>Create Door</li> <li>Spawn : Monster, Minion, CTs, Summon...</li><BR>
	 */
	public final void spawnMe()
	{
		assert getPosition().getWorldRegion() == null && getPosition().getWorldPosition().getX() != 0 && getPosition().getWorldPosition().getY() != 0 && getPosition().getWorldPosition().getZ() != 0;

		synchronized (this)
		{
			// Set the x,y,z position of the L2Object spawn and update its _worldregion
			_isVisible = true;
			getPosition().setWorldRegion(L2World.getInstance().getRegion(getPosition().getWorldPosition()));

			// Add the L2Object spawn in the _allobjects of L2World
			L2World.getInstance().storeObject(this);

			// Add the L2Object spawn to _visibleObjects and if necessary to _allplayers of its L2WorldRegion
			getPosition().getWorldRegion().addVisibleObject(this);
		}

		// Add the L2Object spawn in the world as a visible object -- out of synchronized to avoid deadlocks
		L2World.getInstance().addVisibleObject(this, getPosition().getWorldRegion());

		onSpawn();
	}

	public final void spawnMe(int x, int y, int z)
	{
		assert getPosition().getWorldRegion() == null;

		synchronized (this)
		{
			// Set the x,y,z position of the L2Object spawn and update its _worldregion
			_isVisible = true;

			if (x > L2World.MAP_MAX_X)
				x = L2World.MAP_MAX_X - 5000;
			if (x < L2World.MAP_MIN_X)
				x = L2World.MAP_MIN_X + 5000;
			if (y > L2World.MAP_MAX_Y)
				y = L2World.MAP_MAX_Y - 5000;
			if (y < L2World.MAP_MIN_Y)
				y = L2World.MAP_MIN_Y + 5000;

			getPosition().setWorldPosition(x, y, z);
			getPosition().setWorldRegion(L2World.getInstance().getRegion(getPosition().getWorldPosition()));
		}

		// Add the L2Object spawn in the _allobjects of L2World
		L2World.getInstance().storeObject(this);

		// Add the L2Object spawn to _visibleObjects and if necessary to _allplayers of its L2WorldRegion
		getPosition().getWorldRegion().addVisibleObject(this);

		// Add the L2Object spawn in the world as a visible object
		L2World.getInstance().addVisibleObject(this, getPosition().getWorldRegion());

		onSpawn();
	}

	public void toggleVisible()
	{
		if (isVisible())
			decayMe();
		else
			spawnMe();
	}

	public boolean isAttackable()
	{
		return false;
	}

	public abstract boolean isAutoAttackable(L2Character attacker);

	public boolean isMarker()
	{
		return false;
	}

	/**
	 * A L2Object is visible if <B>_isVisible</B> = true and <B>_worldregion</B> != null.
	 * 
	 * @return the visibilty state of the L2Object.
	 */
	public final boolean isVisible()
	{
		return getPosition().getWorldRegion() != null && _isVisible;
	}

	public final void setIsVisible(boolean value)
	{
		_isVisible = value;

		if (!_isVisible)
			getPosition().setWorldRegion(null);
	}

	public void initKnownList()
	{
		_knownList = new ObjectKnownList(this);
	}

	public ObjectKnownList getKnownList()
	{
		return _knownList;
	}

	public final void setKnownList(ObjectKnownList value)
	{
		_knownList = value;
	}

	public final String getName()
	{
		return _name;
	}

	public void setName(String value)
	{
		_name = value;
	}

	public final int getObjectId()
	{
		return _objectId;
	}

	public final ObjectPoly getPoly()
	{
		if (_poly == null)
			_poly = new ObjectPoly(this);

		return _poly;
	}

	public void initPosition()
	{
		_position = new ObjectPosition(this);
	}

	public ObjectPosition getPosition()
	{
		return _position;
	}

	public final void setObjectPosition(ObjectPosition value)
	{
		_position = value;
	}

	public L2PcInstance getActingPlayer()
	{
		return null;
	}

	/**
	 * @return a reference to the region this object is located.
	 */
	public L2WorldRegion getWorldRegion()
	{
		return getPosition().getWorldRegion();
	}

	/**
	 * Sends the Server->Client info packet for the object. Is Overridden in: <li>L2BoatInstance</li> <li>L2DoorInstance</li> <li>L2PcInstance</li>
	 * <li>L2StaticObjectInstance</li> <li>L2Npc</li> <li>L2Summon</li> <li>L2ItemInstance</li>
	 * 
	 * @param activeChar
	 */
	public void sendInfo(L2PcInstance activeChar)
	{

	}

	@Override
	public String toString()
	{
		return (getClass().getSimpleName() + ":" + getName() + "[" + getObjectId() + "]");
	}
}