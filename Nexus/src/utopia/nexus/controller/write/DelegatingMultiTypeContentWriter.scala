package utopia.nexus.controller.write

import utopia.access.model.{ContentType, HasHeaders, Headers}
import utopia.access.model.enumeration.Status
import utopia.nexus.model.response.ResponseContent

object DelegatingMultiTypeContentWriter
{
	// OTHER    ------------------------
	
	/**
	 * Creates a new delegating content writer
	 * @param delegates The delegates used for performing the actual writing, from most to least preferred.
	 *                  Each entry contains:
	 *                      1. The content type handled by the specified writer
	 *                      1. The writer for that content type
	 * @tparam C Type of required context
	 * @return A new content writer that delegates the writing to one of the specified delegates
	 */
	def apply[C <: HasHeaders](delegates: Iterable[(ContentType, ContentWriter[C])]): DelegatingMultiTypeContentWriter[C] =
		_DelegatingMultiTypeContentWriter(delegates)
	
	
	// NESTED   ------------------------
	
	private case class _DelegatingMultiTypeContentWriter[-C <: HasHeaders](delegates: Iterable[(ContentType, ContentWriter[C])])
		extends DelegatingMultiTypeContentWriter[C]
}
/**
 * Common trait for content writers which delegate the writing to other writers, based on the desired content type
 * @tparam C Required contextual information
 * @author Mikko Hilpinen
 * @since 04.11.2025, v2.0
 */
trait DelegatingMultiTypeContentWriter[-C <: HasHeaders] extends ContentWriter[C]
{
	// ABSTRACT ------------------------
	
	/**
	 * @return The delegates used for performing the actual writing, from most to least preferred.
	 *         Each entry contains:
	 *              1. The content type handled by the specified writer
	 *              1. The writer for that content type
	 */
	protected def delegates: Iterable[(ContentType, ContentWriter[C])]
	
	
	// COMPUTED ------------------------
	
	private def defaultDelegate = delegates.head._2
	
	
	// IMPLEMENTED  --------------------
	
	override def prepare(content: ResponseContent, status: Status, headers: Headers)
	                    (implicit context: C): (WriteResponseBody, Status) =
	{
		// Finds the delegate to use for this result / request
		// Option 1: Content type is specified in response headers already
		val delegate = headers.contentType
			.flatMap { cType => delegates.find { case (out, _) => out.overlapsWith(cType) }.map { _._2 } }
			.getOrElse {
				val accepted = context.headers.acceptedTypes
				// Case: No content type specified => Uses the default delegate
				if (accepted.isEmpty)
					defaultDelegate
				// Case: Accepted content types specified => Looks for a suitable delegate, or uses the default
				else
					delegates.find { case (cType, _) => accepted.exists { _.overlapsWith(cType) } } match {
						case Some((_, delegate)) => delegate
						case None => defaultDelegate
					}
			}
		delegate.prepare(content, status, headers)
	}
}
