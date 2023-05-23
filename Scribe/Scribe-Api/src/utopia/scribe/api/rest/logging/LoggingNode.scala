package utopia.scribe.api.rest.logging

import utopia.access.http.Method
import utopia.access.http.Method.Post
import utopia.access.http.Status.Accepted
import utopia.flow.operator.Identity
import utopia.flow.util.Mutate
import utopia.nexus.http.{Path, Response}
import utopia.nexus.rest.{LeafResource, PostContext}
import utopia.nexus.result.Result
import utopia.scribe.api.controller.logging.Scribe
import utopia.scribe.core.model.post.logging.ClientIssue

object LoggingNode
{
	/**
	  * Creates a new logging node, named "log"
	  * @param authorize function used for authorizing the incoming requests.
	  *                  Accepts request context and a function to perform if the request is authorized.
	  *                  Returns a response.
	  * @tparam C Type of accepted request context
	  * @return A new node that accepts client error logging information
	  */
	def apply[C <: PostContext](authorize: (C, () => Result) => Response) =
		named("log")(authorize)
	
	/**
	  * Creates a new logging node
	  * @param name      Name of this node
	  * @param authorize function used for authorizing the incoming requests.
	  *                  Accepts request context and a function to perform if the request is authorized.
	  *                  Returns a response.
	  * @tparam C Type of accepted request context
	  * @return A new node that accepts client error logging information
	  */
	def named[C <: PostContext](name: String)(authorize: (C, () => Result) => Response) =
		namedWithMutation("log")(authorize)(Identity)
	
	/**
	  * Creates a new logging node, named "log"
	  * @param authorize function used for authorizing the incoming requests.
	  *                  Accepts request context and a function to perform if the request is authorized.
	  *                  Returns a response.
	  * @param mutate    A function for mutating incoming client issues before logging them
	  * @tparam C Type of accepted request context
	  * @return A new node that accepts client error logging information
	  */
	def mutating[C <: PostContext](authorize: (C, () => Result) => Response)
	                              (mutate: Mutate[ClientIssue]) =
		namedWithMutation("log")(authorize)(mutate)
	
	/**
	  * Creates a new logging node
	  * @param name Name of this node
	  * @param authorize function used for authorizing the incoming requests.
	  *                  Accepts request context and a function to perform if the request is authorized.
	  *                  Returns a response.
	  * @param mutate A function for mutating incoming client issues before logging them
	  * @tparam C Type of accepted request context
	  * @return A new node that accepts client error logging information
	  */
	def namedWithMutation[C <: PostContext](name: String)(authorize: (C, () => Result) => Response)
	                                       (mutate: Mutate[ClientIssue]) =
		_apply[C](name) { (context, f) => authorize(context, () => f(mutate)) }
	
	private def _apply[C <: PostContext](name: String)(auth: (C, Mutate[ClientIssue] => Result) => Response) =
		new LoggingNode[C](name, auth)
}

/**
  * A REST node used for sending client-side error data over
  * @author Mikko Hilpinen
  * @since 23.5.2023, v0.1
  */
class LoggingNode[-C <: PostContext](override val name: String = "log",
                                     authorize: (C, Mutate[ClientIssue] => Result) => Response)
	extends LeafResource[C]
{
	// IMPLEMENTED  -----------------------
	
	override def allowedMethods: Iterable[Method] = Some(Post)
	
	override def toResponse(remainingPath: Option[Path])(implicit context: C): Response =
		authorize(context, m => post(m))
	
	
	// OTHER    ---------------------------
	
	private def post(mutate: Mutate[ClientIssue])(implicit context: C) = {
		// Parses the issues from the request body
		context.handleModelArrayPost(ClientIssue) { issues =>
			// Records each issue asynchronously
			issues.foreach { i => Scribe.record(mutate(i)) }
			Result.Success(status = Accepted)
		}
	}
}
