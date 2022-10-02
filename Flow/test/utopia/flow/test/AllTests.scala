package utopia.flow.test

import utopia.flow.test.async.{AsyncTest, AsyncViewTest, FutureRaceTest, LoopTest2, VolatileTest}
import utopia.flow.test.collection.CollectionTest
import utopia.flow.test.datastructure.{GraphTest, TreeNodeTest, WeakListTest}
import utopia.flow.test.generic.{DataTypeTest, ModelConvertibleTest, ModelDeclarationTest, ModelTest, ValueAccessorTest}
import utopia.flow.test.parse.{JSONTest, XmlTest}
import utopia.flow.test.time.TimeNumberTest

/**
  * This test runs all of the other tests
  * @author Mikko Hilpinen
  * @since 8.5.2019, v1+
  */
object AllTests extends App
{
	val s = ""
	
	def run(test: App) =
	{
		println(s"\nRunning ${test.getClass.getName}	------------------------------")
		test.main(Array())
	}
	
	run(StringUtilsTest)
	run(TimeNumberTest)
	run(DataTypeTest)
	run(CollectionTest)
	run(CounterTest)
	run(TreeNodeTest)
	run(GraphTest)
	run(ValueAccessorTest)
	run(ModelDeclarationTest)
	run(ModelTest)
	run(ModelConvertibleTest)
	run(JSONTest)
	run(XmlTest)
	run(VolatileTest)
	run(WeakListTest)
	run(AsyncTest)
	run(AsyncViewTest)
	run(FutureRaceTest)
	run(LoopTest2)
	
	println("All test completed")
}
