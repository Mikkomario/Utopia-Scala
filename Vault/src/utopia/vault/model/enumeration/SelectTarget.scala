package utopia.vault.model.enumeration

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{Pair, Single}
import utopia.vault.model.immutable.{Column, Table}
import utopia.vault.sql.{Select, SqlSegment, SqlTarget}

import scala.language.implicitConversions

/**
  * An enumeration for different styles of selections made within select statements
  * @author Mikko Hilpinen
  * @since 09.08.2024, v1.20
  */
sealed trait SelectTarget
{
	// ABSTRACT --------------------------
	
	/**
	  * @param target Targeted tables, etc.
	  * @return A select statement selecting this target from the specified target
	  */
	def toSelect(target: SqlTarget): SqlSegment
	
	/**
	  * @param other Another target to include, also
	  * @return Copy of this target, also including the specified target
	  */
	def +(other: SelectTarget): SelectTarget
}

object SelectTarget
{
	// IMPLICIT --------------------------
	
	implicit def column(column: Column): SelectTarget = SingleColumn(column)
	implicit def columns(columns: Seq[Column]): SelectTarget = Columns(columns)
	
	implicit def table(table: Table): SelectTarget = SingleTable(table)
	implicit def tables(tables: Seq[Table]): SelectTarget = Tables(tables)
	
	
	// NESTED   --------------------------
	
	sealed trait SelectColumnsTarget extends SelectTarget
	{
		// ABSTRACT ----------------------
		
		/**
		  * @return Columns included in this target
		  */
		def columns: Seq[Column]
		
		
		// IMPLEMENTED  ------------------
		
		override def +(other: SelectTarget): SelectTarget = other match {
			case All => All
			case Nothing => this
			case cols: SelectColumnsTarget => this ++ cols.columns
		}
		
		
		// OTHER    ----------------------
		
		/**
		  * @param column Column to include in this target
		  * @return Copy of this target with the specified column included
		  */
		def +(column: Column) = {
			val myColumns = this.columns
			if (myColumns.contains(column))
				this
			else
				Columns(myColumns :+ column)
		}
		
		/**
		  * @param columns Other columns to include in this target
		  * @return Copy of this target with the specified columns included
		  */
		def ++(columns: IterableOnce[Column]) = {
			val colCount = columns.knownSize
			if (colCount == 0)
				this
			else if (colCount == 1)
				this + columns.iterator.next()
			else {
				val baseIter = columns.iterator
				if (baseIter.hasNext) {
					val myColumns = this.columns
					val myColSet = myColumns.toSet
					val iter = baseIter.filterNot(myColSet.contains)
					if (iter.hasNext)
						Columns(myColumns ++ iter)
					else
						this
				}
				else
					this
			}
		}
	}
	
	sealed trait SelectTablesTarget extends SelectColumnsTarget
	{
		// ABSTRACT --------------------------
		
		/**
		  * @return Tables which are fully included in the selected results
		  */
		def tables: Seq[Table]
		
		
		// IMPLEMENTED  ----------------------
		
		override def columns: Seq[Column] = tables.flatMap { _.columns }
		
		override def +(other: SelectTarget) = other match {
			case other: SelectTablesTarget =>
				val myTables = tables
				val myTableSet = myTables.toSet
				val newTables = myTables ++ other.tables.view.filterNot(myTableSet.contains)
				
				if (newTables.hasSize.of(myTables))
					this
				else
					Tables(newTables)
				
			case other => super.+(other)
		}
	}
	
	
	// VALUES   --------------------------
	
	/**
	  * Used for selecting all tables & columns within the target
	  */
	case object All extends SelectTarget
	{
		override def toSelect(target: SqlTarget): SqlSegment = Select.all(target)
		
		override def +(other: SelectTarget): SelectTarget = this
	}
	
	/**
	  * Used for not selecting anything
	  */
	case object Nothing extends SelectTarget
	{
		override def toSelect(target: SqlTarget): SqlSegment = Select.nothing(target)
		
		override def +(other: SelectTarget): SelectTarget = other
	}
	
	/**
	  * Used for selecting values of a single column
	  * @param column Selected column
	  */
	case class SingleColumn(column: Column) extends SelectColumnsTarget
	{
		override def columns: Seq[Column] = Single(column)
		
		override def toSelect(target: SqlTarget): SqlSegment = Select(target, column)
	}
	
	/**
	  * Used for selecting distinct values of a single column
	  * @param column Selected column
	  */
	case class DistinctColumnValues(column: Column) extends SelectColumnsTarget
	{
		override def columns: Seq[Column] = Single(column)
		
		override def toSelect(target: SqlTarget): SqlSegment = Select.distinct(target, column)
	}
	
	object Columns
	{
		def apply(firstColumn: Column, secondColumn: Column, moreColumns: Column*): Columns =
			apply(Pair(firstColumn, secondColumn) ++ moreColumns)
	}
	/**
	  * Used for selecting 0-n columns
	  * @param columns Selected columns
	  */
	case class Columns(columns: Seq[Column]) extends SelectColumnsTarget
	{
		override def toSelect(target: SqlTarget): SqlSegment = Select(target, columns)
	}
	
	/**
	  * Used for selecting columns from a single table
	  * @param table Targeted table
	  */
	case class SingleTable(table: Table) extends SelectTablesTarget
	{
		override def tables: Seq[Table] = Single(table)
		
		override def toSelect(target: SqlTarget): SqlSegment = {
			if (target.tables.forall { _ == table })
				Select.all(target)
			else
				Select(target, table.columns)
		}
	}
	
	object Tables
	{
		def apply(firstTable: Table, secondTable: Table, moreTables: Table*): Tables =
			apply(Pair(firstTable, secondTable) ++ moreTables)
	}
	/**
	  * Used for selecting columns from 0-n tables
	  * @param tables Targeted tables
	  */
	case class Tables(tables: Seq[Table]) extends SelectTablesTarget
	{
		override def toSelect(target: SqlTarget): SqlSegment = {
			Select.tables(target, tables)
		}
	}
}

