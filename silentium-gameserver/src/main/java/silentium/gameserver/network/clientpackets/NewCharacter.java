/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.network.clientpackets;

import silentium.gameserver.data.xml.CharTemplateData;
import silentium.gameserver.model.base.ClassId;
import silentium.gameserver.network.serverpackets.CharTemplates;

public final class NewCharacter extends L2GameClientPacket
{
	@Override
	protected void readImpl()
	{
	}

	@Override
	protected void runImpl()
	{
		CharTemplates ct = new CharTemplates();

		ct.addChar(CharTemplateData.getInstance().getTemplate(0));
		ct.addChar(CharTemplateData.getInstance().getTemplate(ClassId.fighter));
		ct.addChar(CharTemplateData.getInstance().getTemplate(ClassId.mage));
		ct.addChar(CharTemplateData.getInstance().getTemplate(ClassId.elvenFighter));
		ct.addChar(CharTemplateData.getInstance().getTemplate(ClassId.elvenMage));
		ct.addChar(CharTemplateData.getInstance().getTemplate(ClassId.darkFighter));
		ct.addChar(CharTemplateData.getInstance().getTemplate(ClassId.darkMage));
		ct.addChar(CharTemplateData.getInstance().getTemplate(ClassId.orcFighter));
		ct.addChar(CharTemplateData.getInstance().getTemplate(ClassId.orcMage));
		ct.addChar(CharTemplateData.getInstance().getTemplate(ClassId.dwarvenFighter));

		sendPacket(ct);
	}
}