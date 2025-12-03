package utopia.nexus.controller.write

import utopia.access.model.Headers
import utopia.access.model.enumeration.Status
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.generic.casting.ValueConversions._
import utopia.nexus.model.request.RequestContext
import utopia.nexus.model.response.ResponseContent

object PossiblyEnvelopingContentWriter
{
	// OTHER    ---------------------------
	
	/**
	 * @param envelopingDelegate The delegate content writer that envelops the output
	 * @param plainDelegate The delegate content writer that doesn't envelop the output
	 * @param envelopHeaderNames Names of the headers that control enveloping. Default = "X-Envelop"
	 * @param envelopParamNames Names of the (query) parameters that control enveloping. Default = "envelop".
	 * @param envelopsByDefault Whether to envelope responses by default. Default = false.
	 * @tparam C Type of the required context
	 * @return A new content writer
	 */
	def apply[C <: RequestContext[_]](envelopingDelegate: ContentWriter[C], plainDelegate: ContentWriter[C],
	                                  envelopHeaderNames: Iterable[String] = defaultEnvelopHeaderNames,
	                                  envelopParamNames: Iterable[String] = defaultEnvelopParamNames,
	                                  envelopsByDefault: Boolean = false): PossiblyEnvelopingContentWriter[C] =
		_PossiblyEnvelopingContentWriter[C](envelopingDelegate, plainDelegate, envelopHeaderNames,
			envelopParamNames, envelopsByDefault)
	
	
	// NESTED   ---------------------------
	
	private case class _PossiblyEnvelopingContentWriter[-C <: RequestContext[_]](envelopingDelegate: ContentWriter[C],
	                                                                             plainDelegate: ContentWriter[C],
	                                                                             envelopHeaderNames: Iterable[String],
	                                                                             envelopParamNames: Iterable[String],
	                                                                             envelopsByDefault: Boolean)
		extends PossiblyEnvelopingContentWriter[C]
}
/**
 * Common trait for content writers that support optional enveloping
 * @tparam C Required contextual information
 * @author Mikko Hilpinen
 * @since 04.11.2025, v2.0
 */
trait PossiblyEnvelopingContentWriter[-C <: RequestContext[_]] extends ContentWriter[C]
{
	// ABSTRACT ---------------------------
	
	/**
	 * @return Names of the headers that control enveloping
	 */
	protected def envelopHeaderNames: Iterable[String]
	/**
	 * @return Names of the (query) parameters that control enveloping
	 */
	protected def envelopParamNames: Iterable[String]
	
	/**
	 * @return Whether to envelope responses by default
	 */
	protected def envelopsByDefault: Boolean
	
	/**
	 * @return The delegate content writer that envelops the output
	 */
	protected def envelopingDelegate: ContentWriter[C]
	/**
	 * @return The delegate content writer that doesn't envelop the output
	 */
	protected def plainDelegate: ContentWriter[C]
	
	
	// IMPLEMENTED  -----------------------
	
	override def prepare(content: ResponseContent, status: Status, headers: Headers)
	                    (implicit context: C): (WriteResponseBody, Status) =
	{
		// Determines whether enveloping should be applied
		val shouldEnvelop = test(context.headers).getOrElse {
			val params = context.request.parameters
			envelopParamNames.findMap { params(_).boolean }.getOrElse { test(headers).getOrElse(envelopsByDefault) }
		}
		// Uses the appropriate delegate
		if (shouldEnvelop)
			envelopingDelegate.prepare(content, status, headers)
		else
			plainDelegate.prepare(content, status, headers)
	}
	
	
	// OTHER    ------------------------
	
	private def test(headers: Headers) =
		envelopHeaderNames.findMap { headers.get(_).flatMap { _.boolean } }
}
