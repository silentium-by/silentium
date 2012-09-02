/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.handlers.admin;

import silentium.gameserver.handler.IAdminCommandHandler;
import silentium.gameserver.model.L2Object;
import silentium.gameserver.model.L2World;
import silentium.gameserver.model.actor.L2Character;
import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.L2Summon;
import silentium.gameserver.model.actor.instance.L2ChestInstance;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.network.SystemMessageId;
import silentium.gameserver.network.clientpackets.Say2;
import silentium.gameserver.network.serverpackets.AbstractNpcInfo.NpcInfo;
import silentium.gameserver.network.serverpackets.*;
import silentium.gameserver.tables.SkillTable;
import silentium.gameserver.utils.Broadcast;

import java.util.StringTokenizer;

/**
 * This class handles following admin commands: <li>hide = makes yourself invisible or visible <li>earthquake = causes an earthquake of a given
 * intensity and duration around you <li>gmspeed = temporary Super Haste effect. <li>para/unpara = paralyze/remove paralysis from target <li>
 * para_all/unpara_all = same as para/unpara, affects the whole world. <li>polyself/unpolyself = makes you look as a specified mob. <li>
 * changename = temporary change name <li>social = forces an L2Character instance to broadcast social action packets. <li>effect = forces an
 * L2Character instance to broadcast MSU packets. <li>abnormal = force changes over an L2Character instance's abnormal state. <li>
 * play_sound/play_sounds = Music broadcasting related commands <li>atmosphere = sky change related commands.
 */
public class AdminEffects implements IAdminCommandHandler {
	private static final String[] ADMIN_COMMANDS = { "admin_hide", "admin_earthquake", "admin_earthquake_menu", "admin_gmspeed", "admin_gmspeed_menu", "admin_unpara_all", "admin_para_all", "admin_unpara", "admin_para", "admin_unpara_all_menu", "admin_para_all_menu", "admin_unpara_menu", "admin_para_menu", "admin_changename", "admin_changename_menu", "admin_social", "admin_social_menu", "admin_effect", "admin_effect_menu", "admin_abnormal", "admin_abnormal_menu", "admin_play_sounds", "admin_play_sound", "admin_atmosphere", "admin_atmosphere_menu" };

	@Override
	public boolean useAdminCommand(final String command, final L2PcInstance activeChar) {
		final StringTokenizer st = new StringTokenizer(command);
		st.nextToken();

		if (command.startsWith("admin_hide")) {
			if (!activeChar.getAppearance().getInvisible()) {
				activeChar.getAppearance().setInvisible();
				activeChar.decayMe();
				activeChar.broadcastUserInfo();
				activeChar.spawnMe();
			} else {
				activeChar.getAppearance().setVisible();
				activeChar.broadcastUserInfo();
			}
		} else if (command.startsWith("admin_earthquake")) {
			try {
				final String val1 = st.nextToken();
				final int intensity = Integer.parseInt(val1);
				final String val2 = st.nextToken();
				final int duration = Integer.parseInt(val2);
				final Earthquake eq = new Earthquake(activeChar.getX(), activeChar.getY(), activeChar.getZ(), intensity, duration);
				activeChar.broadcastPacket(eq);
			} catch (Exception e) {
				activeChar.sendChatMessage(0, Say2.ALL, "SYS", "Use: //earthquake <intensity> <duration>");
			}
		} else if (command.startsWith("admin_atmosphere")) {
			try {
				final String type = st.nextToken();
				final String state = st.nextToken();
				adminAtmosphere(type, state, activeChar);
			} catch (Exception ex) {
			}
		} else if ("admin_play_sounds".equals(command))
			AdminHelpPage.showHelpPage(activeChar, "songs/songs.htm");
		else if (command.startsWith("admin_play_sounds")) {
			try {
				AdminHelpPage.showHelpPage(activeChar, "songs/songs" + command.substring(17) + ".htm");
			} catch (StringIndexOutOfBoundsException e) {
			}
		} else if (command.startsWith("admin_play_sound")) {
			try {
				playAdminSound(activeChar, command.substring(17));
			} catch (StringIndexOutOfBoundsException e) {
			}
		} else if (command.startsWith("admin_para") || command.startsWith("admin_para_menu")) {
			String type = "1";
			try {
				type = st.nextToken();
			} catch (Exception e) {
			}
			try {
				final L2Object target = activeChar.getTarget();
				L2Character player = null;
				if (target instanceof L2Character) {
					player = (L2Character) target;
					if ("1".equals(type))
						player.startAbnormalEffect(0x0400);
					else
						player.startAbnormalEffect(0x0800);
					player.setIsParalyzed(true);
					final StopMove sm = new StopMove(player);
					player.sendPacket(sm);
					player.broadcastPacket(sm);
				}
			} catch (Exception e) {
			}
		} else if ("admin_unpara".equals(command) || "admin_unpara_menu".equals(command)) {
			try {
				final L2Object target = activeChar.getTarget();
				L2Character player = null;
				if (target instanceof L2Character) {
					player = (L2Character) target;
					player.stopAbnormalEffect((short) 0x0400);
					player.stopAbnormalEffect((short) 0x0800);
					player.setIsParalyzed(false);
				}
			} catch (Exception e) {
			}
		} else if (command.startsWith("admin_para_all")) {
			try {
				for (final L2PcInstance player : activeChar.getKnownList().getKnownPlayers().values()) {
					if (!player.isGM()) {
						player.startAbnormalEffect(0x0400);
						player.setIsParalyzed(true);
						final StopMove sm = new StopMove(player);
						player.sendPacket(sm);
						player.broadcastPacket(sm);
					}
				}
			} catch (Exception e) {
			}
		} else if (command.startsWith("admin_unpara_all")) {
			try {
				for (final L2PcInstance player : activeChar.getKnownList().getKnownPlayers().values()) {
					player.stopAbnormalEffect(0x0400);
					player.setIsParalyzed(false);
				}
			} catch (Exception e) {
			}
		} else if (command.startsWith("admin_gmspeed")) {
			try {
				final int val = Integer.parseInt(st.nextToken());
				activeChar.stopSkillEffects(7029);
				if (val >= 1 && val <= 4)
					activeChar.doCast(SkillTable.getInstance().getInfo(7029, val));
			} catch (Exception e) {
				activeChar.sendChatMessage(0, Say2.ALL, "SYS", "Use: //gmspeed value (0-4).");
			} finally {
				activeChar.updateEffectIcons();
			}
		} else if (command.startsWith("admin_changename")) {
			try {
				final String name = st.nextToken();
				String oldName = "null";

				final L2Object target = activeChar.getTarget();
				L2Character player = null;

				if (target instanceof L2Character) {
					player = (L2Character) target;
					oldName = player.getName();
				} else {
					player = activeChar;
					oldName = activeChar.getName();
				}

				if (player instanceof L2PcInstance)
					L2World.getInstance().removeFromAllPlayers((L2PcInstance) player);

				player.setName(name);

				if (player instanceof L2PcInstance) {
					L2World.getInstance().addVisibleObject(player, null);
					((L2PcInstance) player).broadcastUserInfo();
				} else if (player instanceof L2Npc)
					player.broadcastPacket(new NpcInfo((L2Npc) player, null));

				activeChar.sendChatMessage(0, Say2.ALL, "SYS", "Changed name from " + oldName + " to " + name + '.');
			} catch (Exception e) {
			}
		} else if (command.startsWith("admin_social")) {
			try {
				String target = null;
				L2Object obj = activeChar.getTarget();
				if (st.countTokens() == 2) {
					final int social = Integer.parseInt(st.nextToken());
					target = st.nextToken();
					if (target != null) {
						final L2PcInstance player = L2World.getInstance().getPlayer(target);
						if (player != null) {
							if (performSocial(social, player, activeChar))
								activeChar.sendChatMessage(0, Say2.ALL, "SYS", player.getName() + " was affected by your request.");
						} else {
							try {
								final int radius = Integer.parseInt(target);
								for (final L2Object object : activeChar.getKnownList().getKnownObjects().values())
									if (activeChar.isInsideRadius(object, radius, false, false))
										performSocial(social, object, activeChar);
								activeChar.sendChatMessage(0, Say2.ALL, "SYS", radius + " units radius affected by your request.");
							} catch (NumberFormatException nbe) {
								activeChar.sendChatMessage(0, Say2.ALL, "SYS", "Incorrect parameter");
							}
						}
					}
				} else if (st.countTokens() == 1) {
					final int social = Integer.parseInt(st.nextToken());
					if (obj == null)
						obj = activeChar;
					if (performSocial(social, obj, activeChar))
						activeChar.sendChatMessage(0, Say2.ALL, "SYS", obj.getName() + " was affected by your request.");
					else
						activeChar.sendPacket(SystemMessageId.NOTHING_HAPPENED);
				} else if (!command.contains("menu"))
					activeChar.sendChatMessage(0, Say2.ALL, "SYS", "Usage: //social <social_id> [player_name|radius]");
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (command.startsWith("admin_abnormal")) {
			try {
				String target = null;
				L2Object obj = activeChar.getTarget();
				if (st.countTokens() == 2) {
					final String parm = st.nextToken();
					final int abnormal = Integer.decode("0x" + parm);
					target = st.nextToken();
					if (target != null) {
						final L2PcInstance player = L2World.getInstance().getPlayer(target);
						if (player != null) {
							if (performAbnormal(abnormal, player))
								activeChar.sendChatMessage(0, Say2.ALL, "SYS", player.getName() + "'s abnormal status was affected by your request.");
							else
								activeChar.sendPacket(SystemMessageId.NOTHING_HAPPENED);
						} else {
							try {
								final int radius = Integer.parseInt(target);
								for (final L2Object object : activeChar.getKnownList().getKnownObjects().values())
									if (activeChar.isInsideRadius(object, radius, false, false))
										performAbnormal(abnormal, object);
								activeChar.sendChatMessage(0, Say2.ALL, "SYS", radius + " units radius affected by your request.");
							} catch (NumberFormatException nbe) {
								activeChar.sendChatMessage(0, Say2.ALL, "SYS", "Usage: //abnormal <hex_abnormal_mask> [player|radius]");
							}
						}
					}
				} else if (st.countTokens() == 1) {
					final int abnormal = Integer.decode("0x" + st.nextToken());
					if (obj == null)
						obj = activeChar;
					if (performAbnormal(abnormal, obj))
						activeChar.sendChatMessage(0, Say2.ALL, "SYS", obj.getName() + "'s abnormal status was affected by your request.");
					else
						activeChar.sendPacket(SystemMessageId.NOTHING_HAPPENED);
				} else if (!command.contains("menu"))
					activeChar.sendChatMessage(0, Say2.ALL, "SYS", "Usage: //abnormal <abnormal_mask> [player_name|radius]");
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (command.startsWith("admin_effect")) {
			try {
				L2Object obj = activeChar.getTarget();
				int level = 1, hittime = 1;
				final int skill = Integer.parseInt(st.nextToken());

				if (st.hasMoreTokens())
					level = Integer.parseInt(st.nextToken());
				if (st.hasMoreTokens())
					hittime = Integer.parseInt(st.nextToken());
				if (obj == null)
					obj = activeChar;
				if (!(obj instanceof L2Character))
					activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
				else {
					final L2Character target = (L2Character) obj;
					target.broadcastPacket(new MagicSkillUse(target, activeChar, skill, level, hittime, 0));
					activeChar.sendChatMessage(0, Say2.ALL, "SYS", obj.getName() + " performs MSU " + skill + '/' + level + " by your request.");
				}
			} catch (Exception e) {
				activeChar.sendChatMessage(0, Say2.ALL, "SYS", "Usage: //effect skill [level | level hittime]");
			}
		}
		if (command.contains("menu"))
			showMainPage(activeChar, command);
		return true;
	}

	/**
	 * @param action bitmask that should be applied over target's abnormal
	 * @param target
	 * @return <i>true</i> if target's abnormal state was affected , <i>false</i> otherwise.
	 */
	private static boolean performAbnormal(final int action, final L2Object target) {
		if (target instanceof L2Character) {
			final L2Character character = (L2Character) target;
			if ((character.getAbnormalEffect() & action) == action)
				character.stopAbnormalEffect(action);
			else
				character.startAbnormalEffect(action);
			return true;
		}
		return false;
	}

	private static boolean performSocial(final int action, final L2Object target, final L2PcInstance activeChar) {
		try {
			if (target instanceof L2Character) {
				if (target instanceof L2Summon || target instanceof L2ChestInstance) {
					activeChar.sendPacket(SystemMessageId.NOTHING_HAPPENED);
					return false;
				}
				if (target instanceof L2Npc && (action < 1 || action > 3)) {
					activeChar.sendPacket(SystemMessageId.NOTHING_HAPPENED);
					return false;
				}
				if (target instanceof L2PcInstance && (action < 2 || action > 16)) {
					activeChar.sendPacket(SystemMessageId.NOTHING_HAPPENED);
					return false;
				}
				final L2Character character = (L2Character) target;
				character.broadcastPacket(new SocialAction(character, action));
			} else
				return false;
		} catch (Exception e) {
		}

		return true;
	}

	private static void adminAtmosphere(final String type, final String state, final L2PcInstance activeChar) {
		L2GameServerPacket packet = null;

		switch (type) {
			case "signsky":
				if ("dawn".equals(state))
					packet = new SignsSky(2);
				else if ("dusk".equals(state))
					packet = new SignsSky(1);
				break;
			case "sky":
				switch (state) {
					case "night":
						packet = SunSet.STATIC_PACKET;
						break;
					case "day":
						packet = SunRise.STATIC_PACKET;
						break;
					case "red":
						packet = new ExRedSky(10);
						break;
				}
				break;
			default:
				activeChar.sendChatMessage(0, Say2.ALL, "SYS", "Usage: //atmosphere <signsky dawn|dusk>|<sky day|night|red>");
				break;
		}
		if (packet != null)
			Broadcast.toAllOnlinePlayers(packet);
	}

	private static void playAdminSound(final L2PcInstance activeChar, final String sound) {
		final PlaySound _snd;

		_snd = sound.contains(".") ? new PlaySound(sound) : new PlaySound(1, sound, 0, 0, 0, 0, 0);

		activeChar.sendPacket(_snd);
		activeChar.broadcastPacket(_snd);
		activeChar.sendChatMessage(0, Say2.ALL, "SYS", "Playing " + sound + '.');
	}

	@Override
	public String[] getAdminCommandList() {
		return ADMIN_COMMANDS;
	}

	private static void showMainPage(final L2PcInstance activeChar, final String command) {
		String filename = "effects_menu";
		if (command.contains("abnormal"))
			filename = "abnormal";
		else if (command.contains("social"))
			filename = "social";
		AdminHelpPage.showHelpPage(activeChar, filename + ".htm");
	}
}