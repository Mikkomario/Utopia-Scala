package utopia.annex.model

import utopia.flow.datastructure.immutable.{Constant, Model}

/**
  * A common trait for classes which represent items before they are recorded on server side. Even server side instances
  * may be handled through their spirit representations
  * @author Mikko Hilpinen
  * @since 16.6.2020, v1
  */
trait Spirit
{
	/**
	  * @return An identifier used for identifying this spirit. Should be unique between currently active spirits of
	  *         this type.
	  */
	def identifier: Any
	
	/**
	  * @return Path used when posting this data to server
	  */
	def postPath: String
	
	/**
	  * @return Body that will be posted to server
	  */
	def postBody: Model[Constant]
}
