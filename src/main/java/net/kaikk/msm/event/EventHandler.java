package net.kaikk.msm.event;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import net.kaikk.msm.util.Tristate;

/**
 * An annotation that defines that the target method is an event handler.
 * 
 * @author Kai
 *
 */
@Target(METHOD)
@Retention(RUNTIME)
public @interface EventHandler {
	/**
	 * Defines the order between event handlers for the same event.<p>
	 * Usually, setting this to {@link Order#POST} will cause the event handler being called after the action has already happened. In this case, cancelling a cancellable event is ineffective.
	 * 
	 * @return the order
	 */
	Order order() default Order.NORMAL;
	
	/**
	 * Defines whether the event handler should be called if the cancellable event is cancelled.
	 * 
	 * FALSE: do not run if the event has been previously cancelled (default)
	 * TRUE: run only if the event has been previously cancelled
	 * UNDEFINED: run in any case
	 * @return the Tristate
	 * */
	Tristate runWhenCancelled() default Tristate.FALSE;
}
