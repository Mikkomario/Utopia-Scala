package utopia.flow.view.template

/**
  * Common trait for items which may be in a binary "set" / on state
  * @author Mikko Hilpinen
  * @since 27.08.2024, v2.5
  */
trait MaybeSet
{
	// ABSTRACT -------------------------
	
	/**
	  * @return Whether this item is currently "set" / on
	  */
	def isSet: Boolean
	
	
	// COMPUTED -------------------------
	
	/**
	  * @return Whether this item is not currently "set" / on
	  */
	def isNotSet = !isSet
}
