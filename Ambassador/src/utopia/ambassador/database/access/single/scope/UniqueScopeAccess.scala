package utopia.ambassador.database.access.single.scope

import java.time.Instant
import utopia.ambassador.database.factory.scope.ScopeFactory
import utopia.ambassador.database.model.scope.ScopeModel
import utopia.ambassador.model.stored.scope.Scope
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.access.template.model.DistinctModelAccess
import utopia.vault.nosql.template.Indexed

/**
  * A common trait for access points that return individual and distinct Scopes.
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
trait UniqueScopeAccess 
	extends SingleRowModelAccess[Scope] with DistinctModelAccess[Scope, Option[Scope], Value] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Id of the service this scope is part of / which uses this scope. None if no instance (or value) was found.
	  */
	def serviceId(implicit connection: Connection) = pullColumn(model.serviceIdColumn).int
	
	/**
	  * Name of this scope in the 3rd party service. None if no instance (or value) was found.
	  */
	def name(implicit connection: Connection) = pullColumn(model.nameColumn).string
	
	/**
	  * Priority assigned for this scope where higher values mean higher priority. Used when multiple scopes can be chosen from.. None if no instance (or value) was found.
	  */
	def priority(implicit connection: Connection) = pullColumn(model.priorityColumn).int
	
	/**
	  * Time when this Scope was first created. None if no instance (or value) was found.
	  */
	def created(implicit connection: Connection) = pullColumn(model.createdColumn).instant
	
	def id(implicit connection: Connection) = pullColumn(index).int
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = ScopeModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = ScopeFactory
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the created of the targeted Scope instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any Scope instance was affected
	  */
	def created_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * Updates the name of the targeted Scope instance(s)
	  * @param newName A new name to assign
	  * @return Whether any Scope instance was affected
	  */
	def name_=(newName: String)(implicit connection: Connection) = putColumn(model.nameColumn, newName)
	
	/**
	  * Updates the priority of the targeted Scope instance(s)
	  * @param newPriority A new priority to assign
	  * @return Whether any Scope instance was affected
	  */
	def priority_=(newPriority: Int)(implicit connection: Connection) = 
		putColumn(model.priorityColumn, newPriority)
	
	/**
	  * Updates the serviceId of the targeted Scope instance(s)
	  * @param newServiceId A new serviceId to assign
	  * @return Whether any Scope instance was affected
	  */
	def serviceId_=(newServiceId: Int)(implicit connection: Connection) = 
		putColumn(model.serviceIdColumn, newServiceId)
}

