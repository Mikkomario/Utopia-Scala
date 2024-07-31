package utopia.citadel.database.access.many.user

import utopia.citadel.database.factory.user.UserFactory
import utopia.citadel.database.model.user.UserModel
import utopia.flow.generic.casting.ValueConversions._
import utopia.metropolis.model.stored.user.User
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.FilterableView
import utopia.vault.sql.Condition

import java.time.Instant

object ManyUsersAccess
{
	// OTHER	--------------------
	
	/**
	  * @param condition Search condition to apply
	  * @return Access to users which fulfill this search condition
	  */
	def apply(condition: Condition): ManyUsersAccess = _ManyUsersAccess(Some(condition))
	
	
	// NESTED	--------------------
	
	private case class _ManyUsersAccess(accessCondition: Option[Condition]) extends ManyUsersAccess
}

/**
  * A common trait for access points which target multiple Users at a time
  * @author Mikko Hilpinen
  * @since 23.10.2021
  */
trait ManyUsersAccess extends ManyRowModelAccess[User] with Indexed with FilterableView[ManyUsersAccess]
{
	// COMPUTED	--------------------
	
	/**
	  * creationTimes of the accessible Users
	  */
	def creationTimes(implicit connection: Connection) = 
		pullColumn(model.createdColumn).flatMap { value => value.instant }
	
	def ids(implicit connection: Connection) = pullColumn(index).flatMap { id => id.int }
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = UserModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = UserFactory
	
	override protected def self = this
	
	override def apply(condition: Condition): ManyUsersAccess = ManyUsersAccess(condition)
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the created of the targeted User instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any User instance was affected
	  */
	def creationTimes_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
}

