package utopia.flow.test.generic

import utopia.flow.collection.immutable.{Empty, Pair}
import utopia.flow.generic.model.immutable.{Constant, Model, PropertyRenames, Value}
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.casting.ValueUnwraps._

/**
 * Tests the immutable model class
 * @author Mikko Hilpinen
 * @since 26.10.2025, v2.7
 */
object ModelTest2 extends App
{
	// COMMON   ----------------------------
	
	val prop1 = Constant("a", 1)
	val prop2 = Constant("b", 2)
	val expectedProps1 = Vector(prop1, prop2, Constant("C", true))
	private val model1 = Model.from("a" -> 1, "b" -> 2, "C" -> true)
	
	
	// TESTS    ----------------------------
	
	// HasValues
	{
		val emptyValues = Model.from("a" -> "", "b" -> (None: Option[Int]))
		
		// isEmpty & nonEmpty
		assert(model1.nonEmpty)
		assert(emptyValues.nonEmpty)
		assert(Model.empty.isEmpty)
		assert(Model(Empty).isEmpty)
		
		// hasNonEmptyValues & hasOnlyEmptyValues
		assert(model1.hasNonEmptyValues)
		assert(emptyValues.hasOnlyEmptyValues)
		assert(Model.empty.hasOnlyEmptyValues)
		
		// contains & containsNonEmpty
		assert(model1.contains("a"))
		assert(!model1.contains("d"))
		assert(emptyValues.contains("b"))
		
		assert(model1.containsNonEmpty("b"))
		assert(!emptyValues.containsNonEmpty("b"))
		
		// apply variants
		assert(model1("a").getInt == 1)
		assert(model1("c").getBoolean)
		assert(emptyValues("a").getString.isEmpty)
		assert(model1("d").int.isEmpty)
		assert(model1("A").getInt == 1)
		
		assert(model1("a", "b").getInt == 1)
		assert(model1("d", "c").getBoolean)
		assert(emptyValues("a", "b").string.isEmpty)
		
		// nonEmpty
		assert(model1.nonEmpty("a").exists { _.getInt == 1 })
		assert(model1.nonEmpty("d").isEmpty)
		assert(emptyValues.nonEmpty("a").isEmpty)
		
		// tryGet
		assert(model1.tryGet("a") { _.tryInt }.toOption.contains(1))
		assert(model1.tryGet("b", "a") { _.tryInt }.toOption.contains(2))
		assert(model1.tryGet("d", "c", "g") { _.tryBoolean }.toOption.contains(true))
		assert(model1.tryGet("d") { _.tryInt }.isFailure)
		assert(emptyValues.tryGet("a") { _.tryInt }.isFailure)
		
		// ==
		assert(model1 == Model.withConstants(expectedProps1))
	}
	
	// HasPropertiesLike
	{
		val expectedProps2 = Vector(prop1, prop2, Constant("c", 3))
		val hasSomeEmptyProps = Model.withConstants(
			expectedProps2 ++ Pair(Constant("d", Value.empty), Constant("e", Value.empty)))
		
		def newLazyModel = Model(Vector[(String, Value)]("a" -> 1, "b" -> 2, "c" -> 3).iterator)
		
		// propertiesIterator
		assert(model1.propertiesIterator.toVector == expectedProps1)
		assert(newLazyModel.propertiesIterator.toVector == expectedProps2)
		
		// properties
		assert(model1.properties == expectedProps1)
		assert(newLazyModel.properties == expectedProps2)
		
		// existingProperty
		assert(model1.existingProperty("a").contains(prop1))
		assert(model1.existingProperty("b").contains(prop2))
		assert(model1.existingProperty("d").isEmpty)
		assert(newLazyModel.existingProperty("a").contains(prop1))
		assert(model1.existingProperty("d", "a").contains(prop1))
		assert(model1.existingProperty("d", "e", "f").isEmpty)
		
		// property
		assert(model1.property("a") == prop1)
		assert(model1.property("d") == Constant("d", Value.empty))
		
		// propertyNamesIterator
		assert(model1.propertyNamesIterator.toVector == Vector("a", "b", "C"))
		assert(newLazyModel.propertyNamesIterator.toVector == Vector("a", "b", "c"))
		assert(hasSomeEmptyProps.propertyNames == Vector("a", "b", "c", "d", "e"))
		
		// nonEmptyPropertiesIterator
		assert(model1.nonEmptyPropertiesIterator.toVector == expectedProps1)
		assert(hasSomeEmptyProps.nonEmptyPropertiesIterator.toVector == expectedProps2)
		assert(newLazyModel.nonEmptyPropertiesIterator.toVector == expectedProps2)
		
		// toMap
		val map1 = hasSomeEmptyProps.toMap[Int]
		
		assert(map1.keySet == Set("a", "b", "c", "d", "e"))
		assert(map1("a") == 1)
		assert(map1("b") == 2)
		assert(map1("d") == 0)
		
		// toPartialMap
		val map2 = hasSomeEmptyProps.toPartialMap[Int]
		val map3 = model1.toPartialMap[Int]
		
		assert(map2.keySet == Set("a", "b", "c"))
		assert(map2("b") == 2)
		assert(map3("c") == 1)
		
		// ~==
		assert(model1 ~== Model.from("a" -> 1, "d" -> 2))
		assert(model1 ~== Model.from("A" -> 1, "c" -> 1, "d" -> 0))
		assert(model1 !~== Model.from("a" -> 1, "b" -> 3))
		assert(model1 !~== Model.from("a" -> "ASD"))
		
		// JSON conversion
		assert(model1.toJson == "{\"a\": 1, \"b\": 2, \"C\": true}", model1.toJson)
		assert(hasSomeEmptyProps.toJson == "{\"a\": 1, \"b\": 2, \"c\": 3, \"d\": null, \"e\": null}")
	}
	
	// ModelLike
	{
		val ba = Model.withConstants(Pair(prop2, prop1))
		val ab = Model.withConstants(prop1, prop2)
		val appendedWithEmpty = model1 + Constant("a", Value.empty)
		
		def lazyModel = Model.withConstants(model1.propertiesIterator)
		
		// +
		assert(model1 + ("d" -> "test") == Model.withConstants(expectedProps1 :+ Constant("d", "test")))
		assert(appendedWithEmpty.properties ==
			Vector(prop2, Constant("C", true), Constant("a", Value.empty)))
		
		// withoutEmptyValues
		assert(appendedWithEmpty.withoutEmptyValues.properties == Pair(prop2, Constant("C", true)))
		
		// sorted & sortBy
		assert(ba.sorted == ab)
		assert(ba.sortBy { _.value.getInt } == ab)
		
		// Renaming
		val renamed = model1 + PropertyRenames("a" -> "asd", "b" -> "BEE")
		
		assert(renamed("a").isEmpty, renamed("a"))
		assert(renamed("asd").getInt == 1)
		assert(renamed("ASD").getInt == 1)
		assert(renamed("bee").getInt == 2)
		assert(renamed.properties == Vector(Constant("asd", 1), Constant("BEE", 2), Constant("C", true)))
		
		// map, mapKeys & mapValues
		assert(ab.map { c => Constant(s"${ c.name }2", c.value.getInt + 1) } == Model.from("a2" -> 2, "b2" -> 3))
		assert(ab.mapKeys { _ + "2" } == Model.from("a2" -> 1, "b2" -> 2))
		assert(ab.mapValues { _.getInt + 1 } == Model.from("a" -> 2, "b" -> 3))
		
		// knownContains
		{
			val m = lazyModel
			
			assert(model1.knownContains("a").isCertainlyTrue)
			assert(model1.knownContains("d").isCertainlyFalse)
			assert(model1.knownContains("c").isCertainlyTrue)
			
			assert(m.knownContains("a").isUncertain)
			assert(m.knownContains("d").isUncertain)
			
			// Triggers the caching of "a"
			assert(m("a").getInt == 1)
			
			assert(m.knownContains("a").isCertainlyTrue)
			assert(m.knownContains("b").isUncertain)
			assert(m.knownContains("d").isUncertain)
			
			// Triggers the caching of "b"
			assert(m("b").getInt == 2)
			
			assert(m.knownContains("b").isCertainlyTrue)
			assert(m.knownContains("c").isUncertain)
			assert(m.knownContains("d").isUncertain)
			
			// Triggers the caching of "C"
			assert(m("c").getBoolean)
			
			assert(m.knownContains("c").isCertainlyTrue)
			assert(m.knownContains("d").isCertainlyFalse)
		}
	}
}
