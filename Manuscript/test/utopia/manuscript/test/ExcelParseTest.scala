package utopia.manuscript.test

import utopia.flow.generic.model.immutable.ModelDeclaration
import utopia.flow.generic.model.mutable.DataType.{IntType, StringType}
import utopia.flow.parse.file.FileExtensions._
import utopia.manuscript.excel.{Excel, UnallocatedHeaders}
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
	private val schema = ModelDeclaration("index" -> IntType, "key" -> StringType)
	
	Excel.open("Manuscript/data/test-material/test.xlsx") { excel =>
		val sheet = excel(FirstSheet).get
		val models = sheet.modelsIteratorCompletingHeaders(UnallocatedHeaders("key") + ("val" -> Set("value"))).toVector
		
		assert(models.size == 5)
		
		val m0 = models.head
		assert(m0("index").getInt == 1)
		assert(m0("key").getString == "Name")
		assert(m0("val").getString == "Test")
		
		val m1 = models(1)
		assert(m1("index").getInt == 2)
		assert(m1("key").getString == "Width")
		assert(m1("Val").getInt == 10)
		
		val m2 = schema.validate(models(2)).get
		assert(m2("index").getInt == 3)
		assert(m2("key").getString == "Height", m2("key").getString)
		assert(m2("val").getDouble == 8.3)
		
		val m3 = models(3)
		assert(m3("Val").getLocalDate == LocalDate.of(2024, 1, 31))
		
		val m4 = models(4)
		println(m4("value").description)
		
	}.get
	
	println("Done!")
}
