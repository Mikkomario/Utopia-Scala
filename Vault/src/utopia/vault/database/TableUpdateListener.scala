package utopia.vault.database

import utopia.vault.model.immutable.TableUpdateEvent

import scala.language.implicitConversions

object TableUpdateListener
{
	// IMPLICIT ---------------------------
	
	implicit def functionToListener(f: TableUpdateEvent => Unit): TableUpdateListener = apply(f)
	
	
	// OTHER    ---------------------------
	
	/**
	  * @param f A function called on table update events
	  * @return A listener wrapping that function
	  */
	def apply(f: TableUpdateEvent => Unit): TableUpdateListener = new FunctionalTableUpdateListener(f)
	
	
	// NESTED   ---------------------------
	
	private class FunctionalTableUpdateListener(f: TableUpdateEvent => Unit) extends TableUpdateListener
	{
		override def onTableUpdate(event: TableUpdateEvent) = f(event)
	}
}

/**
  * Common trait for classes that react to table updates
  * @author Mikko Hilpinen
  * @since 25.11.2021, v1.12
  */
trait TableUpdateListener
{
	/**
	  * Called whenever an event occurs in a listened table
	  * @param event A table event
	  */
	def onTableUpdate(event: TableUpdateEvent): Unit
}
