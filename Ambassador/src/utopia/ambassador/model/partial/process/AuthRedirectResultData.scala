package utopia.ambassador.model.partial.process

import utopia.ambassador.model.enumeration.GrantLevel
import utopia.flow.collection.value.typeless.Model

import java.time.Instant
import utopia.flow.generic.ModelConvertible
import utopia.flow.generic.ValueConversions._
import utopia.flow.time.Now

object AuthRedirectResultData
{
	/**
	  * @param redirectId Id of the redirection event this result completes
	  * @param grantLevel Acquired grant level
	  * @return A redirection result data model
	  */
	def apply(redirectId: Int, grantLevel: GrantLevel): AuthRedirectResultData =
		apply(redirectId, grantLevel.grantedAccess, grantLevel.enablesAccess)
}

/**
  * Records the cases when the user arrives back from the 3rd party OAuth service, 
	whether the authentication succeeded or not.
  * @param redirectId Id of the redirection event this result completes
  * @param didReceiveCode Whether an authentication code was included in the request (implies success)
  * @param didReceiveToken Whether authentication tokens were successfully acquired
  * @param created Time when this AuthRedirectResult was first created
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
case class AuthRedirectResultData(redirectId: Int, didReceiveCode: Boolean = false, 
	didReceiveToken: Boolean = false, created: Instant = Now) 
	extends ModelConvertible
{
	// IMPLEMENTED	--------------------
	
	override def toModel = 
		Model(Vector("redirect_id" -> redirectId, "did_receive_code" -> didReceiveCode, 
			"did_receive_token" -> didReceiveToken, "created" -> created))
}

