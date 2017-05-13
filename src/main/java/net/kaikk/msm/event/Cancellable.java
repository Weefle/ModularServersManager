package net.kaikk.msm.event;

/**
 * An interface representing an event that can be cancelled.
 * 
 * @author Kai
 *
 */
public interface Cancellable extends Event {
	/**
	 * @return true if the event has been cancelled
	 */
	public boolean isCancelled();
	
	/**
	 * @param isCancelled whether this event has to be cancelled.
	 */
	public void setCancelled(boolean isCancelled);
}
