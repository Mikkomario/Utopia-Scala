package utopia.ambassador.database.access.many.scope

import utopia.ambassador.database.model.scope.ScopeModel
import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.FilterableView

import java.time.Instant

/**
  * A common trait for access points which target multiple Scopes or similar instances at a time
  * @author Mikko Hilpinen
  * @since 26.10.2021
  */
trait ManyScopesAccessLike[+A, +Repr <: ManyModelAccess[A]] 
	extends ManyModelAccess[A] with Indexed with FilterableView[Repr]
{
	// COMPUTED	--------------------
	
	/**
	  * serviceIds of the accessible Scopes
	  */
	def serviceIds(implicit connection: Connection) = 
		pullColumn(model.serviceIdColumn).flatMap { value => value.int }
	
	/**
	  * names of the accessible Scopes
	  */
	def names(implicit connection: Connection) = pullColumn(model.nameColumn)
		.flatMap { value => value.string }
	
	/**
	  * priorityLevels of the accessible Scopes
	  */
	def priorityLevels(implicit connection: Connection) = 
		pullColumn(model.priorityColumn).flatMap { value => value.int }
	
	/**
	  * creationTimes of the accessible Scopes
	  */
	def creationTimes(implicit connection: Connection) = 
		pullColumn(model.createdColumn).flatMap { value => value.instant }
	
	def ids(implicit connection: Connection) = pullColumn(index).flatMap { id => id.int }
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = ScopeModel
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the created of the targeted Scope instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any Scope instance was affected
	  */
	def creationTimes_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * @param serviceId Id of the targeted service
	  * @return An access point to those of these scopes which are specific to that service
	  */
	def forServiceWithId(serviceId: Int) = filter(model.withServiceId(serviceId).toCondition)
	
	/**
	  * @param scopeNames Searched scope names
	  * @return An access point to scopes that match the specified names (not all names may be included)
	  */
	def matchingAnyOfNames(scopeNames: Iterable[String]) = filter(model.nameColumn in scopeNames)
	
	/**
	  * Updates the name of the targeted Scope instance(s)
	  * @param newName A new name to assign
	  * @return Whether any Scope instance was affected
	  */
	def names_=(newName: String)(implicit connection: Connection) = putColumn(model.nameColumn, newName)
	
	/**
	  * Updates the priority of the targeted Scope instance(s)
	  * @param newPriority A new priority to assign
	  * @return Whether any Scope instance was affected
	  */
	def priorityLevels_=(newPriority: Int)(implicit connection: Connection) = 
		putColumn(model.priorityColumn, newPriority)
	
	/**
	  * Updates the serviceId of the targeted Scope instance(s)
	  * @param newServiceId A new serviceId to assign
	  * @return Whether any Scope instance was affected
	  */
	def serviceIds_=(newServiceId: Int)(implicit connection: Connection) = 
		putColumn(model.serviceIdColumn, newServiceId)
}

