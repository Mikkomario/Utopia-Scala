package utopia.flow.view.template.eventful

import utopia.flow.collection.immutable.Empty
import utopia.flow.event.listener.ChangingStoppedListener
import utopia.flow.util.logging.Logger

/**
  * Superclass for pointers that may be listened to and which may, at some point,
  * be declared as stopped (i.e. not changing anymore)
  * @author Mikko Hilpinen
  * @since 31.8.2023, v2.2
  * @tparam A Type of changing values in this item
  */
abstract class AbstractMayStopChanging[A](implicit log: Logger) extends AbstractChanging[A] with MayStopChanging[A]
{
	// ATTRIBUTES   ------------------------
	
	private var stopListeners: Seq[ChangingStoppedListener] = Empty
	
	
	// IMPLEMENTED  -----------------------
	
	override protected def declareChangingStopped(): Unit = {
		clearListeners()
		if (stopListeners.nonEmpty) {
			stopListeners.foreach { _.onChangingStopped() }
			stopListeners = Empty
		}
	}
	
	override protected def _addChangingStoppedListener(listener: => ChangingStoppedListener): Unit =
		stopListeners :+= listener
}
