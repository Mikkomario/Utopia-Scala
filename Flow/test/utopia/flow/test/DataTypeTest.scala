package utopia.flow.test

import utopia.flow.generic.DataType
import utopia.flow.generic.AnyType
import utopia.flow.generic.StringType
import utopia.flow.generic.ConversionReliability
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.DoubleType
import java.time.{Instant, LocalDate, LocalDateTime, LocalTime}

import utopia.flow.datastructure.immutable.Model
import utopia.flow.generic.ConversionHandler

import scala.collection.immutable.HashSet
import utopia.flow.generic.IntType
import utopia.flow.generic.ValueConversions._
import utopia.flow.datastructure.mutable.GraphNode
import utopia.flow.generic.InstantType
import utopia.flow.generic.LocalDateTimeType

object DataTypeTest extends App
{
    val testNode = new GraphNode[String, Int]("Test1")
    val testNode2 = new GraphNode[String, Int]("Test2")
    val testNode3 = new GraphNode[String, Int]("Test3")
    testNode2.setConnection(testNode3, 3)
    testNode.setConnection(testNode2, 2)
    
    assert(testNode.isDirectlyConnectedTo(testNode2))
    assert(testNode.isConnectedTo(testNode3))
    assert((testNode/Vector(2, 3)).nonEmpty)
    assert(testNode.routesTo(testNode3).size == 1)
    
    DataType.setup()
    DataType.values.foreach { println(_) }
    
    assert(StringType isOfType StringType)
    assert(StringType.isOfType(AnyType))
    assert(!StringType.isOfType(DoubleType))
    
    assert(ConversionReliability.DANGEROUS < ConversionReliability.NO_CONVERSION)
    
    val str = "123.45".toValue
    val str2 = "true".toValue
    val i = 213.toValue
    val d = 123.4567891234.toValue
    val f = 123.45f.toValue
    val l = 9999999999l.toValue
    val b = true.toValue
    val time = Instant.now().toValue
    val lDate = LocalDate.now().toValue
    val lTime = LocalTime.now().toValue
    val lDT = LocalDateTime.now().toValue
    val vector = Vector(i, d, f, l).toValue
    val model = Model("attributeName", i).toValue
    
    /*
    DataType.values.foreach { fromType => DataType.values.foreach { toType => 
        println(s"Route from $fromType to $toType: " + ConversionHandler.routeString(fromType, toType)) } }
    DataType.values.foreach { fromType => DataType.values.foreach { toType => 
        println(s"Route cost $fromType to $toType: " + ConversionHandler.costOfRoute(fromType, toType)) } }
    */
    
    assert(str.getDouble == 123.45)
    assert(str.getInt == 123)
    assert(str.getLong == 123)
    assert(!str.getBoolean)
    assert(str.string == str.content)
    assert(str2.getBoolean)
    
    assert(i.getDouble == 213)
    assert(i.getBoolean)
    assert(i.getLong == 213)
    assert(i.getString == "213")
    
    assert(d.getInt == 123)
    assert(d.getBoolean)
    assert(d.getLong == 123)
    
    assert(f.getInt == 123)
    assert(f.getLong == 123)
    assert(f.getBoolean)
    
    assert(l.getDouble == 9999999999.0)
    assert(l.getBoolean)
    
    assert(b.getInt == 1)
    assert(b.getString == "true")
    assert(b.getDouble == 1.0)
    
    assert(time.getLong > 0)
    
    val timeToString = time.toString()
    val timeStringToTime = timeToString.instantOr()
    assert(time.long == timeStringToTime.long)
    
    // println(lDT.localDateTimeOr())
    // println(lDT.instantOr())
    // println(lDT.castTo(InstantType).castTo(LocalDateTimeType))
    assert(lDT.castTo(InstantType).castTo(LocalDateTimeType).localDateTime == lDT.localDateTime)
    assert(lDate.castTo(LocalDateTimeType).localDate == lDate.localDate)
    assert(lDate.castTo(StringType).localDate == lDate.localDate)
    
    println(vector.toString())
    assert(vector.vectorOr().length == 4)
    assert(vector.toString().startsWith("["))
    assert(model.vectorOr().length == 1)
    
    assert(str.orElse(i) == str)
    assert(Value.empty.orElse(i) == i)
    
    println(model.toString())
    
    // Tests Multi type conversion
    assert(ConversionHandler.cast(d, HashSet(StringType, IntType)).exists { 
        _.dataType isOfType IntType })
    
    // Tests some specific conversions
    // println(ZonedDateTime.parse("2007-11-20T22:19:17+02:00"))
    // assert(Try(Instant.parse("2007-11-20T22:19:17+02:00")).isSuccess)
    assert("2007-11-20T22:19:17+02:00".instant.isDefined)
    assert("2018-05-15T10:33:16+03:00".instant.isDefined)
    
    println("Success")
}