package utopia.ambassador.database.access.many.process

import java.time.Instant
import utopia.ambassador.database.factory.process.IncompleteAuthFactory
import utopia.ambassador.database.model.process.IncompleteAuthModel
import utopia.ambassador.model.stored.process.IncompleteAuth
import utopia.flow.generic.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.{FilterableView, SubView}
import utopia.vault.sql.Condition

object ManyIncompleteAuthsAccess
{
	// NESTED	--------------------
	
	private class ManyIncompleteAuthsSubView(override val parent: ManyRowModelAccess[IncompleteAuth], 
		override val filterCondition: Condition) 
		extends ManyIncompleteAuthsAccess with SubView
}

/**
  * A common trait for access points which target multiple IncompleteAuths at a time
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
trait ManyIncompleteAuthsAccess extends ManyRowModelAccess[IncompleteAuth] with Indexed
	with FilterableView[ManyIncompleteAuthsAccess]
{
	// COMPUTED	--------------------
	
	/**
	  * serviceIds of the accessible IncompleteAuths
	  */
	def serviceIds(implicit connection: Connection) = 
		pullColumn(model.serviceIdColumn).flatMap { value => value.int }
	
	/**
	  * codes of the accessible IncompleteAuths
	  */
	def codes(implicit connection: Connection) = pullColumn(model.codeColumn)
		.flatMap { value => value.string }
	
	/**
	  * tokens of the accessible IncompleteAuths
	  */
	def tokens(implicit connection: Connection) = pullColumn(model.tokenColumn)
		.flatMap { value => value.string }
	
	/**
	  * expirationTimes of the accessible IncompleteAuths
	  */
	def expirationTimes(implicit connection: Connection) = 
		pullColumn(model.expiresColumn).flatMap { value => value.instant }
	
	/**
	  * creationTimes of the accessible IncompleteAuths
	  */
	def creationTimes(implicit connection: Connection) = 
		pullColumn(model.createdColumn).flatMap { value => value.instant }
	
	def ids(implicit connection: Connection) = pullColumn(index).flatMap { id => id.int }
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = IncompleteAuthModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = IncompleteAuthFactory
	
	override def filter(additionalCondition: Condition): ManyIncompleteAuthsAccess = 
		new ManyIncompleteAuthsAccess.ManyIncompleteAuthsSubView(this, additionalCondition)
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the code of the targeted IncompleteAuth instance(s)
	  * @param newCode A new code to assign
	  * @return Whether any IncompleteAuth instance was affected
	  */
	def codes_=(newCode: String)(implicit connection: Connection) = putColumn(model.codeColumn, newCode)
	
	/**
	  * Updates the created of the targeted IncompleteAuth instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any IncompleteAuth instance was affected
	  */
	def creationTimes_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * Updates the expires of the targeted IncompleteAuth instance(s)
	  * @param newExpires A new expires to assign
	  * @return Whether any IncompleteAuth instance was affected
	  */
	def expirationTimes_=(newExpires: Instant)(implicit connection: Connection) = 
		putColumn(model.expiresColumn, newExpires)
	
	/**
	  * Updates the serviceId of the targeted IncompleteAuth instance(s)
	  * @param newServiceId A new serviceId to assign
	  * @return Whether any IncompleteAuth instance was affected
	  */
	def serviceIds_=(newServiceId: Int)(implicit connection: Connection) = 
		putColumn(model.serviceIdColumn, newServiceId)
	
	/**
	  * Updates the token of the targeted IncompleteAuth instance(s)
	  * @param newToken A new token to assign
	  * @return Whether any IncompleteAuth instance was affected
	  */
	def tokens_=(newToken: String)(implicit connection: Connection) = putColumn(model.tokenColumn, newToken)
}

