package net.kaikk.msm.event;

public enum Order {
	PRE,
	BEFORE,
	NORMAL,
	AFTER,
	POST;
	
	public final static Order[] allButPost = new Order[]{PRE, BEFORE, NORMAL, AFTER};
}
