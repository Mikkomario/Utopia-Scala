package utopia.vault.database

import utopia.flow.async.context.ActionQueue
import utopia.flow.collection.mutable.VolatileList
import utopia.vault.model.immutable.{Table, TableUpdateEvent}

import scala.concurrent.ExecutionContext

/**
  * Used for delivering events about database changes
  * @author Mikko Hilpinen
  * @since 25.11.2021, v1.12
  */
object Triggers
{
	// ATTRIBUTES   -------------------------------
	
	private var queue: Option[ActionQueue] = None
	
	private val globalListeners = VolatileList[TableUpdateListener]()
	private val databaseListeners = Volatile(Map[String, Vector[TableUpdateListener]]())
	private val tableListeners = Volatile(Map[(String, String), Vector[TableUpdateListener]]())
	
	
	// OTHER    ------------------------------------
	
	/**
	  * Provides an execution context for this object to use, enabling asynchronous event handling
	  * @param exc Execution context to use in event handling
	  */
	def specifyExecutionContext(exc: ExecutionContext) = queue = Some(new ActionQueue()(exc))
	
	/**
	  * Delivers an event to be distributed to all interested listeners. The operation is performed asynchronously if
	  * an execution context has been specified for this object. Otherwise the event distribution blocks.
	  * @param event Event to distribute
	  */
	def deliver(databaseName: String, event: TableUpdateEvent): Unit = {
		val listeners = listenersFor(databaseName, event.tableName)
		if (listeners.nonEmpty) {
			// May deliver the event asynchronously, if an execution context has been specified
			queue match {
				case Some(queue) => queue.push { listeners.foreach { _.onTableUpdate(event) } }
				case None => listeners.foreach { _.onTableUpdate(event) }
			}
		}
	}
	
	/**
	  * Adds a new listener to be informed of future events
	  * @param listener Listener to be informed
	  */
	def addListener(listener: TableUpdateListener) = globalListeners :+= listener
	/**
	  * Adds a new listener to be informed of future events in a specific database
	  * @param databaseName Name of the targeted database
	  * @param listener A listener to inform
	  */
	def addDatabaseListener(databaseName: String)(listener: TableUpdateListener) =
	{
		val lowerName = databaseName.toLowerCase
		databaseListeners.update { old => old + (lowerName -> (old.getOrElse(lowerName, Vector()) :+ listener)) }
	}
	/**
	  * Adds a new listener to be informed of future events concerning a specific table
	  * @param databaseName Name of the targeted database
	  * @param tableName Name of the targeted table
	  * @param listener Listener to inform
	  */
	def addTableListener(databaseName: String, tableName: String)(listener: TableUpdateListener) = {
		val namePair = databaseName.toLowerCase -> tableName.toLowerCase
		tableListeners.update { old => old + (namePair -> (old.getOrElse(namePair, Vector()) :+ listener)) }
	}
	/**
	  * Adds a new listener to be informed of future events concerning a specific table
	  * @param table Table to listen to
	  * @param listener Listener to inform
	  */
	def addTableListener(table: Table)(listener: TableUpdateListener): Unit =
		addTableListener(table.databaseName, table.name)(listener)
	
	/**
	  * Removes a listener so that it won't be informed about future events
	  * @param listener Listener to remove
	  */
	def removeListener(listener: Any) = {
		globalListeners -= listener
		databaseListeners.update { _.view.mapValues { _.filterNot { _ == listener } }.toMap }
		tableListeners.update { _.view.mapValues { _.filterNot { _ == listener } }.toMap }
	}
	
	private def listenersFor(databaseName: String, tableName: String) =
		globalListeners.value ++ databaseListeners.value.getOrElse(databaseName.toLowerCase, Vector()) ++
			tableListeners.value.getOrElse(databaseName.toLowerCase -> tableName.toLowerCase, Vector())
}
