package utopia.journey.controller

import utopia.access.http.Headers
import utopia.annex.controller.Api
import utopia.disciple.http.request.StringBody
import utopia.flow.datastructure.immutable.Value

/**
  * This API is used before authorization (session key) is acquired
  * @author Mikko Hilpinen
  * @since 21.6.2020, v1
  */
class UnauthorizedExodusApi(override val rootPath: String) extends Api
{
	// IMPLEMENTED	-----------------------------
	
	override protected def headers = Headers.currentDateHeaders
	
	override protected def makeRequestBody(bodyContent: Value) = StringBody.json(bodyContent.toJson)
	
	
	// OTHER	---------------------------------
}
