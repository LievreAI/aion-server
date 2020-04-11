package com.aionemu.gameserver.controllers;

import com.aionemu.gameserver.ai.event.AIEventType;
import com.aionemu.gameserver.ai.follow.FollowStartService;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.siege.SiegeNpc;
import com.aionemu.gameserver.model.summons.UnsummonType;
import com.aionemu.gameserver.model.templates.npcskill.NpcSkillTemplates;
import com.aionemu.gameserver.services.siege.Siege;
import com.aionemu.gameserver.world.geo.GeoService;

/**
 * @author xTz
 */
public class SiegeWeaponController extends SummonController {

	private NpcSkillTemplates skills;

	public SiegeWeaponController(int npcId) {
		skills = DataManager.NPC_SKILL_DATA.getNpcSkillList(npcId);
	}

	@Override
	public void release(final UnsummonType unsummonType) {
		getMaster().getController().cancelTask(TaskId.SUMMON_FOLLOW);
		getOwner().getMoveController().abortMove();
		super.release(unsummonType);
	}

	@Override
	public void restMode() {
		getMaster().getController().cancelTask(TaskId.SUMMON_FOLLOW);
		super.restMode();
		getOwner().getAi().onCreatureEvent(AIEventType.STOP_FOLLOW_ME, getMaster());
	}

	@Override
	public void setUnkMode() {
		super.setUnkMode();
		getMaster().getController().cancelTask(TaskId.SUMMON_FOLLOW);
	}

	@Override
	public final void guardMode() {
		super.guardMode();
		getMaster().getController().cancelTask(TaskId.SUMMON_FOLLOW);
		getOwner().setTarget(getMaster());
		getOwner().getAi().onCreatureEvent(AIEventType.FOLLOW_ME, getMaster());
		getOwner().getMoveController().moveToTargetObject();
		getMaster().getController().addTask(TaskId.SUMMON_FOLLOW, FollowStartService.newFollowingToTargetCheckTask(getOwner(), getMaster()));
	}

	@Override
	public void attackMode(int targetObjId) {
		Creature target = (Creature) getOwner().getKnownList().getObject(targetObjId);
		if (target == null || !GeoService.getInstance().canSee(getOwner(), target)) {
			return;
		}
		Player master = getOwner().getMaster();
		if (master == null) {
			return;
		}
		Race masterRace = master.getRace();
		if (target.getRace() != Race.DRAKAN && target instanceof SiegeNpc && !((SiegeNpc) target).isBoss()) {
			if (masterRace == Race.ASMODIANS && target.getRace() != Race.PC_LIGHT_CASTLE_DOOR && target.getRace() != Race.DRAGON_CASTLE_DOOR
					&& target.getRace() != Race.GCHIEF_LIGHT && target.getRace() != Race.GCHIEF_DRAGON) {
				return;
			} else if (masterRace == Race.ELYOS && target.getRace() != Race.PC_DARK_CASTLE_DOOR && target.getRace() != Race.DRAGON_CASTLE_DOOR
					&& target.getRace() != Race.GCHIEF_DARK && target.getRace() != Race.GCHIEF_DRAGON) {
				return;
			}
		}
		super.attackMode(targetObjId);
		getOwner().setTarget(target);
		getOwner().getAi().onCreatureEvent(AIEventType.FOLLOW_ME, target);
		getOwner().getMoveController().moveToTargetObject();
		getMaster().getController().addTask(TaskId.SUMMON_FOLLOW, FollowStartService.newFollowingToTargetCheckTask(getOwner(), target));
	}

	@Override
	public void onDie(Creature lastAttacker) {
		getMaster().getController().cancelTask(TaskId.SUMMON_FOLLOW);
		super.onDie(lastAttacker);
	}

	public NpcSkillTemplates getNpcSkillTemplates() {
		return skills;
	}
}
