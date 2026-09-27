package com.Nanbin.client.ClientData;

import net.minecraft.util.math.BlockPos;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongAVLTreeSet;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * 指示牌内容的客户端剪贴板。复制时保存行数、格子数与整体排版（signIds）以及选中的线路（selectedIds），
 * 只有行数和格数都相同的指示牌才能粘贴。同时维护按方块位置区分的撤回历史。
 * 支持单行（String[]）与多行（String[][]）两种数据。
 */
public class SignClipboard {

	private static int lines = -1;
	private static int length = -1;
	private static String[][] signIds;
	private static List<LongAVLTreeSet> selectedIds;

	private static final Deque<State> UNDO_STACK = new ArrayDeque<>();

	private record State(BlockPos pos, int lines, int length, String[][] signIds, List<LongAVLTreeSet> selectedIds) {
	}

	/** 单行指示牌复制（兼容旧调用）。 */
	public static void copy(int length, String[] signIds, LongAVLTreeSet selectedIds) {
		copy(1, length, new String[][]{signIds}, single(selectedIds));
	}

	/** 多行指示牌复制。 */
	public static void copy(int lines, int length, String[][] signIds, List<LongAVLTreeSet> selectedIds) {
		SignClipboard.lines = lines;
		SignClipboard.length = length;
		SignClipboard.signIds = cloneLines(signIds);
		SignClipboard.selectedIds = cloneList(selectedIds);
	}

	/** 只有格数完全相同的指示牌才允许粘贴。 */
	public static boolean canPaste(int targetLength) {
		return canPaste(1, targetLength);
	}

	/** 只有行数和格数都完全相同的指示牌才允许粘贴。 */
	public static boolean canPaste(int targetLines, int targetLength) {
		return signIds != null && selectedIds != null && signIds.length > 0 && signIds[0] != null
				&& lines == targetLines && length == targetLength
				&& signIds.length >= targetLines && signIds[0].length >= targetLength;
	}

	public static void paste(BlockPos pos, int targetLength, String[] targetSignIds, LongAVLTreeSet targetSelectedIds) {
		paste(pos, 1, targetLength, new String[][]{targetSignIds}, single(targetSelectedIds));
	}

	public static void paste(BlockPos pos, int targetLines, int targetLength, String[][] targetSignIds, List<LongAVLTreeSet> targetSelectedIds) {
		if (!canPaste(targetLines, targetLength) || targetSignIds == null || targetSelectedIds == null) {
			return;
		}
		pushUndo(pos, targetLines, targetLength, targetSignIds, targetSelectedIds);
		final int copyLines = Math.min(targetLines, Math.min(signIds.length, targetSignIds.length));
		for (int i = 0; i < copyLines; i++) {
			final String[] sourceLine = signIds[i];
			final String[] targetLine = targetSignIds[i];
			if (sourceLine == null || targetLine == null) {
				continue;
			}
			System.arraycopy(sourceLine, 0, targetLine, 0, Math.min(targetLength, Math.min(sourceLine.length, targetLine.length)));
		}
		copySelected(targetSelectedIds, selectedIds);
	}

	public static void clear(BlockPos pos, int targetLength, String[] targetSignIds, LongAVLTreeSet targetSelectedIds) {
		clear(pos, 1, targetLength, new String[][]{targetSignIds}, single(targetSelectedIds));
	}

	public static void clear(BlockPos pos, int targetLines, int targetLength, String[][] targetSignIds, List<LongAVLTreeSet> targetSelectedIds) {
		if (targetSignIds == null) {
			return;
		}
		pushUndo(pos, targetLines, targetLength, targetSignIds, targetSelectedIds);
		final int clearLines = Math.min(targetLines, targetSignIds.length);
		for (int i = 0; i < clearLines; i++) {
			final String[] line = targetSignIds[i];
			if (line == null) {
				continue;
			}
			for (int j = 0; j < Math.min(targetLength, line.length); j++) {
				line[j] = null;
			}
			if (targetSelectedIds != null && i < targetSelectedIds.size() && targetSelectedIds.get(i) != null) {
				targetSelectedIds.get(i).clear();
			}
		}
	}

	public static boolean canUndo(BlockPos pos) {
		final State state = UNDO_STACK.peek();
		return state != null && pos != null && pos.equals(state.pos());
	}

	public static void undo(BlockPos pos, int targetLength, String[] targetSignIds, LongAVLTreeSet targetSelectedIds) {
		undo(pos, 1, targetLength, new String[][]{targetSignIds}, single(targetSelectedIds));
	}

	public static void undo(BlockPos pos, int targetLines, int targetLength, String[][] targetSignIds, List<LongAVLTreeSet> targetSelectedIds) {
		if (!canUndo(pos) || targetSignIds == null || targetSelectedIds == null) {
			return;
		}
		final State state = UNDO_STACK.pop();
		final int undoLines = Math.min(Math.min(targetLines, state.signIds().length), targetSignIds.length);
		for (int i = 0; i < undoLines; i++) {
			final String[] sourceLine = state.signIds()[i];
			final String[] targetLine = targetSignIds[i];
			if (sourceLine == null || targetLine == null) {
				continue;
			}
			final int copyLength = Math.min(Math.min(targetLength, state.length()), Math.min(sourceLine.length, targetLine.length));
			System.arraycopy(sourceLine, 0, targetLine, 0, copyLength);
		}
		copySelected(targetSelectedIds, state.selectedIds());
	}

	/**
	 * 逐项复制选中的数据集合。
	 * <p>
	 * 按「行」组织的多行指示牌（站台信息屏）与按「格」组织的单行告示牌（铁路告示牌）
	 * 共用这一份实现：两者都只是「长度相同的一串集合」，整体覆盖即可。
	 */
	private static void copySelected(List<LongAVLTreeSet> target, List<LongAVLTreeSet> source) {
		if (target == null || source == null) {
			return;
		}
		final int count = Math.min(target.size(), source.size());
		for (int i = 0; i < count; i++) {
			final LongAVLTreeSet sourceSet = source.get(i);
			final LongAVLTreeSet targetSet = target.get(i);
			if (sourceSet == null || targetSet == null) {
				continue;
			}
			targetSet.clear();
			targetSet.addAll(sourceSet);
		}
	}

	private static void pushUndo(BlockPos pos, int lines, int length, String[][] signIds, List<LongAVLTreeSet> selectedIds) {
		UNDO_STACK.push(new State(pos, lines, length, cloneLines(signIds), cloneList(selectedIds)));
	}

	private static List<LongAVLTreeSet> single(LongAVLTreeSet selectedIds) {
		final List<LongAVLTreeSet> list = new ArrayList<>(1);
		list.add(selectedIds);
		return list;
	}

	private static String[][] cloneLines(String[][] lines) {
		if (lines == null) {
			return null;
		}
		final String[][] result = new String[lines.length][];
		for (int i = 0; i < lines.length; i++) {
			result[i] = lines[i] == null ? null : lines[i].clone();
		}
		return result;
	}

	private static List<LongAVLTreeSet> cloneList(List<LongAVLTreeSet> list) {
		if (list == null) {
			return null;
		}
		final List<LongAVLTreeSet> result = new ArrayList<>(list.size());
		for (final LongAVLTreeSet set : list) {
			result.add(set == null ? null : new LongAVLTreeSet(set));
		}
		return result;
	}
}
