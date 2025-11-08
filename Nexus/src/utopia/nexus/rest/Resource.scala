package utopia.nexus.rest

import utopia.access.model.enumeration.Method
import utopia.flow.collection.immutable.Single
import utopia.nexus.controller.api.node.ApiNode
import utopia.nexus.http.Path
import utopia.nexus.http.Response
import utopia.nexus.model.api.PathFollowResult
import utopia.nexus.model.response.RequestResult

@deprecated("Replaced with ApiNode", "v2.0")
trait Resource[-C <: Context] extends ApiNode[C]
{
    // ABSTRACT PROPERTIES & METHODS ------------------
    
    /**
     * Performs an operation on this resource and forms a response. The resource may expect that 
     * this method will only be called with methods that are allowed by the resource.
     * @param remainingPath if any of the path was left unfollowed by this resource earlier, it 
     * is provided here
     */
    def toResponse(remainingPath: Option[Path])(implicit context: C): Response
    /**
     * Follows the path to a new resource. Returns a result suitable for the situation.
     * @param path the path remaining <b>after</b> this resource
     */
    def follow(path: Path)(implicit context: C): ResourceSearchResult[C]
	
	
	// IMPLEMENTED  ---------------------------------
	
	override def follow(step: String)(implicit context: C): PathFollowResult[C] = follow(Path(Single(step)))(context)
	
	override def apply(method: Method, remainingPath: Seq[String])(implicit context: C): RequestResult = {
		val response = toResponse(if (remainingPath.isEmpty) None else Some(Path(remainingPath)))
		RequestResult.withBody(response.body, response.status, response.headers)
	}
}