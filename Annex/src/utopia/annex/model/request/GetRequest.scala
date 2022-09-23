package utopia.annex.model.request

import utopia.access.http.Method.Get
import utopia.flow.collection.value.typeless.Value

object GetRequest
{
	// OTHER    -----------------------------
	
	/**
	  * @param path Targeted resource path (after root path)
	  * @param deprecationCondition A condition that must be true in order for this request to deprecate (optional).
	  *                             Requests don't deprecate by default.
	  * @return A new request targeting specified path
	  */
	def apply(path: String, deprecationCondition: => Boolean = false): GetRequest =
		new SimpleGetRequest(path, deprecationCondition)
	
	
	// NESTED   -----------------------------
	
	private class SimpleGetRequest(override val path: String, deprecationCondition: => Boolean) extends GetRequest
	{
		override def isDeprecated = deprecationCondition
	}
}

/**
  * Common trait for simple api requests that use the GET method
  * @author Mikko Hilpinen
  * @since 16.6.2020, v1
  */
trait GetRequest extends ApiRequest
{
	override def method = Get
	
	override def body = Value.empty
	
	override def persistingModel = None
}
