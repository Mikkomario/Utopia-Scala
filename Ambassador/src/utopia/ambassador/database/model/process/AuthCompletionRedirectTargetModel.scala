package utopia.ambassador.database.model.process

import utopia.ambassador.database.factory.process.AuthCompletionRedirectTargetFactory
import utopia.ambassador.model.partial.process.AuthCompletionRedirectTargetData
import utopia.ambassador.model.stored.process.AuthCompletionRedirectTarget
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConversions._
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.nosql.storable.DataInserter

object AuthCompletionRedirectTargetModel extends DataInserter[AuthCompletionRedirectTargetModel,
	AuthCompletionRedirectTarget, AuthCompletionRedirectTargetData]
{
	// ATTRIBUTES   ------------------------
	
	/**
	  * Name of the property that contains success (true) / failure (false) / default (None) result filter
	  */
	val resultFilterAttName = "resultStateFilter"
	
	
	// COMPUTED ----------------------------
	
	/**
	  * @return the factory used by this model type
	  */
	def factory = AuthCompletionRedirectTargetFactory
	
	/**
	  * Column that contains success (true) / failure (false) / default (None) result filter
	  */
	def resultFilterColumn = table(resultFilterAttName)
	
	/**
	  * @return A model that is limited to denials of access only
	  */
	def limitedToDenials = withLimitedToDenials(isLimited = true)
	/**
	  * @return A model that is not limited to denials of access only
	  */
	def notLimitedToDenials = withLimitedToDenials(isLimited = false)
	
	/**
	  * @return A condition that only returns denials of access
	  */
	def denialsOfAccessCondition = limitedToDenials.toCondition
	/**
	  * @return A condition that only returns cases where user allowed (didn't deny) access
	  */
	def accessAllowedCondition = notLimitedToDenials.toCondition
	
	
	// IMPLEMENTED  -------------------------
	
	override def table = factory.table
	
	override def apply(data: AuthCompletionRedirectTargetData) =
		apply(None, Some(data.preparationId), Some(data.url), data.resultFilter.successFilter,
			Some(data.resultFilter.deniedFilter))
	
	override protected def complete(id: Value, data: AuthCompletionRedirectTargetData) =
		AuthCompletionRedirectTarget(id.getInt, data)
	
	
	// OTHER    ------------------------------
	
	/**
	  * @param preparationId Id of the preparation for which these targets have been specified
	  * @return A model with that preparation id
	  */
	def withPreparationId(preparationId: Int) =
		apply(preparationId = Some(preparationId))
	/**
	  * @param isLimited Whether this target should be limited to denials of access only
	  * @return A model with that limitation state / filter
	  */
	def withLimitedToDenials(isLimited: Boolean) =
		apply(isLimitedToDenials = Some(isLimited))
}

/**
  * Used for interacting with prepared redirect targets in the DB
  * @author Mikko Hilpinen
  * @since 18.7.2021, v1.0
  */
case class AuthCompletionRedirectTargetModel(id: Option[Int] = None, preparationId: Option[Int] = None,
                                             url: Option[String] = None, resultStateFilter: Option[Boolean] = None,
                                             isLimitedToDenials: Option[Boolean] = None)
	extends StorableWithFactory[AuthCompletionRedirectTarget]
{
	override def factory = AuthCompletionRedirectTargetModel.factory
	
	override def valueProperties = Vector("id" -> id, "preparationId" -> preparationId, "url" -> url,
		"resultStateFilter" -> resultStateFilter, "isLimitedToDenials" -> isLimitedToDenials)
}
