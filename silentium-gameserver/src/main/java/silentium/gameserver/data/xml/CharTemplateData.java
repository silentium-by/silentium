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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import silentium.gameserver.configs.MainConfig;
import silentium.gameserver.data.xml.parsers.XMLDocumentFactory;
import silentium.gameserver.model.L2SkillLearn;
import silentium.gameserver.model.base.ClassId;
import silentium.gameserver.templates.StatsSet;
import silentium.gameserver.templates.chars.L2PcTemplate;

/**
 * @author Unknown, Forsaiken
 */
public class CharTemplateData
{
	private static final Logger _log = LoggerFactory.getLogger(CharTemplateData.class.getName());

	private final Map<Integer, L2PcTemplate> _templates = new HashMap<>();

	public static CharTemplateData getInstance()
	{
		return SingletonHolder._instance;
	}

	protected CharTemplateData()
	{
		final File mainDir = new File(MainConfig.DATAPACK_ROOT, "data/xml/classes");
		if (!mainDir.isDirectory())
		{
			_log.error("CharTemplateData: Main dir " + mainDir.getAbsolutePath() + " hasn't been found.");
			return;
		}

		for (final File file : mainDir.listFiles())
		{
			if (file.isFile() && file.getName().endsWith(".xml"))
				loadFileClass(file);
		}

		_log.info("CharTemplateData: Loaded " + _templates.size() + " character templates.");
		_log.info("CharTemplateData: Loaded " + SkillTreeData.getInstance().getSkillTreesSize() + " classes skills trees.");
	}

	private void loadFileClass(final File f)
	{
		try
		{
			Document doc = XMLDocumentFactory.getInstance().loadDocument(f);

			Node n = doc.getFirstChild();
			for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
			{
				if ("class".equalsIgnoreCase(d.getNodeName()))
				{
					NamedNodeMap attrs = d.getAttributes();
					StatsSet set = new StatsSet();

					final int classId = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
					final int parentId = Integer.parseInt(attrs.getNamedItem("parentId").getNodeValue());
					String items = null;

					set.set("classId", classId);

					for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling())
					{
						if ("set".equalsIgnoreCase(cd.getNodeName()))
						{
							attrs = cd.getAttributes();
							String name = attrs.getNamedItem("name").getNodeValue().trim();
							String value = attrs.getNamedItem("val").getNodeValue().trim();
							set.set(name, value);
						}
						else if ("skillTrees".equalsIgnoreCase(cd.getNodeName()))
						{
							List<L2SkillLearn> skills = new ArrayList<>();
							for (Node cb = cd.getFirstChild(); cb != null; cb = cb.getNextSibling())
							{
								L2SkillLearn skillLearn = null;
								if ("skill".equalsIgnoreCase(cb.getNodeName()))
								{
									attrs = cb.getAttributes();
									final int id = Integer.parseInt(attrs.getNamedItem("skillId").getNodeValue());
									final int lvl = Integer.parseInt(attrs.getNamedItem("skillLvl").getNodeValue());
									final int minLvl = Integer.parseInt(attrs.getNamedItem("minLvl").getNodeValue());
									final int cost = Integer.parseInt(attrs.getNamedItem("sp").getNodeValue());
									skillLearn = new L2SkillLearn(id, lvl, minLvl, cost, 0, 0);
									skills.add(skillLearn);
								}
							}
							SkillTreeData.getInstance().addSkillsToSkillTrees(skills, classId, parentId);
						}
						else if ("items".equalsIgnoreCase(cd.getNodeName()))
						{
							attrs = cd.getAttributes();
							items = attrs.getNamedItem("val").getNodeValue().trim();
						}
					}
					L2PcTemplate pcT = new L2PcTemplate(set);

					// Add items listed in "items" if class possess a filled "items" string.
					if (items != null)
					{
						String[] itemsSplit = items.split(";");
						for (String element : itemsSplit)
							pcT.addItem(Integer.parseInt(element));
					}

					_templates.put(pcT.classId.getId(), pcT);
				}
			}
		}
		catch (Exception e)
		{
			_log.warn("CharTemplateData: Error loading from file: " + f.getName(), e);
		}
	}

	public L2PcTemplate getTemplate(ClassId classId)
	{
		return _templates.get(classId.getId());
	}

	public L2PcTemplate getTemplate(int classId)
	{
		return _templates.get(classId);
	}

	public final String getClassNameById(int classId)
	{
		L2PcTemplate pcTemplate = _templates.get(classId);
		if (pcTemplate == null)
			throw new IllegalArgumentException("No template for classId: " + classId);

		return pcTemplate.className;
	}

	private static class SingletonHolder
	{
		protected static final CharTemplateData _instance = new CharTemplateData();
	}
}
