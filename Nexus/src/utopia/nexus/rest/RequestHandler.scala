package utopia.nexus.rest

import utopia.flow.generic.ValueConversions._
import utopia.flow.util.AutoClose._
import utopia.nexus.http.Request
import utopia.nexus.http.Path
import utopia.nexus.http.Response
import utopia.flow.datastructure.immutable.Model
import utopia.access.http.Headers
import utopia.access.http.Status._
import utopia.nexus.rest.ResourceSearchResult.{Error, Follow, Ready, Redirected}
import utopia.nexus.result.Result.Failure
import utopia.nexus.result.Result.Success

/**
 * This class handles a request by searching for the targeted resource and performing the right 
 * operation on the said resource
 * @author Mikko Hilpinen
 * @since 9.9.2017
 */
class RequestHandler[C <: Context](val childResources: Iterable[Resource[C]], val path: Option[Path] = None,
                                   val makeContext: Request => C)
{
    // COMPUTED PROPERTIES    -------------
    
   //  private def currentDateHeader = Headers().withCurrentDate
    
    private def get(implicit context: C) = 
    {
        val childLinks = childResources.map { child => (child.name, (context.settings.address + "/" + 
                path.map { _/child.name.toString }.getOrElse(child.name)).toValue) }
        Success(Model(childLinks)).toResponse
    }
    
    
    // OPERATORS    -----------------------
    
    /**
     * Forms a response for the specified request
     */
    def apply(request: Request) = handlePath(request.path)(makeContext(request))
    
    
    // OTHER METHODS    -------------------
    
    private def handlePath(targetPath: Option[Path])(implicit context: C): Response = 
    {
        try
        {
            // Parses the target path (= request path - handler path)
            var remainingPath = targetPath
            var error: Option[Error] = None
            var pathToSkip = path
            
            // Skips the path that leads to this handler resource
            while (pathToSkip.isDefined && error.isEmpty)
            {
                if (remainingPath.isEmpty)
                    error = Some(Error(message = Some(s"Expected request path to continue with /${pathToSkip.get}")))
                else if (!remainingPath.get.head.equalsIgnoreCase(pathToSkip.get.head))
                    error = Some(Error(message = Some(s"Expected ${pathToSkip.get}, found ${remainingPath.get}")))
                else
                {
                    remainingPath = remainingPath.get.tail
                    pathToSkip = pathToSkip.get.tail
                }
            }
            
            val firstResource = remainingPath.map{ _.head }.flatMap { resourceName => 
                        childResources.find { _.name.equalsIgnoreCase(resourceName) } }
            if (remainingPath.isDefined && firstResource.isEmpty)
                error = Some(Error(message = Some(s"Couldn't find ${remainingPath.head} under ${
                    path.map { _.toString }.getOrElse("Request Handler") }. Available resources: [${
                    childResources.map { _.name }.mkString(", ")}]")))
            
            // Case: Error
            if (error.isDefined)
                error.get.toResult.toResponse
            // Case: RequestHandler was targeted
            else if (remainingPath.isEmpty)
                get
            else
            {
                // Case: A resource under the handler was targeted
                // Finds the initial resource for the path
                var lastResource = firstResource
                if (lastResource.isDefined)
                {
                    // Drops the first resource from the remaining path
                    remainingPath = remainingPath.flatMap { _.tail }
                }
                
                var foundTarget = remainingPath.isEmpty
                var redirectPath: Option[Path] = None
                
                // Searches as long as there is success and more path to discover
                while (lastResource.isDefined && remainingPath.isDefined && error.isEmpty && 
                        !foundTarget && redirectPath.isEmpty)
                {
                    // Sees what's the resources reaction
                    lastResource.get.follow(remainingPath.get) match
                    {
                        case Ready(remaining) =>
                            foundTarget = true
                            remainingPath = remaining
                        case Follow(next: Resource[C], remaining) =>
                            lastResource = Some(next)
                            remainingPath = remaining
                            
                            // If there is no path left, assumes that the final resource is ready to 
                            // receive the request
                            if (remainingPath.isEmpty)
                                foundTarget = true
                            
                        case Redirected(newPath) => redirectPath = Some(newPath)
                        case foundError: Error => error = Some(foundError)
                    }
                }
                
                // Handles search results
                if (error.isDefined)
                    error.get.toResult.toResponse
                else if (redirectPath.isDefined)
                    handlePath(redirectPath)
                else if (foundTarget)
                {
                    // Makes sure the method can be used on the targeted resource
                    val allowedMethods = lastResource.get.allowedMethods
                    
                    if (allowedMethods.exists { _ == context.request.method })
                        lastResource.get.toResponse(remainingPath)
                    else
                    {
                        val headers = Headers().withCurrentDate.withAllowedMethods(allowedMethods.toVector)
                        new Response(MethodNotAllowed, headers)
                    }
                }
                else
                    Error().toResult.toResponse
            }
        }
        catch 
        {
            case e: Exception => Failure(InternalServerError, Some(e.getMessage)).toResponse
        }
        finally
        {
            context.closeQuietly()
        }
    }
    
    /*
    private def makeNotAllowedResponse(allowedMethods: Seq[Method])(implicit context: C) = 
            Failure(MethodNotAllowed, None, Model(Vector(
            "allowed_methods" -> allowedMethods.toVector.map(_.name)))
            ).toResponse.withModifiedHeaders(_.withAllowedMethods(allowedMethods))*/
}