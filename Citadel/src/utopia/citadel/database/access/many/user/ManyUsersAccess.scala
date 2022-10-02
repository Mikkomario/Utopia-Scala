package utopia.citadel.database.access.many.user

import java.time.Instant
import utopia.citadel.database.factory.user.UserFactory
import utopia.citadel.database.model.user.UserModel
import utopia.flow.generic.casting.ValueConversions._
import utopia.metropolis.model.stored.user.User
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.{FilterableView, SubView}
import utopia.vault.sql.Condition

object ManyUsersAccess
{
	// NESTED	--------------------
	
	private class ManyUsersSubView(override val parent: ManyRowModelAccess[User], 
		override val filterCondition: Condition) 
		extends ManyUsersAccess with SubView
}

/**
  * A common trait for access points which target multiple Users at a time
  * @author Mikko Hilpinen
  * @since 2021-10-23
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
	
	override def filter(additionalCondition: Condition): ManyUsersAccess = 
		new ManyUsersAccess.ManyUsersSubView(this, additionalCondition)
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the created of the targeted User instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any User instance was affected
	  */
	def creationTimes_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
}

