package silentium.scripts.handlers.voiced;

import silentium.gameserver.handler.IVoicedCommandHandler;
import silentium.gameserver.model.L2Skill;
import silentium.gameserver.model.actor.L2Summon;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.tables.SkillTable;

/**
 * Голосовые команды .fbuff , .mbuff , .fullbuff, .cancel Автобафф по команде.
 *
 * @author About, Zeratyl
 */

public class VoiceBuff implements IVoicedCommandHandler {
	private final String[] _voicedCommands = { "fbuff", "mbuff", "fullbuff", "petbuff", "cancel" };

	@Override
	public boolean useVoicedCommand(final String command, final L2PcInstance activeChar, final String target) {
		if ("fbuff".equalsIgnoreCase(command)) {
			if (activeChar.isInCombat() || activeChar.isInOlympiadMode() || activeChar.isInDuel() || activeChar.isInSiege()) {
				activeChar.sendMessage("You can't use this command in PvP, Duel, Olympiad or Siege mode.");
			} else {
				activeChar.sendMessage("You get Fighter-buff complect.");
				final L2Skill fbuff01 = SkillTable.getInstance().getInfo(275, 1);
				fbuff01.getEffects(activeChar, activeChar);
				final L2Skill fbuff02 = SkillTable.getInstance().getInfo(271, 1);
				fbuff02.getEffects(activeChar, activeChar);
				final L2Skill fbuff03 = SkillTable.getInstance().getInfo(274, 1);
				fbuff03.getEffects(activeChar, activeChar);
				final L2Skill fbuff04 = SkillTable.getInstance().getInfo(264, 1);
				fbuff04.getEffects(activeChar, activeChar);
				final L2Skill fbuff05 = SkillTable.getInstance().getInfo(304, 1);
				fbuff05.getEffects(activeChar, activeChar);
				final L2Skill fbuff06 = SkillTable.getInstance().getInfo(267, 1);
				fbuff06.getEffects(activeChar, activeChar);
				final L2Skill fbuff07 = SkillTable.getInstance().getInfo(1240, 3);
				fbuff07.getEffects(activeChar, activeChar);
				final L2Skill fbuff08 = SkillTable.getInstance().getInfo(1035, 4);
				fbuff08.getEffects(activeChar, activeChar);
				final L2Skill fbuff09 = SkillTable.getInstance().getInfo(1068, 3);
				fbuff09.getEffects(activeChar, activeChar);
				final L2Skill fbuff10 = SkillTable.getInstance().getInfo(1045, 6);
				fbuff10.getEffects(activeChar, activeChar);
				final L2Skill fbuff11 = SkillTable.getInstance().getInfo(1048, 6);
				fbuff11.getEffects(activeChar, activeChar);
				final L2Skill fbuff12 = SkillTable.getInstance().getInfo(1077, 3);
				fbuff12.getEffects(activeChar, activeChar);
				final L2Skill fbuff13 = SkillTable.getInstance().getInfo(1086, 2);
				fbuff13.getEffects(activeChar, activeChar);
				final L2Skill fbuff14 = SkillTable.getInstance().getInfo(1036, 2);
				fbuff14.getEffects(activeChar, activeChar);
				final L2Skill fbuff15 = SkillTable.getInstance().getInfo(1040, 3);
				fbuff15.getEffects(activeChar, activeChar);
				final L2Skill fbuff16 = SkillTable.getInstance().getInfo(1242, 3);
				fbuff16.getEffects(activeChar, activeChar);
				final L2Skill fbuff17 = SkillTable.getInstance().getInfo(1062, 2);
				fbuff17.getEffects(activeChar, activeChar);
				final L2Skill fbuff18 = SkillTable.getInstance().getInfo(1388, 3);
				fbuff18.getEffects(activeChar, activeChar);
				final L2Skill fbuff19 = SkillTable.getInstance().getInfo(1268, 4);
				fbuff19.getEffects(activeChar, activeChar);
				final L2Skill fbuff20 = SkillTable.getInstance().getInfo(1259, 4);
				fbuff20.getEffects(activeChar, activeChar);
				final L2Skill fbuff21 = SkillTable.getInstance().getInfo(1243, 6);
				fbuff21.getEffects(activeChar, activeChar);
				final L2Skill fbuff22 = SkillTable.getInstance().getInfo(1087, 3);
				fbuff22.getEffects(activeChar, activeChar);
				final L2Skill fbuff23 = SkillTable.getInstance().getInfo(1204, 2);
				fbuff23.getEffects(activeChar, activeChar);
				final L2Skill fbuff24 = SkillTable.getInstance().getInfo(349, 1);
				fbuff24.getEffects(activeChar, activeChar);
				final L2Skill fbuff25 = SkillTable.getInstance().getInfo(364, 1);
				fbuff25.getEffects(activeChar, activeChar);
				activeChar.broadcastUserInfo();

			}
		} else if ("mbuff".equalsIgnoreCase(command)) {
			if (activeChar.isInCombat() || activeChar.isInOlympiadMode() || activeChar.isInDuel() || activeChar.isInSiege()) {
				activeChar.sendMessage("You can't use this command in PvP, Duel, Olympiad or Siege mode.");
			} else {
				activeChar.sendMessage("You get Mage-buff complect.");
				final L2Skill mbuff01 = SkillTable.getInstance().getInfo(276, 1);
				mbuff01.getEffects(activeChar, activeChar);
				final L2Skill mbuff02 = SkillTable.getInstance().getInfo(273, 1);
				mbuff02.getEffects(activeChar, activeChar);
				final L2Skill mbuff03 = SkillTable.getInstance().getInfo(264, 1);
				mbuff03.getEffects(activeChar, activeChar);
				final L2Skill mbuff04 = SkillTable.getInstance().getInfo(304, 1);
				mbuff04.getEffects(activeChar, activeChar);
				final L2Skill mbuff05 = SkillTable.getInstance().getInfo(267, 1);
				mbuff05.getEffects(activeChar, activeChar);
				final L2Skill mbuff06 = SkillTable.getInstance().getInfo(1085, 3);
				mbuff06.getEffects(activeChar, activeChar);
				final L2Skill mbuff07 = SkillTable.getInstance().getInfo(1062, 2);
				mbuff07.getEffects(activeChar, activeChar);
				final L2Skill mbuff08 = SkillTable.getInstance().getInfo(1078, 6);
				mbuff08.getEffects(activeChar, activeChar);
				final L2Skill mbuff09 = SkillTable.getInstance().getInfo(1059, 3);
				mbuff09.getEffects(activeChar, activeChar);
				final L2Skill mbuff10 = SkillTable.getInstance().getInfo(1303, 2);
				mbuff10.getEffects(activeChar, activeChar);
				final L2Skill mbuff11 = SkillTable.getInstance().getInfo(1204, 2);
				mbuff11.getEffects(activeChar, activeChar);
				final L2Skill mbuff12 = SkillTable.getInstance().getInfo(1036, 2);
				mbuff12.getEffects(activeChar, activeChar);
				final L2Skill mbuff13 = SkillTable.getInstance().getInfo(1040, 3);
				mbuff13.getEffects(activeChar, activeChar);
				final L2Skill mbuff14 = SkillTable.getInstance().getInfo(1389, 3);
				mbuff14.getEffects(activeChar, activeChar);
				final L2Skill mbuff15 = SkillTable.getInstance().getInfo(1045, 6);
				mbuff15.getEffects(activeChar, activeChar);
				final L2Skill mbuff16 = SkillTable.getInstance().getInfo(1048, 6);
				mbuff16.getEffects(activeChar, activeChar);
				final L2Skill mbuff17 = SkillTable.getInstance().getInfo(1397, 3);
				mbuff17.getEffects(activeChar, activeChar);
				final L2Skill mbuff18 = SkillTable.getInstance().getInfo(349, 1);
				mbuff18.getEffects(activeChar, activeChar);
				final L2Skill mbuff19 = SkillTable.getInstance().getInfo(363, 1);
				mbuff19.getEffects(activeChar, activeChar);
				activeChar.broadcastUserInfo();
			}
		} else if ("fullbuff".equalsIgnoreCase(command)) {
			if (activeChar.isInCombat() || activeChar.isInOlympiadMode() || activeChar.isInDuel() || activeChar.isInSiege()) {
				activeChar.sendMessage("You can't use this command in PvP, Duel, Olympiad or Siege mode.");
			} else {
				activeChar.sendMessage("You get Full-buff complect.");
				final L2Skill mbuff01 = SkillTable.getInstance().getInfo(4342, 2);
				mbuff01.getEffects(activeChar, activeChar);
				final L2Skill mbuff02 = SkillTable.getInstance().getInfo(4343, 3);
				mbuff02.getEffects(activeChar, activeChar);
				final L2Skill mbuff03 = SkillTable.getInstance().getInfo(4344, 3);
				mbuff03.getEffects(activeChar, activeChar);
				final L2Skill mbuff04 = SkillTable.getInstance().getInfo(4345, 3);
				mbuff04.getEffects(activeChar, activeChar);
				final L2Skill mbuff05 = SkillTable.getInstance().getInfo(4346, 4);
				mbuff05.getEffects(activeChar, activeChar);
				final L2Skill mbuff06 = SkillTable.getInstance().getInfo(4347, 6);
				mbuff06.getEffects(activeChar, activeChar);
				final L2Skill mbuff07 = SkillTable.getInstance().getInfo(4348, 6);
				mbuff07.getEffects(activeChar, activeChar);
				final L2Skill mbuff08 = SkillTable.getInstance().getInfo(4349, 2);
				mbuff08.getEffects(activeChar, activeChar);
				final L2Skill mbuff09 = SkillTable.getInstance().getInfo(1087, 3);
				mbuff09.getEffects(activeChar, activeChar);
				final L2Skill mbuff10 = SkillTable.getInstance().getInfo(4151, 6);
				mbuff10.getEffects(activeChar, activeChar);
				final L2Skill mbuff11 = SkillTable.getInstance().getInfo(4352, 2);
				mbuff11.getEffects(activeChar, activeChar);
				final L2Skill mbuff12 = SkillTable.getInstance().getInfo(4353, 6);
				mbuff12.getEffects(activeChar, activeChar);
				final L2Skill mbuff13 = SkillTable.getInstance().getInfo(4354, 4);
				mbuff13.getEffects(activeChar, activeChar);
				final L2Skill mbuff14 = SkillTable.getInstance().getInfo(4355, 3);
				mbuff14.getEffects(activeChar, activeChar);
				final L2Skill mbuff15 = SkillTable.getInstance().getInfo(4356, 3);
				mbuff15.getEffects(activeChar, activeChar);
				final L2Skill mbuff16 = SkillTable.getInstance().getInfo(4357, 2);
				mbuff16.getEffects(activeChar, activeChar);
				final L2Skill mbuff17 = SkillTable.getInstance().getInfo(4358, 3);
				mbuff17.getEffects(activeChar, activeChar);
				final L2Skill mbuff18 = SkillTable.getInstance().getInfo(4359, 3);
				mbuff18.getEffects(activeChar, activeChar);
				final L2Skill mbuff19 = SkillTable.getInstance().getInfo(4360, 3);
				mbuff19.getEffects(activeChar, activeChar);
				final L2Skill mbuff20 = SkillTable.getInstance().getInfo(1044, 3);
				mbuff20.getEffects(activeChar, activeChar);
				activeChar.broadcastUserInfo();
			}
		} else if ("petbuff".equalsIgnoreCase(command)) {
			final L2Summon summon = activeChar.getPet();
			if (activeChar.isInCombat() || activeChar.isInOlympiadMode() || activeChar.isInDuel() || activeChar.isInSiege()) {
				activeChar.sendMessage("You can't use this command in PvP, Duel, Olympiad or Siege mods.");
			} else if (summon != null) {
				activeChar.sendMessage("You get a Pet-buff complect.");
				final L2Skill mbuff01 = SkillTable.getInstance().getInfo(4342, 2);
				mbuff01.getEffects(activeChar.getPet(), activeChar.getPet());
				final L2Skill mbuff02 = SkillTable.getInstance().getInfo(4343, 3);
				mbuff02.getEffects(activeChar.getPet(), activeChar.getPet());
				final L2Skill mbuff03 = SkillTable.getInstance().getInfo(4344, 3);
				mbuff03.getEffects(activeChar.getPet(), activeChar.getPet());
				final L2Skill mbuff04 = SkillTable.getInstance().getInfo(4345, 3);
				mbuff04.getEffects(activeChar.getPet(), activeChar.getPet());
				final L2Skill mbuff05 = SkillTable.getInstance().getInfo(4346, 4);
				mbuff05.getEffects(activeChar.getPet(), activeChar.getPet());
				final L2Skill mbuff06 = SkillTable.getInstance().getInfo(4347, 6);
				mbuff06.getEffects(activeChar.getPet(), activeChar.getPet());
				final L2Skill mbuff07 = SkillTable.getInstance().getInfo(4348, 6);
				mbuff07.getEffects(activeChar.getPet(), activeChar.getPet());
				final L2Skill mbuff08 = SkillTable.getInstance().getInfo(4349, 2);
				mbuff08.getEffects(activeChar.getPet(), activeChar.getPet());
				final L2Skill mbuff09 = SkillTable.getInstance().getInfo(1087, 3);
				mbuff09.getEffects(activeChar.getPet(), activeChar.getPet());
				final L2Skill mbuff10 = SkillTable.getInstance().getInfo(4151, 6);
				mbuff10.getEffects(activeChar.getPet(), activeChar.getPet());
				final L2Skill mbuff11 = SkillTable.getInstance().getInfo(4352, 2);
				mbuff11.getEffects(activeChar.getPet(), activeChar.getPet());
				final L2Skill mbuff12 = SkillTable.getInstance().getInfo(4353, 6);
				mbuff12.getEffects(activeChar.getPet(), activeChar.getPet());
				final L2Skill mbuff13 = SkillTable.getInstance().getInfo(4354, 4);
				mbuff13.getEffects(activeChar.getPet(), activeChar.getPet());
				final L2Skill mbuff14 = SkillTable.getInstance().getInfo(4355, 3);
				mbuff14.getEffects(activeChar.getPet(), activeChar.getPet());
				final L2Skill mbuff15 = SkillTable.getInstance().getInfo(4356, 3);
				mbuff15.getEffects(activeChar.getPet(), activeChar.getPet());
				final L2Skill mbuff16 = SkillTable.getInstance().getInfo(4357, 2);
				mbuff16.getEffects(activeChar.getPet(), activeChar.getPet());
				final L2Skill mbuff17 = SkillTable.getInstance().getInfo(4358, 3);
				mbuff17.getEffects(activeChar.getPet(), activeChar.getPet());
				final L2Skill mbuff18 = SkillTable.getInstance().getInfo(4359, 3);
				mbuff18.getEffects(activeChar.getPet(), activeChar.getPet());
				final L2Skill mbuff19 = SkillTable.getInstance().getInfo(4360, 3);
				mbuff19.getEffects(activeChar.getPet(), activeChar.getPet());
				final L2Skill mbuff20 = SkillTable.getInstance().getInfo(1044, 3);
				mbuff20.getEffects(activeChar, activeChar);
				activeChar.broadcastUserInfo();
			} else
				activeChar.sendMessage("No pet or summon.");
		} else if ("cancel".equalsIgnoreCase(command)) {
			if (activeChar.isInCombat() || activeChar.isInOlympiadMode() || activeChar.isInDuel() || activeChar.isInSiege()) {
				activeChar.sendMessage("You can't use this command in PvP, Duel, Olympiad or Siege mode.");
			} else {
				activeChar.sendMessage("You have canceled all you buff.");
				activeChar.stopAllEffectsExceptThoseThatLastThroughDeath();
				activeChar.broadcastUserInfo();
			}
		}
		return true;
	}

	@Override
	public String[] getVoicedCommandList() {
		return _voicedCommands;
	}
}