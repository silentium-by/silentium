/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.skills;

import gnu.trove.map.hash.TIntObjectHashMap;

import java.io.File;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javolution.util.FastList;
import silentium.gameserver.configs.MainConfig;
import silentium.gameserver.model.L2Skill;
import silentium.gameserver.tables.SkillTable;
import silentium.gameserver.templates.item.L2Item;

/**
 * @author mkizub
 */
public class SkillsEngine
{
	protected static final Logger _log = LoggerFactory.getLogger(SkillsEngine.class.getName());

	private final List<File> _itemFiles = new FastList<>();
	private final List<File> _skillFiles = new FastList<>();

	public static SkillsEngine getInstance()
	{
		return SingletonHolder._instance;
	}

	protected SkillsEngine()
	{
		hashFiles("data/xml/items", _itemFiles);
		hashFiles("data/xml/skills", _skillFiles);
	}

	private static void hashFiles(String dirname, List<File> hash)
	{
		File dir = new File(MainConfig.DATAPACK_ROOT, dirname);
		if (!dir.exists())
		{
			_log.warn("Dir " + dir.getAbsolutePath() + " not exists");
			return;
		}
		File[] files = dir.listFiles();
		for (File f : files)
		{
			if (f.getName().endsWith(".xml") && !f.getName().startsWith("custom"))
				hash.add(f);
		}
		File customfile = new File(MainConfig.DATAPACK_ROOT, dirname + "/custom.xml");
		if (customfile.exists())
			hash.add(customfile);
	}

	public List<L2Skill> loadSkills(File file)
	{
		if (file == null)
		{
			_log.warn("Skill file not found.");
			return null;
		}
		DocumentSkill doc = new DocumentSkill(file);
		doc.parse();
		return doc.getSkills();
	}

	public void loadAllSkills(final TIntObjectHashMap<L2Skill> allSkills)
	{
		int count = 0;
		for (File file : _skillFiles)
		{
			List<L2Skill> s = loadSkills(file);
			if (s == null)
				continue;
			for (L2Skill skill : s)
			{
				allSkills.put(SkillTable.getSkillHashCode(skill), skill);
				count++;
			}
		}
		_log.info("SkillsEngine: Loaded " + count + " skill templates from XML files.");
	}

	/**
	 * Return created items
	 *
	 * @return List of {@link L2Item}
	 */
	public List<L2Item> loadItems()
	{
		List<L2Item> list = new FastList<>();
		for (File f : _itemFiles)
		{
			DocumentItem document = new DocumentItem(f);
			document.parse();
			list.addAll(document.getItemList());
		}
		return list;
	}

	private static class SingletonHolder
	{
		protected static final SkillsEngine _instance = new SkillsEngine();
	}
}
