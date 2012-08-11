/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.model.actor.instance;

import java.util.List;

import silentium.commons.utils.StringUtil;
import silentium.gameserver.data.xml.SkillTreeData;
import silentium.gameserver.model.L2Effect;
import silentium.gameserver.model.L2EnchantSkillData;
import silentium.gameserver.model.L2EnchantSkillLearn;
import silentium.gameserver.model.L2Skill;
import silentium.gameserver.model.L2SkillLearn;
import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.status.FolkStatus;
import silentium.gameserver.model.base.ClassId;
import silentium.gameserver.network.SystemMessageId;
import silentium.gameserver.network.serverpackets.AcquireSkillList;
import silentium.gameserver.network.serverpackets.ActionFailed;
import silentium.gameserver.network.serverpackets.ExEnchantSkillList;
import silentium.gameserver.network.serverpackets.NpcHtmlMessage;
import silentium.gameserver.network.serverpackets.SystemMessage;
import silentium.gameserver.skills.effects.EffectBuff;
import silentium.gameserver.skills.effects.EffectDebuff;
import silentium.gameserver.tables.SkillTable;
import silentium.gameserver.tables.SkillTable.FrequentSkill;
import silentium.gameserver.templates.chars.L2NpcTemplate;

public class L2NpcInstance extends L2Npc
{
	public L2NpcInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public FolkStatus getStatus()
	{
		return (FolkStatus) super.getStatus();
	}

	@Override
	public void initCharStatus()
	{
		setStatus(new FolkStatus(this));
	}

	@Override
	public void addEffect(L2Effect newEffect)
	{
		if (newEffect instanceof EffectDebuff || newEffect instanceof EffectBuff)
			super.addEffect(newEffect);
		else if (newEffect != null)
			newEffect.stopEffectTask();
	}

	public List<ClassId> getClassesToTeach()
	{
		return getTemplate().getTeachInfo();
	}

	/**
	 * This method displays SkillList to the player.
	 *
	 * @param player
	 *            The player who requested the method.
	 * @param npc
	 *            The L2Npc linked to the request.
	 * @param classId
	 *            The classId asked. Used to sort available skill list.
	 */
	public static void showSkillList(L2PcInstance player, L2Npc npc, ClassId classId)
	{
		if (((L2NpcInstance) npc).getClassesToTeach() == null)
		{
			NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
			final String sb = StringUtil.concat("<html><body>I cannot teach you. My class list is empty.<br>Ask admin to fix it. Need add my npcid and classes to skill_learn.sql.<br>NpcId:", String.valueOf(npc.getTemplate().getNpcId()), ", Your classId:", String.valueOf(player.getClassId().getId()), "<br></body></html>");
			html.setHtml(sb);
			player.sendPacket(html);
			return;
		}

		if (!npc.getTemplate().canTeach(classId))
		{
			NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
			html.setHtml("<html><body>I cannot teach you any skills.<br>You must find your current class teachers.</body></html>");
			player.sendPacket(html);
			return;
		}

		AcquireSkillList asl = new AcquireSkillList(AcquireSkillList.SkillType.Usual);
		boolean empty = true;

		for (L2SkillLearn sl : SkillTreeData.getInstance().getAvailableSkills(player, classId))
		{
			L2Skill sk = SkillTable.getInstance().getInfo(sl.getId(), sl.getLevel());
			if (sk == null)
				continue;

			asl.addSkill(sl.getId(), sl.getLevel(), sl.getLevel(), sl.getSpCost(), 0);
			empty = false;
		}

		if (empty)
		{
			int minlevel = SkillTreeData.getInstance().getMinLevelForNewSkill(player, classId);

			if (minlevel > 0)
				player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.DO_NOT_HAVE_FURTHER_SKILLS_TO_LEARN_S1).addNumber(minlevel));
			else
				player.sendPacket(SystemMessageId.NO_MORE_SKILLS_TO_LEARN);
		}
		else
			player.sendPacket(asl);

		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	/**
	 * This method displays EnchantSkillList to the player.
	 *
	 * @param player
	 *            The player who requested the method.
	 * @param npc
	 *            The L2Npc linked to the request.
	 * @param classId
	 *            The classId asked. Used to sort available enchant skill list.
	 */
	public static void showEnchantSkillList(L2PcInstance player, L2Npc npc, ClassId classId)
	{
		if (((L2NpcInstance) npc).getClassesToTeach() == null)
		{
			NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
			final String sb = StringUtil.concat("<html><body>I cannot teach you. My class list is empty.<br>Ask admin to fix it. Need add my npcid and classes to skill_learn.sql.<br>NpcId:", String.valueOf(npc.getTemplate().getNpcId()), ", Your classId:", String.valueOf(player.getClassId().getId()), "<br></body></html>");
			html.setHtml(sb);
			player.sendPacket(html);
			return;
		}

		if (!npc.getTemplate().canTeach(classId))
		{
			NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
			html.setHtml("<html><body>I cannot teach you any skills.<br>You must find your current class teachers. </body></html>");
			player.sendPacket(html);
			return;
		}

		if (player.getClassId().level() < 3)
		{
			NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
			html.setHtml("<html><body> You must have 3rd class change quest completed.</body></html>");
			player.sendPacket(html);
			return;
		}

		ExEnchantSkillList esl = new ExEnchantSkillList();
		boolean empty = true;

		List<L2EnchantSkillLearn> esll = SkillTreeData.getInstance().getAvailableEnchantSkills(player);
		for (L2EnchantSkillLearn skill : esll)
		{
			L2Skill sk = SkillTable.getInstance().getInfo(skill.getId(), skill.getLevel());
			if (sk == null)
				continue;

			L2EnchantSkillData data = SkillTreeData.getInstance().getEnchantSkillData(skill.getEnchant());
			if (data == null)
				continue;

			esl.addSkill(skill.getId(), skill.getLevel(), data.getCostSp(), data.getCostExp());
			empty = false;
		}

		if (empty)
		{
			player.sendPacket(SystemMessageId.THERE_IS_NO_SKILL_THAT_ENABLES_ENCHANT);

			if (player.getLevel() < 74)
				player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.DO_NOT_HAVE_FURTHER_SKILLS_TO_LEARN_S1).addNumber(74));
			else
				player.sendPacket(SystemMessageId.NO_MORE_SKILLS_TO_LEARN);
		}
		else
			player.sendPacket(esl);

		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	public void giveBlessingSupport(L2PcInstance player)
	{
		if (player == null)
			return;

		// Select the player
		setTarget(player);

		// If the player is too high level, display a message and return
		if (player.getLevel() > 39 || player.getClassId().level() >= 2)
		{
			String content = "<html><body>Newbie Guide:<br>I'm sorry, but you are not eligible to receive the protection blessing.<br1>It can only be bestowed on <font color=\"LEVEL\">characters below level 39 who have not made a seccond transfer.</font></body></html>";
			insertObjectIdAndShowChatWindow(player, content);
			return;
		}
		doCast(FrequentSkill.BLESSING_OF_PROTECTION.getSkill());
	}

	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		if (command.startsWith("SkillList"))
		{
			player.setSkillLearningClassId(player.getClassId());
			showSkillList(player, player.getCurrentFolkNPC(), player.getClassId());
		}
		else if (command.startsWith("EnchantSkillList"))
			showEnchantSkillList(player, player.getCurrentFolkNPC(), player.getClassId());
		else if (command.startsWith("GiveBlessing"))
			giveBlessingSupport(player);
		else
			super.onBypassFeedback(player, command);
	}
}