package com.aionemu.gameserver.dataholders;

import java.util.List;

import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;
import com.aionemu.gameserver.model.templates.teleport.TeleporterTemplate;

import gnu.trove.map.hash.TIntObjectHashMap;

/**
 * This is a container holding and serving all {@link NpcTemplate} instances.<br>
 * Briefly: Every {@link Npc} instance represents some class of NPCs among which each have the same id, name, items, statistics. Data for such NPC
 * class is defined in {@link NpcTemplate} and is uniquely identified by npc id.
 * 
 * @author orz
 */
@XmlRootElement(name = "npc_teleporter")
@XmlAccessorType(XmlAccessType.FIELD)
public class TeleporterData {

	@XmlElement(name = "teleporter_template")
	private List<TeleporterTemplate> templates;

	/** A map containing all trade list templates */
	private TIntObjectHashMap<TeleporterTemplate> teleporterTemplates = new TIntObjectHashMap<>();

	void afterUnmarshal(Unmarshaller u, Object parent) {
		for (TeleporterTemplate template : templates) {
			teleporterTemplates.put(template.getTeleportId(), template);
		}
		templates = null;
	}

	public int size() {
		return teleporterTemplates.size();
	}

	public TeleporterTemplate getTeleporterTemplateByNpcId(int npcId) {
		for (TeleporterTemplate template : teleporterTemplates.valueCollection()) {
			if (template.containNpc(npcId)) {
				return template;
			}
		}
		return null;
	}

	public TeleporterTemplate getTeleporterTemplateByTeleportId(int teleportId) {
		return teleporterTemplates.get(teleportId);
	}
}
