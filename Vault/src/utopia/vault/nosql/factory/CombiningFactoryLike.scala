package utopia.vault.nosql.factory

/**
  * A common trait for factory classes which combine the results of two factories
  * @author Mikko Hilpinen
  * @since 29.6.2021, v1.8
  */
trait CombiningFactoryLike[+Combined, +Parent, +Child] extends LinkedFactoryLike[Combined, Child]
{
	// ABSTRACT -----------------------------
	
	/**
	  * @return The factory used for reading and parsing parent instance data
	  */
	def parentFactory: FromRowModelFactory[Parent]
	
	
	// IMPLEMENTED  -------------------------
	
	override def table = parentFactory.table
}
