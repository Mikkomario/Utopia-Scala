package utopia.exodus.rest.util

import utopia.access.http.ContentCategory.{Application, Text}
import utopia.access.http.Status.{BadRequest, Forbidden, InternalServerError, Unauthorized}
import utopia.access.http.error.ContentTypeException
import utopia.exodus.database.access.many.DbLanguages
import utopia.exodus.database.access.single.{DbApiKey, DbDeviceKey, DbEmailValidation, DbMembership, DbUser, DbUserSession}
import utopia.exodus.model.stored.{ApiKey, DeviceKey, EmailValidation, UserSession}
import utopia.flow.datastructure.immutable.{Constant, Model, Value}
import utopia.flow.generic.FromModelFactory
import utopia.flow.generic.ValueConversions._
import utopia.flow.parse.JsonParser
import utopia.flow.util.CollectionExtensions._
import utopia.flow.util.StringExtensions._
import utopia.nexus.http.{Request, ServerSettings}
import utopia.nexus.rest.BaseContext
import utopia.nexus.result.{Result, ResultParser, UseRawJSON}
import utopia.vault.database.Connection

import scala.math.Ordering.Double.TotalOrdering
import scala.util.{Failure, Success, Try}

object AuthorizedContext
{
	/**
	  * Creates a new authorized request context
	  * @param request Request wrapped by this context
	  * @param resultParser Parser that determines what server responses should look like. Default =
	  *                     use simple json bodies and http statuses.
	  * @param errorHandler A function for handling possible errors thrown during request handling and database
	  *                     interactions.
	  * @param serverSettings Applied server settings (implicit)
	  * @param jsonParser Json parser used for interpreting request json content (implicit)
	  * @return A new request context
	  */
	def apply(request: Request, resultParser: ResultParser = UseRawJSON)(errorHandler: Throwable => Unit)
			 (implicit serverSettings: ServerSettings, jsonParser: JsonParser) =
		new AuthorizedContext(request, resultParser)(errorHandler)
}

/**
  * This context variation checks user authorization (when required)
  * @author Mikko Hilpinen
  * @since 3.5.2020, v1
  * @param request Request wrapped by this context
  * @param resultParser Parser that determines how server responses should look like.
  *                     Default = simple json bodies and http statuses are used.
  * @param errorHandler A function for handling possible errors thrown during request handling and database interactions
  * @param serverSettings Current server settings (implicit)
  * @param jsonParser A parser used for interpreting json content (implicit)
  */
class AuthorizedContext(request: Request, resultParser: ResultParser = UseRawJSON)(errorHandler: Throwable => Unit)
					   (implicit serverSettings: ServerSettings, jsonParser: JsonParser)
	extends BaseContext(request, resultParser)
{
	import utopia.exodus.util.ExodusContext._
	
	// COMPUTED	----------------------------
	
	/**
	  * @param connection DB Connection (implicit)
	  * @return Languages that were requested in the Accept-Language header. The languages are listed from most to
	  *         least preferred. May be empty.
	  */
	def requestedLanguages(implicit connection: Connection) =
	{
		val acceptedLanguages = request.headers.acceptedLanguages.map { case (code, weight) => code.toLowerCase -> weight }
		if (acceptedLanguages.nonEmpty)
		{
			val acceptedCodes = acceptedLanguages.keySet
			// Maps codes to language ids (if present)
			val languages = DbLanguages.forIsoCodes(acceptedCodes)
			// Orders the languages based on assigned weight
			languages.sortBy { l => -acceptedLanguages(l.isoCode.toLowerCase) }
		}
		else
			Vector()
	}
	
	
	// OTHER	----------------------------
	
	/**
	  * Reads preferred language ids list either from the Accept-Language header or from the user data
	  * @param userId Id of targeted user (call by name)
	  * @param connection DB Connection (implicit)
	  * @return Ids of the requested languages in order from most to least preferred. Empty only if the user doesn't
	  *         exist or has no linked languages
	  */
	def languageIdListFor(userId: => Int)(implicit connection: Connection) =
	{
		// Reads languages list from the headers (if present) or from the user data
		val languagesFromHeaders = requestedLanguages
		if (languagesFromHeaders.nonEmpty)
			languagesFromHeaders.map { _.id }
		else
			DbUser(userId).languages.withFamiliarityLevels.sortBy { _._2.orderIndex }.map { _._1 }
	}
	
	/**
	  * Performs the provided function if the request has correct basic authorization (email + password)
	  * @param f Function called when request is authorized. Accepts userId + database connection. Produces an http result.
	  * @return Function result or a result indicating that the request was unauthorized. Wrapped as a response.
	  */
	def basicAuthorized(f: (Int, Connection) => Result) =
	{
		// Authorizes request with basic auth, finding user id
		val result = request.headers.basicAuthorization match
		{
			case Some(basicAuth) =>
				val (email, password) = basicAuth
				
				connectionPool.tryWith { implicit connection =>
					DbUser.tryAuthenticate(email, password) match
					{
						// Performs the operation on authorized user id
						case Some(userId) => f(userId, connection)
						case None => Result.Failure(Unauthorized, "Invalid email or password")
					}
				}.getOrMap { e =>
					errorHandler(e)
					Result.Failure(InternalServerError, e.getMessage)
				}
			case None => Result.Failure(Unauthorized, "Please provide a basic auth header with user email and password")
		}
		result.toResponse(this)
	}
	
	/**
	  * Perform the specified function if the request can be authorized using a device authentication key
	  * @param f A function called when request is authorized. Accepts device key + database connection. Produces an http result.
	  * @return Function result or a result indicating that the request was unauthorized. Wrapped as a response.
	  */
	def deviceKeyAuthorized(f: (DeviceKey, Connection) => Result) =
	{
		tokenAuthorized("device authentication key") { (token, connection) =>
			DbDeviceKey.matching(token)(connection)
		}(f)
	}
	
	/**
	  * Perform the specified function if the request can be authorized using a session key
	  * @param f A function called when request is authorized. Accepts user session + database connection. Produces an http result.
	  * @return Function result or a result indicating that the request was unauthorized. Wrapped as a response.
	  */
	def sessionKeyAuthorized(f: (UserSession, Connection) => Result) =
	{
		tokenAuthorized("session key") { (token, connection) => DbUserSession.matching(token)(connection) }(f)
	}
	
	/**
	  * Performs the specified function if the request can be authorized using either basic authorization or a
	  * device auth key. Used device auth key will have to match the specified device id. If not, it will be invalidated
	  * as a safety measure.
	  * @param requiredDeviceId Device id that the specified key must be connected to, if present
	  * @param f Function called when request is authorized. Accepts userId + whether device key was used +
	  *          database connection. Produces an http result.
	  * @return Function result or a result indicating that the request was unauthorized. Wrapped as a response.
	  */
	def basicOrDeviceKeyAuthorized(requiredDeviceId: Int)(f: (Int, Boolean, Connection) => Result) =
	{
		// Checks whether basic or device authorization should be used
		request.headers.authorization match
		{
			case Some(authHeader) =>
				val authType = authHeader.untilFirst(" ")
				if (authType ~== "basic")
					basicAuthorized { (userId, connection) => f(userId, false, connection) }
				else if (authType ~== "bearer")
					deviceKeyAuthorized { (key, connection) =>
						// Makes sure the device id in the key matches the required device id. If not, invalidates the
						// key because it may have become compromised
						if (key.deviceId == requiredDeviceId)
							f(key.userId, true, connection)
						else
						{
							DbDeviceKey(key.id).invalidate()(connection)
							Result.Failure(Unauthorized,
								"The key you specified cannot be used for this resource. " +
									"Also, your key has now been invalidated and can no longer be used.")
						}
					}
				else
					Result.Failure(Unauthorized, "Only basic and bearer authorizations are supported").toResponse(this)
			case None => Result.Failure(Unauthorized, "Authorization header is required").toResponse(this)
		}
	}
	
	/**
	  * Authorizes the request using an api key in the bearer auth header. Uses existing database connection.
	  * @param f A function called if the request is authorized (accepts valid api key)
	  * @param connection Implicit database connection
	  * @return Function result if the request was authorized, otherwise an authorization failure
	  */
	def apiKeyAuthorizedWithConnection(f: ApiKey => Result)(implicit connection: Connection) =
	{
		// Checks the bearer auth token
		request.headers.bearerAuthorization match
		{
			case Some(token) =>
				// Makes sure the token is registered in the database
				DbApiKey(token) match
				{
					case Some(key) => f(key)
					case None => Result.Failure(Unauthorized, "Invalid api key")
				}
			case None => Result.Failure(Unauthorized, "Please provide a api key in the auth bearer header")
		}
	}
	
	/**
	  * Authorizes the request using an api key in the bearer auth header
	  * @param f A function called if the request is authorized (accepts valid api key and database connection)
	  * @return Function result if the request was authorized, otherwise an authorization failure
	  */
	def apiKeyAuthorized(f: (ApiKey, Connection) => Result) =
		tokenAuthorized("api key") { (key, connection) => DbApiKey(key)(connection) }(f)
	
	/**
	  * Performs the specified function if the user is authorized (using session key) and they are a member of the
	  * specified organization
	  * @param organizationId Id of the organization the user is supposed to be a member of
	  * @param f              Function called when the user is fully authorized. Takes user session, membership id and database
	  *                       connection as parameters. Returns operation result.
	  * @return An http response based either on the function result or authorization failure.
	  */
	def authorizedInOrganization(organizationId: Int)(f: (UserSession, Int, Connection) => Result) =
	{
		// Authorizes the request using a session key token
		sessionKeyAuthorized { (session, connection) =>
			implicit val c: Connection = connection
			// Makes sure the user belongs to the target organization
			DbUser(session.userId).membershipIdInOrganizationWithId(organizationId).pull match
			{
				case Some(membershipId) => f(session, membershipId, connection)
				case None => Result.Failure(Unauthorized, "You're not a member of this organization")
			}
		}
	}
	
	/**
	  * Performs the specified function if:<br>
	  * 1) The request can be authorized using a valid session key<br>
	  * 2) The authorized user is a member of the specified organization and<br>
	  * 3) The user has the right/authorization to perform the specified task within that organization
	  * @param organizationId Id of the targeted organization
	  * @param taskId Id of the task the user is trying to perform
	  * @param f Function called when the user is fully authorized. Takes user session, membership id and database
	  *          connection as parameters. Returns operation result.
	  * @return An http response based either on the function result or authorization failure.
	  */
	def authorizedForTask(organizationId: Int, taskId: Int)(f: (UserSession, Int, Connection) => Result) =
	{
		// Makes sure the user belongs to the organization and that they have a valid session key authorization
		authorizedInOrganization(organizationId) { (session, membershipId, connection) =>
			implicit val c: Connection = connection
			// Makes sure the user has a right to perform the required task
			if (DbMembership(membershipId).allowsTaskWithId(taskId))
				f(session, membershipId, connection)
			else
				Result.Failure(Forbidden,
					"You haven't been granted the right to perform this task within this organization")
		}
	}
	
	/**
	  * Authorizes a request using bearer token authorization
	  * @param keyTypeName Name used for the key (Eg. 'api key')
	  * @param testKey A function for testing key validity. Accepts the provided token and a database connection.
	  *                Returns a valid item associated with the key (if present)
	  * @param f A function that performs the operation when authentication succeeds. Accepts 1) the item associated
	  *          with the provided token and 2) a database connection and produces a response for the client.
	  * @tparam K Type of item associated with the token
	  * @return Response containing either function <i>f</i> result or an authentication failure
	  *         (if <i>testKey</i> returned None or the token was missing)
	  */
	def tokenAuthorized[K](keyTypeName: => String)(testKey: (String, Connection) => Option[K])
						  (f: (K, Connection) => Result) =
	{
		// Checks the key from token
		val result = request.headers.bearerAuthorization match
		{
			case Some(key) =>
				// Validates the device key against database
				connectionPool.tryWith { connection =>
					testKey(key, connection) match
					{
						case Some(authorizedKey) => f(authorizedKey, connection)
						case None => Result.Failure(Unauthorized, s"Invalid or expired $keyTypeName")
					}
				}.getOrMap { e =>
					errorHandler(e)
					Result.Failure(InternalServerError, e.getMessage)
				}
			case None => Result.Failure(Unauthorized, s"Please provided a bearer auth hearer with a $keyTypeName")
		}
		result.toResponse(this)
	}
	
	/**
	  * Authorizes this request using an email activation token from the bearer token authorization header
	  * @param emailPurposeId Id of the purpose the email is used for (must match the purpose id the validation was
	  *                       first registered with)
	  * @param f A function that is performed if the specified token was valid.
	  *          Accepts an open email validation attempt and a database connection.
	  *          Returns 1) a boolean indicating whether the validation should be closed and
	  *          2) result to send back to the client.
	  * @return A response based on the function result if authorization was successful. A failure response otherwise.
	  */
	def emailAuthorized(emailPurposeId: Int)(f: (EmailValidation, Connection) => (Boolean, Result)) =
	{
		request.headers.bearerAuthorization match
		{
			case Some(key) =>
				connectionPool.tryWith { implicit connection =>
					DbEmailValidation.activateWithKey(key, emailPurposeId) { f(_, connection) }
				} match
				{
					case Success(result) =>
						result match
						{
							case Success(result) => result.toResponse(this)
							case Failure(error) => Result.Failure(Unauthorized, error.getMessage).toResponse(this)
						}
					case Failure(error) =>
						errorHandler(error)
						Result.Failure(InternalServerError, error.getMessage).toResponse(this)
				}
			case None =>
				Result.Failure(Unauthorized,
					"Please provide a bearer authorization header containing an email validation token")
					.toResponse(this)
		}
	}
	
	/**
	  * Parses a value from the request body and uses it to produce a response
	  * @param f Function that will be called if the value was successfully read. Returns an http result.
	  * @return Function result or a failure result if no value could be read.
	  */
	def handleValuePost(f: Value => Result) =
	{
		// Parses the post body first
		request.body.headOption match
		{
			case Some(body) =>
				// Accepts json, xml and text content types
				val value = body.contentType.subType.toLowerCase match
				{
					case "json" => body.bufferedJson.contents
					case "xml" => body.bufferedXml.contents.map { _.toSimpleModel: Value }
					case _ =>
						body.contentType.category match
						{
							case Text => body.bufferedToString.contents.map { s => s: Value }
							case _ => Failure(ContentTypeException.notAccepted(body.contentType,
								Vector(Application.json, Application.xml, Text.plain)))
						}
				}
				value match
				{
					case Success(value) => f(value)
					case Failure(error) => Result.Failure(BadRequest, error.getMessage)
				}
			case None => Result.Failure(BadRequest, "Please specify a body in the request")
		}
	}
	
	/**
	  * Parses a model from the request body and uses it to produce a response
	  * @param parser Model parser
	  * @param f Function that will be called if the model was successfully parsed. Returns an http result.
	  * @tparam A Type of parsed model
	  * @return Function result or a failure result if no model could be parsed.
	  */
	def handlePost[A](parser: FromModelFactory[A])(f: A => Result): Result =
	{
		handleValuePost { value =>
			value.model match
			{
				case Some(model) =>
					parser(model) match
					{
						// Gives the parsed model to specified function
						case Success(parsed) => f(parsed)
						case Failure(error) => Result.Failure(BadRequest, error.getMessage)
					}
				case None => Result.Failure(BadRequest, "Please provide a json object in the request body")
			}
		}
	}
	
	/**
	  * Parses request body into a vector of values and handles them using the specified function.
	  * For non-array bodies, wraps the body in a vector.
	  * @param f Function that will be called if a json body was present. Accepts a vector of values. Returns result.
	  * @return Function result or a failure if no value could be read
	  */
	def handleArrayPost(f: Vector[Value] => Result) = handleValuePost { v: Value =>
		if (v.isEmpty)
			f(Vector())
		else
			v.vector match
			{
				case Some(vector) => f(vector)
				// Wraps the value into a vector if necessary
				case None => f(Vector(v))
			}
	}
	
	/**
	  * Parses request body into a vector of models and handles them using the specified function. Non-vector bodies
	  * are wrapped in vectors, non-object elements are ignored.
	  * @param parser Parser function used for parsing models into objects
	  * @param f Function called if all parsing succeeds
	  * @tparam A Type of parsed item
	  * @return Function result or failure in case of parsing failures
	  */
	def handleModelArrayPost[A](parser: Model[Constant] => Try[A])(f: Vector[A] => Result) = handleArrayPost { values =>
		values.flatMap { _.model }.tryMap { parser(_) } match
		{
			case Success(parsed) => f(parsed)
			case Failure(error) => Result.Failure(BadRequest, error.getMessage)
		}
	}
	
	/**
	  * Parses request body into a vector of models and handles them using the specified function. Non-vector bodies
	  * are wrapped in vectors, non-object elements are ignored.
	  * @param parser Parser used for parsing models into objects
	  * @param f Function called if all parsing succeeds
	  * @tparam A Type of parsed item
	  * @return Function result or failure in case of parsing failures
	  */
	def handleModelArrayPost[A](parser: FromModelFactory[A])(f: Vector[A] => Result): Result =
		handleModelArrayPost[A] { m: Model[Constant] => parser(m) }(f)
}
