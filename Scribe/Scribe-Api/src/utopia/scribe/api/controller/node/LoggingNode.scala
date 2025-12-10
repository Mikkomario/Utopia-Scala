package utopia.scribe.api.controller.node

import utopia.access.model.enumeration.Method
import utopia.access.model.enumeration.Method.Post
import utopia.access.model.enumeration.Status.Accepted
import utopia.flow.operator.Identity
import utopia.flow.util.Mutate
import utopia.nexus.controller.api.context.PostContext
import utopia.nexus.controller.api.node.LeafNode
import utopia.nexus.model.response.RequestResult
import utopia.scribe.api.controller.logging.Scribe
import utopia.scribe.core.model.post.logging.ClientIssue

object LoggingNode
{
	/**
	  * Creates a new logging node, named "log"
	  * @param authorize function used for authorizing the incoming requests.
	  *                  Accepts request context and a function to perform if the request is authorized.
	  *                  Returns a request result.
	  * @tparam C Type of accepted request context
	  * @return A new node that accepts client error logging information
	  */
	def apply[C <: PostContext](authorize: (C, () => RequestResult) => RequestResult) =
		named("log")(authorize)
	
	/**
	  * Creates a new logging node
	  * @param name      Name of this node
	  * @param authorize function used for authorizing the incoming requests.
	  *                  Accepts request context and a function to perform if the request is authorized.
	  *                  Returns a request result.
	  * @tparam C Type of accepted request context
	  * @return A new node that accepts client error logging information
	  */
	def named[C <: PostContext](name: String)(authorize: (C, () => RequestResult) => RequestResult) =
		namedWithMutation[C](name) { (context, f) => authorize(context, () => f(Identity)) }
	
	/**
	  * Creates a new logging node, named "log"
	  * @param authorize function used for authorizing the incoming requests.
	  *                  Accepts two parameters:
	  *                  1) Request context, and
	  *                  2) Function to perform if the request is authorized.
	  *                  This function accepts a mutation function which is applied
	  *                  to all incoming ClientIssue instances.
	  *                  Returns a request result. The response is typically based on either authorization failure,
	  *                  or the result returned by the received function.
	  * @param mutate    A function for mutating incoming client issues before logging them
	  * @tparam C Type of accepted request context
	  * @return A new node that accepts client error logging information
	  */
	def mutating[C <: PostContext](authorize: (C, Mutate[ClientIssue] => RequestResult) => RequestResult)
	                              (mutate: Mutate[ClientIssue]) =
		namedWithMutation("log")(authorize)
	
	/**
	  * Creates a new logging node
	  * @param name Name of this node
	  * @param authorize function used for authorizing the incoming requests.
	  *                  Accepts two parameters:
	  *                     1) Request context, and
	  *                     2) Function to perform if the request is authorized.
	  *                     This function accepts a mutation function which is applied
	  *                     to all incoming ClientIssue instances.
	  *                  Returns a request result. The response is typically based on either authorization failure,
	  *                  or the result returned by the received function.
	  * @tparam C Type of accepted request context
	  * @return A new node that accepts client error logging information
	  */
	def namedWithMutation[C <: PostContext](name: String)
	                                       (authorize: (C, Mutate[ClientIssue] => RequestResult) => RequestResult) =
		new LoggingNode[C](name, authorize)
}

/**
  * A REST node used for sending client-side error data over
  * @author Mikko Hilpinen
  * @since 23.5.2023, v0.1
  */
class LoggingNode[-C <: PostContext](override val name: String = "log",
                                     authorize: (C, Mutate[ClientIssue] => RequestResult) => RequestResult)
	extends LeafNode[C]
{
	// IMPLEMENTED  -----------------------
	
	override def allowedMethods: Iterable[Method] = Some(Post)
	
	override def apply(method: Method, remainingPath: Seq[String])(implicit context: C): RequestResult =
		authorize(context, m => post(m))
	
	
	// OTHER    ---------------------------
	
	private def post(mutate: Mutate[ClientIssue])(implicit context: C) = {
		// Parses the issues from the request body
		context.handleModelArrayPost(ClientIssue) { issues =>
			// Records each issue asynchronously
			issues.foreach { i => Scribe.record(mutate(i)) }
			RequestResult(status = Accepted)
		}
	}
}
