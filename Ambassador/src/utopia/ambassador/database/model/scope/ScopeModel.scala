package utopia.ambassador.database.model.scope

import java.time.Instant
import utopia.ambassador.database.factory.scope.ScopeFactory
import utopia.ambassador.model.partial.scope.ScopeData
import utopia.ambassador.model.stored.scope.Scope
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConversions._
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.nosql.storable.DataInserter

/**
  * Used for constructing ScopeModel instances and for inserting Scopes to the database
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
object ScopeModel extends DataInserter[ScopeModel, Scope, ScopeData]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Name of the property that contains Scope serviceId
	  */
	val serviceIdAttName = "serviceId"
	
	/**
	  * Name of the property that contains Scope name
	  */
	val nameAttName = "name"
	
	/**
	  * Name of the property that contains Scope priority
	  */
	val priorityAttName = "priority"
	
	/**
	  * Name of the property that contains Scope created
	  */
	val createdAttName = "created"
	
	
	// COMPUTED	--------------------
	
	/**
	  * Column that contains Scope serviceId
	  */
	def serviceIdColumn = table(serviceIdAttName)
	
	/**
	  * Column that contains Scope name
	  */
	def nameColumn = table(nameAttName)
	
	/**
	  * Column that contains Scope priority
	  */
	def priorityColumn = table(priorityAttName)
	
	/**
	  * Column that contains Scope created
	  */
	def createdColumn = table(createdAttName)
	
	/**
	  * The factory object used by this model type
	  */
	def factory = ScopeFactory
	
	
	// IMPLEMENTED	--------------------
	
	override def table = factory.table
	
	override def apply(data: ScopeData) = 
		apply(None, Some(data.serviceId), Some(data.name), data.priority, Some(data.created))
	
	override def complete(id: Value, data: ScopeData) = Scope(id.getInt, data)
	
	
	// OTHER	--------------------
	
	/**
	  * @param created Time when this Scope was first created
	  * @return A model containing only the specified created
	  */
	def withCreated(created: Instant) = apply(created = Some(created))
	
	/**
	  * @param id A Scope id
	  * @return A model with that id
	  */
	def withId(id: Int) = apply(Some(id))
	
	/**
	  * @param name Name of this scope in the 3rd party service
	  * @return A model containing only the specified name
	  */
	def withName(name: String) = apply(name = Some(name))
	
	/**
	  * @param priority Priority assigned for this scope where higher values mean higher priority. Used when multiple scopes can be chosen from.
	  * @return A model containing only the specified priority
	  */
	def withPriority(priority: Int) = apply(priority = Some(priority))
	
	/**
	  * @param serviceId Id of the service this scope is part of / which uses this scope
	  * @return A model containing only the specified serviceId
	  */
	def withServiceId(serviceId: Int) = apply(serviceId = Some(serviceId))
}

/**
  * Used for interacting with Scopes in the database
  * @param id Scope database id
  * @param serviceId Id of the service this scope is part of / which uses this scope
  * @param name Name of this scope in the 3rd party service
  * @param priority Priority assigned for this scope where higher values mean higher priority. Used when multiple scopes can be chosen from.
  * @param created Time when this Scope was first created
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
case class ScopeModel(id: Option[Int] = None, serviceId: Option[Int] = None, name: Option[String] = None, 
	priority: Option[Int] = None, created: Option[Instant] = None) 
	extends StorableWithFactory[Scope]
{
	// IMPLEMENTED	--------------------
	
	override def factory = ScopeModel.factory
	
	override def valueProperties = 
	{
		import ScopeModel._
		Vector("id" -> id, serviceIdAttName -> serviceId, nameAttName -> name, priorityAttName -> priority, 
			createdAttName -> created)
	}
	
	
	// OTHER	--------------------
	
	/**
	  * @param created A new created
	  * @return A new copy of this model with the specified created
	  */
	def withCreated(created: Instant) = copy(created = Some(created))
	
	/**
	  * @param name A new name
	  * @return A new copy of this model with the specified name
	  */
	def withName(name: String) = copy(name = Some(name))
	
	/**
	  * @param priority A new priority
	  * @return A new copy of this model with the specified priority
	  */
	def withPriority(priority: Int) = copy(priority = Some(priority))
	
	/**
	  * @param serviceId A new serviceId
	  * @return A new copy of this model with the specified serviceId
	  */
	def withServiceId(serviceId: Int) = copy(serviceId = Some(serviceId))
}

