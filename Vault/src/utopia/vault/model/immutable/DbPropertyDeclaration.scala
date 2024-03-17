package utopia.vault.model.immutable

import scala.language.implicitConversions

object DbPropertyDeclaration
{
	// Provides implicit access to both values
	implicit def autoAccessName(d: DbPropertyDeclaration): String = d.name
	implicit def autoAccessColumn(d: DbPropertyDeclaration): Column = d.column
}

/**
  * A model that provides access to a specific database model property name,
  * also facilitating column access
  * @author Mikko Hilpinen
  * @since 16/03/2024, v1.18.1
  * @constructor Creates a new declaration by specifying both the db property name, as well as the accessed column
  * @param name Name of this database-based property
  * @param column Column being interacted with
  */
case class DbPropertyDeclaration(name: String, column: Column)
{
	override def toString = name
}