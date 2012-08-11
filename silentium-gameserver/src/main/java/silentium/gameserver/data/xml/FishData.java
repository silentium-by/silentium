/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.data.xml;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import silentium.gameserver.configs.MainConfig;
import silentium.gameserver.data.xml.parsers.XMLDocumentFactory;
import silentium.gameserver.model.L2FishData;

/**
 * @author -Nemesiss-
 */
public class FishData
{
	private static Logger _log = LoggerFactory.getLogger(FishData.class.getName());

	private static List<L2FishData> _fishesNormal;
	private static List<L2FishData> _fishesEasy;
	private static List<L2FishData> _fishesHard;

	public static FishData getInstance()
	{
		return SingletonHolder._instance;
	}

	protected FishData()
	{
		try
		{
			_fishesEasy = new ArrayList<>();
			_fishesNormal = new ArrayList<>();
			_fishesHard = new ArrayList<>();

			File f = new File(MainConfig.DATAPACK_ROOT + "/data/xml/fishes.xml");
			Document doc = XMLDocumentFactory.getInstance().loadDocument(f);

			Node n = doc.getFirstChild();
			for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
			{
				if (d.getNodeName().equalsIgnoreCase("fish"))
				{
					int id = Integer.parseInt(d.getAttributes().getNamedItem("id").getNodeValue());
					int lvl = Integer.parseInt(d.getAttributes().getNamedItem("level").getNodeValue());
					String name = String.valueOf(d.getAttributes().getNamedItem("name").getNodeValue());
					int hp = Integer.parseInt(d.getAttributes().getNamedItem("hp").getNodeValue());
					int hpreg = Integer.parseInt(d.getAttributes().getNamedItem("hpregen").getNodeValue());
					int type = Integer.parseInt(d.getAttributes().getNamedItem("fish_type").getNodeValue());
					int group = Integer.parseInt(d.getAttributes().getNamedItem("fish_group").getNodeValue());
					int fish_guts = Integer.parseInt(d.getAttributes().getNamedItem("fish_guts").getNodeValue());
					int guts_check_time = Integer.parseInt(d.getAttributes().getNamedItem("guts_check_time").getNodeValue());
					int wait_time = Integer.parseInt(d.getAttributes().getNamedItem("wait_time").getNodeValue());
					int combat_time = Integer.parseInt(d.getAttributes().getNamedItem("combat_time").getNodeValue());

					L2FishData fish = new L2FishData(id, lvl, name, hp, hpreg, type, group, fish_guts, guts_check_time, wait_time, combat_time);
					switch (fish.getGroup())
					{
						case 0:
							_fishesEasy.add(fish);
							break;
						case 1:
							_fishesNormal.add(fish);
							break;
						case 2:
							_fishesHard.add(fish);
					}
				}
			}
		}
		catch (Exception e)
		{
			_log.warn("FishData: Error while creating table" + e);
		}

		int count = _fishesEasy.size() + _fishesNormal.size() + _fishesHard.size();
		_log.info("FishData: Loaded " + count + " fishes.");
	}

	/**
	 * @param lvl
	 *            The level of the fish.
	 * @param type
	 *            The type of the fish.
	 * @param group
	 *            The group of the fish.
	 * @return List of fishes that can be fished.
	 */
	public List<L2FishData> getFishes(int lvl, int type, int group)
	{
		List<L2FishData> _fishes = null;
		switch (group)
		{
			case 0:
				_fishes = _fishesEasy;
				break;

			case 1:
				_fishes = _fishesNormal;
				break;

			case 2:
				_fishes = _fishesHard;
				break;
		}

		// the fish list is empty
		if (_fishes == null)
		{
			_log.warn("Fishes are not defined !");
			return null;
		}

		List<L2FishData> result = new ArrayList<>();
		for (L2FishData f : _fishes)
		{
			if (f.getLevel() != lvl || f.getType() != type)
				continue;

			result.add(f);
		}

		if (result.isEmpty())
			_log.warn("Couldn't find any fish with lvl: " + lvl + " and type: " + type);

		return result;
	}

	private static class SingletonHolder
	{
		protected static final FishData _instance = new FishData();
	}
}
