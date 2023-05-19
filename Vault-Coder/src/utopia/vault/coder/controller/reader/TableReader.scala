package utopia.vault.coder.controller.reader

import utopia.coder.model.data.Name
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.{Model, Value}
import utopia.flow.generic.model.mutable.DataType.{BooleanType, DoubleType, FloatType, InstantType, IntType, LocalDateType, LocalTimeType, LongType, StringType}
import utopia.flow.util.StringExtensions._
import utopia.coder.model.enumeration.NamingConvention.CamelCase
import utopia.vault.coder.util.Common.exc
import utopia.vault.coder.util.Common.connectionPool
import utopia.vault.database.{References, Tables}
import utopia.vault.database.columnlength.{ColumnLengthLimits, ColumnLengthRules}
import utopia.vault.model.immutable.{Column, ReferencePoint, Table}

/**
  * Interprets an existing database table into an input template
  * @author Mikko Hilpinen
  * @since 17.10.2022, v1.7.1
  */
object TableReader
{
	// ATTRIBUTES   --------------------------
	
	private lazy val tables = new Tables(connectionPool)
	
	
	// OTHER    ------------------------------
	
	/**
	  * Reads all tables that appear in a specific database
	  * @param dbName Name of the targeted database
	  * @return All input models based on the specified tables
	  */
	def apply(dbName: String): Vector[Model] = tables.all(dbName).map(apply).toVector.sortBy { _("name").getString }
	/**
	  * Reads a table and converts it to an input model template
	  * @param dbName Name of the targeted database
	  * @param tableName Name of the targeted table
	  * @return An input model template based on that table
	  */
	def apply(dbName: String, tableName: String): Model = apply(tables(dbName, tableName))
	/**
	  * @param table A table read from the database
	  * @return An input model template for a class that is based on the specified table
	  */
	def apply(table: Table): Model = {
		val name = Name(table.name)
		val prefixIsUsed = table.columns.forall { _.columnName.contains('_') } &&
			table.columns.exists { _.columnName.exists { _.isUpper } }
		
		val idCol = table.primaryColumn
		
		Model.from(
			"name" -> name.singularIn(CamelCase.capitalized),
			"table" -> table.name,
			"id" -> idCol.map { c => nameFrom(c.columnName, prefixIsUsed).singularIn(CamelCase.lower) }
				.filterNot { _ == "id" },
			"use_long_id" -> (if (idCol.exists { _.dataType == LongType }) true else Value.empty),
			"props" -> table.columns.filterNot { _.isPrimary }
				.map { c => apply(table.databaseName, c, References.from(table, c), prefixIsUsed) }
		).withoutEmptyValues
	}
	
	// Primary key should not be included here
	private def apply(databaseName: String, col: Column, reference: Option[ReferencePoint],
	                  isPrefixed: Boolean) =
	{
		val colName = nameFrom(col.columnName, isPrefixed)
		val lengthRule = Some(ColumnLengthRules(databaseName, col)).filterNot { _ == ColumnLengthRules.default }
		val ref = reference.map { ref =>
			val colName = nameFrom(ref.column.columnName, isPrefixed).singular
			if (colName == "id")
				ref.table.name
			else
				s"${ ref.table.name }($colName)"
		}
		
		Model.from(
			"name" -> colName.singularIn(CamelCase.lower),
			"column" -> colName.singular,
			"ref" -> ref,
			"type" -> typeFrom(col, ColumnLengthLimits(databaseName, col).map { _.maxValue }),
			"limit" -> lengthRule.map { _.toString },
			"default" -> col.defaultValue
		).withoutEmptyValues
	}
	
	private def nameFrom(possiblyPrefixedName: String, isPrefixed: Boolean) =
		Name(if (isPrefixed && possiblyPrefixedName.contains('_'))
			possiblyPrefixedName.afterLast("_") else possiblyPrefixedName)
	
	private def typeFrom(col: Column, limit: => Option[Long]) = {
		def limitStr(default: Int) = limit.filterNot { _ == default } match {
			case Some(max) => s"($max)"
			case None => ""
		}
		def opt(innerType: String) = if (col.allowsNull) s"Option[$innerType]" else innerType
		
		col.dataType match {
			case IntType =>
				val lowerName = col.columnName.toLowerCase
				if (lowerName.contains("mins") || lowerName.contains("minute"))
					opt("Duration[minutes]")
				else if (lowerName.contains("sec"))
					opt("Duration[seconds]")
				else if (lowerName.contains("hour"))
					opt("Duration[hours]")
				else if (lowerName.contains("days"))
					opt("Days")
				else
					opt(s"Int${limitStr(2147483647)}")
			case LongType => opt("Long")
			case StringType =>
				val l = limitStr(255)
				if (col.allowsNull) s"String$l" else s"NonEmptyString$l"
			case DoubleType | FloatType => opt("Double")
			case BooleanType => opt("Boolean")
			case InstantType =>
				val lowerName = col.columnName.toLowerCase
				if (col.allowsNull) {
					if (lowerName.contains("deprec") || lowerName.contains("delete") || lowerName.contains("remove"))
						"deprecation"
					else
						"Option[Instant]"
				}
				else if (lowerName.contains("creat"))
					"creation"
				else if (lowerName.contains("update"))
					"updated"
				else if (lowerName.contains("exp"))
					"expiration"
				else
					"Instant"
			case LocalDateType => "Date"
			case LocalTimeType => "Time"
			case other => other.name
		}
	}
}
