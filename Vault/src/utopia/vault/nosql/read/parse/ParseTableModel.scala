package utopia.vault.nosql.read.parse

import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.immutable.Model
import utopia.flow.generic.model.template.{ModelLike, Property}
import utopia.vault.model.immutable.{Row, Table}
import utopia.vault.model.template.{HasTableAsTarget, SelectsTable}

import scala.util.Try

object ParseTableModel
{
	// OTHER    --------------------------
	
	/**
	  * @param table Targeted table
	  * @param f A function for parsing a validated table model. May yield a failure.
	  * @tparam A Type of parsed results, when successful.
	  * @return A new table model parser
	  */
	def apply[A](table: Table)(f: Model => Try[A]): ParseTableModel[A] = new _ParseTableModel[A](table, f)
	/**
	  * @param parser A parser to utilize to parse validated table models
	  * @param table Table to target
	  * @tparam A Type of parsed results, when successful
	  * @return A new table model parser
	  */
	def using[A](parser: FromModelFactory[A], table: Table): ParseTableModel[A] = new DelegatingParser[A](table, parser)
	
	
	// NESTED   --------------------------
	
	private class DelegatingParser[+A](override val table: Table, parser: FromModelFactory[A])
		extends ParseTableModel[A] with HasTableAsTarget
	{
		override protected def fromValid(model: Model): Try[A] = parser(model)
	}
	
	private class _ParseTableModel[+A](override val table: Table, f: Model => Try[A])
		extends ParseTableModel[A] with HasTableAsTarget
	{
		override protected def fromValid(model: Model): Try[A] = f(model)
	}
}

/**
  * An interface for parsing database-originated table models
  * @author Mikko Hilpinen
  * @since 10.07.2025, v1.22
  */
trait ParseTableModel[+A] extends ParseRow[A] with SelectsTable with FromModelFactory[A]
{
	// ABSTRACT --------------------------
	
	/**
	  * @param model A table model to parse.
	  *              Should already be validated against the table schema.
	  * @return Item parsed from the table model. Failure if parsing failed.
	  */
	protected def fromValid(model: Model): Try[A]
	
	
	// IMPLEMENTED  ----------------------
	
	override def apply(row: Row): Try[A] = apply(row(table))
	override def shouldParse(row: Row): Boolean = row.containsDataForTable(table)
	
	override def apply(model: ModelLike[Property]): Try[A] = table.validate(model).flatMap(fromValid)
}
