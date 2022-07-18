package utopia.vault.coder.model.data

/**
  * A common trait for items that have singular and plural names
  * @author Mikko Hilpinen
  * @since 18.7.2022, v1.5.1
  */
trait Named
{
	// ABSTRACT ------------------------
	
	/**
	  * @return Name of this instance
	  */
	def name: Name
}
