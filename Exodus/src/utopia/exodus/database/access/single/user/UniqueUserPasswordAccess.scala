package utopia.exodus.database.access.single.user

import java.time.Instant
import utopia.exodus.database.factory.user.UserPasswordFactory
import utopia.exodus.database.model.user.UserPasswordModel
import utopia.exodus.model.stored.user.UserPassword
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.access.template.model.DistinctModelAccess
import utopia.vault.nosql.template.Indexed

/**
  * A common trait for access points that return individual and distinct UserPasswords.
  * @author Mikko Hilpinen
  * @since 2021-10-25
  */
trait UniqueUserPasswordAccess 
	extends SingleRowModelAccess[UserPassword] 
		with DistinctModelAccess[UserPassword, Option[UserPassword], Value] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Id of the user who owns this password. None if no instance (or value) was found.
	  */
	def userId(implicit connection: Connection) = pullColumn(model.userIdColumn).int
	
	/**
	  * User's hashed password. None if no instance (or value) was found.
	  */
	def hash(implicit connection: Connection) = pullColumn(model.hashColumn).string
	
	/**
	  * Time when this UserPassword was first created. None if no instance (or value) was found.
	  */
	def created(implicit connection: Connection) = pullColumn(model.createdColumn).instant
	
	def id(implicit connection: Connection) = pullColumn(index).int
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = UserPasswordModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = UserPasswordFactory
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the created of the targeted UserPassword instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any UserPassword instance was affected
	  */
	def created_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * Updates the hash of the targeted UserPassword instance(s)
	  * @param newHash A new hash to assign
	  * @return Whether any UserPassword instance was affected
	  */
	def hash_=(newHash: String)(implicit connection: Connection) = putColumn(model.hashColumn, newHash)
	
	/**
	  * Updates the userId of the targeted UserPassword instance(s)
	  * @param newUserId A new userId to assign
	  * @return Whether any UserPassword instance was affected
	  */
	def userId_=(newUserId: Int)(implicit connection: Connection) = putColumn(model.userIdColumn, newUserId)
}

