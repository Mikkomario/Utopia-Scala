package utopia.flow.test

import utopia.flow.datastructure.immutable.Constant
import utopia.flow.datastructure.immutable.Model
import utopia.flow.generic.DataType

import utopia.flow.generic.ValueConversions._

object ValueAccessorTest extends App
{
    DataType.setup()
    
    val i = 1.toValue
    val s = "2".toValue
    val b = true.toValue
    
    val v = Vector(i, s, b).toValue
    
    val prop1 = new Constant("int", i)
    val prop2 = new Constant("string", s)
    val prop3 = new Constant("boolean", b)
    val prop4 = new Constant("vector", v)
    
    val model1 = Model.withConstants(Vector(prop1, prop2, prop3))
    
    val prop5 = new Constant("model", model1)
    
    val model2 = Model.withConstants(Vector(prop4, prop5))
    
    // Prints the model for reference
    println(model2)
    
    // Accesses valid values through model 2
    assert(model2("vector")(0).intOr() == 1)
    assert(model2("vector")(2).booleanOr())
    
    assert(model2("model")("int").intOr() == 1)
    assert(model2("model")("boolean").booleanOr())
    
    // Accesses invalid value through model 2 (should be empty)
    assert(model2("not here")(0).isEmpty)
    assert(model2("vector")(-1).isEmpty)
    assert(model2("vector")(6).isEmpty)
    
    assert(model2("not here")("int").isEmpty)
    
    println("Success!")
}