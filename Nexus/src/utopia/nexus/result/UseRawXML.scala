package utopia.nexus.result

import utopia.access.http.ContentCategory._
import utopia.flow.datastructure.immutable.Value
import utopia.access.http.Status
import utopia.nexus.http.Request
import utopia.flow.parse.XmlElement
import utopia.nexus.http.Response
import utopia.access.http.Headers
import java.nio.charset.StandardCharsets

import utopia.flow.generic.{ModelType, VectorType}
import utopia.flow.parse.XmlWriter

/**
* This result parser parses data into xml format
* @author Mikko Hilpinen
* @since 24.5.2018
**/
case class UseRawXML(rootElementName: String = "Response") extends RawResultParser
{
	def parseDataResponse(data: Value, status: Status, request: Request) =
	{
	    val element = parseElement(rootElementName, data)
	    val charset = request.headers.preferredCharset getOrElse StandardCharsets.UTF_8
	    
	    new Response(status, Headers.withContentType(Application.xml, Some(charset)), Vector(),
	            Some(stream => XmlWriter.writeElementToStream(stream, element, charset)))
	}
	
	private def parseElement(elementName: String, content: Value): XmlElement =
	{
		content.dataType match
		{
			case ModelType => XmlElement(elementName, content.getModel)
			case VectorType => XmlElement(elementName,
				children = content.getVector.map { parseElement("element", _) })
			case _ => XmlElement(elementName, content)
		}
	}
}