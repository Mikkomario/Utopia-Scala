package utopia.manuscript.excel

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.caching.iterable.CachingSeq
import utopia.flow.generic.factory.PropertyFactory.ConstantFactory
import utopia.flow.generic.model.immutable.{Constant, Model, Value}
import utopia.flow.operator.equality.EqualsExtensions._
import utopia.flow.util.UncertainBoolean
import utopia.flow.view.immutable.caching.Lazy
import utopia.paradigm.enumeration.Axis2D
import utopia.paradigm.shape.template.Dimensions

import scala.collection.immutable.VectorBuilder
import scala.collection.mutable

/**
  * Represents a row or a column in a spreadsheet
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
		}
		.flatten
	
	/**
	  * Converts this group of cells into a model
	  * @param headers Headers which provide mapping between cell indices and header names
	  * @param preLoad Whether all cell values should be read immediately.
	  *                If false (default), the resulting model may only be used while the cells are available
	  *                (i.e. while the spreadsheet is open).
	  *                Set to true if you want to store or use the models after the spreadsheet closes.
	  * @return A model based on this row or column
	  */
	def toModel(headers: Headers, preLoad: Boolean = false) = {
		if (preLoad)
			new BufferedRowModel(
				headers.primaryKeyToIndex.map { case (key, index) => index -> Constant(key, apply(index)) }, headers)
		else
			new LazyRowModel(headers)
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
	def matchesHeaders(headers: Headers) = headers.primaryKeyToIndex.forall { case (header, index) =>
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
	  *         None if this line didn't contain all the specified headers.
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
	  *         None if this line didn't contain all the specified headers.
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
	
	private class BufferedRowModel(props: Map[Int, Constant], headers: Headers) extends Model
	{
		// ATTRIBUTES   ---------------------
		
		override lazy val properties: Seq[Constant] = props.toOptimizedSeq.sortBy { _._1 }.map { _._2 }
		
		
		// IMPLEMENTED  ---------------------
		
		override def self: Model = this
		
		override def propertiesIterator: Iterator[Constant] = props.valuesIterator
		
		override def knownContains(propName: String): UncertainBoolean = contains(propName)
		override def contains(propName: String): Boolean = headers.contains(propName)
		override def containsNonEmpty(propName: String): Boolean = existingProperty(propName).exists { _.nonEmpty }
		
		override def existingProperty(propName: String): Option[Constant] = headers.lift(propName).map(props.apply)
	}
	
	/**
	 * A model that lazily maps headers to cell values
	 * @param headers Headers to map
	 */
	private class LazyRowModel(headers: Headers) extends Model
	{
		// ATTRIBUTES   ---------------------
		
		/**
		 * Lazily iterates over the cells, attaching them to matching headers.
		 * Also stores the cell index, which is used in map-accessing.
		 */
		private val indexedProps = cells.flatMap { cell =>
			val index = cell.index(direction)
			headers.lift(index).map { name => index -> Constant.lazily(name, cell.lazyValue) }
		}
		override val properties: Seq[Constant] = indexedProps.map { _._2 }
		
		/**
		 * An iterator from which cell properties are mapped to their respective indices, when necessary
		 */
		private val toMapIterator = indexedProps.iterator
		/**
		 * A mutable map containing all collected cell-index -> cell property -links
		 */
		private val indexToProp = mutable.Map[Int, Constant]()
		
		
		// IMPLEMENTED  ---------------------
		
		override def self: Model = this
		
		override def propertiesIterator: Iterator[Constant] = properties.iterator
		
		// Implements containment through the headers
		override def knownContains(propName: String): UncertainBoolean = contains(propName)
		override def contains(propName: String): Boolean = headers.contains(propName)
		override def containsNonEmpty(propName: String): Boolean = existingProperty(propName).exists { _.nonEmpty }
		
		override def existingProperty(propName: String): Option[Constant] = headers.lift(propName).map(apply)
		
		
		// OTHER    ------------------------
		
		/**
		 * Acquires a property for an index. Caches the result.
		 * @param index Targeted cell index. Must be present in [[headers]].
		 * @return A cell property matching that index.
		 */
		private def apply(index: Int) = indexToProp.getOrElse(index, {
			// If not cached, looks for a property with the matching index from the lazy iterator
			// Case: More items to iterate => Iterates further
			if (toMapIterator.hasNext) {
				var result: Option[Option[Constant]] = None
				do {
					// Moves to the next cell
					val (nextIndex, nextProp) = toMapIterator.next()
					// Caches the generated propert
					indexToProp += (nextIndex -> nextProp)
					
					// Case: Index matches => Yields this property
					if (nextIndex == index)
						result = Some(Some(nextProp))
					// Case: Already passed the targeted index => Won't iterate further
					else if (nextIndex > index)
						result = None
				}
				while (result.isEmpty && toMapIterator.hasNext)
				
				result.flatten.getOrElse { newEmptyProp(index) }
			}
			// Case: All cells already iterated => Generates an empty property
			else
				newEmptyProp(index)
		})
		
		private def newEmptyProp(index: Int) = {
			val prop = Constant(headers(index), Value.empty)
			indexToProp += (index -> prop)
			prop
		}
	}
}
