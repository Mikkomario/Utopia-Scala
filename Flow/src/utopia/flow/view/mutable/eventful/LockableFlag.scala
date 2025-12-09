package utopia.flow.view.mutable.eventful

import utopia.flow.event.model.Destiny.Sealed
import utopia.flow.util.TryExtensions._
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.eventful.{Fixed, FlagView}
import utopia.flow.view.template.eventful.{AbstractMayStopChanging, Flag}

import scala.util.Try

object LockableFlag
{
	// COMPUTED -----------------------------
	
	/**
	  * @return Access to resettable lockable flag constructors
	  */
	def resettable = ResettableLockableFlag
	
	
	// OTHER    -----------------------------
	
	/**
	  * Creates a new lockable flag
	  * @param initialState Initial state of this flag (default = false)
	  * @param log Implicit logging implementation used in event-handling
	  * @return A new flag
	  */
	def apply(initialState: Boolean = false)(implicit log: Logger): LockableFlag =
		if (initialState) new AlreadySetFlag else new _LockableFlag()
	
	
	// NESTED   -----------------------------
	
	private class _LockableFlag(implicit log: Logger) extends AbstractMayStopChanging[Boolean] with LockableFlag
	{
		// ATTRIBUTES   ---------------------
		
		private var _value = false
		private var _locked = false
		
		override lazy val view: Flag = new FlagView(this)
		
		
		// IMPLEMENTED  ---------------------
		
		override def value: Boolean = _value
		override def locked: Boolean = _locked
		
		override def toString = {
			if (_locked) {
				if (_value)
					"Flag.set.locked"
				else
					"Flag.locked"
			}
			else if (_value)
				"Flag.set.lockable"
			else
				"Flag.settable.lockable"
		}
		
		override def set(): Boolean = {
			if (locked) {
				if (isSet)
					false
				else
					throw new IllegalStateException("This flag has already been locked")
			}
			else if (isSet)
				false
			else {
				_value = true
				fireEventIfNecessary(false, true).foreach { a => Try { a() }.log }
				declareChangingStopped()
				true
			}
		}
		
		override def lock(): Unit = {
			if (!locked) {
				_locked = true
				if (isNotSet)
					declareChangingStopped()
			}
		}
	}
	
	private class AlreadySetFlag extends Fixed[Boolean] with LockableFlag
	{
		// ATTRIBUTES   ---------------------
		
		override val value: Boolean = true
		override val destiny = Sealed
		
		private var _locked = false
		
		
		// IMPLEMENTED  ---------------------
		
		override def view: Flag = this
		
		override def locked: Boolean = _locked
		
		override def toString = if (_locked) "Flag.always.set.locked" else "Flat.always.set.lockable"
		
		override def set(): Boolean = false
		override def lock(): Unit = _locked = true
		
		override protected def declareChangingStopped(): Unit = ()
	}
}

/**
  * Common trait for lockable flag implementations
  * @author Mikko Hilpinen
  * @since 30.03.2025, v2.6
  */
trait LockableFlag extends SettableFlag with Lockable[Boolean]