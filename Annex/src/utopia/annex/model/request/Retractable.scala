package utopia.annex.model.request

/**
  * Common trait for (queryable) items that may deprecate, at which event their processing should be cancelled,
  * if reasonably possible.
  * @author Mikko Hilpinen
  * @since 21.12.2023, v1.7
  */
trait Retractable extends Any
{
	// ABSTRACT -------------------------
	
	/**
	  * @return Whether this item has deprecated and shouldn't be processed anymore.
	  */
	def deprecated: Boolean
	
	
	// COMPUTED -------------------------
	
	/**
	  * @return Whether this item is still valid and should continue to be processed.
	  */
	def valid = !deprecated
}
