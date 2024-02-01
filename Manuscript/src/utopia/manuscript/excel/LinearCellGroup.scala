package utopia.manuscript.excel

import utopia.flow.collection.immutable.caching.iterable.CachingSeq
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.generic.factory.PropertyFactory.ConstantFactory
import utopia.flow.generic.model.immutable.{Constant, Model, Value}
import utopia.flow.operator.equality.EqualsExtensions._
import utopia.paradigm.enumeration.Axis2D

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
	def apply(cellIndex: Int) = cells
		.findMap { c =>
			// Matches against the cell's index, which doesn't necessarily match its index within the sequence
			// (because empty cells are not included in that sequence)
			val i = c.index(direction)
			if (i == cellIndex)
				Some(c.value)
			else if (i > cellIndex)
				Some(Value.empty)
			else
				None
		}
		.getOrElse(Value.empty)
	
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
			Model(Vector.empty, new CellsToPropsFactory(headers))
	}
	
	/**
	  * @param headers A set of indexed headers
	  * @return Whether this line specifies the header names at those locations
	  */
	def matchesHeaders(headers: Headers) = headers.keyToIndex
		.forall { case (header, index) => apply(index).getString ~== header }
	/**
	  * @param headerNames A set of header names
	  * @return Whether all of those header names can be found from this line (as cell values)
	  */
	def listsHeaders(headerNames: Iterable[String]) = {
		val listedNames = cells.map { _.value.getString }
		headerNames.forall { header => listedNames.exists { _ ~== header } }
	}
	/**
	  * Searches for the specified headers on this line and locates their cell-indices.
	  * Unlike [[completeHeaders()]], this function only reads / matches the specified headers.
	  * @param headerNames Names of the headers to include in the result.
	  *                    Expects a complete set of used header names.
	  * @return Headers where each of the specified name matches a cell index.
	  *         None if this line didn't contain all of the specified headers.
	  */
	def locateHeaders(headerNames: Iterable[String]) = {
		val builder = new VectorBuilder[(String, Int)]()
		val potentialHeaders = cells.map { c => c.value.getString -> c.index(direction) }
		val isApplicable = headerNames.forall { header =>
			potentialHeaders.find { _._1 ~== header } match {
				case Some((_, index)) =>
					builder += (header -> index)
					true
				case None => false
			}
		}
		if (isApplicable) Some(Headers(builder.result().toMap)) else None
	}
	/**
	  * Completes, if possible, a partial set of headers to match the cell values in this row
	  * @param partialHeaders Headers that must appear on this line in order to form the complete headers
	  * @return Complete set of headers from this line.
	  *         None if this line didn't contain all of the specified headers.
	  */
	// TODO: Consider adding support for PropertyDeclaration in order to support alternative header names
	def completeHeaders(partialHeaders: Iterable[String]) = {
		val potentialHeaders = cells.map { c => c.value.getString -> c.index(direction) }
		// All specified headers must be found
		val isComplete = partialHeaders.forall { header => potentialHeaders.exists { _._1 ~== header } }
		// If the specified headers were found, completes the other headers
		if (isComplete)
			Some(Headers(potentialHeaders.toMap))
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
	}
}
