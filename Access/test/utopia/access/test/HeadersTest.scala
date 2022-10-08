package utopia.access.test

import java.time.Instant
import utopia.access.http.ContentCategory.Text
import utopia.access.http.Headers
import utopia.access.http.Method.Get
import utopia.access.http.Method.Post
import utopia.access.http.Method.Delete
import utopia.flow.generic.model.mutable.DataType

/**
 * This app tests use of headers
 */
object HeadersTest extends App
{
    
    
    val contentType = Text/"plain"
    val empty = Headers()
    
    assert(empty.withContentType(contentType).contentType.contains(contentType))
    
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