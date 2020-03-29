package utopia.flow.test

import utopia.flow.generic.DataType
import scala.collection.immutable.HashMap

/**
 * This test tests a simple implementation of the ModelConvertible and FromModelFactory traits 
 * (TestModel)
 * @author Mikko Hilpinen
 * @since 24.6.2017
 */
object ModelConvertibleTest extends App
{
    DataType.setup()
    
    // Basic equality tests
    val stats1 = HashMap("def" -> 2, "mag" -> 5, "str" -> 3)
    val stats2 = HashMap("mag" -> 5, "def" -> 2, "str" -> 3)
    
    assert(stats1 == stats2)
    assert(stats1.hashCode() == stats2.hashCode())
    
    val model1 = new TestModel("Anna", 2, stats1)
    val model2 = new TestModel("Anna", 2, stats2)
    
    assert(model1 == model2)
    assert(model1.toModel == model2.toModel)
    
    // Model parse test
    val fromModel = TestModel(model1.toModel)
    assert(fromModel.isSuccess)
    assert(fromModel.get == model1)
    
    // JSON parse test
    val fromJSON = TestModel.fromJSON(model1.toJSON)
    assert(fromJSON.isSuccess)
    assert(fromJSON.get == model1)
    
    println("Success!")
}