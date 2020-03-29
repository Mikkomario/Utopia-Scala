package utopia.genesis.util

/**
 * Classes extending this trait can be compared with instances of a class using approximate comparison
 * @author Mikko Hilpinen
 * @since 1.8.2017
 */
trait ApproximatelyEquatable[-A]
{
    /**
     * Checks whether the two instances are approximately equal
     */
    def ~==(other: A): Boolean
    
    /**
     * Checks whether the two instances are <b>not</b> approximately equal
     */
    def !~==(other: A) = !(this ~== other)
}