package utopia.access.model

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.immutable.{Model, Value}
import utopia.flow.generic.model.template.HasPropertiesLike.HasProperties
import utopia.flow.generic.model.template.ModelConvertible

import scala.util.Try

object Cookie extends FromModelFactory[Cookie]
{
	/**
	  * Parses a cookie from the provided model. The model must have a 'name' property or None is
	  * returned.
	  */
	// Name property is required
	override def apply(model: HasProperties): Try[Cookie] = model("name").tryString.map { name =>
		Cookie(name, model("value"), model("life_limit_seconds").int, model("secure").getBoolean)
	}
}

/**
  * Cookies are used for storing data on the client side. The client should send cookies back to
  * the server on the consequent requests.
  * @author Mikko Hilpinen
  * @since 3.9.2017
  */
case class Cookie(name: String, value: Value, lifeLimitSeconds: Option[Int] = None, isSecure: Boolean = false)
	extends ModelConvertible
{
	// IMPLEMENTED METHODS / PROPERTIES    ------------
	
	override def toModel = Model(Vector("name" -> name, "value" -> value, "life_limit_seconds" -> lifeLimitSeconds,
		"secure" -> isSecure))
	
	override def toString = toJson
}