package utopia.nexus.result

import utopia.access.http.ContentCategory._
import utopia.access.http.Status
import utopia.flow.generic.model.immutable.Value
import utopia.nexus.http.Request

/**
* This raw result parser uses json or xml, depending on the request headers
* @author Mikko Hilpinen
* @since 24.5.2018
**/
case class UseRawXmlOrJson(rootElementName: String = "Response", preferJson: Boolean = true) extends RawResultParser
{
    private val xmlParser = UseRawXml(rootElementName)
    
	def parseDataResponse(data: Value, status: Status, request: Request) =
	{
		val jsonAccepted = request.headers.accepts(Application.json)
		if (jsonAccepted && preferJson)
				UseRawJson.parseDataResponse(data, status, request)
		else
		{
			val xmlAccepted = request.headers.accepts(Application.xml)
			if (xmlAccepted || !preferJson)
					xmlParser.parseDataResponse(data, status, request)
			else
					UseRawJson.parseDataResponse(data, status, request)
		}
	}
}