package utopia.nexus.controller.write

import utopia.access.model.enumeration.ContentCategory.{Application, Text}
import utopia.access.model.{ContentType, HasHeaders}
import utopia.flow.collection.immutable.Pair
import utopia.nexus.controller.write.JsonContentWriter.JsonEnveloper.JsonEnvelopeNames
import utopia.nexus.controller.write.JsonContentWriter.{JsonEnveloper, PlainJsonContentWriter}
import utopia.nexus.controller.write.JsonOrXmlContentWriter.{JsonOrXmlEnveloper, PlainJsonOrXmlContentWriter}
import utopia.nexus.controller.write.XmlContentWriter.XmlEnveloper.XmlEnvelopeNames
import utopia.nexus.controller.write.XmlContentWriter.{PlainXmlContentWriter, XmlElementNames, XmlEnveloper}
import utopia.nexus.model.request.RequestContext

object JsonOrXmlContentWriter
{
	// OTHER    ---------------------------
	
	/**
	 * Creates a content writer that supports both JSON and XML, with optional enveloping support
	 * @param envelopHeaderNames Names of the headers that control enveloping. Default = "X-Envelop"
	 * @param envelopParamNames Names of the (query) parameters that control enveloping. Default = "envelop".
	 * @param preferXml Whether to default to XML responses when no preference is specified
	 *                  (default = false = defaults to JSON)
	 * @param envelopsByDefault Whether to envelope responses by default. Default = false.
	 * @param noEmptyElementsInXmlEnvelope Whether to exclude empty XML elements from the XML envelope.
	 *                                     Default = false = The elements are always present, but may be empty.
	 * @param descriptionMayBePlainText Whether to write description only -results as plain text,
	 *                                  in non-enveloped JSON responses.
	 *                                  Default = false = descriptions will always be presented in JSON objects.
	 * @param jsonNaming Property names used in JSON
	 * @param xmlNaming Element names used in XML
	 * @return A new content writer
	 */
	def apply(envelopHeaderNames: Iterable[String] = defaultEnvelopHeaderNames,
	          envelopParamNames: Iterable[String] = defaultEnvelopParamNames, preferXml: Boolean = false,
	          envelopsByDefault: Boolean = false, noEmptyElementsInXmlEnvelope: Boolean = false,
	          descriptionMayBePlainText: Boolean = false)
	         (implicit jsonNaming: JsonEnvelopeNames = JsonEnvelopeNames.default,
	          xmlNaming: XmlEnvelopeNames = XmlEnvelopeNames.default) =
		new JsonOrXmlContentWriter(envelopHeaderNames, envelopParamNames, preferXml,
			envelopsByDefault, noEmptyElementsInXmlEnvelope, descriptionMayBePlainText)
	
	/**
	 * Creates a new content writer that always applies enveloping, supporting both JSON and XML.
	 * @param preferXml Whether to default to XML responses when no preference is specified
	 *                  (default = false = defaults to JSON)
	 * @param noEmptyXmlElements Whether to exclude empty XML elements from the XML responses.
	 *                           Default = false = The elements are always present, but may be empty.
	 * @param jsonNaming Property names used in JSON
	 * @param xmlNaming Element names used in XML
	 * @return A new content writer
	 */
	def enveloper(preferXml: Boolean = false, noEmptyXmlElements: Boolean = false)
	             (implicit jsonNaming: JsonEnvelopeNames = JsonEnvelopeNames.default,
	              xmlNaming: XmlEnvelopeNames = XmlEnvelopeNames.default) =
		new JsonOrXmlEnveloper(preferXml, noEmptyXmlElements)
	/**
	 * Creates a new content writer that supports both JSON and XML
	 * @param descriptionPropName Name of the property containing the result's description property
	 *                            (when applicable). Default = "message".
	 * @param preferXml Whether to default to XML responses when no preference is specified
	 *                  (default = false = defaults to JSON)
	 * @param descriptionMayBePlainText Whether to write description only -results as plain text in JSON responses.
	 *                                  Default = false = descriptions will always be presented in JSON objects.
	 * @param xmlNaming Element names used in XML
	 * @return A new content writer
	 */
	def plain(descriptionPropName: String = JsonContentWriter.defaultDescriptionPropName, preferXml: Boolean = false,
	          descriptionMayBePlainText: Boolean = false)
	         (implicit xmlNaming: XmlElementNames = XmlElementNames.default) =
		new PlainJsonOrXmlContentWriter(descriptionPropName, preferXml, descriptionMayBePlainText)
	
	
	// NESTED   ---------------------------
	
	/**
	 * A content writer that always applies enveloping, supporting both JSON and XML.
	 * @param preferXml Whether to default to XML responses when no preference is specified
	 *                  (default = false = defaults to JSON)
	 * @param noEmptyXmlElements Whether to exclude empty XML elements from the XML responses.
	 *                           Default = false = The elements are always present, but may be empty.
	 * @param jsonNaming Property names used in JSON
	 * @param xmlNaming Element names used in XML
	 */
	class JsonOrXmlEnveloper(preferXml: Boolean = false, noEmptyXmlElements: Boolean = false)
	                        (implicit jsonNaming: JsonEnvelopeNames = JsonEnvelopeNames.default,
	                         xmlNaming: XmlEnvelopeNames = XmlEnvelopeNames.default)
		extends DelegatingMultiTypeContentWriter[HasHeaders]
	{
		override protected val delegates: Iterable[(ContentType, ContentWriter[HasHeaders])] = {
			val json = Application.json -> new JsonEnveloper()
			val xml = Application.xml -> new XmlEnveloper(noEmptyXmlElements)
			if (preferXml) Pair(xml, json) else Pair(json, xml)
		}
	}
	/**
	 * A content writer that supports both JSON and XML
	 * @param descriptionPropName Name of the property containing the result's description property
	 *                            (when applicable). Default = "message".
	 * @param preferXml Whether to default to XML responses when no preference is specified
	 *                  (default = false = defaults to JSON)
	 * @param descriptionMayBePlainText Whether to write description only -results as plain text in JSON responses.
	 *                                  Default = false = descriptions will always be presented in JSON objects.
	 * @param xmlNaming Element names used in XML
	 */
	class PlainJsonOrXmlContentWriter(descriptionPropName: String = JsonContentWriter.defaultDescriptionPropName,
	                                  preferXml: Boolean = false, descriptionMayBePlainText: Boolean = false)
	                                 (implicit xmlNaming: XmlElementNames = XmlElementNames.default)
		extends DelegatingMultiTypeContentWriter[HasHeaders]
	{
		// ATTRIBUTES   --------------------
		
		override protected val delegates: Iterable[(ContentType, ContentWriter[HasHeaders])] = {
			val jsonWriter = new PlainJsonContentWriter(descriptionPropName, descriptionMayBePlainText)
			val json = Application.json -> jsonWriter
			val xml = Application.xml -> new PlainXmlContentWriter()
			val text = Text.plain -> jsonWriter
			
			if (preferXml) Vector(xml, json, text) else Vector(json, xml, text)
		}
	}
}
/**
 * A content writer that supports both JSON and XML, with optional enveloping support
 * @param envelopHeaderNames Names of the headers that control enveloping. Default = "X-Envelop"
 * @param envelopParamNames Names of the (query) parameters that control enveloping. Default = "envelop".
 * @param preferXml Whether to default to XML responses when no preference is specified
 *                  (default = false = defaults to JSON)
 * @param envelopsByDefault Whether to envelope responses by default. Default = false.
 * @param noEmptyElementsInXmlEnvelope Whether to exclude empty XML elements from the XML envelope.
 * @param descriptionMayBePlainText Whether to write description only -results as plain text,
 *                                  in non-enveloped JSON responses.
 *                                  Default = false = descriptions will always be presented in JSON objects.
 * @param jsonNaming Property names used in JSON
 * @param xmlNaming Element names used in XML
 * @author Mikko Hilpinen
 * @since 04.11.2025, v2.0
 */
class JsonOrXmlContentWriter(override protected val envelopHeaderNames: Iterable[String] = defaultEnvelopHeaderNames,
                             override protected val envelopParamNames: Iterable[String] = defaultEnvelopParamNames,
                             preferXml: Boolean = false, override protected val envelopsByDefault: Boolean = false,
                             noEmptyElementsInXmlEnvelope: Boolean = false,
                             descriptionMayBePlainText: Boolean = false)
                            (implicit jsonNaming: JsonEnvelopeNames = JsonEnvelopeNames.default,
                             xmlNaming: XmlEnvelopeNames = XmlEnvelopeNames.default)
	extends PossiblyEnvelopingContentWriter[RequestContext[_]]
{
	override protected lazy val envelopingDelegate: ContentWriter[RequestContext[_]] =
		new JsonOrXmlEnveloper(preferXml, noEmptyElementsInXmlEnvelope)
	override protected lazy val plainDelegate: ContentWriter[RequestContext[_]] =
		new PlainJsonOrXmlContentWriter(jsonNaming.description, preferXml, descriptionMayBePlainText)
}
