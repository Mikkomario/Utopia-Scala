package utopia.vault.nosql.read.parse

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{Empty, Single}
import utopia.vault.error.HandleError
import utopia.vault.model.immutable.{Column, Row}
import utopia.vault.model.mutable.ResultStream
import utopia.vault.model.template.HasTable

import scala.collection.View
import scala.util.{Failure, Success, Try}

/**
  * An interface for result parsers which handle rows grouped by a primary table's index
  * @author Mikko Hilpinen
  * @since 27.07.2025, v1.22
  */
trait ParseGroupedRows[+A] extends ParseRows[Seq[A]] with HasTable
{
	// ABSTRACT -------------------------
	
	/**
	  * Parses a grouped set of rows, where all rows represent the same item
	  * @param rows Rows to parse
	  * @return Item parsed from the specified rows. Failure if parsing failed.
	  */
	def parseGroup(rows: Seq[Row]): Try[A]
	
	
	// IMPLEMENTED  ---------------------
	
	override def apply(rows: Seq[Row]): Seq[A] = table.primaryColumn match {
		case Some(index) => apply(rows, index)
		// Case: No primary column to group by (unexpected) => Attempts to parse all rows at once
		case None => parseWithoutIndex(rows)
	}
	override def apply(stream: ResultStream): IndexedSeq[A] = table.primaryColumn match {
		case Some(index) => apply(stream.rowsIterator, index)
		case None => parseWithoutIndex(stream.rowsIterator.toOptimizedSeq)
	}
	
	
	// OTHER    ------------------------
	
	/**
	  * @param rows Rows to parse
	  * @return Items parsed from the specified rows
	  */
	def apply(rows: IterableOnce[Row]): IndexedSeq[A] = table.primaryColumn match {
		case Some(index) =>
			// Groups the rows by the primary key, then parses the results
			// For buffered collections, uses groupBy
			rows match {
				case rowsView: View[Row] => apply(rowsView.iterator, index)
				case rows: Seq[Row] => apply(rows, index)
				case rows: Iterable[Row] =>
					rows.groupBy { _(index) }.valuesIterator.flatMap { rows => tryParseGroup(rows.toOptimizedSeq) }
						.toOptimizedSeq
				case rows => apply(rows.iterator, index)
			}
		
		// Case: The primary target's table doesn't contain a primary key (unexpected) => Can't perform grouping
		case None => parseWithoutIndex(rows.toOptimizedSeq)
	}
	
	/**
	  * @param rows Rows to parse. Already grouped to represent the same item.
	  * @return Item parsed from the specified rows. None if no item could be parsed.
	  */
	def tryParseGroup(rows: Seq[Row]) = {
		// Parses the row group. Delegates failures to the common "error handler"
		parseGroup(rows) match {
			case Success(item) => Some(item)
			case Failure(error) =>
				HandleError.duringRowParsing(error)
				None
		}
	}
	
	private def apply(rows: Seq[Row], index: Column) =
		rows.groupBy { _(index) }.valuesIterator.flatMap(tryParseGroup).toOptimizedSeq
	private def apply(rowsIter: Iterator[Row], index: Column) =
		rowsIter.groupBy { _(index) }.flatMap { case (_, rows) => tryParseGroup(rows) }.toOptimizedSeq
	
	/**
	  * Attempts to parse rows without grouping.
	  * Should only be used as a backup when [[table]] is not correctly set up.
	  * @param rows Rows to parse
	  * @return Parsed rows
	  */
	private def parseWithoutIndex(rows: Seq[Row]) = parseGroup(rows) match {
		case Success(item) => Single(item)
		case Failure(error) =>
			HandleError.duringRowParsing(error)
			Empty
	}
}
