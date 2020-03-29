package utopia.vault.nosql.access

/**
 * A common trait for access points that don't apply any conditions
 * @author Mikko Hilpinen
 * @since 30.1.2020, v0.1
 */
trait UnconditionalAccess[+A] extends Access[A]
{
	// Global condition is forced to be None
	final override def globalCondition: None.type = None
}
