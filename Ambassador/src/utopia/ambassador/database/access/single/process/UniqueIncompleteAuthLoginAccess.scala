package utopia.ambassador.database.access.single.process

import java.time.Instant
import utopia.ambassador.database.factory.process.IncompleteAuthLoginFactory
import utopia.ambassador.database.model.process.IncompleteAuthLoginModel
import utopia.ambassador.model.stored.process.IncompleteAuthLogin
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.access.template.model.DistinctModelAccess
import utopia.vault.nosql.template.Indexed

/**
  * A common trait for access points that return individual and distinct IncompleteAuthLogins.
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
trait UniqueIncompleteAuthLoginAccess 
	extends SingleRowModelAccess[IncompleteAuthLogin] 
		with DistinctModelAccess[IncompleteAuthLogin, Option[IncompleteAuthLogin], Value] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Id of the incomplete authentication this login completes. None if no instance (or value) was found.
	  */
	def authId(implicit connection: Connection) = pullColumn(model.authIdColumn).int
	
	/**
	  * Id of the user who logged in. None if no instance (or value) was found.
	  */
	def userId(implicit connection: Connection) = pullColumn(model.userIdColumn).int
	
	/**
	  * Time when this IncompleteAuthLogin was first created. None if no instance (or value) was found.
	  */
	def created(implicit connection: Connection) = pullColumn(model.createdColumn).instant
	
	/**
	  * Whether authentication tokens were successfully acquired from the 3rd party service. None if no instance (or value) was found.
	  */
	def wasSuccess(implicit connection: Connection) = pullColumn(model.wasSuccessColumn).boolean
	
	def id(implicit connection: Connection) = pullColumn(index).int
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = IncompleteAuthLoginModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = IncompleteAuthLoginFactory
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the authId of the targeted IncompleteAuthLogin instance(s)
	  * @param newAuthId A new authId to assign
	  * @return Whether any IncompleteAuthLogin instance was affected
	  */
	def authId_=(newAuthId: Int)(implicit connection: Connection) = putColumn(model.authIdColumn, newAuthId)
	
	/**
	  * Updates the created of the targeted IncompleteAuthLogin instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any IncompleteAuthLogin instance was affected
	  */
	def created_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * Updates the userId of the targeted IncompleteAuthLogin instance(s)
	  * @param newUserId A new userId to assign
	  * @return Whether any IncompleteAuthLogin instance was affected
	  */
	def userId_=(newUserId: Int)(implicit connection: Connection) = putColumn(model.userIdColumn, newUserId)
	
	/**
	  * Updates the wasSuccess of the targeted IncompleteAuthLogin instance(s)
	  * @param newWasSuccess A new wasSuccess to assign
	  * @return Whether any IncompleteAuthLogin instance was affected
	  */
	def wasSuccess_=(newWasSuccess: Boolean)(implicit connection: Connection) = 
		putColumn(model.wasSuccessColumn, newWasSuccess)
}

