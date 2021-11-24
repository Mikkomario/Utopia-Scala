package utopia.exodus.database.access.many.auth

import java.time.Instant
import utopia.exodus.database.factory.auth.EmailValidatedSessionFactory
import utopia.exodus.database.model.auth.EmailValidatedSessionModel
import utopia.exodus.model.stored.auth.EmailValidatedSession
import utopia.flow.generic.ValueConversions._
import utopia.flow.time.Now
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.SubView
import utopia.vault.sql.Condition

object ManyEmailValidatedSessionsAccess
{
	// NESTED	--------------------
	
	private class ManyEmailValidatedSessionsSubView(override val parent: ManyRowModelAccess[EmailValidatedSession], 
		override val filterCondition: Condition) 
		extends ManyEmailValidatedSessionsAccess with SubView
}

/**
  * A common trait for access points which target multiple EmailValidatedSessions at a time
  * @author Mikko Hilpinen
  * @since 24.11.2021, v3.1
  */
trait ManyEmailValidatedSessionsAccess extends ManyRowModelAccess[EmailValidatedSession] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * validationIds of the accessible EmailValidatedSessions
	  */
	def validationIds(implicit connection: Connection) = 
		pullColumn(model.validationIdColumn).map { v => v.getInt }
	
	/**
	  * tokens of the accessible EmailValidatedSessions
	  */
	def tokens(implicit connection: Connection) = pullColumn(model.tokenColumn).map { v => v.getString }
	
	/**
	  * expirationTimes of the accessible EmailValidatedSessions
	  */
	def expirationTimes(implicit connection: Connection) = pullColumn(model.expiresColumn)
		.map { _.getInstant }
	
	/**
	  * creationTimes of the accessible EmailValidatedSessions
	  */
	def creationTimes(implicit connection: Connection) = pullColumn(model.createdColumn).map { _.getInstant }
	
	/**
	  * closedAfters of the accessible EmailValidatedSessions
	  */
	def closedAfters(implicit connection: Connection) = pullColumn(model.closedAfterColumn)
		.flatMap { _.instant }
	
	def ids(implicit connection: Connection) = pullColumn(index).flatMap { id => id.int }
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = EmailValidatedSessionModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = EmailValidatedSessionFactory
	
	override protected def defaultOrdering = Some(factory.defaultOrdering)
	
	override def filter(additionalCondition: Condition): ManyEmailValidatedSessionsAccess = 
		new ManyEmailValidatedSessionsAccess.ManyEmailValidatedSessionsSubView(this, additionalCondition)
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the closedAfter of the targeted EmailValidatedSession instance(s)
	  * @param newClosedAfter A new closedAfter to assign
	  * @return Whether any EmailValidatedSession instance was affected
	  */
	def closedAfters_=(newClosedAfter: Instant)(implicit connection: Connection) = 
		putColumn(model.closedAfterColumn, newClosedAfter)
	
	/**
	  * Updates the created of the targeted EmailValidatedSession instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any EmailValidatedSession instance was affected
	  */
	def creationTimes_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * Deprecates all accessible EmailValidatedSessions
	  * @return Whether any row was targeted
	  */
	def deprecate()(implicit connection: Connection) = closedAfters = Now
	
	/**
	  * Updates the expires of the targeted EmailValidatedSession instance(s)
	  * @param newExpires A new expires to assign
	  * @return Whether any EmailValidatedSession instance was affected
	  */
	def expirationTimes_=(newExpires: Instant)(implicit connection: Connection) = 
		putColumn(model.expiresColumn, newExpires)
	
	/**
	  * Updates the token of the targeted EmailValidatedSession instance(s)
	  * @param newToken A new token to assign
	  * @return Whether any EmailValidatedSession instance was affected
	  */
	def tokens_=(newToken: String)(implicit connection: Connection) = putColumn(model.tokenColumn, newToken)
	
	/**
	  * Updates the validationId of the targeted EmailValidatedSession instance(s)
	  * @param newValidationId A new validationId to assign
	  * @return Whether any EmailValidatedSession instance was affected
	  */
	def validationIds_=(newValidationId: Int)(implicit connection: Connection) = 
		putColumn(model.validationIdColumn, newValidationId)
}

