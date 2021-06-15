package utopia.nexus.rest

import utopia.access.http.Status
import utopia.access.http.Status._
import utopia.nexus.http.Path
import utopia.nexus.result.Result.Failure

/**
 * There are different types of results that can be get when following a path alongside resources. 
 * All of those result types are under this trait.
 */
sealed trait ResourceSearchResult[-C <: Context]

object ResourceSearchResult
{
    /**
     * Ready means that the resource is ready to fulfil the request and form the response
     * @param resource The resource ready to be targeted
     * @param remainingPath the path that is still left to cover, if there is any
     */
    final case class Ready[-C <: Context](resource: Resource[C], remainingPath: Option[Path] = None)
        extends ResourceSearchResult[C]
    
    /**
     * Follow means that the next resource was found but there is still some path to cover. A follow
     * response should be followed by another search.
     * @param resource The next resource on the path
     * @param remainingPath The path remaining after the provided resource, if one exists
     */
    final case class Follow[-C <: Context](resource: Resource[C], remainingPath: Option[Path])
        extends ResourceSearchResult[C]
    
    /**
     * A redirect is returned when a link is found and must be followed using a separate path
     * @param newPath The new path to follow to the original destination resource
     */
    final case class Redirected(newPath: Path)
        extends ResourceSearchResult[Context]
    
    /**
     * An error is returned when the next resource is not found or is otherwise not available
     */
    final case class Error(status: Status = NotFound, message: Option[String] = None)
        extends ResourceSearchResult[Context]
    {
        def toResult = Failure(status, message)
    }
}