package quest.verteron;

import static com.aionemu.gameserver.model.DialogAction.*;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;

/**
 * @author Rolandas
 * @reworked vlog
 */
public class _1170HeadlessStoneStatue extends QuestHandler {

	public _1170HeadlessStoneStatue() {
		super(1170);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(730000).addOnQuestStart(questId);
		qe.registerQuestNpc(730000).addOnTalkEvent(questId);
		qe.registerQuestNpc(700033).addOnTalkEvent(questId);
		qe.registerOnMovieEndQuest(16, questId);
		qe.registerGetingItem(182200504, questId);
	}

	@Override
	public boolean onDialogEvent(final QuestEnv env) {
		Player player = env.getPlayer();
		int targetId = env.getTargetId();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		int dialogActionId = env.getDialogActionId();

		if (qs == null || qs.isStartable()) {
			if (targetId == 730000) { // Headless Stone Statue
				if (dialogActionId == QUEST_SELECT) {
					return sendQuestDialog(env, 1011);
				} else {
					return sendQuestStartDialog(env);
				}
			}
		} else if (qs.getStatus() == QuestStatus.START) {
			if (targetId == 700033) { // Head of Stone Statue
				if (dialogActionId == USE_OBJECT) {
					return giveQuestItem(env, 182200504, 1);
				}
			}
		} else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 730000) { // Headless Stone Statue
				if (dialogActionId == USE_OBJECT) {
					return sendQuestDialog(env, 1352);
				} else if (dialogActionId == SETPRO1) {
					playQuestMovie(env, 16);
					return closeDialogWindow(env);
				}
			}
		}
		return false;
	}

	@Override
	public boolean onGetItemEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs != null && qs.getStatus() == QuestStatus.START) {
			changeQuestStep(env, 0, 0, true); // reward
			return true;
		}
		return false;
	}

	@Override
	public boolean onMovieEndEvent(QuestEnv env, int movieId) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs != null && qs.getStatus() == QuestStatus.REWARD) {
			if (movieId == 16) {
				removeQuestItem(env, 182200504, 1);
				return QuestService.finishQuest(env);
			}
		}
		return false;
	}
}
