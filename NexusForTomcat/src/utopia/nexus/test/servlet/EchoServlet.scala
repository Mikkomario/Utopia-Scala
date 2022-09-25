package utopia.nexus.test.servlet

import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import utopia.flow.generic.casting.ValueConversions._
import utopia.nexus.http.Response

import javax.servlet.annotation.MultipartConfig
import utopia.nexus.servlet.HttpExtensions._
import utopia.nexus.http.ServerSettings
import utopia.flow.generic.model.mutable
import utopia.flow.generic.model.immutable.Model
import utopia.flow.generic.model.mutable.DataType
import utopia.flow.parse.json.{JsonReader, JsonParser}
import utopia.nexus.http.Body

/**
 * This servlet implementation responds with data collected from the request
 * @author Mikko Hilpinen
 * @since 21.8.2017
 */
@MultipartConfig(
        fileSizeThreshold   = 1048576,  // 1 MB
        maxFileSize         = 10485760, // 10 MB
        maxRequestSize      = 20971520, // 20 MB
)
// Used to contain: location            = "D:/Uploads"
class EchoServlet extends HttpServlet
{
    // INITIAL CODE    -----------------------
    
    DataType.setup()
    private implicit val settings: ServerSettings = ServerSettings("http://localhost:9999")
    private implicit val jsonParser: JsonParser = JsonReader
    
    
    // IMPLEMENTED METHODS    ----------------
    
    override def doGet(req: HttpServletRequest, res: HttpServletResponse) = 
    {
        val request = req.toRequest.get
        
        val buffer = mutable.Model()
        buffer.update("method", request.method.toString())
        buffer.update("url", request.targetUrl)
        buffer.update("path", request.path.map(_.toString()))
        buffer.update("parameters", request.parameters)
        buffer.update("headers", Model.fromMap(request.headers.fields))
        buffer.update("parts", request.body.toVector.map(partToModel))
        
        Response.fromModel(buffer.immutableCopy()).update(res)
    }
    
    override def doPost(request: HttpServletRequest, response: HttpServletResponse) = doGet(request, response)
    override def doPut(request: HttpServletRequest, response: HttpServletResponse) = doGet(request, response)
    override def doDelete(request: HttpServletRequest, response: HttpServletResponse) = doGet(request, response)
    override def doHead(request: HttpServletRequest, response: HttpServletResponse) = doGet(request, response)
    
    private def partToModel(part: Body) = Model(Vector("name" -> part.name, 
            "size" -> part.contentLength, "type" -> part.contentType.toString, 
            "headers" -> Model.fromMap(part.headers.fields)))
}