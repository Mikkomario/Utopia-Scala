package utopia.exodus.database.access.single.auth

import java.time.Instant
import utopia.exodus.database.factory.auth.ScopeFactory
import utopia.exodus.database.model.auth.ScopeModel
import utopia.exodus.model.stored.auth.Scope
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.access.template.model.DistinctModelAccess
import utopia.vault.nosql.template.Indexed

/**
  * A common trait for access points that return individual and distinct scopes.
  * @author Mikko Hilpinen
  * @since 18.02.2022, v4.0
  */
trait UniqueScopeAccess 
	extends SingleRowModelAccess[Scope] with DistinctModelAccess[Scope, Option[Scope], Value] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Technical name or identifier of this scope. None if no instance (or value) was found.
	  */
	def name(implicit connection: Connection) = pullColumn(model.nameColumn).string
	
	/**
	  * Time when this scope was first created. None if no instance (or value) was found.
	  */
	def created(implicit connection: Connection) = pullColumn(model.createdColumn).instant
	
	def id(implicit connection: Connection) = pullColumn(index).int
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = ScopeModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = ScopeFactory
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the creation times of the targeted scopes
	  * @param newCreated A new created to assign
	  * @return Whether any scope was affected
	  */
	def created_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * Updates the names of the targeted scopes
	  * @param newName A new name to assign
	  * @return Whether any scope was affected
	  */
	def name_=(newName: String)(implicit connection: Connection) = putColumn(model.nameColumn, newName)
}

