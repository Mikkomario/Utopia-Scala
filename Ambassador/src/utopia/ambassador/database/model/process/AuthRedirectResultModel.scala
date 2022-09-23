package utopia.ambassador.database.model.process

import java.time.Instant
import utopia.ambassador.database.factory.process.AuthRedirectResultFactory
import utopia.ambassador.model.partial.process.AuthRedirectResultData
import utopia.ambassador.model.stored.process.AuthRedirectResult
import utopia.flow.collection.value.typeless.Value
import utopia.flow.generic.ValueConversions._
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.nosql.storable.DataInserter

/**
  * Used for constructing AuthRedirectResultModel instances and for inserting AuthRedirectResults to the database
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
object AuthRedirectResultModel 
	extends DataInserter[AuthRedirectResultModel, AuthRedirectResult, AuthRedirectResultData]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Name of the property that contains AuthRedirectResult redirectId
	  */
	val redirectIdAttName = "redirectId"
	
	/**
	  * Name of the property that contains AuthRedirectResult didReceiveCode
	  */
	val didReceiveCodeAttName = "didReceiveCode"
	
	/**
	  * Name of the property that contains AuthRedirectResult didReceiveToken
	  */
	val didReceiveTokenAttName = "didReceiveToken"
	
	/**
	  * Name of the property that contains AuthRedirectResult created
	  */
	val createdAttName = "created"
	
	
	// COMPUTED	--------------------
	
	/**
	  * Column that contains AuthRedirectResult redirectId
	  */
	def redirectIdColumn = table(redirectIdAttName)
	
	/**
	  * Column that contains AuthRedirectResult didReceiveCode
	  */
	def didReceiveCodeColumn = table(didReceiveCodeAttName)
	
	/**
	  * Column that contains AuthRedirectResult didReceiveToken
	  */
	def didReceiveTokenColumn = table(didReceiveTokenAttName)
	
	/**
	  * Column that contains AuthRedirectResult created
	  */
	def createdColumn = table(createdAttName)
	
	/**
	  * The factory object used by this model type
	  */
	def factory = AuthRedirectResultFactory
	
	
	// IMPLEMENTED	--------------------
	
	override def table = factory.table
	
	override def apply(data: AuthRedirectResultData) = 
		apply(None, Some(data.redirectId), Some(data.didReceiveCode), Some(data.didReceiveToken), 
			Some(data.created))
	
	override def complete(id: Value, data: AuthRedirectResultData) = AuthRedirectResult(id.getInt, data)
	
	
	// OTHER	--------------------
	
	/**
	  * @param created Time when this AuthRedirectResult was first created
	  * @return A model containing only the specified created
	  */
	def withCreated(created: Instant) = apply(created = Some(created))
	
	/**
	  * @param didReceiveCode Whether an authentication code was included in the request (implies success)
	  * @return A model containing only the specified didReceiveCode
	  */
	def withDidReceiveCode(didReceiveCode: Boolean) = apply(didReceiveCode = Some(didReceiveCode))
	
	/**
	  * @param didReceiveToken Whether authentication tokens were successfully acquired
	  * @return A model containing only the specified didReceiveToken
	  */
	def withDidReceiveToken(didReceiveToken: Boolean) = apply(didReceiveToken = Some(didReceiveToken))
	
	/**
	  * @param id A AuthRedirectResult id
	  * @return A model with that id
	  */
	def withId(id: Int) = apply(Some(id))
	
	/**
	  * @param redirectId Id of the redirection event this result completes
	  * @return A model containing only the specified redirectId
	  */
	def withRedirectId(redirectId: Int) = apply(redirectId = Some(redirectId))
}

/**
  * Used for interacting with AuthRedirectResults in the database
  * @param id AuthRedirectResult database id
  * @param redirectId Id of the redirection event this result completes
  * @param didReceiveCode Whether an authentication code was included in the request (implies success)
  * @param didReceiveToken Whether authentication tokens were successfully acquired
  * @param created Time when this AuthRedirectResult was first created
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
case class AuthRedirectResultModel(id: Option[Int] = None, redirectId: Option[Int] = None, 
	didReceiveCode: Option[Boolean] = None, didReceiveToken: Option[Boolean] = None, 
	created: Option[Instant] = None) 
	extends StorableWithFactory[AuthRedirectResult]
{
	// IMPLEMENTED	--------------------
	
	override def factory = AuthRedirectResultModel.factory
	
	override def valueProperties = 
	{
		import AuthRedirectResultModel._
		Vector("id" -> id, redirectIdAttName -> redirectId, didReceiveCodeAttName -> didReceiveCode, 
			didReceiveTokenAttName -> didReceiveToken, createdAttName -> created)
	}
	
	
	// OTHER	--------------------
	
	/**
	  * @param created A new created
	  * @return A new copy of this model with the specified created
	  */
	def withCreated(created: Instant) = copy(created = Some(created))
	
	/**
	  * @param didReceiveCode A new didReceiveCode
	  * @return A new copy of this model with the specified didReceiveCode
	  */
	def withDidReceiveCode(didReceiveCode: Boolean) = copy(didReceiveCode = Some(didReceiveCode))
	
	/**
	  * @param didReceiveToken A new didReceiveToken
	  * @return A new copy of this model with the specified didReceiveToken
	  */
	def withDidReceiveToken(didReceiveToken: Boolean) = copy(didReceiveToken = Some(didReceiveToken))
	
	/**
	  * @param redirectId A new redirectId
	  * @return A new copy of this model with the specified redirectId
	  */
	def withRedirectId(redirectId: Int) = copy(redirectId = Some(redirectId))
}

