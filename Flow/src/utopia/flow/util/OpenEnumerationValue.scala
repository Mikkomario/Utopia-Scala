package utopia.flow.util

/**
 * Common trait for enumerations which allow values to be added from sub-modules or other sources.
 * @tparam A Type of identifier used when representing this value
 * @author Mikko Hilpinen
 * @since 23.04.2024, v2.4
 */
trait OpenEnumerationValue[+A]
{
	/**
	 * @return Identifier that distinguishes this enumeration value from the other values within the same enumeration.
	 */
	def identifier: A
}
