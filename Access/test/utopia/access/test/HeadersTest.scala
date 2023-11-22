package utopia.access.test

import utopia.access.http.ContentCategory.Text
import utopia.access.http.Headers
import utopia.access.http.Method.{Delete, Get, Post}

import java.time.Instant

/**
 * This app tests use of headers
 */
object HeadersTest extends App
{
    val contentType = Text/"plain"
    val empty = Headers()
    val contentTypeHeaders = empty.withContentType(contentType)
    
    println(contentTypeHeaders.fields)
    assert(contentTypeHeaders("Content-Type").contains("text/plain"), contentTypeHeaders("Content-Type"))
    assert(contentTypeHeaders.semicolonSeparatedValues("Content-Type") == Vector("text/plain"),
        contentTypeHeaders.semicolonSeparatedValues("Content-Type"))
    println(contentTypeHeaders)
    assert(contentTypeHeaders.contentType.contains(contentType),
        empty.withContentType(contentType).contentType)
    assert(empty.withHeader("X-Test", "A").apply("X-Test").contains("A"))
    assert(contentTypeHeaders.withHeaderAdded("X-Test", "A")
        .withHeaderAdded("X-Test", "B").apply("X-Test").contains("A,B"))
    
    val allowed = Vector(Get, Post)
    val allowHeaders = empty.withAllowedMethods(allowed)
    
    assert(allowHeaders.allowedMethods == allowed)
    assert(allowHeaders.allows(Get))
    assert(allowHeaders.withMethodAllowed(Delete).allows(Delete))
    assert(!allowHeaders.allows(Delete))
    
    val epoch = Instant.EPOCH
    
    assert(empty.withDate(epoch).date.contains(epoch))
    
    assert(Headers(allowHeaders.toModel).toOption.contains(allowHeaders))
    
    println("Success!")
}