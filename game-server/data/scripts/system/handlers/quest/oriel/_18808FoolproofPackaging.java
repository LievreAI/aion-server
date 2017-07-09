package quest.oriel;

import static com.aionemu.gameserver.model.DialogAction.*;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.AbstractQuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * @author Bobobear & Ritsu
 */
public class _18808FoolproofPackaging extends AbstractQuestHandler {

	public _18808FoolproofPackaging() {
		super(18808);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(830193).addOnQuestStart(questId);
		qe.registerQuestNpc(830193).addOnTalkEvent(questId);
		qe.registerQuestNpc(730534).addOnTalkEvent(questId);
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		int dialogActionId = env.getDialogActionId();
		int targetId = env.getTargetId();

		if (qs == null || qs.isStartable()) {
			if (targetId == 830193) {
				if (dialogActionId == QUEST_SELECT)
					return sendQuestDialog(env, 1011);
				if (dialogActionId == QUEST_ACCEPT_SIMPLE || dialogActionId == QUEST_ACCEPT_1) {
					if (giveQuestItem(env, 182213215, 1))
						return sendQuestStartDialog(env);
					else
						return true;
				} else
					return sendQuestStartDialog(env);
			}
		} else if (qs.getStatus() == QuestStatus.START) {
			int var = qs.getQuestVarById(0);
			switch (targetId) {
				case 730534:
					switch (dialogActionId) {
						case USE_OBJECT: {
							if (var == 0)
								return sendQuestDialog(env, 2375);
							return false;
						}
						case SELECT_QUEST_REWARD: {
							changeQuestStep(env, 0, 0, true);
							return sendQuestDialog(env, 5);
						}
					}

			}
		} else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 730534)
				return sendQuestEndDialog(env);
		}
		return false;
	}
}
