package utopia.trove.event

import scala.language.implicitConversions

object DatabaseSetupListener
{
	// IMPLICIT 	-------------------------
	
	implicit def functionToListener[U](function: DatabaseSetupEvent => U): DatabaseSetupListener =
		new FunctionalListener(function)
	
	
	// NESTED	-----------------------------
	
	private class FunctionalListener[U](f: DatabaseSetupEvent => U) extends DatabaseSetupListener
	{
		override def onDatabaseSetupEvent(event: DatabaseSetupEvent) = f(event)
	}
}

/**
  * A listener that is interested in receiving database setup events
  * @author Mikko Hilpinen
  * @since 19.9.2020, v1
  */
trait DatabaseSetupListener
{
	/**
	  * This method is called whenever a new event is generated
	  * @param event The event that was just generated
	  */
	def onDatabaseSetupEvent(event: DatabaseSetupEvent): Unit
}
