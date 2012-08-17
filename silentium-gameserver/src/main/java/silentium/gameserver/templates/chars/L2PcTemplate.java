/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.templates.chars;

import java.util.List;

import javolution.util.FastList;
import silentium.gameserver.model.base.ClassId;
import silentium.gameserver.model.base.Race;
import silentium.gameserver.tables.ItemTable;
import silentium.gameserver.templates.StatsSet;
import silentium.gameserver.templates.item.L2Item;

/**
 * @author mkizub
 */
public class L2PcTemplate extends L2CharTemplate
{
	/** The Class object of the L2PcInstance */
	public final ClassId classId;

	public final Race race;
	public final String className;

	public final int _currentCollisionRadius;
	public final int _currentCollisionHeight;
	public final int _fallingHeight;

	public final int spawnX;
	public final int spawnY;
	public final int spawnZ;

	public final int classBaseLevel;
	public final float lvlHpAdd;
	public final float lvlHpMod;
	public final float lvlCpAdd;
	public final float lvlCpMod;
	public final float lvlMpAdd;
	public final float lvlMpMod;

	private final List<L2Item> _items = new FastList<>();

	public L2PcTemplate(StatsSet set)
	{
		super(set);
		classId = ClassId.values()[set.getInteger("classId")];
		race = Race.values()[set.getInteger("raceId")];
		className = set.getString("className");

		_currentCollisionRadius = set.getInteger("collision_radius");
		_currentCollisionHeight = set.getInteger("collision_height");
		_fallingHeight = set.getInteger("falling_height", 333);

		spawnX = set.getInteger("spawnX");
		spawnY = set.getInteger("spawnY");
		spawnZ = set.getInteger("spawnZ");

		classBaseLevel = set.getInteger("baseLvl");
		lvlHpAdd = set.getFloat("lvlHpAdd");
		lvlHpMod = set.getFloat("lvlHpMod");
		lvlCpAdd = set.getFloat("lvlCpAdd");
		lvlCpMod = set.getFloat("lvlCpMod");
		lvlMpAdd = set.getFloat("lvlMpAdd");
		lvlMpMod = set.getFloat("lvlMpMod");
	}

	/**
	 * Add starter equipement.
	 * 
	 * @param itemId
	 *            the item to add if template is found
	 */
	public void addItem(int itemId)
	{
		L2Item item = ItemTable.getInstance().getTemplate(itemId);
		if (item != null)
			_items.add(item);
	}

	/**
	 * @return itemIds of all the starter equipment
	 */
	public L2Item[] getItems()
	{
		return _items.toArray(new L2Item[_items.size()]);
	}

	@Override
	public int getCollisionRadius()
	{
		return _currentCollisionRadius;
	}

	@Override
	public int getCollisionHeight()
	{
		return _currentCollisionHeight;
	}

	public final int getFallHeight()
	{
		return _fallingHeight;
	}
}