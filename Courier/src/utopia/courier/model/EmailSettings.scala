package utopia.courier.model

import java.util.Properties
import scala.util.Try

/**
  * A common trait for settings instances which modify message read / send properties
  * @author Mikko Hilpinen
  * @since 10.9.2021, v0.1
  */
trait EmailSettings
{
	// ABSTRACT -------------------------
	
	/**
	  * @return Address of the email service
	  */
	def hostAddress: String
	
	/**
	  * @return Properties that will be assigned
	  */
	def properties: Map[String, AnyRef]
	
	/**
	  * @return Properties that will be removed
	  */
	def removedProperties: Set[String]
	
	
	// OTHER    ------------------------
	
	/**
	  * Modifies the provided set of properties
	  * @param original Original set of properties
	  * @return A modified version of these properties (same instance)
	  */
	def modify(original: Properties) = Try {
		properties.foreach { case (key, value) => original.put(key, value) }
		original
	}
}
