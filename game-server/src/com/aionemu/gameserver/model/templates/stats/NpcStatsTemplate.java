package com.aionemu.gameserver.model.templates.stats;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Luno
 * @modified Estrayl
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "npc_stats_template")
public class NpcStatsTemplate extends StatsTemplate {

	@XmlAttribute(name = "matk")
	private int matk;
	@XmlAttribute(name = "maxXp")
	private int maxXp;

	@Override
	public float getGroupWalkSpeed() {
		return speeds == null ? 0 : speeds.getGroupWalkSpeed();
	}

	@Override
	public float getRunSpeedFight() {
		return speeds == null ? 0 : speeds.getRunSpeedFight();
	}

	@Override
	public float getGroupRunSpeedFight() {
		return speeds == null ? 0 : speeds.getGroupRunSpeedFight();
	}

	public int getMagicalAttack() {
		return matk;
	}
	
	public void setMagicalAttack(int matk) {
		this.matk = matk;
	}

	public int getMaxXp() {
		return maxXp;
	}
	
	public void setMaxXp(int maxXp) {
		this.maxXp = maxXp;
	}

}
