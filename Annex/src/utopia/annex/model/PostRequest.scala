package utopia.annex.model

import utopia.access.http.Method
import utopia.access.http.Method.Post
import utopia.flow.datastructure.immutable.Model
import utopia.flow.generic.ModelConvertible
import utopia.flow.generic.ValueConversions._

object PostRequest
{
	// OTHER	-------------------------
	
	/**
	  * @param spirit A spirit to post
	  * @tparam S Type of posted spirit
	  * @return A new request for posting spirit data
	  */
	def apply[S <: Spirit with ModelConvertible](spirit: S): PostRequest[S] = SimplePostRequest[S](spirit)
	
	
	// NESTED	-------------------------
	
	private case class SimplePostRequest[+S <: Spirit with ModelConvertible](spirit: S) extends PostRequest[S]
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
