package net.kaikk.msm.server;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Defines a console line
 * 
 * @author Kai
 *
 */
public class ConsoleLine {
	protected static final AtomicLong cid = new AtomicLong();
	protected final String line;
	protected final long time;
	protected final boolean err;
	protected final long id;
	
	public ConsoleLine(String line, boolean err) {
		this.time = System.currentTimeMillis();
		this.line = line;
		this.err = err;
		this.id = cid.getAndIncrement();
	}

	/**
	 * @return the line
	 */
	public String getLine() {
		return line;
	}

	/**
	 * @return the time this line was received
	 */
	public long getTime() {
		return time;
	}

	/**
	 * @return whether this is a line sent to System.err, usually denoting an error line.
	 */
	public boolean isErr() {
		return err;
	}

	/**
	 * This returns the line id. The line id is guaranteed to be unique while the ModularServersManager instance is running.
	 * 
	 * @return the line id
	 */
	public long getId() {
		return id;
	}
	
	/** 
	 * The line
	 * 
	 * @return the line
	 */
	@Override
	public String toString() {
		return line;
	}
}
