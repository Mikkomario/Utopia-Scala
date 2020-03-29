package utopia.nexus.servlet

import utopia.flow.util.NullSafe._

import collection.JavaConverters._
import utopia.access.http.ContentCategory._
import javax.servlet.http.HttpServletResponse
import utopia.nexus.http.Response
import javax.servlet.http.HttpServletRequest
import utopia.nexus.http.Path
import utopia.flow.datastructure.immutable.Model
import utopia.flow.parse.JSONReader
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.StringType

import scala.util.{Success, Try}
import utopia.nexus.http.Request
import utopia.nexus.http.ServerSettings
import utopia.access.http.Method
import utopia.access.http.Headers
import utopia.access.http.Cookie
import utopia.access.http.ContentType
import utopia.nexus.http.StreamedBody
import javax.servlet.http.Part
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URLDecoder
import java.nio.charset.Charset
import java.util

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
                val javaCookie = new javax.servlet.http.Cookie(cookie.name, cookie.value.toJSON)
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
        def toRequest(implicit settings: ServerSettings) = 
        {
            val method = r.getMethod.toOption.flatMap(Method.parse)
            
            if (method.isDefined)
            {
                val path = r.getRequestURI.toOption.flatMap(Path.parse)

                val paramValues = r.getParameterNames.asScala.map { pName => 
                        (pName, parseQueryParam(r.getParameter(pName))) }.flatMap { case (name, value) =>
                    value.toOption.map { name -> _ } }
                val parameters = Model(paramValues.toVector)

                val headers = Headers(r.getHeaderNames.asScala.map { hName => (hName, r.getHeader(hName)) }.toMap)

                val javaCookies = Option(r.getCookies).map { _.toVector }.getOrElse(Vector())
                val cookies = javaCookies.map { javaCookie => Cookie(javaCookie.getName, 
                        javaCookie.getValue.toOption.flatMap { 
                        JSONReader(_).toOption }.getOrElse(Value.emptyWithType(StringType)),
                        if (javaCookie.getMaxAge < 0) None else Some(javaCookie.getMaxAge), 
                        javaCookie.getSecure) }
                
                val body = bodyFromRequest(r, headers).filter { !_.isEmpty }
                /*
                val uploads = Try(r.getParts).toOption.map { _.asScala.flatMap {part => 
                        part.getContentType.toOption.flatMap(ContentType.parse).map { 
                        new FileUpload(part.getName, part.getSize, _, part.getSubmittedFileName, 
                        part.getInputStream, part.write) }}}*/
                
                Some(new Request(method.get, r.getRequestURL.toString, path, parameters, headers, body, cookies))
            }
            else
                None
        }
        
        private def parseQueryParam(paramValue: String)(implicit settings: ServerSettings) =
        {
            val decoded = settings.expectedParameterEncoding match
            {
                case Some(encoding) => Try { URLDecoder.decode(paramValue, encoding.charSet.name()) }
                case None => Success(paramValue)
            }
            decoded.flatMap { JSONReader(_) }
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
            val contentType = part.getContentType.toOption.flatMap(ContentType.parse).getOrElse(Text.plain)
            val charset = headers.charset.getOrElse(Charset.defaultCharset())
            
            new StreamedBody(new BufferedReader(new InputStreamReader(part.getInputStream, charset)), 
                    contentType, Some(part.getSize), headers, 
                    part.getSubmittedFileName.toOption.orElse(part.getName.toOption))
        }
        
        private def parseHeaders(headerNames: util.Collection[String], getValue: String => String) =
                Headers(headerNames.asScala.map(hName => (hName, getValue(hName))).toMap)
    }
}