package utopia.citadel.database.model.organization

import java.time.Instant
import utopia.citadel.database.factory.organization.UserRoleFactory
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.partial.organization.UserRoleData
import utopia.metropolis.model.stored.organization.UserRole
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.nosql.storable.DataInserter

/**
  * Used for constructing UserRoleModel instances and for inserting UserRoles to the database
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object UserRoleModel extends DataInserter[UserRoleModel, UserRole, UserRoleData]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Name of the property that contains UserRole created
	  */
	val createdAttName = "created"
	
	
	// COMPUTED	--------------------
	
	/**
	  * Column that contains UserRole created
	  */
	def createdColumn = table(createdAttName)
	
	/**
	  * The factory object used by this model type
	  */
	def factory = UserRoleFactory
	
	
	// IMPLEMENTED	--------------------
	
	override def table = factory.table
	
	override def apply(data: UserRoleData) = apply(None, Some(data.created))
	
	override def complete(id: Value, data: UserRoleData) = UserRole(id.getInt, data)
	
	
	// OTHER	--------------------
	
	/**
	  * @param created Time when this UserRole was first created
	  * @return A model containing only the specified created
	  */
	def withCreated(created: Instant) = apply(created = Some(created))
	
	/**
	  * @param id A UserRole id
	  * @return A model with that id
	  */
	def withId(id: Int) = apply(Some(id))
}

/**
  * Used for interacting with UserRoles in the database
  * @param id UserRole database id
  * @param created Time when this UserRole was first created
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
case class UserRoleModel(id: Option[Int] = None, created: Option[Instant] = None) 
	extends StorableWithFactory[UserRole]
{
	// IMPLEMENTED	--------------------
	
	override def factory = UserRoleModel.factory
	
	override def valueProperties = 
	{
		import UserRoleModel._
		Vector("id" -> id, createdAttName -> created)
	}
	
	
	// OTHER	--------------------
	
	/**
	  * @param created A new created
	  * @return A new copy of this model with the specified created
	  */
	def withCreated(created: Instant) = copy(created = Some(created))
}

