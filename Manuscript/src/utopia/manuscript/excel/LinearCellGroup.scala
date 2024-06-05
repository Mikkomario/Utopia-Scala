package utopia.manuscript.excel

import utopia.flow.collection.immutable.caching.iterable.CachingSeq
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Empty
import utopia.flow.generic.factory.PropertyFactory.ConstantFactory
import utopia.flow.generic.model.immutable.{Constant, Model, Value}
import utopia.flow.operator.equality.EqualsExtensions._
import utopia.flow.view.immutable.caching.Lazy
import utopia.paradigm.enumeration.Axis2D
import utopia.paradigm.shape.template.Dimensions

import scala.collection.immutable.VectorBuilder

/**
  * Represents a row or a column in a spread-sheet
  * @author Mikko Hilpinen
  * @since 31/01/2024, v1.0
  */
class LinearCellGroup(val cells: CachingSeq[Cell], val index: Int, val direction: Axis2D)
{
	// COMPUTED --------------------------
	
	/**
	  * @return A set of headers containing this line's values
	  */
	def toHeaders = Headers(cells.map { c => c.value.getString -> c.index(direction) }.toMap)
	
	
	// OTHER    --------------------------
	
	/**
	  * @param cellIndex Index of the targeted cell
	  * @return Value associated with that cell.
	  *         Empty value if the specified index was out of range.
	  */
	def apply(cellIndex: Int) = findCellAt(cellIndex) match {
		case Some(cell) => cell.value
		case None => Value.empty
	}
	/**
	  * Checks whether the specified cell contains a non-empty value
	  * @param cellIndex Targeted cell index
	  * @return Whether the targeted cell contains a non-empty value
	  */
	def containsIndex(cellIndex: Int) = findCellAt(cellIndex).exists { _.value.nonEmpty }
	
	/**
	  * @param index Targeted cell index
	  * @return Cell at that index in this sequence
	  */
	def cellAt(index: Int) = findCellAt(index)
		.getOrElse { new Cell(Dimensions.int(index, this.index, direction), Lazy.initialized(Value.empty)) }
	/**
	  * @param index Index of the targeted cell
	  * @return A cell at that index. None if no cell was specified for that index.
	  */
	def findCellAt(index: Int) = cells
		.findMap { c =>
			// Matches against the cell's index, which doesn't necessarily match its index within the sequence
			// (because empty cells are not included in that sequence)
			val i = c.index(direction)
			if (i == index)
				Some(Some(c))
			else if (i > index)
				Some(None)
			else
				None
		}.flatten
	
	/**
	  * Converts this group of cells into a model
	  * @param headers Headers which provide mapping between cell indices and header names
	  * @param preLoad Whether all cell values should be read immediately.
	  *                If false (default), the resulting model may only be used while the cells are available
	  *                (i.e. while the spread-sheet is open).
	  *                Set to true if you want to store or use the models after the spread-sheet closes.
	  * @return A model based on this row or column
	  */
	def toModel(headers: Headers, preLoad: Boolean = false) = {
		if (preLoad)
			Model.withConstants(headers.keyToIndex.map { case (key, index) => Constant(key, apply(index)) })
		else
			Model(Empty, new CellsToPropsFactory(headers))
	}
	
	/**
	  * @param headers A set of headers
	  * @return Whether all of those headers can be found from this line (as cell values)
	  */
	def listsHeaders(headers: UnallocatedHeaders) = headers.areFoundFrom(cells.map { _.value.getString })
	/**
	  * @param headers A set of already located headers
	  * @return Whether this line matches those headers (i.e. lists all the same keys in some form at the same indices)
	  */
	def matchesHeaders(headers: Headers) = headers.keyToIndex.forall { case (header, index) =>
		apply(index).string.forall { value =>
			(value ~== header) || headers.alternativeKeys.get(value.toLowerCase).exists { _ ~== header }
		}
	}
	
	/**
	  * Searches for the specified headers on this line and locates their cell-indices.
	  * Unlike [[completeHeaders()]], this function only reads / matches the specified headers.
	  * @param headers Headers to include in the result.
	  *                Expects a complete set of used header names.
	  * @return Headers where each of the specified name matches a cell index.
	  *         None if this line didn't contain all of the specified headers.
	  */
	def locateHeaders(headers: UnallocatedHeaders) = {
		val builder = new VectorBuilder[(String, Int)]()
		val potentialHeaders = cells.map { c => c.value.getString -> c.index(direction) }
		// Each key must be found from the specified headers. Otherwise the process terminates.
		val isApplicable = headers.headers.forall { case (header, altForms) =>
			potentialHeaders.find { case (key, _) => (key ~== header) || altForms.exists { _ ~== key } } match {
				case Some((_, index)) =>
					builder += (header -> index)
					true
				case None => false
			}
		}
		if (isApplicable) Some(Headers(builder.result().toMap, headers.secondaryToPrimary)) else None
	}
	/**
	  * Completes, if possible, a partial set of headers to match the cell values in this row
	  * @param partialHeaders Headers that must appear on this line in order to form the complete headers
	  * @return Complete set of headers from this line.
	  *         None if this line didn't contain all of the specified headers.
	  */
	def completeHeaders(partialHeaders: UnallocatedHeaders) = {
		val potentialHeaders = cells.map { c => c.value.getString -> c.index(direction) }
		val listedValues = potentialHeaders.map { _._1 }
		
		// Case: This is the header row => Forms the complete headers
		if (partialHeaders.areFoundFrom(listedValues)) {
			// Finds the primary form and possible alternatives for each listed key
			val (headers, alternatives) = potentialHeaders.splitMap { case (key, index) =>
				val (header, alternatives) = partialHeaders(key)
				(header -> index) -> alternatives.map { _.toLowerCase -> header }
			}
			Some(Headers(headers.toMap, alternatives.flatten.toMap))
		}
		// Case: This is not the header row
		else
			None
	}
	
	
	// NESTED   --------------------------
	
	private class CellsToPropsFactory(headers: Headers) extends ConstantFactory
	{
		override def apply(propertyName: String, value: Value) = {
			val value = headers.lift(propertyName) match {
				case Some(index) => LinearCellGroup.this(index)
				case None => Value.empty
			}
			Constant(propertyName, value)
		}
		
		override def generatesNonEmpty(propertyName: String): Boolean = headers.lift(propertyName).exists(containsIndex)
	}
}
