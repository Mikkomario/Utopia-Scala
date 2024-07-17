package utopia.annex.test

import utopia.access.http.Status.OK
import utopia.annex.model.response.{RequestFailure, Response}
import utopia.annex.test.TestClientContext._
import utopia.flow.async.AsyncExtensions._
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Model

/**
  * Tests the echo request
  * @author Mikko Hilpinen
  * @since 17.07.2024, v1.8
  */
object EchoTest extends App
{
	val sentModel = Model.from("prop1" -> "a", "prop2" -> 2, "prop3" -> Vector(1, 2, 3))
	println("Acquiring an echo...")
	client.send(GetEcho(sentModel)).waitFor().get match {
		case Response.Success(value: Model, status, _) =>
			println(value)
			assert(status == OK)
			assert(value("body")("content").getModel ~== sentModel)
		case f: RequestFailure => throw f.cause
	}
	
	println("Success!")
}
