package utopia.ambassador.database.model.process

import utopia.ambassador.database.factory.process.AuthRedirectResultFactory
import utopia.ambassador.model.partial.process.AuthRedirectResultData
import utopia.ambassador.model.stored.process.AuthRedirectResult
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConversions._
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.model.template.DataInserter

import java.time.Instant

object AuthRedirectResultModel
	extends DataInserter[AuthRedirectResultModel, AuthRedirectResult, AuthRedirectResultData]
{
	// COMPUTED ----------------------------
	
	/**
	  * @return The factory used by this model type
	  */
	def factory = AuthRedirectResultFactory
	
	
	// IMPLEMENTED  ------------------------
	
	override def table = factory.table
	
	override def apply(data: AuthRedirectResultData) =
		apply(None, Some(data.redirectId), Some(data.grantLevel.grantedAccess), Some(data.grantLevel.enablesAccess),
			Some(data.grantLevel.isFull), Some(data.created))
	
	override protected def complete(id: Value, data: AuthRedirectResultData) = AuthRedirectResult(id.getInt, data)
	
	
	// OTHER    ----------------------------
	
	/**
	  * @param redirectId id of the associated redirection attempt
	  * @return A model with that redirection id
	  */
	def withRedirectId(redirectId: Int) = apply(redirectId = Some(redirectId))
}

/**
  * Used for interacting with authentication redirection results in the DB
  * @author Mikko Hilpinen
  * @since 18.7.2021, v1.0
  */
case class AuthRedirectResultModel(id: Option[Int] = None, redirectId: Option[Int] = None,
                                   didReceiveCode: Option[Boolean] = None, didReceiveToken: Option[Boolean] = None,
                                   didReceiveFullScope: Option[Boolean] = None, created: Option[Instant] = None)
	extends StorableWithFactory[AuthRedirectResult]
{
	override def factory = AuthRedirectResultModel.factory
	
	override def valueProperties = Vector("id" -> id, "redirectId" -> redirectId, "didReceiveCode" -> didReceiveCode,
		"didReceiveToken" -> didReceiveToken, "didReceiveFullScope" -> didReceiveFullScope, "created" -> created)
}
