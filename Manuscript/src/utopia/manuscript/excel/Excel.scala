package utopia.manuscript.excel

import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.openxml4j.opc.{OPCPackage, PackageAccess}
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.parse.AutoClose._
import utopia.flow.parse.file.FileExtensions._

import java.io.{FileInputStream, FileNotFoundException, IOException}
import java.nio.file.Path
import scala.util.{Failure, Success, Try}

object Excel
{
	// ATTRIBUTES   ----------------------
	
	/**
	  * File extensions that can be read by this object (in lower case)
	  */
	val supportedExtensions = Vector("xlsx", "xls")
	
	
	// OTHER    -------------------------
	
	/**
	  * Reads an .xls or .xlsx file and presents it to the specified function
	  * @param path Path to read
	  * @param f Function that will process the opened document.
	  *          NB: The specified Excel instance will become unusable (i.e. closed) after the function call ends.
	  * @tparam A Function result type
	  * @return Function result value or a failure if file-opening failed or if the function threw.
	  */
	def open[A](path: Path)(f: Excel => A) = {
		if (path.isRegularFile) {
			Try {
				// Uses either xls (HSSF) or xlsx parsing
				path.fileType.toLowerCase match {
					// Case: Xls
					case "xls" =>
						new FileInputStream(path.toFile)
							.consume { stream => new Excel(new HSSFWorkbook(stream)).consume(f) }
					// Case: Xlsx
					case "xlsx" =>
						Option(OPCPackage.open(path.toFile, PackageAccess.READ))
							.toTry {  new IOException(s"Couldn't open excel file from $path") }.get
							.consume { pkg => new Excel(new XSSFWorkbook(pkg)).consume(f) }
				}
			}
		}
		// Case: Attempting to open a directory or a non-existing file
		else
			Failure(new FileNotFoundException(s"There is no existing regular file at $path"))
	}
}

/**
  * An interface for interacting with Excel / workbook documents
  * @author Mikko Hilpinen
  * @since 31/01/2024, v1.0
  */
class Excel(workBook: Workbook) extends AutoCloseable
{
	// ATTRIBUTES   ------------------------
	
	private val numberOfSheets = workBook.getNumberOfSheets
	private val sheetNames = (0 until numberOfSheets).toVector.lazyMap(workBook.getSheetName)
	
	
	// IMPLEMENTED  -----------------------
	
	override def close() = workBook.close()
	
	
	// OTHER    ---------------------------
	
	/**
	  * Targets a specific sheet within this document
	  * @param target A target used for locating the targeted sheet
	  * @return Targeted spread-sheet.
	  *         Failure if no spread-sheet was identified.
	  */
	def apply(target: SheetTarget) = target(sheetNames).flatMap { index =>
		Try {
			Option(workBook.getSheetAt(index)) match {
				case Some(sheet) => Success(new SpreadSheet(sheet))
				case None => Failure(new IllegalArgumentException(s"Specified sheet index $index was invalid"))
			}
		}.flatten
	}
}
