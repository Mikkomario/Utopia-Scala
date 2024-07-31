package utopia.exodus.database.access.many.auth

import utopia.exodus.database.model.auth.ScopeModel
import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.FilterableView

import java.time.Instant

/**
  * A common trait for access points which target multiple scopes or similar instances at a time
  * @author Mikko Hilpinen
  * @since 19.02.2022, v4.0
  */
trait ManyScopesAccessLike[+A, +Repr <: ManyModelAccess[A]] 
	extends ManyModelAccess[A] with Indexed with FilterableView[Repr]
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

