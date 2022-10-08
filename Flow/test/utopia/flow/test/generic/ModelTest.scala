package utopia.flow.test.generic

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.PropertyFactory
import utopia.flow.generic.model.mutable
import utopia.flow.generic.model.immutable.{Constant, Model}
import utopia.flow.generic.model.mutable.DataType
import utopia.flow.parse.json.JsonReader
import utopia.flow.util.StringExtensions._

/**
 *
 * @author Mikko Hilpinen
 * @since 5.4.2021, v
 */
object ModelTest extends App
{
	
	
	// Tests variable creation
	val generator2 = PropertyFactory.variableWithDefault(0)
	
	val generated = generator2("Test")
	
	// Tests models
	// 1) Model with no default value
	val model1 = mutable.MutableModel()
	model1("Test") = 2
	model1("Another") = "Hello"
	
	assert(model1.properties.size == 2)
	assert(model1("Test").content.get == 2)
	
	model1("another") = "Hello2"
	
	assert(model1.properties.size == 2)
	assert(model1("Another").content.get == "Hello2")
	
	assert(model1("something", "another").getString == "Hello2")
	assert(model1.properties.size == 3)
	
	// 2) model with default value
	val model2 = mutable.MutableModel.using(generator2)
	assert(model2.existing("Test").isEmpty)
	assert(model2("Test").content.get == 0)
	
	// 3) immutable model with no default value
	val constants = Vector(Constant("Test1", 1), Constant("Test2", 2))
	val model3 = Model.withConstants(constants)
	
	assert(model3 == Model.withConstants(constants))
	assert(model3.properties.size == 2)
	assert(model3("Test1").content.get == 1)
	
	// Tests model renaming too
	val renamed1 = model3.renamed(Vector("Test1" -> "Renamed", "non-existing" -> "still-not-existing"))
	
	assert(renamed1("Renamed") == model3("Test1"))
	assert(renamed1.properties.size == 2)
	assert(renamed1("Test2").getInt == 2)
	
	val mutableModel2 = mutable.MutableModel()
	mutableModel2("Test1") = 1
	mutableModel2("Test2") = 2
	assert(mutableModel2.immutableCopy == model3)
	
	val model4 = model3 + Constant("Test3", 3)
	
	assert(model4.properties.size == 3)
	assert(model4("Test3").content.get == 3)
	
	// 4) Immutable model with a default value
	val generator3 = PropertyFactory.constantWithDefault(0)
	val model5 = Model.withConstants(constants, generator3)
	
	assert(model5 != model3)
	assert(model5("nonexisting").content.get == 0)
	assert(model5.properties.size == constants.size)
	
	println(model5.toString())
	
	// Tests other model generator functions
	val model6 = mutable.MutableModel(Vector("a" -> 1, "b" -> 2))
	assert(model6.properties.size == 2)
	
	val model7 = Model(Vector("a" -> 1, "b" -> 2))
	assert(model7.properties.size == 2)
	
	// Tests immutable model filter
	val model3Filtered = model3.filter(_.value.intOr() == 2)
	assert(model3Filtered("Test1").isEmpty)
	assert(model3Filtered("Test2").intOr() == 2)
	
	val model3Filtered2 = model3.filterNot(_.value.intOr() == 2)
	assert(model3Filtered2("Test2").isEmpty)
	assert(model3Filtered2("Test1").intOr() == 1)
	
	val parsedModel = JsonReader.apply(
		"{\"CODE\": \"05601JZ\", \"MFR\": \"LANNING CHARLES A\", \"MODEL\": \"ROTORWAY EXEC\", \"TYPE-ACFT\": \"6\", \"TYPE-ENG\": \"1\", \"AC-CAT\": \"3\", \"BUILD-CERT-IND\": \"1\", \"NO-ENG\": \"1\", \"NO-SEATS\": \"2\", \"AC-WEIGHT\": \"CLASS 1\", \"SPEED\": \"0\"}")
		.get.getModel
	println(parsedModel)
	println(parsedModel("MFR").getString)
	parsedModel.propertyMap.keySet.toVector.sorted.foreach { k => println(s"'$k': [${k.getBytes.mkString("")}]") }
	println(s"Comparing to 'mfr': [${"mfr".getBytes.mkString("")}]")
	
	val attMap = parsedModel.propertyMap
	assert(attMap.keySet.forall(attMap.contains))
	assert(attMap.keySet.map { _.stripControlCharacters }.forall(attMap.contains))
	
	assert(parsedModel("MFR").getString == "LANNING CHARLES A")
	println("Success")
}
