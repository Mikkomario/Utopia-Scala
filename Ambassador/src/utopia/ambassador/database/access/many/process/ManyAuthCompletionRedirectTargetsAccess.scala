package utopia.ambassador.database.access.many.process

import utopia.ambassador.database.factory.process.AuthCompletionRedirectTargetFactory
import utopia.ambassador.database.model.process.AuthCompletionRedirectTargetModel
import utopia.ambassador.model.enumeration.GrantLevel
import utopia.ambassador.model.enumeration.GrantLevel.{AccessDenied, AccessFailed, FullAccess, PartialAccess}
import utopia.ambassador.model.stored.process.AuthCompletionRedirectTarget
import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.{FilterableView, SubView}
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
		with FilterableView[ManyAuthCompletionRedirectTargetsAccess]
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
	
	/**
	  * @return An access point to targets that apply to the full access granted -case
	  */
	def forFullAccess = forResult(FullAccess)
	
	
	// IMPLEMENTED	--------------------
	
	override protected def self = this
	
	override def factory = AuthCompletionRedirectTargetFactory
	
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
	  * @param wasSuccess A result state: Success (true) or Failure (false)
	  * @param didDenyAccess Whether the user denied access partially or fully (default = false)
	  * @return An access point to targets that apply to that state
	  */
	def forResult(wasSuccess: Boolean, didDenyAccess: Boolean = false) =
	{
		val resultCondition = model.withResultStateFilter(wasSuccess).toCondition
		val finalResultCondition = if (didDenyAccess) resultCondition else
			resultCondition && model.notLimitedToDenialsCondition
		
		filter(model.anyResultStateCondition || finalResultCondition)
	}
	/**
	  * @param grantLevel Grant level given by the user
	  * @return An access point to available redirect targets for that result
	  */
	def forResult(grantLevel: GrantLevel): ManyAuthCompletionRedirectTargetsAccess =
		grantLevel match
		{
			case PartialAccess => forResult(wasSuccess = true, didDenyAccess = true)
			case FullAccess => forResult(wasSuccess = true)
			case AccessDenied => forResult(wasSuccess = false, didDenyAccess = true)
			case AccessFailed => forResult(wasSuccess = false)
		}
	
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

