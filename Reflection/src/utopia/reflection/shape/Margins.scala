package utopia.reflection.shape

/**
  * An object that provides access to simple length values
  * @author Mikko Hilpinen
  * @since 17.11.2019, v1
  * @param medium The standard margin
  */
case class Margins(medium: Double)
{
	/**
	  * @return A smaller version of margin
	  */
	def small = medium / 2.0
	/**
	  * @return A very small version of margin
	  */
	def verySmall = medium / 4.0
	/**
	  * @return A large version of margin
	  */
	def large = medium * 2.0
}
