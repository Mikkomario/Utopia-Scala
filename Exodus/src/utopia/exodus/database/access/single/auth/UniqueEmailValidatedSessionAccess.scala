package utopia.exodus.database.access.single.auth

import java.time.Instant
import utopia.exodus.database.factory.auth.EmailValidatedSessionFactory
import utopia.exodus.database.model.auth.EmailValidatedSessionModel
import utopia.exodus.model.stored.auth.EmailValidatedSession
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConversions._
import utopia.flow.time.Now
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.access.template.model.DistinctModelAccess
import utopia.vault.nosql.template.Indexed

/**
  * A common trait for access points that return individual and distinct EmailValidatedSessions.
  * @author Mikko Hilpinen
  * @since 24.11.2021, v3.1
  */
trait UniqueEmailValidatedSessionAccess 
	extends SingleRowModelAccess[EmailValidatedSession] 
		with DistinctModelAccess[EmailValidatedSession, Option[EmailValidatedSession], Value] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Reference to the email validation used as the basis for this session. None if no instance (or value)
	  *  was found.
	  */
	def validationId(implicit connection: Connection) = pullColumn(model.validationIdColumn).int
	
	/**
	  * Token used to authenticate against this session. None if no instance (or value) was found.
	  */
	def token(implicit connection: Connection) = pullColumn(model.tokenColumn).string
	
	/**
	  * Time when this EmailValidatedSession expires / 
		becomes invalid. None if no instance (or value) was found.
	  */
	def expires(implicit connection: Connection) = pullColumn(model.expiresColumn).instant
	
	/**
	  * Time when this EmailValidatedSession was first created. None if no instance (or value) was found.
	  */
	def created(implicit connection: Connection) = pullColumn(model.createdColumn).instant
	
	/**
	  * Time after which this session was manually closed. None if no instance (or value) was found.
	  */
	def closedAfter(implicit connection: Connection) = pullColumn(model.closedAfterColumn).instant
	
	def id(implicit connection: Connection) = pullColumn(index).int
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = EmailValidatedSessionModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = EmailValidatedSessionFactory
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the closedAfter of the targeted EmailValidatedSession instance(s)
	  * @param newClosedAfter A new closedAfter to assign
	  * @return Whether any EmailValidatedSession instance was affected
	  */
	def closedAfter_=(newClosedAfter: Instant)(implicit connection: Connection) = 
		putColumn(model.closedAfterColumn, newClosedAfter)
	
	/**
	  * Updates the created of the targeted EmailValidatedSession instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any EmailValidatedSession instance was affected
	  */
	def created_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * Deprecates all accessible EmailValidatedSessions
	  * @return Whether any row was targeted
	  */
	def deprecate()(implicit connection: Connection) = closedAfter = Now
	
	/**
	  * Updates the expires of the targeted EmailValidatedSession instance(s)
	  * @param newExpires A new expires to assign
	  * @return Whether any EmailValidatedSession instance was affected
	  */
	def expires_=(newExpires: Instant)(implicit connection: Connection) = 
		putColumn(model.expiresColumn, newExpires)
	
	/**
	  * Updates the token of the targeted EmailValidatedSession instance(s)
	  * @param newToken A new token to assign
	  * @return Whether any EmailValidatedSession instance was affected
	  */
	def token_=(newToken: String)(implicit connection: Connection) = putColumn(model.tokenColumn, newToken)
	
	/**
	  * Updates the validationId of the targeted EmailValidatedSession instance(s)
	  * @param newValidationId A new validationId to assign
	  * @return Whether any EmailValidatedSession instance was affected
	  */
	def validationId_=(newValidationId: Int)(implicit connection: Connection) = 
		putColumn(model.validationIdColumn, newValidationId)
}

