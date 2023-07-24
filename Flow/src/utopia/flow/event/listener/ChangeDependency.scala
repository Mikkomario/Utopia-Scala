package utopia.flow.event.listener

import utopia.flow.event.model.ChangeResponse.{Continue, ContinueAnd}
import utopia.flow.event.model.{ChangeEvent, ChangeResponse}

@deprecated("Replaced with ChangeListener", "v2.2")
object ChangeDependency
{
	// OTHER    -----------------------------
	
	/**
	  * Creates a new change dependency
	  * @param before A function called before a change event (accepts change event, may return an intermediate value)
	  * @param after A function called after the change event if an intermediate value was generated
	  *              (accepts the generated intermediate value)
	  * @tparam A Type of source change event
	  * @tparam B Type of intermediate value
	  * @return A new change dependency
	  */
	def apply[A, B](before: ChangeEvent[A] => Option[B])(after: B => Unit): ChangeDependency[A] =
		new RawFunctionalChangeDependency[A]({ e => before(e).map { v => () => after(v) } })
	
	/**
	  * Creates a new change dependency that performs an action before and after the actual change event
	  * @param before A function called before the change event is fired (accepts the event)
	  * @param after A function called after the change event is fired (accepts the return value of 'before' function)
	  * @tparam A Type of changing value
	  * @return A new change dependency
	  */
	def beforeAndAfter[A, B](before: ChangeEvent[A] => B)(after: B => Unit): ChangeDependency[A] =
		new FunctionalChangeDependency[A, B](before, Some(after))
	
	/**
	  * Creates a new change dependency that only performs an action before a change event is fired
	  * @param f A function called before a change event is fired (accepts the event)
	  * @tparam A Type of changing value
	  * @return A new change dependency
	  */
	def onlyBefore[A](f: ChangeEvent[A] => Unit): ChangeDependency[A] = new FunctionalChangeDependency[A, Unit](f, None)
	
	/**
	  * Creates a new change dependency that only performs an action before a change event is fired
	  * @param f A function called before a change event is fired
	  * @return A new change dependency
	  */
	def beforeAnyChange(f: => Unit) = onlyBefore[Any] { _ => f }
	
	
	// NESTED   ----------------------------
	
	private class RawFunctionalChangeDependency[A](f: ChangeEvent[A] => Option[() => Unit]) extends ChangeDependency[A]
	{
		override def beforeChangeEvent(event: ChangeEvent[A]) = f(event)
	}
	
	private class FunctionalChangeDependency[A, B](before: ChangeEvent[A] => B, after: Option[B => Unit])
		extends ChangeDependency[A]
	{
		override def beforeChangeEvent(event: ChangeEvent[A]) =
		{
			val value = before(event)
			after.map { f => () => f(value) }
		}
	}
}

/**
  * A common trait for high-priority pointer / change dependencies which need to be updated before firing normal
  * change events
  * @author Mikko Hilpinen
  * @since 22.3.2021, v1.9
  */
@deprecated("Replaced with ChangeListener", "v2.2")
trait ChangeDependency[-A] extends ChangeListener[A]
{
	// ABSTRACT -----------------------
	
	/**
	  * This method is called before a normal change event is fired
	  * @param event The event that will be fired later
	  * @return A function that should be performed when / after the event is actually fired (optional)
	  */
	def beforeChangeEvent(event: ChangeEvent[A]): Option[() => Unit]
	
	
	// IMPLEMENTED  ------------------
	
	override def onChangeEvent(event: ChangeEvent[A]): ChangeResponse = {
		beforeChangeEvent(event) match {
			case Some(effect) => ContinueAnd(Vector(effect))
			case None => Continue
		}
	}
}
