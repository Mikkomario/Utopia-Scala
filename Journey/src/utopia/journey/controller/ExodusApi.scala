package utopia.journey.controller

import utopia.access.http.Headers
import utopia.annex.controller.Api
import utopia.disciple.http.request.StringBody
import utopia.flow.datastructure.immutable.Value
import utopia.journey.model.UserCredentials

/**
  * An interface used for accessing the Exodus API
  * @author Mikko Hilpinen
  * @since 20.6.2020, v1
  */
class ExodusApi(override val rootPath: String, credentials: Either[UserCredentials, String], initialSessionKey: String) extends Api
{
	// ATTRIBUTES	---------------------------
	
	private var sessionKey = initialSessionKey
	
	
	// IMPLEMENTED	---------------------------
	
	override protected def headers = Headers.currentDateHeaders.withBearerAuthorization(sessionKey)
	
	override protected def makeRequestBody(bodyContent: Value) = StringBody.json(bodyContent.toJson)
}
