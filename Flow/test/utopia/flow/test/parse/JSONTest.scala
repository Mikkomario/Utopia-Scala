package utopia.flow.test.parse

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.{Constant, Model, Value}
import utopia.flow.generic.model.mutable.DataType
import utopia.flow.generic.model.mutable.DataType.{IntType, ModelType, StringType}
import utopia.flow.parse.json.JsonReader

import java.time.Instant

/**
 *
 * @author Mikko Hilpinen
 * @since 5.4.2021, v
 */
object JSONTest extends App
{
	
	
	def assertJSON(value: Value, json: String) =
	{
		val result = value.toJson
		println(s"${value.description} => $result")
		assert(result == json)
	}
	
	// Tests value JSON parsing first
	val i = 123.toValue
	val d = 222.222.toValue
	val s = "Hello World!".toValue
	val time = Instant.now().toValue
	val b = true.toValue
	val empty = Value.emptyWithType(StringType)
	val v = Vector(empty, b, i).toValue
	
	assertJSON(empty, "null")
	
	assertJSON(i, "123")
	assertJSON(d, "222.222")
	assertJSON(s, "\"Hello World!\"")
	assertJSON(time, "\"" + time + "\"")
	assertJSON(b, "true")
	assertJSON(v, "[null, true, 123]")
	assertJSON(Vector[Value](), "[]")
	assertJSON(Model.empty, "{}")
	
	// Tests Property writing next
	val prop1 = Constant("test1", i)
	val prop2 = Constant("test2", s)
	val prop3 = Constant("test3", empty)
	
	assert(prop3.toJson == "\"test3\": null")
	assert(prop1.toJson == "\"test1\": 123")
	
	// Tests / prints model writing
	val model = Model.withConstants(Vector(prop1, prop2, prop3))
	println(model.toJson)
	
	// Tests value reading
	assert(JsonReader("1").get.intOr() == 1)
	println(JsonReader("[1, 2, 3]").get.description)
	assert(JsonReader("[1, 2, 3]").get.vectorOr() == Vector[Value](1, 2, 3))
	
	// Tests model reading
	val readModel1 = JsonReader(model.toJson).get.getModel
	println(readModel1)
	// assert(readModel1 == model)
	
	val readModel2 = JsonReader("{\"name\" : \"Matti\", \"age\": 39, \"empty\": \"\", \"length\": 76.24}").get.getModel
	println(readModel2)
	
	assert(readModel2("name").stringOr() == "Matti")
	assert(readModel2("age").dataType == IntType)
	assert(readModel2("length").getDouble == 76.24)
	
	val readModel2Converted = JsonReader(readModel2.toJson).get.getModel
	println(readModel2Converted)
	assert(readModel2 == readModel2Converted)
	
	// Tests more difficult data types
	val prop4 = Constant("test4", v)
	val prop5 = Constant("test5", model)
	val prop6 = Constant("test6", time)
	
	val model2 = Model.withConstants(Vector(prop4, prop5, prop6))
	println(model2)
	
	val readModel3 = JsonReader(model2.toJson).get.getModel
	println(readModel3)
	
	val readTime = readModel3("test6").instant
	assert(readTime.contains(time.getInstant))
	assert(readModel3("test4").vectorOr().length == 3)
	assert(readModel3("test5").dataType == ModelType)
	
	// Tests value reading vs. model reading
	assert(JsonReader(readModel2.toJson).get.getModel == readModel2)
	
	// This kind of setting was causing a problem earlier
	val test = Vector(1)
	println(test)
	println(test.toValue)
	
	// Tests model parsing with empty vector values
	println()
	println("Testing empty vectors and models")
	val model4 = Model(Vector("vec" -> Vector[Value](), "normal" -> "a"))
	
	println(model4)
	val parsed = JsonReader(model4.toJson).get.getModel
	println(parsed)
	assert(parsed == model4)
	
	val model5 = Model(Vector("mod" -> Model(Vector())))
	
	println(model5)
	val parsed5 = JsonReader(model5.toJson).get.getModel
	println(parsed5)
	assert(parsed5 == model5)
	
	assert(JsonReader("[]").get == Vector[Value]().toValue)
	assert(JsonReader("[ ]").get == Vector[Value]().toValue)
	assert(JsonReader("[null]").get == Vector(Value.empty).toValue)
	assert(JsonReader("[,]").get == Vector(Value.empty, Value.empty).toValue)
	
	// Testing JSON reading when quoted portion contains json markers
	val jsonWithQuotes = Model(Vector("Test1" -> "This portion contains, special values",
		"Test2" -> "This one is also { tough }", "Even worse [when, array, in, property, name]" -> true)).toJson
	val parsed6 = JsonReader(jsonWithQuotes).get.getModel
	
	assert(parsed6("Test1").getString == "This portion contains, special values")
	assert(parsed6("Test2").getString == "This one is also { tough }")
	assert(parsed6("Even worse [when, array, in, property, name]").getBoolean)
	
	// Testing int and double handling on missing and empty (null) values
	val readModel4 = JsonReader("{\"name\" : \"Matti\", \"age\": null, \"empty\": \"\"}").get.getModel
	
	assert(readModel4("none").int.isEmpty)
	assert(readModel4("none").double.isEmpty)
	assert(readModel4("none").isEmpty)
	assert(readModel4("age").int.isEmpty)
	assert(readModel4("age").double.isEmpty)
	assert(readModel4("age").isEmpty)
	assert(readModel4("empty").int.isEmpty)
	assert(readModel4("empty").double.isEmpty)
	assert(readModel4("name").int.isEmpty)
	assert(readModel4("name").double.isEmpty)
	
	// Testing JSONReader parseValue with quotations
	assert(JsonReader("\"Test\"").get.string.contains("Test"))
	
	println("Success!")
}
