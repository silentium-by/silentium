/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.ai;

import silentium.gameserver.ai.CtrlIntention;
import silentium.gameserver.ai.DefaultMonsterAI;
import silentium.gameserver.model.L2Object;
import silentium.gameserver.model.L2Skill;
import silentium.gameserver.model.actor.L2Attackable;
import silentium.gameserver.model.actor.L2Character;
import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.L2Playable;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.actor.instance.L2PetInstance;
import silentium.gameserver.scripting.ScriptFile;
import silentium.gameserver.tables.SkillTable;
import silentium.gameserver.templates.skills.L2SkillType;
import silentium.gameserver.utils.Util;

import java.util.ArrayList;
import java.util.List;

public class Monastery extends DefaultMonsterAI implements ScriptFile {
	private static final int[] mobs1 = { 22124, 22125, 22126, 22127, 22129 };

	private static final int[] mobs2 = { 22134, 22135 };

	public static void onLoad() {
		new Monastery(-1, "Monastery", "Monastery", "ai");
	}

	public Monastery(final int scriptId, final String name, final String dname, final String path) {
		super(scriptId, name, dname, path);
		registerMobs(mobs1, QuestEventType.ON_AGGRO_RANGE_ENTER, QuestEventType.ON_SPAWN, QuestEventType.ON_SPELL_FINISHED);
		registerMobs(mobs2, QuestEventType.ON_SKILL_SEE);
	}

	@Override
	public String onAggroRangeEnter(final L2Npc npc, final L2PcInstance player, final boolean isPet) {
		if (!npc.isInCombat() && npc.getTarget() == null) {
			if (player.getActiveWeaponInstance() != null) {
				npc.setTarget(player);
				npc.broadcastNpcSay("Brother " + player.getName() + ", move your weapon away!!");
				switch (npc.getNpcId()) {
					case 22124:
					case 22126:
						npc.doCast(SkillTable.getInstance().getInfo(4589, 8));
						break;
					default:
						npc.setIsRunning(true);
						((L2Attackable) npc).addDamageHate(player, 0, 999);
						npc.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, player);
						break;
				}
			} else if (((L2Attackable) npc).getMostHated() == null)
				return null;
		}
		return super.onAggroRangeEnter(npc, player, isPet);
	}

	@Override
	public String onSkillSee(final L2Npc npc, final L2PcInstance caster, final L2Skill skill, final L2Object[] targets, final boolean isPet) {
		if (skill.getSkillType() == L2SkillType.AGGDAMAGE && targets.length != 0) {
			for (final L2Object obj : targets) {
				if (obj.equals(npc)) {
					npc.broadcastNpcSay("Brother " + caster.getName() + ", move your weapon away!!");
					((L2Attackable) npc).addDamageHate(caster, 0, 999);
					npc.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, caster);
					break;
				}
			}
		}
		return super.onSkillSee(npc, caster, skill, targets, isPet);
	}

	@Override
	public String onSpawn(final L2Npc npc) {
		final List<L2Playable> result = new ArrayList<>();
		for (final L2Object obj : npc.getKnownList().getKnownObjects().values()) {
			if (obj instanceof L2PcInstance || obj instanceof L2PetInstance) {
				if (Util.checkIfInRange(npc.getAggroRange(), npc, obj, true) && !((L2Character) obj).isDead())
					result.add((L2Playable) obj);
			}
		}

		if (!result.isEmpty()) {
			final Object[] characters = result.toArray();
			for (final Object obj : characters) {
				final L2PcInstance target = ((L2Playable) obj).getActingPlayer();
				if (target.getActiveWeaponInstance() != null && !npc.isInCombat() && npc.getTarget() == null) {
					npc.setTarget(target);
					npc.broadcastNpcSay("Brother " + target.getName() + ", move your weapon away!!");
					switch (npc.getNpcId()) {
						case 22124:
						case 22126:
						case 22127:
							final L2Skill skill = SkillTable.getInstance().getInfo(4589, 8);
							npc.doCast(skill);
							break;
						default:
							npc.setIsRunning(true);
							((L2Attackable) npc).addDamageHate(target, 0, 999);
							npc.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, target);
							break;
					}
				}
			}
		}
		return super.onSpawn(npc);
	}

	@Override
	public String onSpellFinished(final L2Npc npc, final L2PcInstance player, final L2Skill skill) {
		if (skill.getId() == 4589) {
			npc.setIsRunning(true);
			((L2Attackable) npc).addDamageHate(player, 0, 999);
			npc.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, player);
		}
		return super.onSpellFinished(npc, player, skill);
	}
}