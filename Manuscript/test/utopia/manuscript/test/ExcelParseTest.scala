package utopia.manuscript.test

import utopia.flow.parse.file.FileExtensions._
import utopia.manuscript.excel.Excel
import utopia.manuscript.excel.SheetTarget.FirstSheet
import utopia.paradigm.generic.ParadigmDataType

import java.time.LocalDate

/**
  * A basic test for excel-parsing
  * @author Mikko Hilpinen
  * @since 31/01/2024, v1.0
  */
object ExcelParseTest extends App
{
	ParadigmDataType.setup()
	
	Excel.open("Manuscript/data/test-material/test.xlsx") { excel =>
		val sheet = excel(FirstSheet).get
		val models = sheet.modelsIteratorCompletingHeaders(Vector("key")).toVector
		
		assert(models.size == 5)
		
		val m0 = models.head
		assert(m0("index").getInt == 1)
		assert(m0("key").getString == "Name")
		assert(m0("value").getString == "Test")
		
		val m1 = models(1)
		assert(m1("index").getInt == 2)
		assert(m1("key").getString == "Width")
		assert(m1("Value").getInt == 10)
		
		val m2 = models(2)
		assert(m2("index").getInt == 3)
		assert(m2("key").getString == "Height", m2("key").getString)
		assert(m2("value").getDouble == 8.3)
		
		val m3 = models(3)
		assert(m3("Value").getLocalDate == LocalDate.of(2024, 1, 31))
		
		val m4 = models(4)
		println(m4("value").description)
		
	}.get
	
	println("Done!")
}
