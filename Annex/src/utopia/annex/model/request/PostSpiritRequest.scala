package utopia.annex.model.request

import utopia.access.http.Method
import utopia.access.http.Method.Post
import utopia.annex.controller.ApiClient
import utopia.annex.controller.ApiClient.PreparedRequest
import utopia.annex.model.Spirit
import utopia.annex.model.response.RequestResult2
import utopia.flow.collection.immutable.Pair
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration, PropertyDeclaration}
import utopia.flow.generic.model.mutable.DataType.ModelType
import utopia.flow.generic.model.template.{ModelConvertible, ModelLike, Property}
import utopia.flow.view.immutable.eventful.Fixed

import scala.concurrent.Future

@deprecated("Deprecated for removal. No replacement is intended at this time.", "v1.8")
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
	def apply[S <: Spirit with ModelConvertible, A](spirit: S, method: Method = Post)
	                                               (send: PreparedRequest => Future[RequestResult2[A]]): PostSpiritRequest[S, A] =
		new SimplePostSpiritRequest[S, A](spirit, method)(send)
	
	/**
	  * @param spiritFactory A factory for parsing spirit data from models
	  * @tparam S Type of parsed spirit
	  * @return A factory for parsing persisted post requests
	  */
	def factory[S <: Spirit with ModelConvertible, A](spiritFactory: FromModelFactory[S])
	                                                 (send: PreparedRequest => Future[RequestResult2[A]]): FromModelFactory[PostSpiritRequest[S, A]] =
		PostRequestFactory(spiritFactory)(send)
	
	
	// NESTED	-------------------------
	
	private case class PostRequestFactory[+S <: Spirit with ModelConvertible, +A](spiritFactory: FromModelFactory[S])
	                                                                             (sendFunction: PreparedRequest => Future[RequestResult2[A]])
		extends FromModelFactory[PostSpiritRequest[S, A]]
	{
		override def apply(model: ModelLike[Property]) =
			baseSchema.validate(model).flatMap { valid =>
				spiritFactory(valid("spirit").getModel).map { spirit =>
					PostSpiritRequest(spirit, valid("method").string.flatMap(Method.parse).getOrElse(Post))(sendFunction)
				}
			}
	}
	
	private class SimplePostSpiritRequest[+S <: Spirit with ModelConvertible, +A](override val spirit: S,
	                                                                              override val method: Method = Post)
	                                                                             (f: PreparedRequest => Future[RequestResult2[A]])
		extends PostSpiritRequest[S, A]
	{
		override def send(prepared: ApiClient.PreparedRequest): Future[RequestResult2[A]] = f(prepared)
	}
}

/**
  * A common trait for requests used for posting new data to the server side
  * @author Mikko Hilpinen
  * @since 16.6.2020, v1
  */
@deprecated("Deprecated for removal. No replacement is intended at this time.", "v1.8")
trait PostSpiritRequest[+S <: Spirit with ModelConvertible, +A] extends ApiRequest2[A] with Persisting
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
