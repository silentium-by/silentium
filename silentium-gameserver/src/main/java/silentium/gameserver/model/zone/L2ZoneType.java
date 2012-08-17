/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.model.zone;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javolution.util.FastMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import silentium.gameserver.model.L2Object;
import silentium.gameserver.model.actor.L2Character;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.quest.Quest;
import silentium.gameserver.model.quest.Quest.QuestEventType;
import silentium.gameserver.network.serverpackets.L2GameServerPacket;

/**
 * Abstract base class for any zone type Handles basic operations
 * 
 * @author durgus
 */
public abstract class L2ZoneType
{
	protected static final Logger _log = LoggerFactory.getLogger(L2ZoneType.class.getName());

	private final int _id;
	protected L2ZoneForm _zone;
	protected FastMap<Integer, L2Character> _characterList;

	/** Parameters to affect specific characters */
	private boolean _checkAffected = false;

	private String _name = null;
	private int _minLvl;
	private int _maxLvl;
	private int[] _race;
	private int[] _class;
	private char _classType;
	private Map<QuestEventType, List<Quest>> _questEvents;
	private String _target = "L2Character"; // default all chars

	protected L2ZoneType(int id)
	{
		_id = id;
		_characterList = new FastMap<Integer, L2Character>().shared();

		_minLvl = 0;
		_maxLvl = 0xFF;

		_classType = 0;

		_race = null;
		_class = null;
	}

	/**
	 * @return Returns the id.
	 */
	public int getId()
	{
		return _id;
	}

	/**
	 * Setup new parameters for this zone
	 * 
	 * @param name
	 *            parameter name.
	 * @param value
	 *            new parameter value.
	 */
	public void setParameter(String name, String value)
	{
		_checkAffected = true;

		// Zone name
		if (name.equals("name"))
		{
			_name = value;
		}
		// Minimum level
		else if (name.equals("affectedLvlMin"))
		{
			_minLvl = Integer.parseInt(value);
		}
		// Maximum level
		else if (name.equals("affectedLvlMax"))
		{
			_maxLvl = Integer.parseInt(value);
		}
		// Affected Races
		else if (name.equals("affectedRace"))
		{
			// Create a new array holding the affected race
			if (_race == null)
			{
				_race = new int[1];
				_race[0] = Integer.parseInt(value);
			}
			else
			{
				int[] temp = new int[_race.length + 1];

				int i = 0;
				for (; i < _race.length; i++)
					temp[i] = _race[i];

				temp[i] = Integer.parseInt(value);

				_race = temp;
			}
		}
		// Affected classes
		else if (name.equals("affectedClassId"))
		{
			// Create a new array holding the affected classIds
			if (_class == null)
			{
				_class = new int[1];
				_class[0] = Integer.parseInt(value);
			}
			else
			{
				int[] temp = new int[_class.length + 1];

				int i = 0;
				for (; i < _class.length; i++)
					temp[i] = _class[i];

				temp[i] = Integer.parseInt(value);

				_class = temp;
			}
		}
		// Affected class type
		else if (name.equals("affectedClassType"))
		{
			if (value.equals("Fighter"))
				_classType = 1;
			else
				_classType = 2;
		}
		else if (name.equals("targetClass"))
		{
			_target = value;
		}
		else
			_log.info(getClass().getSimpleName() + ": Unknown parameter - " + name + " in zone: " + getId());
	}

	/**
	 * Checks if the given character is affected by this zone
	 * 
	 * @param character
	 * @return
	 */
	private boolean isAffected(L2Character character)
	{
		// Check lvl
		if (character.getLevel() < _minLvl || character.getLevel() > _maxLvl)
			return false;

		// check obj class
		try
		{
			if (!(Class.forName("silentium.gameserver.model.actor." + _target).isInstance(character)))
				return false;
		}
		catch (ClassNotFoundException e)
		{
			e.printStackTrace();
		}

		if (character instanceof L2PcInstance)
		{
			// Check class type
			if (_classType != 0)
			{
				if (((L2PcInstance) character).isMageClass())
				{
					if (_classType == 1)
						return false;
				}
				else if (_classType == 2)
					return false;
			}

			// Check race
			if (_race != null)
			{
				boolean ok = false;

				for (int element : _race)
				{
					if (((L2PcInstance) character).getRace().ordinal() == element)
					{
						ok = true;
						break;
					}
				}

				if (!ok)
					return false;
			}

			// Check class
			if (_class != null)
			{
				boolean ok = false;

				for (int _clas : _class)
				{
					if (((L2PcInstance) character).getClassId().ordinal() == _clas)
					{
						ok = true;
						break;
					}
				}

				if (!ok)
					return false;
			}
		}
		return true;
	}

	/**
	 * Set the zone for this L2ZoneType Instance
	 * 
	 * @param zone
	 */
	public void setZone(L2ZoneForm zone)
	{
		if (_zone != null)
			throw new IllegalStateException("Zone already set");
		_zone = zone;
	}

	/**
	 * @return this zone form.
	 */
	public L2ZoneForm getZone()
	{
		return _zone;
	}

	/**
	 * Set the zone name.
	 * 
	 * @param name
	 */
	public void setName(String name)
	{
		_name = name;
	}

	/**
	 * Returns zone name
	 * 
	 * @return
	 */
	public String getName()
	{
		return _name;
	}

	/**
	 * @param x
	 * @param y
	 * @return true if the given coordinates are within zone's plane
	 */
	public boolean isInsideZone(int x, int y)
	{
		return _zone.isInsideZone(x, y, _zone.getHighZ());
	}

	/**
	 * @param x
	 * @param y
	 * @param z
	 * @return true if the given coordinates are within the zone
	 */
	public boolean isInsideZone(int x, int y, int z)
	{
		return _zone.isInsideZone(x, y, z);
	}

	/**
	 * @param object
	 *            check object's X/Y positions.
	 * @return true if the given object is inside the zone.
	 */
	public boolean isInsideZone(L2Object object)
	{
		return isInsideZone(object.getX(), object.getY(), object.getZ());
	}

	public double getDistanceToZone(int x, int y)
	{
		return getZone().getDistanceToZone(x, y);
	}

	public double getDistanceToZone(L2Object object)
	{
		return getZone().getDistanceToZone(object.getX(), object.getY());
	}

	public void revalidateInZone(L2Character character)
	{
		// If the character can't be affected by this zone return
		if (_checkAffected)
		{
			if (!isAffected(character))
				return;
		}

		// If the object is inside the zone...
		if (isInsideZone(character.getX(), character.getY(), character.getZ()))
		{
			// Was the character not yet inside this zone?
			if (!_characterList.containsKey(character.getObjectId()))
			{
				List<Quest> quests = getQuestByEvent(QuestEventType.ON_ENTER_ZONE);
				if (quests != null)
				{
					for (Quest quest : quests)
						quest.notifyEnterZone(character, this);
				}
				_characterList.put(character.getObjectId(), character);
				onEnter(character);
			}
		}
		else
		{
			// Was the character inside this zone?
			if (_characterList.containsKey(character.getObjectId()))
			{
				List<Quest> quests = getQuestByEvent(QuestEventType.ON_EXIT_ZONE);
				if (quests != null)
				{
					for (Quest quest : quests)
						quest.notifyExitZone(character, this);
				}
				_characterList.remove(character.getObjectId());
				onExit(character);
			}
		}
	}

	/**
	 * Force fully removes a character from the zone Should use during teleport / logoff
	 * 
	 * @param character
	 */
	public void removeCharacter(L2Character character)
	{
		if (_characterList.containsKey(character.getObjectId()))
		{
			List<Quest> quests = getQuestByEvent(QuestEventType.ON_EXIT_ZONE);
			if (quests != null)
			{
				for (Quest quest : quests)
					quest.notifyExitZone(character, this);
			}
			_characterList.remove(character.getObjectId());
			onExit(character);
		}
	}

	/**
	 * Will scan the zones char list for the character
	 * 
	 * @param character
	 * @return
	 */
	public boolean isCharacterInZone(L2Character character)
	{
		return _characterList.containsKey(character.getObjectId());
	}

	protected abstract void onEnter(L2Character character);

	protected abstract void onExit(L2Character character);

	public abstract void onDieInside(L2Character character);

	public abstract void onReviveInside(L2Character character);

	public FastMap<Integer, L2Character> getCharactersInside()
	{
		return _characterList;
	}

	/**
	 * @return all players within this zone.
	 */
	public List<L2PcInstance> getPlayersInside()
	{
		List<L2PcInstance> players = new ArrayList<>();

		for (L2Character temp : _characterList.values())
		{
			if (temp != null && temp instanceof L2PcInstance)
				players.add((L2PcInstance) temp);
		}

		return players;
	}

	public void addQuestEvent(QuestEventType EventType, Quest q)
	{
		if (_questEvents == null)
			_questEvents = new HashMap<>();

		List<Quest> questByEvents = _questEvents.get(EventType);
		if (questByEvents == null)
			questByEvents = new ArrayList<>();

		if (!questByEvents.contains(q))
			questByEvents.add(q);

		_questEvents.put(EventType, questByEvents);
	}

	public List<Quest> getQuestByEvent(QuestEventType EventType)
	{
		if (_questEvents == null)
			return null;

		return _questEvents.get(EventType);
	}

	/**
	 * Broadcasts packet to all players inside the zone
	 * 
	 * @param packet
	 *            The packet to use.
	 */
	public void broadcastPacket(L2GameServerPacket packet)
	{
		if (_characterList.isEmpty())
			return;

		for (L2Character character : _characterList.values())
		{
			if (character != null && character instanceof L2PcInstance)
				character.sendPacket(packet);
		}
	}

	public void setTargetType(String type)
	{
		_target = type;
		_checkAffected = true;
	}

	@Override
	public String toString()
	{
		return getClass().getSimpleName() + "[" + _id + "]";
	}

	public void visualizeZone(int z)
	{
		getZone().visualizeZone(z);
	}
}
