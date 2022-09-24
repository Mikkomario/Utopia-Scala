package utopia.flow.test.generic

import utopia.flow.generic._
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.{DeclarationConstantGenerator, DeclarationVariableGenerator}
import utopia.flow.generic.model.immutable.PropertyDeclaration
import utopia.flow.generic.model.mutable.{BooleanType, DataType, IntType, Model, StringType}

/**
 *
 * @author Mikko Hilpinen
 * @since 5.4.2021, v
 */
object ModelDeclarationTest extends App
{
	DataType.setup()
	
	// Tests property declarations
	val prop1 = PropertyDeclaration("test1", IntType)
	val prop2 = PropertyDeclaration("test2", 0)
	
	// (not the usual use case but possible)
	val prop3 = PropertyDeclaration("test3", StringType, Some(3))
	
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
	val generator1 = new DeclarationConstantGenerator(modelDec2)
	
	assert(generator1("test1").value.isEmpty)
	assert(generator1("test2").value.isDefined)
	assert(generator1("test3").value.dataType == StringType)
	assert(generator1("not here").value.isEmpty)
	
	// 2) Generator with a default value
	val generator2 = new DeclarationConstantGenerator(modelDec2, 0)
	
	assert(generator2("test1").value.isDefined)
	assert(generator2("test3").value.dataType == StringType)
	assert(generator2("test4").value.content.get == false)
	assert(generator2("something else").value.isDefined)
	
	// Quick test of variable generation
	val generator4 = new DeclarationVariableGenerator(modelDec2)
	
	assert(generator4("test4", Some(1)).value.content.get == true)
	
	// Tests model validation
	val testModel1 = Model(Vector("test1" -> 12, "test2" -> 5))
	val testModel2 = Model(Vector("test2" -> 17, "test3" -> 11))
	val testModel3 = Model(Vector("test1" -> 12))
	val testModel4 = Model(Vector("test1" -> 12, "test4" -> "Hello"))
	val testModel5 = Model(Vector("test1" -> "Hello"))
	val testModel6 = Model(Vector("test1" -> 12, "test2" -> "Hello"))
	
	assert(modelDec.validate(testModel1).success.get.attributes.size == 3)
	assert(modelDec.validate(testModel2).missingProperties.size == 1)
	assert(modelDec.validate(testModel3).success.get.attributes.size == 3)
	assert(modelDec.validate(testModel4).success.get.attributes.size == 4)
	assert(modelDec.validate(testModel5).invalidConversions.size == 1)
	assert(modelDec.validate(testModel6).invalidConversions.size == 1)
	
	println("Success")
}
