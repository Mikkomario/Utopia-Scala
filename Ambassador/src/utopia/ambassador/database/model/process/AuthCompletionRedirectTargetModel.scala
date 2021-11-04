package utopia.ambassador.database.model.process

import utopia.ambassador.database.factory.process.AuthCompletionRedirectTargetFactory
import utopia.ambassador.model.partial.process.AuthCompletionRedirectTargetData
import utopia.ambassador.model.stored.process.AuthCompletionRedirectTarget
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConversions._
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.nosql.storable.DataInserter

/**
  * Used for constructing AuthCompletionRedirectTargetModel instances and for inserting AuthCompletionRedirectTargets to the database
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
object AuthCompletionRedirectTargetModel 
	extends DataInserter[AuthCompletionRedirectTargetModel, AuthCompletionRedirectTarget, 
		AuthCompletionRedirectTargetData]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Name of the property that contains AuthCompletionRedirectTarget preparationId
	  */
	val preparationIdAttName = "preparationId"
	/**
	  * Name of the property that contains AuthCompletionRedirectTarget url
	  */
	val urlAttName = "url"
	/**
	  * Name of the property that contains AuthCompletionRedirectTarget resultStateFilter
	  */
	val resultStateFilterAttName = "resultStateFilter"
	/**
	  * Name of the property that contains AuthCompletionRedirectTarget isLimitedToDenials
	  */
	val isLimitedToDenialsAttName = "isLimitedToDenials"
	
	/**
	  * A condition that only returns targets that are limited to success states
	  */
	lazy val onlySuccessesCondition = limitedToSuccesses.toCondition
	/**
	  * A condition that only returns targets that are limited to failure states
	  */
	lazy val onlyFailuresCondition = limitedToFailures.toCondition
	/**
	  * A condition that only returns targets that are not limited to a specific result state
	  */
	lazy val anyResultStateCondition = resultStateFilterColumn.isNull
	/**
	  * A condition that only returns targets that are limited to denials of access
	  */
	lazy val onlyDenialsCondition = limitedToDenials.toCondition
	/**
	  * A condition that only returns targets that are not limited to denials of access
	  */
	lazy val notLimitedToDenialsCondition = notLimitedToDenials.toCondition
	
	
	// COMPUTED	--------------------
	
	/**
	  * Column that contains AuthCompletionRedirectTarget preparationId
	  */
	def preparationIdColumn = table(preparationIdAttName)
	/**
	  * Column that contains AuthCompletionRedirectTarget url
	  */
	def urlColumn = table(urlAttName)
	/**
	  * Column that contains AuthCompletionRedirectTarget resultStateFilter
	  */
	def resultStateFilterColumn = table(resultStateFilterAttName)
	/**
	  * Column that contains AuthCompletionRedirectTarget isLimitedToDenials
	  */
	def isLimitedToDenialsColumn = table(isLimitedToDenialsAttName)
	
	/**
	  * The factory object used by this model type
	  */
	def factory = AuthCompletionRedirectTargetFactory
	
	/**
	  * @return A model that has resultStateFilter = true / only handles success
	  */
	def limitedToSuccesses = withResultStateFilter(resultStateFilter = true)
	/**
	  * @return A model that has resultStateFilter = false / only handles failures
	  */
	def limitedToFailures = withResultStateFilter(resultStateFilter = false)
	/**
	  * @return A model that has isLimitedToDenials = true / only handles denials of access
	  */
	def limitedToDenials = withIsLimitedToDenials(isLimitedToDenials = true)
	/**
	  * @return A model that has isLimitedToDenials = false (not limited to denials of access)
	  */
	def notLimitedToDenials = withIsLimitedToDenials(isLimitedToDenials = false)
	
	
	// IMPLEMENTED	--------------------
	
	override def table = factory.table
	
	override def apply(data: AuthCompletionRedirectTargetData) = 
		apply(None, Some(data.preparationId), Some(data.url), data.resultStateFilter, 
			Some(data.isLimitedToDenials))
	
	override def complete(id: Value, data: AuthCompletionRedirectTargetData) = 
		AuthCompletionRedirectTarget(id.getInt, data)
	
	
	// OTHER	--------------------
	
	/**
	  * @param id A AuthCompletionRedirectTarget id
	  * @return A model with that id
	  */
	def withId(id: Int) = apply(Some(id))
	/**
	  * @param isLimitedToDenials Whether this target is only used for denial of access -cases
	  * @return A model containing only the specified isLimitedToDenials
	  */
	def withIsLimitedToDenials(isLimitedToDenials: Boolean) = apply(isLimitedToDenials = Some(isLimitedToDenials))
	/**
	  * @param preparationId Id of the preparation during which these targets were specified
	  * @return A model containing only the specified preparationId
	  */
	def withPreparationId(preparationId: Int) = apply(preparationId = Some(preparationId))
	/**
	  * @param resultStateFilter True when only successes are accepted. False when only failures are accepted. None when both are accepted.
	  * @return A model containing only the specified resultStateFilter
	  */
	def withResultStateFilter(resultStateFilter: Boolean) = apply(resultStateFilter = Some(resultStateFilter))
	/**
	  * @param url Url where the user will be redirected
	  * @return A model containing only the specified url
	  */
	def withUrl(url: String) = apply(url = Some(url))
}

/**
  * Used for interacting with AuthCompletionRedirectTargets in the database
  * @param id AuthCompletionRedirectTarget database id
  * @param preparationId Id of the preparation during which these targets were specified
  * @param url Url where the user will be redirected
  * @param resultStateFilter True when only successes are accepted. False when only failures are accepted. None when both are accepted.
  * @param isLimitedToDenials Whether this target is only used for denial of access -cases
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
case class AuthCompletionRedirectTargetModel(id: Option[Int] = None, preparationId: Option[Int] = None, 
	url: Option[String] = None, resultStateFilter: Option[Boolean] = None, 
	isLimitedToDenials: Option[Boolean] = None) 
	extends StorableWithFactory[AuthCompletionRedirectTarget]
{
	// IMPLEMENTED	--------------------
	
	override def factory = AuthCompletionRedirectTargetModel.factory
	
	override def valueProperties = 
	{
		import AuthCompletionRedirectTargetModel._
		Vector("id" -> id, preparationIdAttName -> preparationId, urlAttName -> url, 
			resultStateFilterAttName -> resultStateFilter, isLimitedToDenialsAttName -> isLimitedToDenials)
	}
	
	
	// OTHER	--------------------
	
	/**
	  * @param isLimitedToDenials A new isLimitedToDenials
	  * @return A new copy of this model with the specified isLimitedToDenials
	  */
	def withIsLimitedToDenials(isLimitedToDenials: Boolean) = copy(isLimitedToDenials = Some(isLimitedToDenials))
	
	/**
	  * @param preparationId A new preparationId
	  * @return A new copy of this model with the specified preparationId
	  */
	def withPreparationId(preparationId: Int) = copy(preparationId = Some(preparationId))
	
	/**
	  * @param resultStateFilter A new resultStateFilter
	  * @return A new copy of this model with the specified resultStateFilter
	  */
	def withResultStateFilter(resultStateFilter: Boolean) = copy(resultStateFilter = Some(resultStateFilter))
	
	/**
	  * @param url A new url
	  * @return A new copy of this model with the specified url
	  */
	def withUrl(url: String) = copy(url = Some(url))
}

