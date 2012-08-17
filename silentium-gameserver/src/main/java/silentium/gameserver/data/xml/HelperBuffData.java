/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
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
import silentium.gameserver.templates.L2HelperBuff;
import silentium.gameserver.templates.StatsSet;

/**
 * This class represents the Newbie Helper Buff list Author: Ayor
 */
public class HelperBuffData
{
	private static Logger _log = LoggerFactory.getLogger(HelperBuffData.class.getName());

	/** The table containing all Buff of the Newbie Helper */
	private final List<L2HelperBuff> _helperBuff;

	/**
	 * The player level since Newbie Helper can give the first buff <BR>
	 * Used to generate message : "Come back here when you have reached level ...")
	 */
	private int _magicClassLowestLevel = 100;
	private int _physicClassLowestLevel = 100;

	/**
	 * The player level above which Newbie Helper won't give any buff <BR>
	 * Used to generate message : "Only novice character of level ... or less can receive my support magic.")
	 */
	private int _magicClassHighestLevel = 1;
	private int _physicClassHighestLevel = 1;

	public static HelperBuffData getInstance()
	{
		return SingletonHolder._instance;
	}

	/**
	 * Create and Load the Newbie Helper Buff list from helper_buff_list.xml
	 */
	protected HelperBuffData()
	{
		_helperBuff = new ArrayList<>();

		try
		{
			File f = new File(MainConfig.DATAPACK_ROOT + "/data/xml/helper_buff_list.xml");
			Document doc = XMLDocumentFactory.getInstance().loadDocument(f);

			Node n = doc.getFirstChild();
			for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
			{
				if (d.getNodeName().equalsIgnoreCase("buff"))
				{
					int id = Integer.valueOf(d.getAttributes().getNamedItem("id").getNodeValue());
					int skill_id = Integer.valueOf(d.getAttributes().getNamedItem("skill_id").getNodeValue());
					int skill_level = Integer.valueOf(d.getAttributes().getNamedItem("skill_level").getNodeValue());
					int lower_level = Integer.valueOf(d.getAttributes().getNamedItem("lower_level").getNodeValue());
					int upper_level = Integer.valueOf(d.getAttributes().getNamedItem("upper_level").getNodeValue());
					boolean is_magic_class = Boolean.valueOf(d.getAttributes().getNamedItem("is_magic_class").getNodeValue());

					StatsSet helperBuffDat = new StatsSet();

					helperBuffDat.set("id", id);
					helperBuffDat.set("skillID", skill_id);
					helperBuffDat.set("skillLevel", skill_level);
					helperBuffDat.set("lowerLevel", lower_level);
					helperBuffDat.set("upperLevel", upper_level);
					helperBuffDat.set("isMagicClass", is_magic_class);

					if (!is_magic_class)
					{
						if (lower_level < _physicClassLowestLevel)
							_physicClassLowestLevel = lower_level;

						if (upper_level > _physicClassHighestLevel)
							_physicClassHighestLevel = upper_level;
					}
					else
					{
						if (lower_level < _magicClassLowestLevel)
							_magicClassLowestLevel = lower_level;

						if (upper_level > _magicClassHighestLevel)
							_magicClassHighestLevel = upper_level;
					}

					// Add this Helper Buff to the Helper Buff List
					_helperBuff.add(new L2HelperBuff(helperBuffDat));
				}
			}
		}
		catch (Exception e)
		{
			_log.error("HelperBuffData: Error while creating table" + e);
		}

		_log.info("HelperBuffData: Loaded " + _helperBuff.size() + " buffs.");
	}

	/**
	 * @return the Helper Buff List
	 */
	public List<L2HelperBuff> getHelperBuffData()
	{
		return _helperBuff;
	}

	/**
	 * @return Returns the magicClassHighestLevel.
	 */
	public int getMagicClassHighestLevel()
	{
		return _magicClassHighestLevel;
	}

	/**
	 * @return Returns the magicClassLowestLevel.
	 */
	public int getMagicClassLowestLevel()
	{
		return _magicClassLowestLevel;
	}

	/**
	 * @return Returns the physicClassHighestLevel.
	 */
	public int getPhysicClassHighestLevel()
	{
		return _physicClassHighestLevel;
	}

	/**
	 * @return Returns the physicClassLowestLevel.
	 */
	public int getPhysicClassLowestLevel()
	{
		return _physicClassLowestLevel;
	}

	private static class SingletonHolder
	{
		protected static final HelperBuffData _instance = new HelperBuffData();
	}
}
