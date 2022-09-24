package utopia.access.http

import utopia.flow.collection.template.typeless
import utopia.flow.collection.value.typeless.Value
import utopia.flow.generic.model
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.datastructure.template
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.immutable.Value
import utopia.flow.generic.model.template.{ModelConvertible, Property}

import scala.util.{Failure, Success, Try}

object Cookie extends FromModelFactory[Cookie]
{
    /**
     * Parses a cookie from the provided model. The model must have a 'name' property or None is 
     * returned.
     */
    override def apply(model: model.template.Model[Property]): Try[Cookie] =
    {
        // Name property is required
        val name = model("name").string
        
        if (name.isDefined)
            Success(Cookie(name.get, model("value"), model("life_limit_seconds").int, model("secure").getBoolean))
        else
            Failure(new NoSuchElementException(s"Cannot parse a Cookie from $model without 'name' property"))
    }
}

/**
 * Cookies are used for storing data on the client side. The client should send cookies back to 
 * the server on the consequent requests.
 * @author Mikko Hilpinen
 * @since 3.9.2017
 */
case class Cookie(name: String, value: Value, lifeLimitSeconds: Option[Int] = None, isSecure: Boolean = false) extends ModelConvertible
{
    // IMPLEMENTED METHODS / PROPERTIES    ------------
    
    override def toModel = Model(Vector("name" -> name, "value" -> value, "life_limit_seconds" -> lifeLimitSeconds,
        "secure" -> isSecure))
    
    override def toString = toJson
}