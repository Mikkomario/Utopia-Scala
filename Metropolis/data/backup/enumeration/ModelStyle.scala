package utopia.metropolis.model.enumeration

import utopia.flow.util.StringExtensions._

/**
  * An enumeration for different ways / formats / styles to which a model can be formed
  * @author Mikko Hilpinen
  * @since 29.6.2021, v1.0.1
  */
sealed trait ModelStyle
{
	/**
	  * A string key matching this model style
	  */
	val key: String
	/**
	  * An id matching this model style
	  */
	val id: Int
}

object ModelStyle
{
	// ATTRIBUTES   -------------------------
	
	/**
	  * Known model styles
	  */
	lazy val values = Vector[ModelStyle](Full, Simple)
	
	
	// OTHER    -----------------------------
	
	/**
	  * @param id A model style id
	  * @return A model style matching that id (None if not found)
	  */
	def forId(id: Int) = values.find { _.id == id }
	/**
	  * @param key A model style key ("full" or "simple")
	  * @return A model style matching that key. None if none of the styles match.
	  */
	def forKey(key: String) = values.find { _.key ~== key }
	
	
	// NESTED   -----------------------------
	
	/**
	  * A model style where all known data is listed.
	  * This style should be used when converting a model into json for transportation purposes
	  * where the other end will parse a model with the same object / database structure.
	  * Namely, this concerns Scala clients that use this Metropolis module.
	  */
	case object Full extends ModelStyle
	{
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
		override val key = "simple"
		override val id = 2
	}
}
