package utopia.exodus.database.access.many.user

import java.time.Instant
import utopia.exodus.database.factory.user.UserPasswordFactory
import utopia.exodus.database.model.user.UserPasswordModel
import utopia.exodus.model.stored.user.UserPassword
import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.{FilterableView, SubView}
import utopia.vault.sql.Condition

object ManyUserPasswordsAccess
{
	// NESTED	--------------------
	
	private class ManyUserPasswordsSubView(override val parent: ManyRowModelAccess[UserPassword], 
		override val filterCondition: Condition) 
		extends ManyUserPasswordsAccess with SubView
}

/**
  * A common trait for access points which target multiple UserPasswords at a time
  * @author Mikko Hilpinen
  * @since 2021-10-25
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
	
	override def filter(additionalCondition: Condition): ManyUserPasswordsAccess = 
		new ManyUserPasswordsAccess.ManyUserPasswordsSubView(this, additionalCondition)
	
	
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

