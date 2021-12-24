package utopia.ambassador.database.access.many.process

import java.time.Instant
import utopia.ambassador.database.factory.process.IncompleteAuthLoginFactory
import utopia.ambassador.database.model.process.IncompleteAuthLoginModel
import utopia.ambassador.model.stored.process.IncompleteAuthLogin
import utopia.flow.generic.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.SubView
import utopia.vault.sql.Condition

object ManyIncompleteAuthLoginsAccess
{
	// NESTED	--------------------
	
	private class ManyIncompleteAuthLoginsSubView(override val parent: ManyRowModelAccess[IncompleteAuthLogin], 
		override val filterCondition: Condition) 
		extends ManyIncompleteAuthLoginsAccess with SubView
}

/**
  * A common trait for access points which target multiple IncompleteAuthLogins at a time
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
trait ManyIncompleteAuthLoginsAccess extends ManyRowModelAccess[IncompleteAuthLogin] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * authIds of the accessible IncompleteAuthLogins
	  */
	def authIds(implicit connection: Connection) = pullColumn(model.authIdColumn)
		.flatMap { value => value.int }
	
	/**
	  * userIds of the accessible IncompleteAuthLogins
	  */
	def userIds(implicit connection: Connection) = pullColumn(model.userIdColumn)
		.flatMap { value => value.int }
	
	/**
	  * creationTimes of the accessible IncompleteAuthLogins
	  */
	def creationTimes(implicit connection: Connection) = 
		pullColumn(model.createdColumn).flatMap { value => value.instant }
	
	/**
	  * wereSuccesses of the accessible IncompleteAuthLogins
	  */
	def wereSuccesses(implicit connection: Connection) = 
		pullColumn(model.wasSuccessColumn).flatMap { value => value.boolean }
	
	def ids(implicit connection: Connection) = pullColumn(index).flatMap { id => id.int }
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = IncompleteAuthLoginModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = IncompleteAuthLoginFactory
	
	override def filter(additionalCondition: Condition): ManyIncompleteAuthLoginsAccess = 
		new ManyIncompleteAuthLoginsAccess.ManyIncompleteAuthLoginsSubView(this, additionalCondition)
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the authId of the targeted IncompleteAuthLogin instance(s)
	  * @param newAuthId A new authId to assign
	  * @return Whether any IncompleteAuthLogin instance was affected
	  */
	def authIds_=(newAuthId: Int)(implicit connection: Connection) = putColumn(model.authIdColumn, newAuthId)
	
	/**
	  * Updates the created of the targeted IncompleteAuthLogin instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any IncompleteAuthLogin instance was affected
	  */
	def creationTimes_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * Updates the userId of the targeted IncompleteAuthLogin instance(s)
	  * @param newUserId A new userId to assign
	  * @return Whether any IncompleteAuthLogin instance was affected
	  */
	def userIds_=(newUserId: Int)(implicit connection: Connection) = putColumn(model.userIdColumn, newUserId)
	
	/**
	  * Updates the wasSuccess of the targeted IncompleteAuthLogin instance(s)
	  * @param newWasSuccess A new wasSuccess to assign
	  * @return Whether any IncompleteAuthLogin instance was affected
	  */
	def wereSuccesses_=(newWasSuccess: Boolean)(implicit connection: Connection) = 
		putColumn(model.wasSuccessColumn, newWasSuccess)
}

