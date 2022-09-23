package utopia.exodus.database.model.auth

import java.time.Instant
import utopia.exodus.database.factory.auth.ScopeFactory
import utopia.exodus.model.partial.auth.ScopeData
import utopia.exodus.model.stored.auth.Scope
import utopia.flow.collection.value.typeless.Value
import utopia.flow.generic.ValueConversions._
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.nosql.storable.DataInserter

/**
  * Used for constructing ScopeModel instances and for inserting scopes to the database
  * @author Mikko Hilpinen
  * @since 18.02.2022, v4.0
  */
object ScopeModel extends DataInserter[ScopeModel, Scope, ScopeData]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Name of the property that contains scope name
	  */
	val nameAttName = "name"
	
	/**
	  * Name of the property that contains scope created
	  */
	val createdAttName = "created"
	
	
	// COMPUTED	--------------------
	
	/**
	  * Column that contains scope name
	  */
	def nameColumn = table(nameAttName)
	
	/**
	  * Column that contains scope created
	  */
	def createdColumn = table(createdAttName)
	
	/**
	  * The factory object used by this model type
	  */
	def factory = ScopeFactory
	
	
	// IMPLEMENTED	--------------------
	
	override def table = factory.table
	
	override def apply(data: ScopeData) = apply(None, Some(data.name), Some(data.created))
	
	override def complete(id: Value, data: ScopeData) = Scope(id.getInt, data)
	
	
	// OTHER	--------------------
	
	/**
	  * @param created Time when this scope was first created
	  * @return A model containing only the specified created
	  */
	def withCreated(created: Instant) = apply(created = Some(created))
	
	/**
	  * @param id A scope id
	  * @return A model with that id
	  */
	def withId(id: Int) = apply(Some(id))
	
	/**
	  * @param name Technical name or identifier of this scope
	  * @return A model containing only the specified name
	  */
	def withName(name: String) = apply(name = Some(name))
}

/**
  * Used for interacting with Scopes in the database
  * @param id scope database id
  * @param name Technical name or identifier of this scope
  * @param created Time when this scope was first created
  * @author Mikko Hilpinen
  * @since 18.02.2022, v4.0
  */
case class ScopeModel(id: Option[Int] = None, name: Option[String] = None, created: Option[Instant] = None) 
	extends StorableWithFactory[Scope]
{
	// IMPLEMENTED	--------------------
	
	override def factory = ScopeModel.factory
	
	override def valueProperties = {
		import ScopeModel._
		Vector("id" -> id, nameAttName -> name, createdAttName -> created)
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
}

