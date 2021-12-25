package utopia.exodus.database.access.many.auth

import java.time.Instant
import utopia.exodus.database.factory.auth.ApiKeyFactory
import utopia.exodus.database.model.auth.ApiKeyModel
import utopia.exodus.model.stored.auth.ApiKey
import utopia.flow.generic.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.{FilterableView, SubView}
import utopia.vault.sql.Condition

object ManyApiKeysAccess
{
	// NESTED	--------------------
	
	private class ManyApiKeysSubView(override val parent: ManyRowModelAccess[ApiKey], 
		override val filterCondition: Condition) 
		extends ManyApiKeysAccess with SubView
}

/**
  * A common trait for access points which target multiple ApiKeys at a time
  * @author Mikko Hilpinen
  * @since 2021-10-25
  */
trait ManyApiKeysAccess extends ManyRowModelAccess[ApiKey] with Indexed with FilterableView[ManyApiKeysAccess]
{
	// COMPUTED	--------------------
	
	/**
	  * tokens of the accessible ApiKeys
	  */
	def tokens(implicit connection: Connection) = pullColumn(model.tokenColumn)
		.flatMap { value => value.string }
	
	/**
	  * names of the accessible ApiKeys
	  */
	def names(implicit connection: Connection) = pullColumn(model.nameColumn)
		.flatMap { value => value.string }
	
	/**
	  * creationTimes of the accessible ApiKeys
	  */
	def creationTimes(implicit connection: Connection) = 
		pullColumn(model.createdColumn).flatMap { value => value.instant }
	
	def ids(implicit connection: Connection) = pullColumn(index).flatMap { id => id.int }
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = ApiKeyModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = ApiKeyFactory
	
	override def filter(additionalCondition: Condition): ManyApiKeysAccess = 
		new ManyApiKeysAccess.ManyApiKeysSubView(this, additionalCondition)
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the created of the targeted ApiKey instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any ApiKey instance was affected
	  */
	def creationTimes_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * Updates the name of the targeted ApiKey instance(s)
	  * @param newName A new name to assign
	  * @return Whether any ApiKey instance was affected
	  */
	def names_=(newName: String)(implicit connection: Connection) = putColumn(model.nameColumn, newName)
	
	/**
	  * Updates the token of the targeted ApiKey instance(s)
	  * @param newToken A new token to assign
	  * @return Whether any ApiKey instance was affected
	  */
	def tokens_=(newToken: String)(implicit connection: Connection) = putColumn(model.tokenColumn, newToken)
}

