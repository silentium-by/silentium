/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.data.xml;

import gnu.trove.map.hash.TIntObjectHashMap;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import silentium.gameserver.configs.MainConfig;
import silentium.gameserver.data.xml.parsers.XMLDocumentFactory;
import silentium.gameserver.model.L2DropCategory;
import silentium.gameserver.model.L2DropData;
import silentium.gameserver.tables.ItemTable;

/**
 * This class loads herbs drop rules.
 */
public class HerbDropData
{
	private static Logger _log = LoggerFactory.getLogger(HerbDropData.class.getName());

	private final TIntObjectHashMap<List<L2DropCategory>> _herbGroups;

	public static HerbDropData getInstance()
	{
		return SingletonHolder._instance;
	}

	protected HerbDropData()
	{
		_herbGroups = new TIntObjectHashMap<>();
		restoreData();
	}

	private void restoreData()
	{
		try
		{
			File file = new File(MainConfig.DATAPACK_ROOT + "/data/xml/herbs_droplist.xml");
			Document doc = XMLDocumentFactory.getInstance().loadDocument(file);

			Node n = doc.getFirstChild();
			for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
			{
				if ("group".equalsIgnoreCase(d.getNodeName()))
				{
					NamedNodeMap attrs = d.getAttributes();
					int groupId = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());

					List<L2DropCategory> category;
					if (_herbGroups.contains(groupId))
						category = _herbGroups.get(groupId);
					else
					{
						category = new ArrayList<>();
						_herbGroups.put(groupId, category);
					}

					L2DropData dropDat = null;
					int id, chance, categoryType = 0;

					for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling())
					{
						dropDat = new L2DropData();
						if ("item".equalsIgnoreCase(cd.getNodeName()))
						{
							attrs = cd.getAttributes();
							id = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
							categoryType = Integer.parseInt(attrs.getNamedItem("category").getNodeValue());
							chance = Integer.parseInt(attrs.getNamedItem("chance").getNodeValue());

							dropDat.setItemId(id);
							dropDat.setMinDrop(1);
							dropDat.setMaxDrop(1);
							dropDat.setChance(chance);

							if (ItemTable.getInstance().getTemplate(dropDat.getItemId()) == null)
							{
								_log.warn("HerbDropData: Herb data for undefined item template! GroupId: " + groupId + ", itemId: " + dropDat.getItemId());
								continue;
							}

							boolean catExists = false;
							for (L2DropCategory cat : category)
							{
								// if the category exists, add the drop to this category.
								if (cat.getCategoryType() == categoryType)
								{
									cat.addDropData(dropDat, false);
									catExists = true;
									break;
								}
							}

							// if the category doesn't exit, create it and add the drop
							if (!catExists)
							{
								L2DropCategory cat = new L2DropCategory(categoryType);
								cat.addDropData(dropDat, false);
								category.add(cat);
							}
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			_log.warn("HerbDropData: Error while creating table: " + e);
		}
		_log.info("HerbDropData: Loaded " + _herbGroups.size() + " herbs groups.");
	}

	public List<L2DropCategory> getHerbDroplist(int groupId)
	{
		return _herbGroups.get(groupId);
	}

	private static class SingletonHolder
	{
		protected static final HerbDropData _instance = new HerbDropData();
	}
}
