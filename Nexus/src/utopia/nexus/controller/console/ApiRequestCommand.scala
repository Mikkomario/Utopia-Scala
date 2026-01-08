package utopia.nexus.controller.console

import utopia.access.model.Headers
import utopia.access.model.enumeration.Method
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{Empty, OptimizedIndexedSeq, Single}
import utopia.flow.collection.mutable.iterator.OptionsIterator
import utopia.flow.generic.casting.BasicValueCaster
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.{Constant, Model, Value}
import utopia.flow.util.StringExtensions._
import utopia.flow.util.console.ConsoleExtensions._
import utopia.flow.util.console.{ArgumentSchema, Command, ConsoleStream}
import utopia.flow.util.logging.Logger
import utopia.flow.util.result.TryExtensions._
import utopia.nexus.controller.api.ApiRoot
import utopia.nexus.model.api.ApiVersion
import utopia.nexus.model.request.{Request, StreamOrReader}

import scala.concurrent.ExecutionContext
import scala.io.StdIn

/**
 * A command for interacting with an internal API
 * @author Mikko Hilpinen
 * @since 08.01.2026, v2.0
 */
object ApiRequestCommand
{
	/**
	 * Creates a new command for performing API requests
	 * @param api The API utilized, accepts streamed requests
	 * @param token API key passed into the bearer-token Authorization header
	 * @param exc Implicit execution context
	 * @param log Implicit logging interface
	 * @return A new command for performing API requests
	 * @see [[apply]] if you need to customize the requests further
	 */
	def usingToken(api: ApiRoot[_, StreamOrReader], token: String)(implicit exc: ExecutionContext, log: Logger) =
		apply(api) {
			_.mapBody { body => StreamOrReader.readString(body.toJson) }.mapHeaders { _.withBearerAuthorization(token) }
		}
	
	/**
	 * Creates a new command for performing API requests
	 * @param api The API utilized
	 * @param prepareRequest A function which converts the collected request into a form that may be handled by 'api'
	 * @param exc Implicit execution context
	 * @param log Implicit logging interface
	 * @tparam B Type of the request bodies accepted by the specified API
	 * @return A new command for performing API requests
	 */
	def apply[B](api: ApiRoot[_, B])(prepareRequest: Request[Model] => Request[B])
	            (implicit exc: ExecutionContext, log: Logger) =
		Command("api", help = "Calls an internal API")(
			ArgumentSchema("method", defaultValue = "GET", help = "Applied HTTP method"),
			ArgumentSchema("path", help = "Path to the targeted API node", defaultDescription = "API root"),
			ArgumentSchema("params", defaultValue = Empty: Seq[Value],
				help = "A JSON array containing the key-value pairs passed as query parameters. \nSeparate keys from values with ':'."),
			ArgumentSchema("body", defaultValue = Empty: Seq[Value],
				help = "A JSON array containing the key-value pairs to place in the request (JSON) body. \nSeparate keys from values with ':'."),
			ArgumentSchema("headers", defaultValue = Empty: Seq[Value],
				help = "A JSON array containing the header-value pairs to place in the request headers. \nSeparate keys from values with ':'."),
			ArgumentSchema("version", defaultValue = api.latestVersion.value, help = "Targeted API version"),
			ArgumentSchema.flag("write-body", "B",
				help = "Include this flag to prompt the creation of the request body")) {
			args =>
				// Prepares the request
				val method = Method(args("method").getString.toUpperCase)
				val version = ApiVersion(args("version").getInt)
				val path = OptimizedIndexedSeq.concat(
					api.rootPath, Single(version.toString), args("path").getString.split('/'))
				val params = parseValues(args("params"))
				val headers = parseValues(args("headers"))
				val body = {
					if (args("write-body").getBoolean)
						Model.withConstants(OptionsIterator
							.continually {
								StdIn.readNonEmptyLine(
									"Write the next body property's name (empty input stops building the request body)")
									.map { key => Constant(key, StdIn.read(s"Specify the values of \"$key\" in JSON")) }
							}
							.toOptimizedSeq
						)
					else
						parseValues(args("body"))
				}
				
				val request = prepareRequest(Request(method, body, s"http://localhost/${ path.mkString("/") }", path,
					params, Headers.parseFrom(headers)))
				
				// Writes the response
				val response = api(request)
				println()
				val separator = "--------------------------"
				println(separator)
				println(response.status)
				println(separator)
				response.body.notEmpty.foreach { body =>
					body.writeTo(ConsoleStream).foreach { result =>
						println(separator)
						result.logWithMessage("Failed to print/write the response body")
					}
				}
		}
		
	private def parseValues(arg: Value) =
		Model.withConstants(arg.getVector.map { pair =>
			val (key, value) = pair.getString.splitAtFirst(":").map { _.trim }.toTuple
			Constant(key, BasicValueCaster.jsonParser.valueOf(value))
		})
}
