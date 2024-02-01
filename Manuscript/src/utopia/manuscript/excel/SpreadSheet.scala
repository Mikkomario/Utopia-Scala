package utopia.manuscript.excel

import org.apache.poi.ss.usermodel.{CellType, DateUtil, Sheet}
import utopia.flow.collection.immutable.caching.iterable.CachingSeq
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.generic.model.immutable.{Model, Value}
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.operator.ScopeUsable
import utopia.flow.view.immutable.caching.Lazy
import utopia.paradigm.enumeration.Axis.X
import utopia.paradigm.shape.template.Dimensions

import java.time.LocalTime
import scala.annotation.tailrec
import scala.jdk.CollectionConverters._

/**
  * An interface used for interacting with a specific spread-sheet.
  * This interface should be used only while the associated document is open.
  * @author Mikko Hilpinen
  * @since 31/01/2024, v1.0
  */
class SpreadSheet(sheet: Sheet) extends ScopeUsable[SpreadSheet]
{
	// ATTRIBUTES   -----------------------
	
	// Caches the rows (optional feature)
	private val lazyRows = Lazy { CachingSeq.from(_rowsIterator) }
	
	
	// COMPUTED ---------------------------
	
	/**
	  * @return All rows within this spread-sheet
	  * @see [[rowsIterator]]
	  */
	def rows = lazyRows.value
	/**
	  * @return An iterator that lists all (non-empty) rows in this spread-sheet from top to bottom
	  */
	def rowsIterator = lazyRows.current match {
		case Some(rows) => rows.iterator
		case None => _rowsIterator
	}
	
	private def _rowsIterator = sheet.rowIterator().asScala.map { row =>
		val rowIndex = row.getRowNum
		val cellsIterator = row.cellIterator().asScala.map { cell =>
			val lazyValue = Lazy[Value] {
				cell.getCellType match {
					case CellType.BOOLEAN => cell.getBooleanCellValue
					case CellType.FORMULA => cell.getCellFormula
					case CellType.NUMERIC =>
						// Number may also represent a date (with or without time)
						if (DateUtil.isCellDateFormatted(cell)) {
							val date = cell.getLocalDateTimeCellValue
							if (date.toLocalTime.equals(LocalTime.MIDNIGHT))
								date.toLocalDate
							else
								date
						}
						else
							cell.getNumericCellValue
					case CellType.STRING => cell.getStringCellValue.trim
					case _ => Value.empty
				}
			}
			new Cell(Dimensions.int(cell.getColumnIndex, rowIndex), lazyValue)
		}
		new LinearCellGroup(CachingSeq.from(cellsIterator), rowIndex, X)
	}
	
	
	// IMPLEMENTED  ------------------
	
	override def self: SpreadSheet = this
	
	
	// OTHER    ----------------------
	
	/**
	  * Iterates the rows in this spread-sheet as converted to models
	  * @param headers Headers that specify, which model property name appears in which column (using 0-based indexing)
	  * @param locateInDocument Whether a header row is expected to be found within this spread-sheet.
	  *                         If set to true, all rows until and including the header row will be skipped.
	  *                         Default = false, which causes all rows to be read / processed.
	  * @param preLoadModels Whether all cell values should be read immediately.
	  *                If false (default), the resulting model may only be used while the cells are available
	  *                (i.e. while the spread-sheet is open).
	  *                Set to true if you want to store or use the models after the spread-sheet closes.
	  * @return An iterator that yields models, each parsed from a row by mapping its values to the specified headers
	  */
	@tailrec
	final def modelsIteratorUsingHeaders(headers: Headers, locateInDocument: Boolean = false,
	                                     preLoadModels: Boolean = false): Iterator[Model] =
	{
		val iter = rowsIterator
		// Case: It is expected that the headers are present in this document =>
		// Finds the row that lists the specified headers and only starts reading after that
		if (locateInDocument)
			iter.find { _.matchesHeaders(headers) } match {
				case Some(_) => iter.map { _.toModel(headers, preLoadModels) }
				// Case: Headers were not found => Reads the whole document
				case None => modelsIteratorUsingHeaders(headers, preLoadModels = preLoadModels)
			}
		// Case: Using external headers => Reads the whole document
		else
			iter.map { _.toModel(headers, preLoadModels) }
	}
	/**
	  * Iterates the rows in this spread-sheet as converted to models.
	  * @param headers Headers, which are used as model property names.
	  *                Expects a full list. I.e. other headers won't be read / accessible.
	  * @param preLoadModels Whether all cell values should be read immediately.
	  *                If false (default), the resulting model may only be used while the cells are available
	  *                (i.e. while the spread-sheet is open).
	  *                Set to true if you want to store or use the models after the spread-sheet closes.
	  * @return An iterator that yields models, each parsed from a row by mapping its values to the specified headers.
	  *         Rows before and including the actual header row are not included in this iterator.
	  * @see [[modelsIteratorCompletingHeaders]]
	  */
	def modelsIteratorLocatingHeaders(headers: UnallocatedHeaders, preLoadModels: Boolean = false) =
		_modelsIterator(preLoadModels) { _.locateHeaders(headers) }
	/**
	  * Iterates the rows in this spread-sheet as converted to models.
	  * @param partialHeaders Headers which must appear on the header row.
	  *                       Other values listed on that row will be converted to headers, also.
	  *                       (I.e. this list doesn't need to include all the used headers,
	  *                       just the ones used for locating the header row).
	  *
	  *                       If the specified list contains all used headers, please use
	  *                       [[modelsIteratorLocatingHeaders]] instead.
	  *
	  * @param preLoadModels Whether all cell values should be read immediately.
	  *                If false (default), the resulting model may only be used while the cells are available
	  *                (i.e. while the spread-sheet is open).
	  *                Set to true if you want to store or use the models after the spread-sheet closes.
	  * @return An iterator that yields models, each parsed from a row by mapping its values to the specified headers.
	  *         Rows before and including the actual header row are not included in this iterator.
	  */
	def modelsIteratorCompletingHeaders(partialHeaders: UnallocatedHeaders, preLoadModels: Boolean = false) =
		_modelsIterator(preLoadModels) { _.completeHeaders(partialHeaders) }
	
	private def _modelsIterator(preLoadModels: Boolean = false)(rowToHeaders: LinearCellGroup => Option[Headers]) =
	{
		val iter = rowsIterator
		// Finds the header row
		iter.findMapNext(rowToHeaders) match {
			// Case: Header row found => Converts the remaining rows into models
			case Some(headers) => iter.map { _.toModel(headers, preLoadModels) }
			// Case: No header row found
			case None => Iterator.empty
		}
	}
}
