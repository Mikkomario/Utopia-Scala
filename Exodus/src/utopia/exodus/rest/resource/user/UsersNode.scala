package utopia.exodus.rest.resource.user

import utopia.access.http.Method.Post
import utopia.access.http.Status._
import utopia.exodus.database.access.many.DbUsers
import utopia.exodus.rest.util.AuthorizedContext
import utopia.flow.generic.ValueConversions._
import utopia.flow.util.StringExtensions._
import utopia.metropolis.model.error.AlreadyUsedException
import utopia.metropolis.model.post.NewUser
import utopia.nexus.http.{Path, Response}
import utopia.nexus.rest.Resource
import utopia.nexus.rest.ResourceSearchResult.{Error, Follow}
import utopia.nexus.result.Result
import utopia.vault.database.Connection

import scala.util.{Failure, Success}

object UsersNode
{
	// COMPUTED	-----------------------------
	
	/**
	  * @return A users node that allows all user post requests
	  */
	def public = apply { (context, f) =>
		import utopia.exodus.util.ExodusContext._
		implicit val c: AuthorizedContext = context
		
		connectionPool.tryWith(f) match
		{
			case Success(result) => result.toResponse
			case Failure(error) => Result.Failure(InternalServerError, error.getMessage).toResponse
		}
	}
	
	/**
	  * @return A users node that allows requests that contain a registered api key
	  */
	def forApiKey = apply { (context, onAuthorized) =>
		context.apiKeyAuthorized { (_, connection) => onAuthorized(connection) }
	}
	
	
	// OTHER	------------------------------
	
	/**
	  * @param authorize A function for authorizing a request. Accepts request context and a function to call if
	  *                  the request is authorized. Returns the final response.
	  * @return A new users node with specified authorization feature
	  */
	def apply(authorize: (AuthorizedContext, Connection => Result) => Response) = new UsersNode(authorize)
}

/**
  * A rest-resource for all users
  * @author Mikko Hilpinen
  * @since 2.5.2020, v1
  */
class UsersNode(authorize: (AuthorizedContext, Connection => Result) => Response) extends Resource[AuthorizedContext]
{
	// IMPLEMENTED	---------------------------
	
	override val name = "users"
	
	override val allowedMethods = Vector(Post)
	
	override def toResponse(remainingPath: Option[Path])(implicit context: AuthorizedContext) =
	{
		// Authorizes the request using the specified function
		// Parses and stores new user data if authorization succeeds
		authorize(context, postUser()(context, _))
	}
	
	override def follow(path: Path)(implicit context: AuthorizedContext) =
	{
		if (path.head ~== "me")
			Follow(MeNode, path.tail)
		else
			Error(message = Some(s"Currently only 'me' is available under $name"))
	}
	
	
	// OTHER	----------------------------
	
	private def postUser()(implicit context: AuthorizedContext, connection: Connection) =
	{
		// Parses the post model first
		context.handlePost(NewUser) { newUser =>
			// Saves the new user data to DB
			DbUsers.tryInsert(newUser) match
			{
				case Success(userData) =>
					// Returns a summary of the new data
					Result.Success(userData.toModel, Created)
				case Failure(error) =>
					error match
					{
						case a: AlreadyUsedException => Result.Failure(Forbidden, a.getMessage)
						case _ => Result.Failure(BadRequest, error.getMessage)
					}
			}
		}
	}
}
