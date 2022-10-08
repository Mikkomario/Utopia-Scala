package utopia.annex.model.request

import utopia.access.http.Method.Delete
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration, ModelValidationFailedException, PropertyDeclaration, Value}
import utopia.flow.generic.model.mutable.DataType.StringType
import utopia.flow.generic.model.template.{ModelLike, Property}
import utopia.flow.operator.EqualsExtensions._

import scala.util.{Failure, Success}

object DeleteRequest extends FromModelFactory[DeleteRequest]
{
	// ATTRIBUTES	-----------------------
	
	private val schema = ModelDeclaration(PropertyDeclaration("path", StringType))
	
	
	// IMPLEMENTED	-----------------------
	
	override def apply(model: ModelLike[Property]) = schema.validate(model).toTry.flatMap { valid =>
		if (valid("method").string.forall { _ ~== Delete.toString })
			Success(apply(valid("path").getString))
		else
			Failure(new ModelValidationFailedException(s"Trying to parse a DELETE request from model with method ${
				valid("method").description}"))
	}
	
	
	// OTHER	---------------------------
	
	/**
	  * @param path Path to targeted resource (root path not included)
	  * @return A request for deleting the targeted resource
	  */
	def apply(path: String): DeleteRequest = SimpleDeleteRequest(path)
	
	
	// NESTED	---------------------------
	
	private case class SimpleDeleteRequest(path: String) extends DeleteRequest
}

/**
  * These requests are used for deleting items at the server side
  * @author Mikko Hilpinen
  * @since 17.6.2020, v1
  */
trait DeleteRequest extends ApiRequest
{
	override def method = Delete
	
	override def body = Value.empty
	
	override def isDeprecated = false
	
	override def persistingModel = Some(Model(Vector("method" -> method.toString, "path" -> path)))
}
