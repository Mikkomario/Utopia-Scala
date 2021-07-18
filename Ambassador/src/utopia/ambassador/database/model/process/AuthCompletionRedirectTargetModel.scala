package utopia.ambassador.database.model.process

import utopia.ambassador.database.factory.process.AuthCompletionRedirectTargetFactory
import utopia.ambassador.model.partial.process.AuthCompletionRedirectTargetData
import utopia.ambassador.model.stored.process.AuthCompletionRedirectTarget
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConversions._
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.model.template.DataInserter

object AuthCompletionRedirectTargetModel extends DataInserter[AuthCompletionRedirectTargetModel,
	AuthCompletionRedirectTarget, AuthCompletionRedirectTargetData]
{
	// COMPUTED ----------------------------
	
	/**
	  * @return the factory used by this model type
	  */
	def factory = AuthCompletionRedirectTargetFactory
	
	
	// IMPLEMENTED  -------------------------
	
	override def table = factory.table
	
	override def apply(data: AuthCompletionRedirectTargetData) =
		apply(None, Some(data.preparationId), Some(data.url), data.resultFilter.successFilter,
			Some(data.resultFilter.deniedFilter))
	
	override protected def complete(id: Value, data: AuthCompletionRedirectTargetData) =
		AuthCompletionRedirectTarget(id.getInt, data)
}

/**
  * Used for interacting with prepared redirect targets in the DB
  * @author Mikko Hilpinen
  * @since 18.7.2021, v1.0
  */
case class AuthCompletionRedirectTargetModel(id: Option[Int], preparationId: Option[Int] = None,
                                             url: Option[String] = None, resultStateFilter: Option[Boolean] = None,
                                             isLimitedToDenials: Option[Boolean] = None)
	extends StorableWithFactory[AuthCompletionRedirectTarget]
{
	override def factory = AuthCompletionRedirectTargetModel.factory
	
	override def valueProperties = Vector("id" -> id, "preparationId" -> preparationId, "url" -> url,
		"resultStateFilter" -> resultStateFilter, "isLimitedToDenials" -> isLimitedToDenials)
}
