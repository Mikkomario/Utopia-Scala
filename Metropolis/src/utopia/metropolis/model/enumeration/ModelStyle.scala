package utopia.metropolis.model.enumeration

import utopia.flow.operator.EqualsExtensions._
import utopia.flow.util.CollectionExtensions._

import java.util.NoSuchElementException

/**
  * An enumeration for different ways / formats / styles to which a model can be formed
  * @author Mikko Hilpinen
  * @since 29.6.2021, v1.0.1
  */
sealed trait ModelStyle
{
	// ABSTRACT	--------------------
	
	/**
	  * A string key matching this model style
	  */
	def key: String
	/**
	  * Id used for this value in database / SQL
	  */
	def id: Int
}

object ModelStyle
{
	// ATTRIBUTES	--------------------
	
	/**
	  * All available values of this enumeration
	  */
	val values: Vector[ModelStyle] = Vector(Full, Simple)
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Id representing a ModelStyle
	  * @return ModelStyle matching that id. None if the id didn't match any ModelStyle
	  */
	def findForId(id: Int) = values.find { _.id == id }
	/**
	  * @param id Id matching a ModelStyle
	  * @return ModelStyle matching that id. Failure if no suitable value was found.
	  */
	def forId(id: Int) = 
		findForId(id).toTry { new NoSuchElementException(s"No value of ModelStyle matches id '$id'") }
	
	/**
	  * @param key A model style key ("full" or "simple")
	  * @return A model style matching that key. None if none of the styles match.
	  */
	def findForKey(key: String) = values.find { _.key ~== key }
	/**
	  * @param key Key matching a ModelStyle
	  * @return ModelStyle matching that id. Failure if no suitable value was found.
	  */
	def forKey(key: String) =
		findForKey(key).toTry { new NoSuchElementException(s"No value of ModelStyle matches key '$key'") }
	
	
	// NESTED	--------------------
	
	/**
	  * A model style where all known data is listed.
	  * This style should be used when converting a model into json for transportation purposes
	  * where the other end will parse a model with the same object / database structure.
	  * Namely, this concerns Scala clients that use this Metropolis module.
	  */
	case object Full extends ModelStyle
	{
		// ATTRIBUTES	--------------------
		
		override val key = "full"
		override val id = 1
	}
	
	/**
	  * A model style where the model format is simplified and some information omitted.
	  * This style should be used when the client side uses a different model / object format
	  * than the server side. Using this model style can make http responses more
	  * readable and easy to process. This, however, means that the same objects cannot be parsed
	  * from those that were originally converted; Therefore, this style shouldn't be used when attempting
	  * to convert response data back to Metropolis models.
	  */
	case object Simple extends ModelStyle
	{
		// ATTRIBUTES	--------------------
		
		override def key = "simple"
		override val id = 2
	}
}

