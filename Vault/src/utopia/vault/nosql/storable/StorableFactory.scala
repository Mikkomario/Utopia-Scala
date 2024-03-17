package utopia.vault.nosql.storable

import utopia.vault.model.immutable.{DbPropertyDeclaration, Storable}

/**
  * A partial implementation for storable model factories,
  * which also provide utility access to database property declarations.
  * @author Mikko Hilpinen
  * @since 16/03/2024, v1.18.1
  */
trait StorableFactory[+DbModel <: Storable, +Complete, Data] extends DataInserter[DbModel, Complete, Data]
{
	// OTHER    -------------------------
	
	/**
	  * @param propName Name of the targeted database-property
	  * @return Access to the specified property name, as well as column
	  */
	def property(propName: String) = DbPropertyDeclaration(propName, table(propName))
}
