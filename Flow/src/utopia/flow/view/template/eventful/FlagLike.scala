package utopia.flow.view.template.eventful

/**
  * A common trait for items which resemble a boolean flag
  * @author Mikko Hilpinen
  * @since 18.9.2022, v1.17
  */
trait FlagLike extends ChangingLike[Boolean]
{
	/**
	  * @return Whether this flag has been set
	  */
	def isSet = value
	
	/**
	  * @return Whether this flag hasn't been set yet
	  */
	def isNotSet = !isSet
}
