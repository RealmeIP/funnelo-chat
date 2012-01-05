package cz.jabbim.android.tools;

import java.util.EventListener;

public interface OnAbortListener extends EventListener {

	public void onAbort(PressedEvent e);
}