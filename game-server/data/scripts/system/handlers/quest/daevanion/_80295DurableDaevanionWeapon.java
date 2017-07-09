package quest.daevanion;

import static com.aionemu.gameserver.model.DialogAction.*;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.AbstractQuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

public class _80295DurableDaevanionWeapon extends AbstractQuestHandler {

	public _80295DurableDaevanionWeapon() {
		super(80295);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(831387).addOnQuestStart(questId);
		qe.registerQuestNpc(831387).addOnTalkEvent(questId);
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();

		int targetId = 0;
		if (env.getVisibleObject() instanceof Npc)
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		QuestState qs = player.getQuestStateList().getQuestState(questId);

		if (qs == null || qs.isStartable()) {
			if (targetId == 831387) {
				if (env.getDialogActionId() == QUEST_SELECT) {
					int plate = player.getEquipment().itemSetPartsEquipped(303);
					int chain = player.getEquipment().itemSetPartsEquipped(302);
					int leather = player.getEquipment().itemSetPartsEquipped(301);
					int cloth = player.getEquipment().itemSetPartsEquipped(300);
					int gunner = player.getEquipment().itemSetPartsEquipped(372);

					if (plate != 5 && chain != 5 && leather != 5 && cloth != 5 && gunner != 5)
						return sendQuestDialog(env, 1003);
					else
						return sendQuestDialog(env, 4762);
				} else
					return sendQuestStartDialog(env);
			}
		}

		if (qs == null)
			return false;

		int var = qs.getQuestVarById(0);

		if (qs.getStatus() == QuestStatus.START) {
			if (targetId == 831387) {
				switch (env.getDialogActionId()) {
					case QUEST_SELECT:
						if (var == 0)
							return sendQuestDialog(env, 1011);
						return false;
					case CHECK_USER_HAS_QUEST_ITEM:
						if (var == 0) {
							return checkQuestItems(env, 0, 1, true, 5, 0);
						}
						break;
					case SELECT2:
						if (var == 0)
							return sendQuestDialog(env, 1352);
				}
			}
			return false;
		} else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 831387) {
				return sendQuestEndDialog(env);
			}
			return false;
		}
		return false;
	}
}
