package utopia.annex.model.request

import utopia.access.http.Method
import utopia.access.http.Method.Post
import utopia.annex.model.Spirit
import utopia.flow.collection.immutable.Pair
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration, PropertyDeclaration}
import utopia.flow.generic.model.mutable.DataType.ModelType
import utopia.flow.generic.model.template.{ModelConvertible, ModelLike, Property}
import utopia.flow.view.immutable.eventful.Fixed

object PostSpiritRequest
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
	def apply[S <: Spirit with ModelConvertible](spirit: S, method: Method = Post): PostSpiritRequest[S] =
		SimplePostSpiritRequest[S](spirit, method)
	
	/**
	  * @param spiritFactory A factory for parsing spirit data from models
	  * @tparam S Type of parsed spirit
	  * @return A factory for parsing persisted post requests
	  */
	def factory[S <: Spirit with ModelConvertible](spiritFactory: FromModelFactory[S]): FromModelFactory[PostSpiritRequest[S]] =
		PostRequestFactory(spiritFactory)
	
	
	// NESTED	-------------------------
	
	private case class PostRequestFactory[+S <: Spirit with ModelConvertible](spiritFactory: FromModelFactory[S])
		extends FromModelFactory[PostSpiritRequest[S]]
	{
		override def apply(model: ModelLike[Property]) = baseSchema.validate(model).flatMap { valid =>
			spiritFactory(valid("spirit").getModel).map { spirit =>
				PostSpiritRequest(spirit, valid("method").string.flatMap(Method.parse).getOrElse(Post))
			}
		}
	}
	
	private case class SimplePostSpiritRequest[+S <: Spirit with ModelConvertible](spirit: S, override val method: Method = Post)
		extends PostSpiritRequest[S]
}

/**
  * A common trait for requests used for posting new data to the server side
  * @author Mikko Hilpinen
  * @since 16.6.2020, v1
  */
trait PostSpiritRequest[+S <: Spirit with ModelConvertible] extends ApiRequest with Persisting
{
	// ABSTRACT ---------------------------
	
	/**
	  * @return Spirit linked to this request
	  */
	def spirit: S
	
	
	// IMPLEMENTED    ---------------------
	
	def method: Method = Post
	override def path = spirit.postPath
	override def body = spirit.postBody
	
	override def deprecated = false
	
	override def persistingModelPointer =
		Fixed(Some(Model(Pair("method" -> method.toString, "spirit" -> spirit.toModel))))
}
