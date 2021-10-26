package utopia.ambassador.model.partial.process

import utopia.flow.datastructure.immutable.Model
import utopia.flow.generic.ModelConvertible
import utopia.flow.generic.ValueConversions._

/**
  * Used for storing client-given rules for redirecting the user after the OAuth process completion. Given during the OAuth preparation.
  * @param preparationId Id of the preparation during which these targets were specified
  * @param url Url where the user will be redirected
  * @param resultStateFilter True when only successes are accepted. False when only failures are accepted. None when both are accepted.
  * @param isLimitedToDenials Whether this target is only used for denial of access -cases
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
case class AuthCompletionRedirectTargetData(preparationId: Int, url: String, 
	resultStateFilter: Option[Boolean] = None, isLimitedToDenials: Boolean = false) 
	extends ModelConvertible
{
	// IMPLEMENTED	--------------------
	
	override def toModel = 
		Model(Vector("preparation_id" -> preparationId, "url" -> url, 
			"result_state_filter" -> resultStateFilter, "is_limited_to_denials" -> isLimitedToDenials))
}

