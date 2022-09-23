package utopia.nexus.test

import utopia.access.http.Method
import utopia.access.http.Method._
import utopia.access.http.Status._
import utopia.flow.collection.value.typeless.{Model, Value}
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.DataType
import utopia.flow.generic.ValueConversions._
import utopia.flow.parse.JSONReader
import utopia.flow.test.TestContext._
import utopia.nexus.http.{Path, Request, Response, ServerSettings}
import utopia.nexus.rest.{BaseContext, FilesResource, RequestHandler}

import java.io.ByteArrayOutputStream
import java.nio.file.Paths

/**
 * This test makes sure the rest test resource and the request handler are working
 */
object RestResourceTest extends App
{
    DataType.setup()
    
    // Creates the main resources first
    implicit val settings: ServerSettings = ServerSettings("https://localhost:9999")
    
    val rootResource = new TestRestResource("root")
    val filesResource = new FilesResource("files", Paths.get("D:/Uploads"))
    // FIXME: Changed to updated RequestHandler class without updating this test => will fail
    val handler = RequestHandler(Map("v1" -> Vector(rootResource, filesResource)),
        Some(Path("rest"))) { new BaseContext(_) }
    
    def responseToString(response: Response) = 
    {
        if (response.writeBody.isDefined)
        {
            val out = new ByteArrayOutputStream()
            try
            {
                response.writeBody.get(out)
                Some(out.toString(response.headers.charset.map(_.name()).getOrElse("UTF-8")))
            }
            finally
                out.close()
        }
        else
            None
    }
    
    def stringToModel(s: String) = JSONReader(s).toOption.flatMap { _.model }
    
    def responseToModel(response: Response) = responseToString(response).flatMap(stringToModel)
    
    def makeRequest(method: Method, path: Path, parameters: Model = Model(Vector())) =
    {
        new Request(method, path.toServerUrl, Some(path), parameters)
    }
    
    def getString(path: Path) = responseToString(handler(makeRequest(Get, path)))
    
    def getModel(path: Path) =  responseToModel(handler(makeRequest(Get, path)))
    
    def testModelExists(path: Path) = 
    {
        val response = handler(makeRequest(Get, path))
        assert(response.status == OK)
        val model = responseToModel(response)
        assert(model.isDefined)
        println(model.get)
    }
    
    def testNotFound(path: Path) = assert(handler(makeRequest(Get, path)).status == NotFound)
    
    def testAttributeExists(path: Path, attName: String) = 
    {
        val response = handler(makeRequest(Get, path))
        val responseString = responseToString(response)
        
        if (response.status == OK)
        {
            assert(responseString.isDefined)
            
            println(responseString.get)
            assert(stringToModel(responseString.get).exists(_(attName).isDefined))
        }
        else 
        {
            println(s"Status: ${ response.status }")
            assert(false)
        }
    }
    
    def testPutAttribute(path: Path, attName: String, value: Value) = 
    {
        val response = handler(makeRequest(Put, path, Model(Vector(attName -> value))))
        assert(response.status == OK)
    }
    
    def testPostModel(path: Path, model: Model) =
    {
        val response = handler(makeRequest(Post, path, model))
        assert(response.status == Created)
        response.headers.location.foreach(println)
    }
    
    def testDelete(path: Path) = 
    {
        val response = handler(makeRequest(Delete, path))
        assert(response.status == OK)
    }
    
    def testAttValue(path: Path, attName: String, expectedValue: Value) = 
    {
        assert(getModel(path).exists(_(attName) == expectedValue))
    }
    
    testAttributeExists(Path("rest"), "root")
    
    val rootPath = Path("rest", "root")
    testModelExists(rootPath)
    testNotFound(Path("rest", "not_here"))
    
    testPutAttribute(rootPath, "att1", 1)
    testPutAttribute(rootPath, "att2", "test2")
    testPutAttribute(rootPath, "model", Model(Vector("a" -> 1, "b" -> 2)))
    
    testAttributeExists(rootPath, "att1")
    testAttributeExists(rootPath, "model")
    
    val model2Path = rootPath/"model2"
    
    testPostModel(model2Path, Model(Vector("test1" -> "test", "test2" -> 2)))
    testModelExists(model2Path)
    
    testAttValue(model2Path, "test1", "test")
    testPutAttribute(model2Path, "test1", 1)
    testAttValue(model2Path, "test1", 1)
    
    testDelete(model2Path)
    testNotFound(model2Path)
    
    val filesPath = Path("rest", "files")
    testModelExists(filesPath)
    testModelExists(filesPath/"testikansio")
    
    println("Success!")
}