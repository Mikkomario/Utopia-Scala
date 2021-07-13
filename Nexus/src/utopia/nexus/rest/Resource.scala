package utopia.nexus.rest

import utopia.nexus.http.Path
import utopia.nexus.http.Response
import utopia.access.http.Method

trait Resource[-C <: Context]
{
    // ABSTRACT PROPERTIES & METHODS ------------------
    
    /**
     * The name of this resource
     */
    def name: String
    
    /**
     * The methods this resource supports
     */
    def allowedMethods: Iterable[Method]
    
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
}