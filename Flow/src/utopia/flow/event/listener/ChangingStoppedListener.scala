package utopia.flow.event.listener

import scala.language.implicitConversions

object ChangingStoppedListener
{
	// OTHER    ------------------------
	
	/**
	  * Converts a function into a listener
	  * @param f A function that should be called once the targeted pointer stops changing
	  * @tparam U Arbitrary result type
	  * @return A new listener based on the specified function
	  */
	implicit def apply[U](f: => U): ChangingStoppedListener = new _ChangingStoppedListener(() => f)
	
	
	// NESTED   ------------------------
	
	private class _ChangingStoppedListener(f: () => Unit) extends ChangingStoppedListener
	{
		override def onChangingStopped(): Unit = f()
	}
}

/**
  * Common trait for listeners that are interested in events where a pointer
  * stops changing.
  * @author Mikko Hilpinen
  * @since 31.8.2023, v2.2
  */
trait ChangingStoppedListener
{
	/**
	  * This method is called when the targeted pointer stops changing
	  */
	def onChangingStopped(): Unit
}
