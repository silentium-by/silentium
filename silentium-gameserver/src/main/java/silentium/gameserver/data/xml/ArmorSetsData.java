/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.data.xml;

import gnu.trove.map.hash.TIntObjectHashMap;

import java.io.File;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import silentium.gameserver.configs.MainConfig;
import silentium.gameserver.data.xml.parsers.XMLParser;
import silentium.gameserver.model.L2ArmorSet;
import silentium.gameserver.templates.StatsSet;

/**
 * @author godson, Luno, nBd
 */
public class ArmorSetsData
{
	private static Logger _log = LoggerFactory.getLogger(ArmorSetsData.class.getName());

	private TIntObjectHashMap<L2ArmorSet> _armorSets;

	public static ArmorSetsData getInstance()
	{
		return SingletonHolder._instance;
	}

	protected ArmorSetsData()
	{
		_armorSets = new TIntObjectHashMap<>();

		try
		{
			File f = new File(MainConfig.DATAPACK_ROOT + "/data/xml/armorsets.xml");
			if (!f.exists())
			{
				_log.warn("ArmorSetsData: armorsets.xml is missing in data folder.");
				return;
			}

			XMLParser parser = new XMLParser(f, "armorset");
			List<StatsSet> sets = parser.parseDocument();

			if (!sets.isEmpty())
			{
				for (StatsSet set : sets)
				{
					int chest = set.getInteger("chest", 0);
					int legs = set.getInteger("legs", 0);
					int head = set.getInteger("head", 0);
					int gloves = set.getInteger("gloves", 0);
					int feet = set.getInteger("feet", 0);
					int skill_id = set.getInteger("skill_id", 0);
					int shield = set.getInteger("shield", 0);
					int shield_skill_id = set.getInteger("shield_skill_id", 0);
					int enchant6skill = set.getInteger("enchant6skill", 0);
					_armorSets.put(chest, new L2ArmorSet(chest, legs, head, gloves, feet, skill_id, shield, shield_skill_id, enchant6skill));
				}
			}
		}
		catch (Exception e)
		{
			_log.warn("ArmorSetsData: Error loading armorsets.xml", e);
		}
		_log.info("ArmorSetsData: Loaded " + _armorSets.size() + " armor sets.");
	}

	public boolean setExists(int chestId)
	{
		return _armorSets.containsKey(chestId);
	}

	public L2ArmorSet getSet(int chestId)
	{
		return _armorSets.get(chestId);
	}

	private static class SingletonHolder
	{
		protected static final ArmorSetsData _instance = new ArmorSetsData();
	}
}
