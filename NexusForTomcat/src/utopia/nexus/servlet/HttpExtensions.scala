package utopia.nexus.servlet

import utopia.access.http.ContentCategory._
import utopia.access.http.{ContentType, Cookie, Headers, Method}
import utopia.flow.generic.model.immutable.{Model, Value}
import utopia.flow.parse.json.JsonParser
import utopia.nexus.http.{Path, Request, Response, ServerSettings, StreamedBody}

import java.io.{BufferedReader, InputStreamReader}
import java.net.URLDecoder
import java.nio.charset.Charset
import java.util
import javax.servlet.http.{HttpServletRequest, HttpServletResponse, Part}
import scala.jdk.CollectionConverters._
import scala.util.{Success, Try}

/**
 * This object contains extensions that can be used with HttpServletRequest and HttpServletResponse 
 * classes on server side.
 * @author Mikko Hilpinen
 * @since 9.9.2017
 */
object HttpExtensions
{
    implicit class TomcatResponse(val r: Response) extends AnyVal
    {
        /**
         * Updates the contents of a servlet response to match those of this response
         */
        def update(response: HttpServletResponse) = 
        {
            response.setStatus(r.status.code)
            r.headers.contentType.foreach { cType => response.setContentType(cType.toString) }
            
            r.headers.fields.foreach { case (headerName, value) => response.addHeader(headerName, value) }
            
            r.setCookies.foreach( cookie => 
            {
                val javaCookie = new javax.servlet.http.Cookie(cookie.name, cookie.value.toJson)
                cookie.lifeLimitSeconds.foreach(javaCookie.setMaxAge)
                javaCookie.setSecure(cookie.isSecure)
                
                response.addCookie(javaCookie)
            })
            
            if (r.writeBody.isDefined)
            {
                val stream = response.getOutputStream
                try
                {
                    r.writeBody.get(stream)
                }
                finally
                {
                    try { stream.flush() } finally { stream.close() }
                }
            }
        }
    }
    
    implicit class ConvertibleRequest(val r: HttpServletRequest) extends AnyVal
    {
        /**
         * Converts a httpServletRequest into a http Request
         */
        def toRequest(implicit settings: ServerSettings, jsonParser: JsonParser) =
        {
            Option(r.getMethod).flatMap(Method.parse).map { method =>
                val path = Option(r.getRequestURI).flatMap(Path.parse)
    
                val paramValues = r.getParameterNames.asScala.map { pName =>
                    (pName, parseQueryParam(r.getParameter(pName))) }.flatMap { case (name, value) =>
                    value.toOption.map { name -> _ } }
                val parameters = Model(paramValues.toVector)
    
                val headers = Headers(r.getHeaderNames.asScala.map { hName => (hName, r.getHeader(hName)) }.toMap)
    
                val javaCookies = Option(r.getCookies).map { _.toVector }.getOrElse(Vector())
                val cookies = javaCookies.map { javaCookie => Cookie(javaCookie.getName,
                    Option(javaCookie.getValue).map { jsonParser.valueOf }.getOrElse(Value.empty),
                    if (javaCookie.getMaxAge < 0) None else Some(javaCookie.getMaxAge),
                    javaCookie.getSecure) }
    
                val body = bodyFromRequest(r, headers).filter { !_.isEmpty }
                /*
                val uploads = Try(r.getParts).toOption.map { _.asScala.flatMap {part =>
                        part.getContentType.toOption.flatMap(ContentType.parse).map {
                        new FileUpload(part.getName, part.getSize, _, part.getSubmittedFileName,
                        part.getInputStream, part.write) }}}*/
    
                new Request(method, r.getRequestURL.toString, path, parameters, headers, body, cookies)
            }
        }
        
        private def parseQueryParam(paramValue: String)(implicit settings: ServerSettings, jsonParser: JsonParser) =
        {
            val decoded = settings.expectedParameterEncoding match
            {
                case Some(encoding) => Try { URLDecoder.decode(paramValue, encoding.charSet.name()) }
                case None => Success(paramValue)
            }
            decoded.map(jsonParser.valueOf)
        }
        
        private def bodyFromRequest(request: HttpServletRequest, headers: Headers) = 
        {
            val contentType = headers.contentType
            
            if (contentType.isEmpty)
                Vector()
            else if (contentType.get.category == MultiPart)
                Try(request.getParts).toOption.map { _.asScala.map(partToBody) }.toVector.flatten
            else
                Vector(new StreamedBody(request.getReader, contentType.get, 
                        Some(request.getContentLengthLong).filter { _ >= 0 }, headers))
        }
        
        private def partToBody(part: Part) =
        {
            val headers = parseHeaders(part.getHeaderNames, part.getHeader)
            val contentType = Option(part.getContentType).flatMap(ContentType.parse).getOrElse(Text.plain)
            val charset = headers.charset.getOrElse(Charset.defaultCharset())
            
            new StreamedBody(new BufferedReader(new InputStreamReader(part.getInputStream, charset)), 
                    contentType, Some(part.getSize), headers, 
                   Option(part.getSubmittedFileName).orElse(Option(part.getName)))
        }
        
        private def parseHeaders(headerNames: util.Collection[String], getValue: String => String) =
                Headers(headerNames.asScala.map(hName => (hName, getValue(hName))).toMap)
    }
}