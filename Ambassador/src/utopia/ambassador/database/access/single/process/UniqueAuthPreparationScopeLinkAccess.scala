package utopia.ambassador.database.access.single.process

import utopia.ambassador.database.factory.process.AuthPreparationScopeLinkFactory
import utopia.ambassador.database.model.process.AuthPreparationScopeLinkModel
import utopia.ambassador.model.stored.process.AuthPreparationScopeLink
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.access.template.model.DistinctModelAccess
import utopia.vault.nosql.template.Indexed

/**
  * A common trait for access points that return individual and distinct AuthPreparationScopeLinks.
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
trait UniqueAuthPreparationScopeLinkAccess 
	extends SingleRowModelAccess[AuthPreparationScopeLink] 
		with DistinctModelAccess[AuthPreparationScopeLink, Option[AuthPreparationScopeLink], Value] 
		with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Id of the described OAuth preparation. None if no instance (or value) was found.
	  */
	def preparationId(implicit connection: Connection) = pullColumn(model.preparationIdColumn).int
	
	/**
	  * Id of the requested scope. None if no instance (or value) was found.
	  */
	def scopeId(implicit connection: Connection) = pullColumn(model.scopeIdColumn).int
	
	def id(implicit connection: Connection) = pullColumn(index).int
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = AuthPreparationScopeLinkModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = AuthPreparationScopeLinkFactory
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the preparationId of the targeted AuthPreparationScopeLink instance(s)
	  * @param newPreparationId A new preparationId to assign
	  * @return Whether any AuthPreparationScopeLink instance was affected
	  */
	def preparationId_=(newPreparationId: Int)(implicit connection: Connection) = 
		putColumn(model.preparationIdColumn, newPreparationId)
	
	/**
	  * Updates the scopeId of the targeted AuthPreparationScopeLink instance(s)
	  * @param newScopeId A new scopeId to assign
	  * @return Whether any AuthPreparationScopeLink instance was affected
	  */
	def scopeId_=(newScopeId: Int)(implicit connection: Connection) = putColumn(model.scopeIdColumn, 
		newScopeId)
}

