package utopia.nexus.model.api

import utopia.access.model.enumeration.Status
import utopia.nexus.controller.api.node.ApiNode
import utopia.nexus.model.response.RequestResult

/**
 * An enumeration for different reactions to an attempt to follow a request path through an API node.
 * @tparam C Type of the contextual information required by the next API node, if applicable.
 *           For results which don't refer to any API nodes, this is Any.
 * @author Mikko Hilpinen
 * @since 6.11.2025, v2.0, based on ResourceSearchResult written a long time ago
 */
sealed trait PathFollowResult[-C]

object PathFollowResult
{
	// VALUES   -----------------------
	
	/**
	 * Yielded when the API node is ready to process the request,
	 * and there's no need to follow the path further.
	 *
	 * The remaining path will be received by the same node when executing the request.
	 */
	case object Ready extends PathFollowResult[Any]
	
	/**
	 * Yielded when the next node on the path is found
	 * @param nextNode The next resource on the path
	 */
	case class Follow[C](nextNode: ApiNode[C]) extends PathFollowResult[C]
	
	/**
	 * Yielded in situations where the next node is not known, and a new request path should be taken.
	 * @param newPath The new path to follow to the targeted API node.
	 * @param noRemainder Whether to omit the path that remained after this node.
	 *                    Default = false = the remaining path will be appended to 'newPath'.
	 */
	case class Redirected(newPath: Seq[String], noRemainder: Boolean = false) extends PathFollowResult[Any]
	
	/**
	 * Yielded in situations where the next node on the path can't be identified.
	 * @param message Message to send back to the client
	 */
	case class NotFound(message: String = "") extends PathFollowResult[Any]
	{
		/**
		 * @return A failure result based on this result
		 */
		def toRequestResult: RequestResult = Status.NotFound -> message
		@deprecated("Renamed to .toRequestResult", "v2.0")
		def toResult = toRequestResult
	}
}