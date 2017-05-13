package net.kaikk.msm.event;

/**
 * A class that represents an event that can be cancelled.
 * 
 * @author Kai
 *
 */
public abstract class CancellableEvent implements Cancellable {
	protected boolean cancelled;
	
	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public void setCancelled(boolean isCancelled) {
		this.cancelled = this.isCancelled();
	}
}
