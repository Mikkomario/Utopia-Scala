package utopia.exodus.database.access.many.auth

import java.time.Instant
import utopia.exodus.database.factory.auth.ScopeFactory
import utopia.exodus.database.model.auth.ScopeModel
import utopia.exodus.model.stored.auth.Scope
import utopia.flow.generic.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.{FilterableView, SubView}
import utopia.vault.sql.Condition

object ManyScopesAccess
{
	// NESTED	--------------------
	
	private class ManyScopesSubView(override val parent: ManyRowModelAccess[Scope], 
		override val filterCondition: Condition) 
		extends ManyScopesAccess with SubView
}

/**
  * A common trait for access points which target multiple scopes at a time
  * @author Mikko Hilpinen
  * @since 18.02.2022, v4.0
  */
trait ManyScopesAccess extends ManyRowModelAccess[Scope] with FilterableView[ManyScopesAccess] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * names of the accessible scopes
	  */
	def names(implicit connection: Connection) = pullColumn(model.nameColumn).map { v => v.getString }
	
	/**
	  * creation times of the accessible scopes
	  */
	def creationTimes(implicit connection: Connection) = pullColumn(model.createdColumn).map { _.getInstant }
	
	def ids(implicit connection: Connection) = pullColumn(index).flatMap { id => id.int }
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = ScopeModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = ScopeFactory
	
	override def filter(additionalCondition: Condition): ManyScopesAccess = 
		new ManyScopesAccess.ManyScopesSubView(this, additionalCondition)
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the creation times of the targeted scopes
	  * @param newCreated A new created to assign
	  * @return Whether any scope was affected
	  */
	def creationTimes_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * Updates the names of the targeted scopes
	  * @param newName A new name to assign
	  * @return Whether any scope was affected
	  */
	def names_=(newName: String)(implicit connection: Connection) = putColumn(model.nameColumn, newName)
}

