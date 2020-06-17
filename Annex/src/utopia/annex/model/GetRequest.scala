package utopia.annex.model

import utopia.access.http.Method.Get
import utopia.flow.datastructure.immutable.Model

object GetRequest
{
	
	// OTHER    -----------------------------
	
	/**
	  * @param path Targeted resource path (after root path)
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
	
	override def body = Model.empty
	
	override def persistingModel = None
}
