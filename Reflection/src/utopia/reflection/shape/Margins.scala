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
	val small = (medium / 2.0).round
	/**
	  * @return A very small version of margin
	  */
	val verySmall = (medium / 4.0).round
	/**
	  * @return A large version of margin
	  */
	val large = (medium * 2.0).round
}
