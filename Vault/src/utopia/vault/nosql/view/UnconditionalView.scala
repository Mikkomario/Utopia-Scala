package utopia.vault.nosql.view

/**
  * A common trait for access points that don't apply any conditions
  * @author Mikko Hilpinen
  * @since 11.7.2021, v1.8
  */
trait UnconditionalView extends View
{
	// Global condition is forced to be None
	final override def accessCondition: None.type = None
}
