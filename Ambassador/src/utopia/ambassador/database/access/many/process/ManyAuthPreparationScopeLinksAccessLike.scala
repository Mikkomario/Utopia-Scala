package utopia.ambassador.database.access.many.process

import utopia.ambassador.database.model.process.AuthPreparationScopeLinkModel
import utopia.flow.generic.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.sql.Condition

/**
  * A common trait for access points which target multiple AuthPreparationScopeLinks or similar instances at a time
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
trait ManyAuthPreparationScopeLinksAccessLike[+A, +Repr <: ManyModelAccess[A]]
	extends ManyModelAccess[A] with Indexed
{
	// ABSTRACT --------------------
	
	protected def _filter(condition: Condition): Repr
	
	
	// COMPUTED	--------------------
	
	/**
	  * preparationIds of the accessible AuthPreparationScopeLinks
	  */
	def preparationIds(implicit connection: Connection) = 
		pullColumn(model.preparationIdColumn).flatMap { value => value.int }
	/**
	  * scopeIds of the accessible AuthPreparationScopeLinks
	  */
	def scopeIds(implicit connection: Connection) = pullColumn(model.scopeIdColumn)
		.flatMap { value => value.int }
	
	def ids(implicit connection: Connection) = pullColumn(index).flatMap { id => id.int }
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = AuthPreparationScopeLinkModel
	
	
	// IMPLEMENTED	--------------------
	
	override def filter(additionalCondition: Condition) = _filter(additionalCondition)
	
	
	// OTHER	--------------------
	
	/**
	  * @param preparationId Id of the targeted authentication preparation
	  * @return An access point to scope links attached to that preparation
	  */
	def withPreparationId(preparationId: Int) =
		filter(model.withPreparationId(preparationId).toCondition)
	
	/**
	  * Updates the preparationId of the targeted AuthPreparationScopeLink instance(s)
	  * @param newPreparationId A new preparationId to assign
	  * @return Whether any AuthPreparationScopeLink instance was affected
	  */
	def preparationIds_=(newPreparationId: Int)(implicit connection: Connection) = 
		putColumn(model.preparationIdColumn, newPreparationId)
	/**
	  * Updates the scopeId of the targeted AuthPreparationScopeLink instance(s)
	  * @param newScopeId A new scopeId to assign
	  * @return Whether any AuthPreparationScopeLink instance was affected
	  */
	def scopeIds_=(newScopeId: Int)(implicit connection: Connection) = putColumn(model.scopeIdColumn, 
		newScopeId)
}

