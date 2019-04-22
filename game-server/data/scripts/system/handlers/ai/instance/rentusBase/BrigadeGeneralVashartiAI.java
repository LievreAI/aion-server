package ai.instance.rentusBase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.ai.AIName;
import com.aionemu.gameserver.ai.AIState;
import com.aionemu.gameserver.ai.manager.EmoteManager;
import com.aionemu.gameserver.ai.manager.WalkManager;
import com.aionemu.gameserver.model.EmotionType;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.state.CreatureState;
import com.aionemu.gameserver.model.geometry.Point3D;
import com.aionemu.gameserver.model.skill.QueuedNpcSkillEntry;
import com.aionemu.gameserver.model.templates.npcskill.QueuedNpcSkillTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.PositionUtil;
import com.aionemu.gameserver.utils.ThreadPoolManager;
import com.aionemu.gameserver.world.WorldMapInstance;

import ai.AggressiveNpcAI;

/**
 * @author xTz
 * @modified Yeats, Estrayl
 */
@AIName("brigade_general_vasharti")
public class BrigadeGeneralVashartiAI extends AggressiveNpcAI {

	private List<Integer> percents = new ArrayList<>();
	private AtomicBoolean isHome = new AtomicBoolean(true);
	private AtomicBoolean isInFlameShowerEvent = new AtomicBoolean();
	private Future<?> enrageSchedule, flameShieldBuffSchedule, seaOfFireSpawnTask;

	public BrigadeGeneralVashartiAI(Npc owner) {
		super(owner);
	}

	@Override
	protected void handleAttack(Creature creature) {
		super.handleAttack(creature);
		if (isHome.compareAndSet(true, false)) {
			getPosition().getWorldMapInstance().getDoors().get(70).setOpen(false);
			enrageSchedule = ThreadPoolManager.getInstance().schedule(this::handleEnrageEvent, 10, TimeUnit.MINUTES);
			scheduleFlameShieldBuffEvent(5000);
		}
		checkPercentage(getLifeStats().getHpPercentage());
	}

	private synchronized void checkPercentage(int hpPercentage) {
		if (isInFlameShowerEvent.get())
			return;
		for (Integer percent : percents) {
			if (hpPercentage <= percent) {
				percents.remove(percent);
				cancelTasks(flameShieldBuffSchedule);
				getOwner().getQueuedSkills().clear();
				getOwner().getQueuedSkills().offer(new QueuedNpcSkillEntry(new QueuedNpcSkillTemplate(20532, 1, 100, 0, 10000))); // off (skill name)
				break;
			}
		}
	}

	private void scheduleFlameShieldBuffEvent(int delay) {
		flameShieldBuffSchedule = ThreadPoolManager.getInstance().schedule(() -> {
			getOwner().getQueuedSkills().offer(new QueuedNpcSkillEntry(new QueuedNpcSkillTemplate(Rnd.get(0, 1) == 0 ? 20530 : 20531, 60, 100)));
		}, delay);
	}

	private void handleEnrageEvent() {
		getOwner().getQueuedSkills().clear();
		getOwner().getQueuedSkills().offer(new QueuedNpcSkillEntry(new QueuedNpcSkillTemplate(19962, 1, 100, 0, 15000))); // Purple Flame Weapon
		getOwner().getQueuedSkills().offer(new QueuedNpcSkillEntry(new QueuedNpcSkillTemplate(19907, 1, 100, 0, 0))); // Chastise
	}

	private void handleSeaOfFireEvent() {
		int percent = getLifeStats().getHpPercentage();
		int npcId = percent <= 70 ? percent <= 40 ? 283012 : 283011 : 283010;

		spawn(npcId, 188.33f, 414.61f, 260.61f, (byte) 244); // FX
		spawn(283007, 188.33f, 414.61f, 260.61f, (byte) 0); // de-buff

		seaOfFireSpawnTask = ThreadPoolManager.getInstance().scheduleAtFixedRate(() -> {
			int smashCount = (npcId - 283007) * 5 + 1; // 15, 20, 25
			for (int i = 2; i < smashCount; i++) {
				Point3D p = getRndPos();
				spawn(i % 2 == 0 ? 283008 : 283009, p.getX(), p.getY(), p.getZ(), (byte) 0);
			}
		}, 750, 7000);
	}

	@Override
	public void onStartUseSkill(SkillTemplate skillTemplate) {
		switch (skillTemplate.getSkillId()) {
			case 20534:
				handleSeaOfFireEvent();
				break;
		}
	}

	@Override
	public void onEndUseSkill(SkillTemplate skillTemplate) {
		switch (skillTemplate.getSkillId()) {
			case 19907: // repeat until reset
				getOwner().getQueuedSkills().offer(new QueuedNpcSkillEntry(new QueuedNpcSkillTemplate(19907, 1, 100, 0, 0))); // Chastise
				break;
			case 20530:
			case 20531:
				WorldMapInstance instance = getPosition().getWorldMapInstance();
				if (instance != null) {
					if (instance.getNpc(283000) == null)
						spawn(283000, 171.330f, 417.57f, 261f, (byte) 116);
					if (instance.getNpc(283001) == null)
						spawn(283001, 205.280f, 410.53f, 261f, (byte) 56);
				}
				scheduleFlameShieldBuffEvent(33000);
				break;
			case 20532:
				EmoteManager.emoteStopAttacking(getOwner());
				getOwner().getQueuedSkills().clear();
				ThreadPoolManager.getInstance().schedule(() -> {
					WalkManager.startForcedWalking(this, 188.17f, 414.06f, 260.75488f);
					getOwner().setState(CreatureState.ACTIVE, true);
					PacketSendUtility.broadcastPacket(getOwner(), new SM_EMOTION(getOwner(), EmotionType.START_EMOTE2, 0, getObjectId()));
				}, 800);
				break;
			case 20533:
				SkillEngine.getInstance().getSkill(getOwner(), 20534, 1, getOwner()).useSkill(); // Sea of Fire
				break;
		}

	}

	@Override
	public void onEffectEnd(Effect effect) {
		if (effect != null && effect.getSkillId() == 20534 && isInFlameShowerEvent.compareAndSet(true, false)) {
			cancelTasks(seaOfFireSpawnTask);
			scheduleFlameShieldBuffEvent(10000);
		}
	}

	@Override
	protected boolean isDestinationReached() {
		if (getState() == AIState.FORCED_WALKING && PositionUtil.getDistance(getOwner().getX(), getOwner().getY(), 188.17f, 414.06f) <= 1f
			&& isInFlameShowerEvent.compareAndSet(false, true)) {
			SkillEngine.getInstance().getSkill(getOwner(), 20533, 1, getOwner()).useSkill(); // off (skill name)
		}
		return super.isDestinationReached();
	}

	private Point3D getRndPos() {
		double radian = Math.toRadians(Rnd.get(1, 360));
		float distance = Rnd.get() * 29f;
		float x1 = (float) (Math.cos(Math.PI * radian) * distance);
		float y1 = (float) (Math.sin(Math.PI * radian) * distance);
		return new Point3D(getOwner().getSpawn().getX() + x1, getOwner().getSpawn().getY() + y1, getOwner().getSpawn().getZ());
	}

	private void clearSpawns() {
		WorldMapInstance instance = getPosition().getWorldMapInstance();
		if (instance != null) {
			deleteNpcs(instance.getNpcs(283002));
			deleteNpcs(instance.getNpcs(283003));
			deleteNpcs(instance.getNpcs(283004));
			deleteNpcs(instance.getNpcs(283005));
			deleteNpcs(instance.getNpcs(283006));
			deleteNpcs(instance.getNpcs(283007));
			deleteNpcs(instance.getNpcs(283010));
			deleteNpcs(instance.getNpcs(283011));
			deleteNpcs(instance.getNpcs(283012));
			deleteNpcs(instance.getNpcs(283000));
			deleteNpcs(instance.getNpcs(283001));
		}
	}

	private void deleteNpcs(List<Npc> npcs) {
		npcs.stream().filter(npc -> npc != null).forEach(npc -> npc.getController().delete());
	}

	private void addPercent() {
		percents.clear();
		Collections.addAll(percents, 75, 50, 25, 10);
	}

	private void cancelTasks(Future<?>... tasks) {
		for (Future<?> task : tasks)
			if (task != null && !task.isCancelled())
				task.cancel(true);
	}

	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		addPercent();
	}

	@Override
	protected void handleDespawned() {
		cancelTasks(enrageSchedule, flameShieldBuffSchedule, seaOfFireSpawnTask);
		clearSpawns();
		super.handleDespawned();
	}

	@Override
	protected void handleBackHome() {
		addPercent();
		isHome.set(true);
		getPosition().getWorldMapInstance().getDoors().get(70).setOpen(true);
		cancelTasks(enrageSchedule, flameShieldBuffSchedule, seaOfFireSpawnTask);
		clearSpawns();
		super.handleBackHome();
	}

	@Override
	protected void handleDied() {
		getPosition().getWorldMapInstance().getDoors().get(70).setOpen(true);
		cancelTasks(enrageSchedule, flameShieldBuffSchedule, seaOfFireSpawnTask);
		PacketSendUtility.broadcastMessage(getOwner(), 1500410);
		clearSpawns();
		super.handleDied();
	}
}
