package utopia.access.test

import scala.collection.immutable.HashMap
import utopia.access.http.ContentCategory.Application
import utopia.access.http.ContentCategory
import utopia.access.http.ContentCategory.Custom
import utopia.access.http.ContentType

/**
 * This app tests parsing of different content types
 * @author Mikko Hilpinen
 * @since 21.8.2017
 */
object ContentTypeTest extends App
{
    // Tests category parsing
    val applicationString = Application.toString()
    
    assert(applicationString == "application")
    assert(ContentCategory.parse(applicationString) == Application)
    
    val customCategory = Custom("test")
    val customString = customCategory.toString()
    
    assert(customString == "X-test")
    assert(ContentCategory.parse(customString) == customCategory)
    
    // Tests content type parsing
    val json = Application.json
    
    assert(json.toString() == "application/json")
    assert(ContentType.parse(json.toString()).contains(json))
    
    val customType = ContentType(customCategory, "custom", HashMap("att1" -> "a", "att2" -> "b"))
    
    println(customType)
    assert(ContentType.parse(customType.toString()).contains(customType))
    
    // Tests content type guessing
    assert(ContentType.guessFrom("test.jpg").isDefined)
    assert(ContentType.guessFrom("test.png").isDefined)
    assert(ContentType.guessFrom("test.wav").isDefined)
    
    println("Success!")
}