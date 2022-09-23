package utopia.citadel.database.access.single.organization

import java.time.Instant
import utopia.citadel.database.factory.organization.UserRoleFactory
import utopia.citadel.database.model.organization.UserRoleModel
import utopia.flow.collection.value.typeless.Value
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.stored.organization.UserRole
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.access.template.model.DistinctModelAccess
import utopia.vault.nosql.template.Indexed

/**
  * A common trait for access points that return individual and distinct UserRoles.
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
trait UniqueUserRoleAccess 
	extends SingleRowModelAccess[UserRole] with DistinctModelAccess[UserRole, Option[UserRole], Value] 
		with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Time when this UserRole was first created. None if no instance (or value) was found.
	  */
	def created(implicit connection: Connection) = pullColumn(model.createdColumn).instant
	
	def id(implicit connection: Connection) = pullColumn(index).int
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = UserRoleModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = UserRoleFactory
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the created of the targeted UserRole instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any UserRole instance was affected
	  */
	def created_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
}

