package utopia.flow.view.mutable.eventful

import utopia.flow.event.model.Destiny
import utopia.flow.event.model.Destiny.{MaySeal, Sealed}
import utopia.flow.util.EitherExtensions._
import utopia.flow.util.logging.Logger
import utopia.flow.util.TryExtensions._
import utopia.flow.view.mutable.{MaybeAssignable, Switch}
import utopia.flow.view.template.MaybeSet
import utopia.flow.view.template.eventful.{AbstractMayStopChanging, Changing, ChangingWrapper}

import scala.concurrent.Future
import scala.util.Try

object MutableOnce
{
	/**
	  * @param initialValue Initial value of this pointer
	  * @param log Implicit logging implementation for change event -handling
	  * @tparam A Type of values in this pointer
	  * @return A new pointer that may be mutated once
	  */
	def apply[A](initialValue: A)(implicit log: Logger) = new MutableOnce[A](initialValue)
}

/**
  * A pointer that, besides the initial value, may only be set once
  * @author Mikko Hilpinen
  * @since 22.12.2022, v2.0
  * @tparam A Type of value held within this pointer
  */
class MutableOnce[A](initialValue: A)(implicit log: Logger)
	extends AbstractMayStopChanging[A] with EventfulPointer[A] with MaybeAssignable[A] with MaybeSet
{
	// ATTRIBUTES   -----------------------
	
	private var _setFlag: Either[Switch, SettableFlag] = Left(Switch())
	private var _value = initialValue
	
	/**
	  * @return A flag that becomes set when this item is mutated
	  */
	// Only initializes this flag when it is first called,
	// converting the previously managed Switch instance into a SettableFlag
	lazy val flag = _setFlag match {
		case Left(raw) =>
			val eventful = SettableFlag(raw.value)
			_setFlag = Right(eventful)
			eventful.view
			
		case Right(eventful) => eventful.view
	}
	/**
	  * A future that resolves when this item is mutated
	  */
	lazy val future = if (isSet) Future.successful(value) else flag.findMapFuture { if (_) Some(_value) else None }
	
	override lazy val readOnly: Changing[A] = if (isSet) this else ChangingWrapper(this)
	
	
	// COMPUTED ---------------------------
	
	private def setFlag = _setFlag.either
	
	
	// IMPLEMENTED  -----------------------
	
	override def isSet: Boolean = setFlag.value
	override def destiny: Destiny = if (isSet) Sealed else MaySeal
	
	override def value = _value
	override def value_=(newValue: A) = {
		if (isSet)
			throw new IllegalStateException("This pointer has already been set")
		else {
			val oldValue = _value
			_value = newValue
			setFlag.set()
			fireEventIfNecessary(oldValue, newValue).foreach { effect => Try { effect() }.log }
			declareChangingStopped()
		}
	}
	
	/**
	  * Assigns a new value to this item
	  * @param newValue New value to assign
	  * @throws IllegalStateException If this pointer was already set before
	  */
	@throws[IllegalStateException]("This pointer has already been set once")
	override def set(newValue: A) = value = newValue
	/**
	  * Attempts to assign a new value to this item.
	  * Will not assign the value if this pointer has already been set before.
	  * @param newValue A new value to assign
	  * @return Whether that value was assigned
	  */
	override def trySet(newValue: => A) = {
		if (isNotSet) {
			value = newValue
			true
		}
		else
			false
	}
}
