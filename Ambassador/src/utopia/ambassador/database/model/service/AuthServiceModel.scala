package utopia.ambassador.database.model.service

import java.time.Instant
import utopia.ambassador.database.factory.service.AuthServiceFactory
import utopia.ambassador.model.partial.service.AuthServiceData
import utopia.ambassador.model.stored.service.AuthService
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConversions._
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.nosql.storable.DataInserter

/**
  * Used for constructing AuthServiceModel instances and for inserting AuthServices to the database
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
object AuthServiceModel extends DataInserter[AuthServiceModel, AuthService, AuthServiceData]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Name of the property that contains AuthService name
	  */
	val nameAttName = "name"
	
	/**
	  * Name of the property that contains AuthService created
	  */
	val createdAttName = "created"
	
	
	// COMPUTED	--------------------
	
	/**
	  * Column that contains AuthService name
	  */
	def nameColumn = table(nameAttName)
	
	/**
	  * Column that contains AuthService created
	  */
	def createdColumn = table(createdAttName)
	
	/**
	  * The factory object used by this model type
	  */
	def factory = AuthServiceFactory
	
	
	// IMPLEMENTED	--------------------
	
	override def table = factory.table
	
	override def apply(data: AuthServiceData) = apply(None, Some(data.name), Some(data.created))
	
	override def complete(id: Value, data: AuthServiceData) = AuthService(id.getInt, data)
	
	
	// OTHER	--------------------
	
	/**
	  * @param created Time when this AuthService was first created
	  * @return A model containing only the specified created
	  */
	def withCreated(created: Instant) = apply(created = Some(created))
	
	/**
	  * @param id A AuthService id
	  * @return A model with that id
	  */
	def withId(id: Int) = apply(Some(id))
	
	/**
	  * @param name Name of this service (from the customer's perspective)
	  * @return A model containing only the specified name
	  */
	def withName(name: String) = apply(name = Some(name))
}

/**
  * Used for interacting with AuthServices in the database
  * @param id AuthService database id
  * @param name Name of this service (from the customer's perspective)
  * @param created Time when this AuthService was first created
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
case class AuthServiceModel(id: Option[Int] = None, name: Option[String] = None, 
	created: Option[Instant] = None) 
	extends StorableWithFactory[AuthService]
{
	// IMPLEMENTED	--------------------
	
	override def factory = AuthServiceModel.factory
	
	override def valueProperties = 
	{
		import AuthServiceModel._
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

