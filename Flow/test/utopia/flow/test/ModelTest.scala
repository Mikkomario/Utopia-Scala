package utopia.flow.test

import utopia.flow.generic.SimpleVariableGenerator
import utopia.flow.datastructure.immutable
import utopia.flow.datastructure.mutable
import utopia.flow.generic.DataType
import utopia.flow.datastructure.immutable.Constant
import utopia.flow.generic.SimpleConstantGenerator

import utopia.flow.generic.ValueConversions._

object ModelTest extends App
{
    DataType.setup()
    
    // Tests variable creation
    val generator2 = new SimpleVariableGenerator(0)
    
    assert(generator2.defaultValue.isDefined)
    val generated = generator2("Test", None)
    assert(generated.value == generator2.defaultValue)
    
    // Tests models
    // 1) Model with no default value
    val model1 = mutable.Model()
    model1("Test") = 2
    model1("Another") = "Hello"
    
    assert(model1.attributes.size == 2)
    assert(model1("Test").content.get == 2)
    
    model1("another") = "Hello2"
    
    assert(model1.attributes.size == 2)
    assert(model1("Another").content.get == "Hello2")
    
    // 2) model with default value
    val model2 = new mutable.Model(generator2)
    assert(model2.findExisting("Test").isEmpty)
    assert(model2("Test").content.get == 0)
    
    // 3) immutable model with no default value
    val constants = Vector(Constant("Test1", 1), Constant("Test2", 2))
    val model3 = immutable.Model.withConstants(constants)
    
    assert(model3.attributeGenerator == model3.attributeGenerator)
    assert(model3.attributeGenerator == new SimpleConstantGenerator())
    
    assert(model3 == immutable.Model.withConstants(constants))
    assert(model3.attributes.size == 2)
    assert(model3("Test1").content.get == 1)
    
    // Tests model renaming too
    val renamed1 = model3.renamed(Vector("Test1" -> "Renamed", "non-existing" -> "still-not-existing"))
    
    assert(renamed1("Renamed") == model3("Test1"))
    assert(renamed1.attributes.size == 2)
    assert(renamed1("Test2").getInt == 2)
    
    val mutableModel2 = mutable.Model()
    mutableModel2("Test1") = 1
    mutableModel2("Test2") = 2
    assert(mutableModel2.immutableCopy() == model3)
    
    val model4 = model3 + Constant("Test3", 3)
    
    assert(model4.attributes.size == 3)
    assert(model4("Test3").content.get == 3)
    
    // 4) Immutable model with a default value
    val generator3 = new SimpleConstantGenerator(0)
    val model5 = new immutable.Model(constants, generator3)
    
    assert(model5 != model3)
    assert(model5("nonexisting").content.get == 0)
    assert(model5.attributes.size == constants.size)
    
    println(model5.toString())
    
    // Tests other model generator functions
    val model6 = mutable.Model(Vector("a" -> 1, "b" -> 2))
    assert(model6.attributes.size == 2)
    
    val model7 = immutable.Model(Vector("a" -> 1, "b" -> 2))
    assert(model7.attributes.size == 2)
    
    // Tests immutable model filter
    val model3Filtered = model3.filter(_.value.intOr() == 2)
    assert(model3Filtered("Test1").isEmpty)
    assert(model3Filtered("Test2").intOr() == 2)
    
    val model3Filtered2 = model3.filterNot(_.value.intOr() == 2)
    assert(model3Filtered2("Test2").isEmpty)
    assert(model3Filtered2("Test1").intOr() == 1)
    
    println("Success")
}