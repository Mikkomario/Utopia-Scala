package utopia.nexus.rest

import utopia.access.http.Status._
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Model
import utopia.flow.operator.EqualsExtensions._
import utopia.flow.parse.AutoClose._
import utopia.flow.util.CollectionExtensions._
import utopia.flow.util.logging.Logger
import utopia.nexus.http.Path._
import utopia.nexus.http.{Path, Request, Response}
import utopia.nexus.rest.ResourceSearchResult.{Error, Follow, Ready, Redirected}
import utopia.nexus.result.Result

import scala.annotation.tailrec
import scala.util.Try

object RequestHandler
{
    /**
     * Creates a new request handler
     * @param childResources Resources per api version
     * @param path Base path of this handler (before the version number) (optional)
     * @param makeContext A function for creating a new request context
      * @param logger Implicit logging implementation to use in case of server errors
     * @tparam C Type of request context used
     * @return A new request handler
     */
    def apply[C <: Context](childResources: Map[String, Iterable[Resource[C]]], path: Option[Path] = None)
                           (makeContext: Request => C)
                           (implicit logger: Logger) =
        new RequestHandler[C](childResources, path, makeContext)
}

/**
 * This class handles a request by searching for the targeted resource and performs the right
 * operation on that resource
 * @author Mikko Hilpinen
 * @since 9.9.2017
 */
class RequestHandler[-C <: Context](childResources: Map[String, Iterable[Resource[C]]], path: Option[Path] = None,
                                    makeContext: Request => C)(implicit logger: Logger)
{
    // COMPUTED PROPERTIES    -------------
    
    private def get(version: String)(implicit context: C) =
        Result.Success(Model(Vector("version" -> version,
            "children" -> childResources.getOrElse(version.toLowerCase, Vector()).map { _.name }.toVector))).toResponse
    
    
    // OPERATORS    -----------------------
    
    /**
     * Forms a response for the specified request
     */
    def apply(request: Request): Response = handleBasePath(request.path) match
    {
        case Right((version, remaining)) =>
            remaining match
            {
                // Targeting a resource under a version
                case Some(remaining) =>
                    makeContext(request).consume { implicit c =>
                        // Catches exceptions and wraps them as internal server errors
                        Try {
                            // Finds the final target resource
                            findTarget(version, remaining) match {
                                case Right((resource, path)) =>
                                    // Makes sure the resource supports the method being used
                                    val allowed = resource.allowedMethods
                                    if (allowed.isEmpty)
                                        Result.Failure(NotImplemented).toResponse
                                    else if (allowed.exists { _ == request.method })
                                        resource.toResponse(path)
                                    else
                                        Result.Failure(MethodNotAllowed,
                                            s"${request.method} is not allowed on this resource. Allowed methods: [${
                                                allowed.mkString(", ")}]").toResponse
                                            .withModifiedHeaders { _.withCurrentDate.withAllowedMethods(allowed.toSeq) }
                                case Left(error) => error.toResult.toResponse
                            }
                        }.getOrMap { error =>
                            // In addition to sending error data forward, logs it
                            logger(error, s"Failed to handle ${request.method} ${request.path}")
                            Result.Failure(InternalServerError, error.getMessage).toResponse
                        }
                    }
                // Case: Targeting an API version
                case None => makeContext(request).consume { implicit c => get(version) }
            }
        // Case: initial path handling failed => returns a failure
        case Left(error) => makeContext(request).consume { implicit c => error.toResult.toResponse }
    }
    
    
    // OTHER METHODS    -------------------
    
    private def handleBasePath(basePath: Option[Path]) = handleRemainingBasePath(basePath, path)
    @tailrec
    private def handleRemainingBasePath(remainingPath: Option[Path],
                                        pathToSkip: Option[Path]): Either[Error, (String, Option[Path])] =
    {
        pathToSkip match {
            // Case: Path remains to be skipped => Expects the provided path to match
            case Some(pathToSkip) =>
                remainingPath match {
                    case Some(remaining) =>
                        if (remaining.head ~== pathToSkip.head)
                            handleRemainingBasePath(remaining.tail, pathToSkip.tail)
                        else
                            Left(Error(message = Some(s"Expected $pathToSkip, found $remaining")))
                    // Case: Too short a path
                    case None => Left(Error(message = Some(s"Expected request path to continue with /$pathToSkip")))
                }
            // Case: No more path to skip => Expects the next path piece to show a version
            case None =>
                remainingPath match {
                    case Some(remaining) =>
                        // Case: Found a valid version => Continues with the resources in that version
                        if (childResources.contains(remaining.head.toLowerCase))
                            Right(remaining.head -> remaining.tail)
                        // Case: Didn't find a valid version
                        else
                            Left(Error(message = Some(
                                s"'${remaining.head}' is not a valid version. Available versions: [${
                                    childResources.keys.mkString(", ")}]")))
                    // Case: Too short a path
                    case None => Left(Error(message = Some(
                        s"Expected request path to continue with a version. Available versions: [${
                            childResources.keys.mkString(", ")}]")))
                }
        }
    }
    
    // Expects a valid version
    private def findTarget(version: String, remainingPath: Path)
                          (implicit context: C): Either[Error, (Resource[C], Option[Path])] =
    {
        val resources = childResources(version)
        val first = remainingPath.head
        resources.find { _.name ~== first } match
        {
            // Case: Root resource found => follows that one
            case Some(resource) => follow(version, resource, remainingPath.tail)
            // Case: Root resource not found
            case None => Left(Error(message = Some(s"Resource '$first' not found under ${
                path/version}. Available resources: [${resources.map { _.name }.mkString(", ")}]")))
        }
    }
    
    @tailrec
    private def follow[C2 <: C](version: String, resource: Resource[C2], path: Option[Path])
                      (implicit context: C2): Either[Error, (Resource[C2], Option[Path])] = path match
    {
        case Some(remaining) =>
            // TODO: The match may not be exhaustive (because of the type parameter requirement)
            resource.follow(remaining) match
            {
                case Follow(next, remaining) => follow(version, next, remaining)
                case Ready(resource, path) => Right(resource -> path)
                case e: Error => Left(e)
                case Redirected(path) => findTarget(version, path)
            }
        case None => Right(resource -> None)
    }
}