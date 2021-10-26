package utopia.ambassador.database.access.many.process

import utopia.ambassador.database.factory.process.AuthCompletionRedirectTargetFactory
import utopia.ambassador.database.model.process.AuthCompletionRedirectTargetModel
import utopia.ambassador.model.stored.process.AuthCompletionRedirectTarget
import utopia.flow.generic.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.SubView
import utopia.vault.sql.Condition

object ManyAuthCompletionRedirectTargetsAccess
{
	// NESTED	--------------------
	
	private class ManyAuthCompletionRedirectTargetsSubView(override val parent: ManyRowModelAccess[AuthCompletionRedirectTarget], 
		override val filterCondition: Condition) 
		extends ManyAuthCompletionRedirectTargetsAccess with SubView
}

/**
  * A common trait for access points which target multiple AuthCompletionRedirectTargets at a time
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
trait ManyAuthCompletionRedirectTargetsAccess 
	extends ManyRowModelAccess[AuthCompletionRedirectTarget] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * preparationIds of the accessible AuthCompletionRedirectTargets
	  */
	def preparationIds(implicit connection: Connection) = 
		pullColumn(model.preparationIdColumn).flatMap { value => value.int }
	/**
	  * urls of the accessible AuthCompletionRedirectTargets
	  */
	def urls(implicit connection: Connection) = pullColumn(model.urlColumn).flatMap { value => value.string }
	/**
	  * resultStateFilters of the accessible AuthCompletionRedirectTargets
	  */
	def resultStateFilters(implicit connection: Connection) = 
		pullColumn(model.resultStateFilterColumn).flatMap { value => value.boolean }
	/**
	  * areLimitedToDenials of the accessible AuthCompletionRedirectTargets
	  */
	def areLimitedToDenials(implicit connection: Connection) = 
		pullColumn(model.isLimitedToDenialsColumn).flatMap { value => value.boolean }
	
	def ids(implicit connection: Connection) = pullColumn(index).flatMap { id => id.int }
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = AuthCompletionRedirectTargetModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = AuthCompletionRedirectTargetFactory
	
	override protected def defaultOrdering = None
	
	override def filter(additionalCondition: Condition): ManyAuthCompletionRedirectTargetsAccess = 
		new ManyAuthCompletionRedirectTargetsAccess.ManyAuthCompletionRedirectTargetsSubView(this, 
			additionalCondition)
	
	
	// OTHER	--------------------
	
	/**
	  * @param preparationId Id of the targeted authentication preparation
	  * @return An access point to that preparation's redirection targets
	  */
	def forPreparationWithId(preparationId: Int) =
		filter(model.withPreparationId(preparationId).toCondition)
	
	/**
	  * Updates the isLimitedToDenials of the targeted AuthCompletionRedirectTarget instance(s)
	  * @param newIsLimitedToDenials A new isLimitedToDenials to assign
	  * @return Whether any AuthCompletionRedirectTarget instance was affected
	  */
	def areLimitedToDenials_=(newIsLimitedToDenials: Boolean)(implicit connection: Connection) = 
		putColumn(model.isLimitedToDenialsColumn, newIsLimitedToDenials)
	/**
	  * Updates the preparationId of the targeted AuthCompletionRedirectTarget instance(s)
	  * @param newPreparationId A new preparationId to assign
	  * @return Whether any AuthCompletionRedirectTarget instance was affected
	  */
	def preparationIds_=(newPreparationId: Int)(implicit connection: Connection) = 
		putColumn(model.preparationIdColumn, newPreparationId)
	/**
	  * Updates the resultStateFilter of the targeted AuthCompletionRedirectTarget instance(s)
	  * @param newResultStateFilter A new resultStateFilter to assign
	  * @return Whether any AuthCompletionRedirectTarget instance was affected
	  */
	def resultStateFilters_=(newResultStateFilter: Boolean)(implicit connection: Connection) = 
		putColumn(model.resultStateFilterColumn, newResultStateFilter)
	/**
	  * Updates the url of the targeted AuthCompletionRedirectTarget instance(s)
	  * @param newUrl A new url to assign
	  * @return Whether any AuthCompletionRedirectTarget instance was affected
	  */
	def urls_=(newUrl: String)(implicit connection: Connection) = putColumn(model.urlColumn, newUrl)
}

