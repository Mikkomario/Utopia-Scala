package utopia.nexus.controller.api.node

import utopia.access.model.enumeration.Method
import utopia.nexus.model.response.RequestResult
import utopia.nexus.model.api.PathFollowResult

/**
 * Common trait for all API node implementations
 * @tparam C Type of the required contextual information
 * @author Mikko Hilpinen
 * @since 6.11.2025, v2.0, based on Resource written a long time ago
 */
trait ApiNode[-C]
{
    // ABSTRACT -------------------------
    
    /**
     * The name of this node, as it appears on the path
     */
    def name: String
    /**
     * The methods this resource supports
     */
    def allowedMethods: Iterable[Method]
	
	/**
	 * Follows the specified path to the next pointed node
	 * @param step The next element on the request path after this node.
	 * @param context Implicit request context
	 * @return Action that should be taken next:
	 *              - [[utopia.nexus.model.api.PathFollowResult.Ready]], if this node should process the request
	 *              - [[utopia.nexus.model.api.PathFollowResult.Follow]], if the next node was located successfully
	 *              - [[utopia.nexus.model.api.PathFollowResult.NotFound]], if the next node couldn't be located
	 *              - [[utopia.nexus.model.api.PathFollowResult.Redirected]], if a different path should be taken instead
	 */
	def follow(step: String)(implicit context: C): PathFollowResult[C]
    /**
     * Executes one of the [[allowedMethods]] on this node.
     * @param method The method to execute on this node
     * @param remainingPath The part of the request path, which was not followed further.
     *                      Empty if targeting this node directly.
     */
    def apply(method: Method, remainingPath: Seq[String])(implicit context: C): RequestResult
}