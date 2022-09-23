package utopia.annex.model.request

import utopia.access.http.Method
import utopia.access.http.Method.Post
import utopia.annex.model.Spirit
import utopia.flow.collection.template.typeless
import utopia.flow.collection.template.typeless.Property
import utopia.flow.collection.value.typeless.PropertyDeclaration
import utopia.flow.datastructure.template
import utopia.flow.generic.{FromModelFactory, ModelConvertible, ModelType}
import utopia.flow.generic.ValueConversions._

object PostRequest
{
	// ATTRIBUTES	---------------------
	
	private val baseSchema = ModelDeclaration(PropertyDeclaration("spirit", ModelType))
	
	
	// OTHER	-------------------------
	
	/**
	  * @param spirit A spirit to post
	  * @param method Method to use (default = Post)
	  * @tparam S Type of posted spirit
	  * @return A new request for posting spirit data
	  */
	def apply[S <: Spirit with ModelConvertible](spirit: S, method: Method = Post): PostRequest[S] =
		SimplePostRequest[S](spirit, method)
	
	/**
	  * @param spiritFactory A factory for parsing spirit data from models
	  * @tparam S Type of parsed spirit
	  * @return A factory for parsing persisted post requests
	  */
	def factory[S <: Spirit with ModelConvertible](spiritFactory: FromModelFactory[S]): FromModelFactory[PostRequest[S]] =
		PostRequestFactory(spiritFactory)
	
	
	// NESTED	-------------------------
	
	private case class PostRequestFactory[+S <: Spirit with ModelConvertible](spiritFactory: FromModelFactory[S])
		extends FromModelFactory[PostRequest[S]]
	{
		override def apply(model: typeless.Model[Property]) = baseSchema.validate(model).toTry.flatMap { valid =>
			spiritFactory(valid("spirit").getModel).map { spirit =>
				PostRequest(spirit, valid("method").string.flatMap(Method.parse).getOrElse(Post))
			}
		}
	}
	
	private case class SimplePostRequest[+S <: Spirit with ModelConvertible](spirit: S, override val method: Method = Post)
		extends PostRequest[S]
}

/**
  * A common trait for requests used for posting new data to the server side
  * @author Mikko Hilpinen
  * @since 16.6.2020, v1
  */
trait PostRequest[+S <: Spirit with ModelConvertible] extends ApiRequest
{
	// ABSTRACT ---------------------------
	
	/**
	  * @return Spirit linked to this request
	  */
	def spirit: S
	
	
	// IMPLEMENTED    ---------------------
	
	override def isDeprecated = false
	
	override def persistingModel = Some(Model(Vector("method" -> method.toString,
		"spirit" -> spirit.toModel)))
	
	def method: Method = Post
	
	override def path = spirit.postPath
	
	override def body = spirit.postBody
}
