package utopia.citadel.database.access.single.user

import java.time.Instant
import utopia.citadel.database.factory.user.UserFactory
import utopia.citadel.database.model.user.UserModel
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.stored.user.User
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.access.template.model.DistinctModelAccess
import utopia.vault.nosql.template.Indexed

/**
  * A common trait for access points that return individual and distinct Users.
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
trait UniqueUserAccess 
	extends SingleRowModelAccess[User] with DistinctModelAccess[User, Option[User], Value] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Time when this User was first created. None if no instance (or value) was found.
	  */
	def created(implicit connection: Connection) = pullColumn(model.createdColumn).instant
	
	def id(implicit connection: Connection) = pullColumn(index).int
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = UserModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = UserFactory
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the created of the targeted User instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any User instance was affected
	  */
	def created_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
}

