package utopia.flow.test.generic

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration, PropertyDeclaration}
import utopia.flow.generic.model.mutable.DataType.{BooleanType, IntType, StringType}
import utopia.flow.generic.model.mutable.MutableModel

/**
 *
 * @author Mikko Hilpinen
 * @since 5.4.2021, v
 */
object ModelDeclarationTest extends App
{
	// Tests property declarations
	val prop1 = PropertyDeclaration("test1", IntType)
	val prop2 = PropertyDeclaration.withDefault("test2", 0)
	
	// (not the usual use case but possible)
	val prop3 = PropertyDeclaration("test3", StringType, defaultValue = Some(3))
	
	assert(prop1 == PropertyDeclaration("test1", IntType))
	assert(prop1.defaultValue.isEmpty)
	assert(prop2.defaultValue.isDefined)
	
	// Tests model declaration
	val modelDec = ModelDeclaration(prop1, prop2, prop3)
	
	assert(modelDec.find("TEST1").isDefined)
	assert(modelDec.find("kkk").isEmpty)
	assert(modelDec.declarations.size == 3)
	
	val modelDec2 = modelDec + PropertyDeclaration("Test4", BooleanType)
	
	assert(modelDec2.declarations.size == 4)
	
	// Tests constant generation
	// 1) Generator with no default value
	val generator1 = modelDec2.toConstantFactory
	
	assert(generator1("test1").value.isEmpty)
	assert(generator1("test2").value.isDefined)
	// assert(generator1("test3").value.dataType == StringType, generator1("test3").value.description)
	assert(generator1("not here").value.isEmpty)
	
	// 2) Generator with a default value
	/*
	val generator2 = new DeclarationConstantGenerator(modelDec2, 0)
	
	assert(generator2("test1").value.isDefined)
	assert(generator2("test3").value.dataType == StringType)
	assert(generator2("test4").value.content.get == false)
	assert(generator2("something else").value.isDefined)
	 */
	
	// Quick test of variable generation
	val generator4 = modelDec2.toVariableFactory
	
	assert(generator4("test4", Some(1)).value.content.get == true)
	
	// Tests model validation
	val testModel1 = MutableModel(Vector("test1" -> 12, "test2" -> 5))
	val testModel2 = MutableModel(Vector("test2" -> 17, "test3" -> 11))
	val testModel3 = MutableModel(Vector("test1" -> 12))
	val testModel4 = MutableModel(Vector("test1" -> 12, "test4" -> "Hello"))
	val testModel5 = MutableModel(Vector("test1" -> "Hello"))
	val testModel6 = MutableModel(Vector("test1" -> 12, "test2" -> "Hello"))
	
	assert(modelDec.validate(testModel1).success.get.properties.size == 3)
	assert(modelDec.validate(testModel2).missingProperties.size == 1)
	assert(modelDec.validate(testModel3).success.get.properties.size == 3)
	assert(modelDec.validate(testModel4).success.get.properties.size == 4)
	assert(modelDec.validate(testModel5).invalidConversions.size == 1)
	assert(modelDec.validate(testModel6).invalidConversions.size == 1)
	
	// Tests optional property declarations
	val prop4 = PropertyDeclaration.optional("test2", StringType, Vector("TST"))
	val dec2 = ModelDeclaration("test1" -> StringType) + prop4
	val val1 = dec2.validate(Model.from("test1" -> "asd"))
	val val2 = dec2.validate(Model.from("test1" -> 1, "TST" -> "hello"))
	
	assert(prop4.isOptional)
	assert(!prop4.isRequired)
	assert(dec2.contains("test2"))
	assert(val1.isSuccess)
	assert(val2.isSuccess)
	
	val m = val2.success.get
	
	assert(m("test2").getString == "hello")
	assert(m("test1").dataType == StringType)
	
	println("Success")
}
