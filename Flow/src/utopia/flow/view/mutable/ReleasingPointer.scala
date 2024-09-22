package utopia.flow.view.mutable

import utopia.flow.async.process.ShutdownReaction.Cancel
import utopia.flow.async.process.WaitTarget.WaitDuration
import utopia.flow.async.process.{DelayedProcess, Process}
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.EitherExtensions._
import utopia.flow.util.logging.{Logger, SysErrLogger}
import utopia.flow.view.mutable.async.Volatile

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.ref.WeakReference

object ReleasingPointer
{
	/**
	  * Creates a new pointer that releases its contents after a while
	  * @param initialValue Initially held value (optional)
	  * @param referenceDuration A function for calculating the duration of strong referencing for each value
	  * @param exc Implicit execution context
	  * @tparam A Type of held value
	  * @return A new pointer
	  */
	def apply[A <: AnyRef](initialValue: Option[A] = None)(referenceDuration: A => Duration)
	                      (implicit exc: ExecutionContext) =
		new ReleasingPointer[A](initialValue)(referenceDuration)
	/**
	  * Creates a new pointer that releases its contents after a while
	  * @param initialValue      Initially held value
	  * @param referenceDuration A function for calculating the duration of strong referencing for each value
	  * @param exc               Implicit execution context
	  * @tparam A Type of held value
	  * @return A new pointer
	  */
	def defined[A <: AnyRef](initialValue: A)(referenceDuration: A => Duration)(implicit exc: ExecutionContext) =
		apply[A](Some(initialValue))(referenceDuration)
	/**
	  * Creates a new pointer that releases its contents after a while.
	  * This pointer starts out as empty.
	  * @param referenceDuration A function for calculating the duration of strong referencing for each value
	  * @param exc               Implicit execution context
	  * @tparam A Type of held value
	  * @return A new pointer
	  */
	def empty[A <: AnyRef](referenceDuration: A => Duration)(implicit exc: ExecutionContext) =
		apply[A](None)(referenceDuration)
	
	/**
	  * Creates a new pointer that releases its contents after a while
	  * @param referenceDuration Duration how long the values should be held
	  * @param initialValue      Initially held value (optional)
	  * @param exc               Implicit execution context
	  * @tparam A Type of held value
	  * @return A new pointer
	  */
	def after[A <: AnyRef](referenceDuration: Duration, initialValue: Option[A] = None)(implicit exc: ExecutionContext) =
		apply[A](initialValue) { _ => referenceDuration }
}

/**
  * A mutable pointer that "releases" its contents after a time.
  * Releasing, in this context, means that this pointer will only hold a weak reference to its contents,
  * meaning that it may get cleared.
  * @author Mikko Hilpinen
  * @since 17.11.2022, v2.0
  */
class ReleasingPointer[A <: AnyRef](initialValue: Option[A] = None)(referenceDuration: A => Duration)
                         (implicit exc: ExecutionContext)
	extends Pointer[Option[A]] with Resettable
{
	// ATTRIBUTES   ----------------------
	
	private implicit val log: Logger = SysErrLogger
	
	// Contains:
	//      1) Held value (weak or strong)
	//      2) Release process (running)
	private val pointer = Volatile.optional[(Either[WeakReference[A], A], Option[Process])]()
	
	
	// INITIAL CODE ----------------------
	
	initialValue.foreach(set)
	
	
	// IMPLEMENTED  ----------------------
	
	override def isSet: Boolean = pointer.value.exists { case (current, _) =>
		current match {
			case Left(weak) => weak.get.isDefined
			case Right(_) => true
		}
	}
	
	override def value = pointer.value.flatMap { case (current, _) =>
		current match {
			case Left(weak) => weak.get
			case Right(v) => Some(v)
		}
	}
	override def value_=(newValue: Option[A]) = newValue match {
		// Case: Setting a non-empty value
		case Some(v) =>
			// Calculates the duration how long the value should be held in memory
			val duration = referenceDuration(v)
			pointer.update { earlier =>
				// Cancels the earlier release process, if pending
				earlier.foreach { _._2.foreach { _.stopIfRunning() } }
				// Case: The item shouldn't be held in memory => Releases immediately
				if (duration <= Duration.Zero)
					Some(Left(WeakReference(v)) -> None)
				else
					duration.finite match {
						// Case: Item should be held for a while => Schedules a release process
						case Some(duration) => Some(Right(v) -> Some(releaseProcess(duration)))
						// Case: Item should be held indefinitely => Doesn't schedule a release
						case None => Some(Right(v) -> None)
					}
			}
		// Case: Resetting this pointer
		case None => pointer.pop().foreach { _._2.foreach { _.stopIfRunning() } }
	}
	// Commented out 13.11.2023 because of a name clash introduced in Scala 2.13.12
	// def value_=(newValue: A): Unit = this.value = Some(newValue)
	
	override def reset() = {
		val current = pointer.pop()
		current.foreach { _._2.foreach { _.stopIfRunning() } }
		current.isDefined
	}
	
	
	// OTHER    ---------------------
	
	/**
	  * Assigns a new concrete value to this pointer
	  * @param value The value to assign
	  */
	def set(value: A) = this.value = Some(value)
	
	private def releaseProcess(after: FiniteDuration) = {
		val p = DelayedProcess(WaitDuration(after), shutdownReaction = Some(Cancel), isRestartable = false) { _ =>
			// Updates the pointer to only contain a weak reference to the held value
			pointer.update { _.map { case (v, _) =>
				val weakValue = v.leftOrMap { WeakReference(_) }
				Left(weakValue) -> None
			} }
		}
		p.runAsync()
		p
	}
}
