package com.Nanbin.signage.crt;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 布局编辑的撤销/重做历史（基于 JSON 快照，简单可靠；单块牌最多保留
 * {@link #MAX_HISTORY} 步）。
 *
 * <p>网页端用 JSON 深拷贝做快照，这里保持同样的思路，避免为每种编辑操作写反向操作。</p>
 */
public final class CRTSignHistory {

	public static final int MAX_HISTORY = 64;

	private final Deque<Entry> undoStack = new ArrayDeque<>();
	private final Deque<Entry> redoStack = new ArrayDeque<>();

	private static final class Entry {
		final String json;
		@Nullable
		final String label;

		Entry(String json, @Nullable String label) {
			this.json = json;
			this.label = label;
		}
	}

	/** 在一次改动**之前**记录当前快照。 */
	public void push(CRTSignLayout layout, @Nullable String label) {
		if (layout == null) {
			return;
		}
		undoStack.push(new Entry(layout.toJson(), label));
		while (undoStack.size() > MAX_HISTORY) {
			undoStack.removeLast();
		}
		redoStack.clear();
	}

	public boolean canUndo() {
		return !undoStack.isEmpty();
	}

	public boolean canRedo() {
		return !redoStack.isEmpty();
	}

	/** 撤销：把当前布局记入重做栈，返回上一份快照；没有可撤销时返回 null。 */
	@Nullable
	public CRTSignLayout undo(CRTSignLayout current) {
		final Entry entry = undoStack.poll();
		if (entry == null) {
			return null;
		}
		if (current != null) {
			redoStack.push(new Entry(current.toJson(), entry.label));
		}
		return CRTSignLayout.fromJson(entry.json);
	}

	/** 重做：返回下一份快照；没有可重做时返回 null。 */
	@Nullable
	public CRTSignLayout redo(CRTSignLayout current) {
		final Entry entry = redoStack.poll();
		if (entry == null) {
			return null;
		}
		if (current != null) {
			undoStack.push(new Entry(current.toJson(), entry.label));
		}
		return CRTSignLayout.fromJson(entry.json);
	}

	/** 最近一步操作的标签（供界面显示，可为 null）。 */
	@Nullable
	public String peekUndoLabel() {
		final Entry entry = undoStack.peek();
		return entry == null ? null : entry.label;
	}

	public int undoDepth() {
		return undoStack.size();
	}

	public void clear() {
		undoStack.clear();
		redoStack.clear();
	}
}
