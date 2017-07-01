package ai.instance.illuminaryObelisk;

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import com.aionemu.gameserver.ai.AIName;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.skill.NpcSkillEntry;
import com.aionemu.gameserver.model.skill.QueuedNpcSkillEntry;
import com.aionemu.gameserver.model.templates.npcskill.QueuedNpcSkillTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.ThreadPoolManager;

import ai.SummonerAI;

/**
 * @author Estrayl
 */
@AIName("dynatoum")
public class DynatoumAI extends SummonerAI {

	private AtomicBoolean isStarted = new AtomicBoolean();
	private AtomicBoolean areEntriesRemoved = new AtomicBoolean();
	protected Future<?> despawnTask;

	@Override
	protected void handleAttack(Creature creature) {
		super.handleAttack(creature);
		if (isStarted.compareAndSet(false, true))
			scheduleDespawn(1);
		if (getLifeStats().getHpPercentage() <= 70 && areEntriesRemoved.compareAndSet(false, true))
			removeBossEntries();
	}

	protected void removeBossEntries() {
		PacketSendUtility.broadcastToMap(getOwner(), SM_SYSTEM_MESSAGE.STR_MSG_IDF5_U3_BOSS_PORTAL_DESTROY());
		getPosition().getWorldMapInstance().getNpcs(702216).stream().filter(p -> p != null).forEach(p -> p.getController().delete());
	}

	protected void scheduleDespawn(int delayInSec) {
		despawnTask = ThreadPoolManager.getInstance().schedule(() -> {
			if (!isDead()) {
				switch (delayInSec) {
					case 1:
						PacketSendUtility.broadcastToMap(getOwner(), SM_SYSTEM_MESSAGE.STR_MSG_IDF5_U3_BOSS_TIMER_01());
						scheduleDespawn(300);
						break;
					case 300:
						PacketSendUtility.broadcastToMap(getOwner(), SM_SYSTEM_MESSAGE.STR_MSG_IDF5_U3_BOSS_TIMER_02());
						scheduleDespawn(240);
						break;
					case 240:
						PacketSendUtility.broadcastToMap(getOwner(), SM_SYSTEM_MESSAGE.STR_MSG_IDF5_U3_BOSS_TIMER_03());
						scheduleDespawn(60);
						break;
					case 60:
						getOwner().getQueuedSkills().offer(new QueuedNpcSkillEntry(new QueuedNpcSkillTemplate(21534, 1, 100, 0, 3000, true)));
						break;
				}
			}
		}, delayInSec * 1000);
	}

	@Override
	public void onEndUseSkill(NpcSkillEntry usedSkill) {
		switch (usedSkill.getSkillId()) {
			case 21534:
				getOwner().getController().delete();
				break;
		}
	}

	protected void cancelDespawnTask() {
		if (despawnTask != null && !despawnTask.isCancelled())
			despawnTask.cancel(true);
	}

	@Override
	protected void handleDespawned() {
		cancelDespawnTask();
		PacketSendUtility.broadcastToMap(getOwner(), SM_SYSTEM_MESSAGE.STR_MSG_IDF5_U3_BOSS_TIMER_04());
		super.handleDespawned();
	}

	@Override
	protected void handleDied() {
		cancelDespawnTask();
		super.handleDied();
	}

	@Override
	protected void handleBackHome() {
		cancelDespawnTask();
		super.handleBackHome();
	}
}
