package utopia.citadel.coder.model.scala

/**
  * Declares a scala item of some sort
  * @author Mikko Hilpinen
  * @since 30.8.2021, v0.1
  */
trait Declaration extends Referencing
{
	/**
	  * @return Visibility of this declaration
	  */
	def visibility: Visibility
	
	/**
	  * @return Keyword for this declaration (E.g. "def" or "val")
	  */
	def keyword: String
	
	/**
	  * @return Name of this method / property
	  */
	def name: String
}
