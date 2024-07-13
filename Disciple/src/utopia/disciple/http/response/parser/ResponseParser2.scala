package utopia.disciple.http.response.parser

import utopia.access.http.{Headers, Status}

import java.io.InputStream
import scala.concurrent.{ExecutionContext, Future}

object ResponseParser2
{
	// OTHER    ----------------------
	
	def apply[I, F](f: (Status, Headers, Option[InputStream]) => (I, Future[F])): ResponseParser2[I, F] =
		new _ResponseParser(f)
		
	def blocking[A](f: (Status, Headers, Option[InputStream]) => A) =
		apply { (status, headers, stream) =>
			val res = f(status, headers, stream)
			res -> Future.successful(res)
		}
	
	
	// NESTED   ----------------------
	
	private class _ResponseParser[+I, +F](f: (Status, Headers, Option[InputStream]) => (I, Future[F]))
		extends ResponseParser2[I, F]
	{
		override def apply(status: Status, headers: Headers, stream: Option[InputStream]) =
			f(status, headers, stream)
	}
}

/**
  * An interface for processing streamed responses
  * @tparam I Type of immediately available results
  * @tparam F Type of the final (asynchronous) results
  * @author Mikko Hilpinen
  * @since 12.07.2024, v1.7
  */
trait ResponseParser2[+I, +F]
{
	// ABSTRACT ----------------------
	
	/**
	  * Processes a response, parsing its contents.
	  * The parsing may be completed asynchronously or synchronously (i.e. blocking).
	  * @param status Response status
	  * @param headers Response headers
	  * @param stream Response body as a stream. None if the response was empty.
	  * @return 1) Immediately available result, and 2) a Future which resolves into the final result.
	  */
	def apply(status: Status, headers: Headers, stream: Option[InputStream] = None): (I, Future[F])
	
	
	// OTHER    ---------------------
	
	/**
	  * Processes a response, parsing its contents.
	  * The parsing may be completed asynchronously or synchronously (i.e. blocking).
	  * @param status Response status
	  * @param headers Response headers
	  * @param stream Response body as a stream
	  * @return 1) Immediately available result, and 2) a Future which resolves into the final result.
	  */
	def apply(status: Status, headers: Headers, stream: InputStream): (I, Future[F]) =
		apply(status, headers, Some(stream))
}
