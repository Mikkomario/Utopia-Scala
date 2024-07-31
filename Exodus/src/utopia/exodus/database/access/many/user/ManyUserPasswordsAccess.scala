package utopia.exodus.database.access.many.user

import utopia.exodus.database.factory.user.UserPasswordFactory
import utopia.exodus.database.model.user.UserPasswordModel
import utopia.exodus.model.stored.user.UserPassword
import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.{FilterableView, ViewFactory}
import utopia.vault.sql.Condition

import java.time.Instant

object ManyUserPasswordsAccess extends ViewFactory[ManyUserPasswordsAccess]
{
	// IMPLEMENTED	--------------------
	
	/**
	  * @param condition Condition to apply to all requests
	  * @return An access point that applies the specified filter condition (only)
	  */
	override def apply(condition: Condition): ManyUserPasswordsAccess = 
		 new _ManyUserPasswordsAccess(condition)
	
	
	// NESTED	--------------------
	
	private class _ManyUserPasswordsAccess(condition: Condition) extends ManyUserPasswordsAccess
	{
		// IMPLEMENTED	--------------------
		
		override def accessCondition = Some(condition)
	}
}

/**
  * A common trait for access points which target multiple UserPasswords at a time
  * @author Mikko Hilpinen
  * @since 25.10.2021
  */
trait ManyUserPasswordsAccess 
	extends ManyRowModelAccess[UserPassword] with Indexed with FilterableView[ManyUserPasswordsAccess]
{
	// COMPUTED	--------------------
	
	/**
	  * userIds of the accessible UserPasswords
	  */
	def userIds(implicit connection: Connection) = pullColumn(model.userIdColumn)
		.flatMap { value => value.int }
	
	/**
	  * hashes of the accessible UserPasswords
	  */
	def hashes(implicit connection: Connection) = pullColumn(model.hashColumn)
		.flatMap { value => value.string }
	
	/**
	  * creationTimes of the accessible UserPasswords
	  */
	def creationTimes(implicit connection: Connection) = 
		pullColumn(model.createdColumn).flatMap { value => value.instant }
	
	def ids(implicit connection: Connection) = pullColumn(index).flatMap { id => id.int }
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = UserPasswordModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = UserPasswordFactory
	
	override protected def self = this
	
	override def apply(condition: Condition): ManyUserPasswordsAccess = ManyUserPasswordsAccess(condition)
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the created of the targeted UserPassword instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any UserPassword instance was affected
	  */
	def creationTimes_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * Updates the hash of the targeted UserPassword instance(s)
	  * @param newHash A new hash to assign
	  * @return Whether any UserPassword instance was affected
	  */
	def hashes_=(newHash: String)(implicit connection: Connection) = putColumn(model.hashColumn, newHash)
	
	/**
	  * Updates the userId of the targeted UserPassword instance(s)
	  * @param newUserId A new userId to assign
	  * @return Whether any UserPassword instance was affected
	  */
	def userIds_=(newUserId: Int)(implicit connection: Connection) = putColumn(model.userIdColumn, newUserId)
}

