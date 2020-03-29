package utopia.disciple.test

import scala.concurrent.ExecutionContext.Implicits.global
import utopia.flow.generic.ValueConversions._
import utopia.access.http.ContentCategory._

import utopia.flow.util.TimeExtensions._
import utopia.flow.async.AsyncExtensions._
import utopia.flow.generic.DataType
import utopia.disciple.http.Request
import utopia.access.http.Method._
import utopia.disciple.apache.Gateway
import utopia.flow.datastructure.immutable.Model
import utopia.disciple.http.FileBody
import java.io.File

/**
 * Tests the Gateway interface
 * @author Mikko Hilpinen
 * @since 15.3.2018
 */
object GatewayTest extends App
{
    DataType.setup()
    Gateway.maxConnectionsPerRoute = 10
    Gateway.maxConnectionsTotal = 70
    
    val uri = "http://localhost:9999/TestServer/echo"
    def makeRequest(request: Request) = Gateway.getStringResponse(request).waitFor(10.seconds).get
    
    val get1 = new Request(uri, Get)
    
    println("Sending request")
    val response1 = makeRequest(get1)
    
    println(response1.status)
    println(response1.headers)
    println(response1.body)
    
    val post1 = new Request(uri, Post, Model(Vector("test" -> "a", "another" -> 2))).withModifiedHeaders(
            _.withTypeAccepted(Application/"json"))
    
    val response2 = makeRequest(post1)
    
    println(response2.status)
    println(response2.headers)
    println(response2.body)
    
    val file = new File("testData/ankka.jpg")
    val postImage = new Request(requestUri = "http://localhost:9999/TestServer/echo", 
            method = Post, params = Model(Vector("name" -> "ankka")), 
            body = Some(new FileBody(file, Image/"jpg")))
    
    val response3 = makeRequest(postImage)
    
    println(response3.status)
    println(response3.headers)
    println(response3.body)
}