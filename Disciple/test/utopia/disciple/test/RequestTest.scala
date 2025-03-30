package utopia.disciple.test

import utopia.access.model.Headers
import utopia.access.model.enumeration.Status
import utopia.disciple.controller.Gateway
import utopia.disciple.controller.interceptor.AccessLogger
import utopia.disciple.model.request.Request
import utopia.flow.async.AsyncExtensions._
import utopia.flow.parse.json.{JsonParser, JsonReader}
import utopia.flow.test.TestContext._
import utopia.flow.util.logging.SysErrLogger

import scala.io.StdIn

/**
  * Used for sending out test request
  * @author Mikko Hilpinen
  * @since 20.1.2023, v1.6
  */
object RequestTest extends App
{
	Status.setup()
	implicit val jsonParser: JsonParser = JsonReader
	
	val acc = AccessLogger.using(SysErrLogger)
	val gateway = new Gateway(/*Vector(JsonBunny), */requestInterceptors = Vector(acc), responseInterceptors = Vector(acc))
	
	println("Sending request...")
	println("Please type in the API-key to use")
	val response = gateway.valueResponseFor(Request("https://asd",
		headers = Headers.withBearerAuthorization(StdIn.readLine())))
		.waitForResult().get
	println("Response received")
	println(s"Status: ${ response.status }")
	println("Headers:")
	response.headers.fields.foreach { case (key, value) => println(s"$key : $value") }
	println("Body:")
	println(response.body.getString)
}
