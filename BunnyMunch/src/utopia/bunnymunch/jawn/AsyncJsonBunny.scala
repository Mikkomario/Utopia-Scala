package utopia.bunnymunch.jawn

import org.typelevel.jawn.AsyncParser
import org.typelevel.jawn.AsyncParser.{Mode, UnwrapArray, ValueStream}
import utopia.flow.async.context.TwoThreadBuffer
import utopia.flow.async.context.TwoThreadBuffer.EmptyInput
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.generic.model.immutable.Value
import utopia.flow.util.logging.SysErrLogger

import java.io.{FileInputStream, InputStream}
import java.nio.file.Path
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

/**
  * An interface for parsing and processing json data asynchronously, without buffering all data to memory at once.
  * @author Mikko Hilpinen
  * @since 12.07.2024, v1.0.5
  */
object AsyncJsonBunny
{
	// ATTRIBUTES   ------------------------
	
	private implicit val facade: ValueFacade.type = ValueFacade
	
	private val defaultBufferSize = 1024
	
	
	// OTHER    ---------------------------
	
	/**
	  * Processes the contents of a json file asynchronously,
	  * calling the specified 'process' function whenever data is read.
	  * Assumes that the file contains a single json value array.
	  *
	  * Please note that the 'process' function blocks the file-reading process.
	  * In case you need to perform extended operations during this process, it might be useful to
	  * either do that asynchronously (taking into account the additional memory requirements),
	  * or to use [[bufferArrayFile]] instead.
	  *
	  * The file is kept open until all data has been read and processed.
	  *
	  * @param path Path to the json file to read / parse
	  * @param bufferSize Number of bytes read from the stream at once. Default = 1024.
	  * @param flatten Whether consecutive json value arrays should be merged together (i.e. flattened).
	  *                Default = false.
	  * @param process A function called whenever new values are read and parsed.
	  * @param exc Implicit execution context
	  * @tparam U Arbitrary function result type
	  * @return A future which resolves into a success or a failure once all data has been read and processed.
	  *         Contains a failure if file-reading failed, if json parsing failed or if 'process' threw an exception.
	  */
	def processArrayFile[U](path: Path, bufferSize: Int = defaultBufferSize, flatten: Boolean = false)
	                       (process: scala.collection.Seq[Value] => U)
	                       (implicit exc: ExecutionContext) =
		processFile(path, UnwrapArray, bufferSize, flatten)(process)
	/**
	  * Processes the contents of a json file asynchronously,
	  * calling the specified 'process' function whenever data is read.
	  *
	  * Please note that the 'process' function blocks the file-reading process.
	  * In case you need to perform extended operations during this process, it might be useful to
	  * either do that asynchronously (taking into account the additional memory requirements),
	  * or to use [[bufferFile]] instead.
	  *
	  * The file is kept open until all data has been read and processed.
	  *
	  * @param path Path to the json file to read / parse
	  * @param mode Mode which determines how read data is interpreted.
	  *             Use [[UnwrapArray]] when asynchronously reading a json array.
	  *             Use [[ValueStream]] when reading 0-n json values, separated by whitespace.
	  * @param bufferSize Number of bytes read from the stream at once. Default = 1024.
	  * @param flattenJsonArrays Whether consecutive json value arrays should be merged together (i.e. flattened).
	  *                          Only applicable when 'mode' = UnwrapArray.
	  *                          Default = false.
	  * @param process A function called whenever new values are read and parsed.
	  * @param exc Implicit execution context
	  * @tparam U Arbitrary function result type
	  * @return A future which resolves into a success or a failure once all data has been read and processed.
	  *         Contains a failure if file-reading failed, if json parsing failed or if 'process' threw an exception.
	  */
	def processFile[U](path: Path, mode: Mode, bufferSize: Int = defaultBufferSize, flattenJsonArrays: Boolean = false)
	                  (process: scala.collection.Seq[Value] => U)
	                  (implicit exc: ExecutionContext) =
	{
		// Opens the file
		Try { new FileInputStream(path.toFile) } match {
			// Case: File successfully opened => Starts reading and processing its contents
			case Success(fileStream) =>
				val resultFuture = this.process(fileStream, mode, bufferSize, flattenJsonArrays)(process)
				// Closes the file only once the processing has completed
				resultFuture.onComplete { _ => Try { fileStream.close() } }
				resultFuture
				
			// Case: Failed to open the file => Returns an immediate failure
			case Failure(error) => Future.successful(Failure(error))
		}
	}
	
	/**
	  * Processes the json contents of an input stream asynchronously,
	  * calling the specified 'process' function whenever new data becomes available.
	  * Assumes that the stream consists of 0-n json values, separated by whitespace.
	  *
	  * Please note that the 'process' function blocks the stream-reading process.
	  * In case you need to perform extended operations during this process, it might be useful to
	  * either do that asynchronously (taking into account the additional memory requirements),
	  * or to use [[buffer]] instead.
	  *
	  * @param stream Stream from which json values will be read
	  * @param bufferSize Number of bytes read from the stream at once. Default = 1024.
	  * @param process A function called whenever new values are read and parsed.
	  * @param exc Implicit execution context
	  * @tparam U Arbitrary function result type
	  * @return A future which resolves into a success or a failure once all streamed data has been processed.
	  *         Contains a failure if stream-reading failed, if json parsing failed or if 'process' threw an exception.
	  */
	def processStreamedValues[U](stream: InputStream, bufferSize: Int = defaultBufferSize)
	                            (process: scala.collection.Seq[Value] => U)
	                            (implicit exc: ExecutionContext) =
		this.process(stream, ValueStream, bufferSize)(process)
	/**
	  * Processes the json array values of an input stream asynchronously,
	  * calling the specified 'process' function whenever new data becomes available.
	  * Assumes that the stream contains a single json value array.
	  *
	  * Please note that the 'process' function blocks the stream-reading process.
	  * In case you need to perform extended operations during this process, it might be useful to
	  * either do that asynchronously (taking into account the additional memory requirements),
	  * or to use [[buffer]] instead.
	  *
	  * @param stream Stream from which json values will be read
	  * @param bufferSize Number of bytes read from the stream at once. Default = 1024.
	  * @param flatten Whether consecutive json value arrays should be merged together (i.e. flattened).
	  *                Default = false.
	  * @param process A function called whenever new values are read and parsed.
	  * @param exc Implicit execution context
	  * @tparam U Arbitrary function result type
	  * @return A future which resolves into a success or a failure once all streamed data has been processed.
	  *         Contains a failure if stream-reading failed, if json parsing failed or if 'process' threw an exception.
	  */
	def processStreamedArray[U](stream: InputStream, bufferSize: Int = defaultBufferSize, flatten: Boolean = false)
	                           (process: scala.collection.Seq[Value] => U)
	                           (implicit exc: ExecutionContext) =
		this.process(stream, UnwrapArray, bufferSize, flatten)(process)
	/**
	  * Processes the json contents of an input stream asynchronously,
	  * calling the specified 'process' function whenever new data becomes available.
	  *
	  * Please note that the 'process' function blocks the stream-reading process.
	  * In case you need to perform extended operations during this process, it might be useful to
	  * either do that asynchronously (taking into account the additional memory requirements),
	  * or to use [[buffer]] instead.
	  *
	  * @param stream Stream from which json values will be read
	  * @param mode Mode which determines how read data is interpreted.
	  *             Use [[UnwrapArray]] when asynchronously reading a json array.
	  *             Use [[ValueStream]] when reading 0-n json values, separated by whitespace.
	  * @param bufferSize Number of bytes read from the stream at once. Default = 1024.
	  * @param flattenJsonArrays Whether consecutive json value arrays should be merged together (i.e. flattened).
	  *                          Only applicable when 'mode' = UnwrapArray.
	  *                          Default = false.
	  * @param process A function called whenever new values are read and parsed.
	  * @param exc Implicit execution context
	  * @tparam U Arbitrary function result type
	  * @return A future which resolves into a success or a failure once all streamed data has been processed.
	  *         Contains a failure if stream-reading failed, if json parsing failed or if 'process' threw an exception.
	  */
	def process[U](stream: InputStream, mode: Mode, bufferSize: Int = defaultBufferSize,
	               flattenJsonArrays: Boolean = false)
	              (process: scala.collection.Seq[Value] => U)
	              (implicit exc: ExecutionContext) =
		Future {
			Try { processStream(stream, AsyncParser(mode, multiValue = flattenJsonArrays), bufferSize)(process) }
				.flatten
		}
	
	/**
	  * Parses the contents of a json file by utilizing an asynchronous buffer.
	  * Assumes the file to contain a single json value array.
	  *
	  * The buffer will be filled automatically as soon as data is read from the file,
	  * but only 'valueBufferSize' parsed values will ever be buffered to memory at once.
	  *
	  * Please note that the returned buffer MUST be consumed in order for the file to be closed.
	  * Failure to process buffered contents may lead to the file becoming inaccessible to other applications.
	  *
	  * @param path Path to the json file to read
	  * @param valueBufferSize Maximum number of values that may be placed in the buffer at once.
	  *                        A higher value may use more memory, but may result in faster processing.
	  *
	  * @param byteBufferSize Number of bytes read from the stream at once. Default = 1024.
	  * @param flatten Whether consecutive json value arrays should be merged together (i.e. flattened).
	  *                Default = false.
	  * @param exc Implicit execution context.
	  * @return Returns 2 values:
	  *             1. An interface for reading buffered json values
	  *             1. A future which resolves into a success or failure,
	  *                once all data has been read or if a read or parsing failure occurs.
	  *
	  *         NOTICE: The returned future will NOT COMPLETE until ALL data from the file has been buffered or read.
	  *                 Since the buffer has a maximum capacity,
	  *                 reading the collected data may be necessary for the returned future to complete
	  *                 and the file to be closed.
	  */
	def bufferArrayFile(path: Path, valueBufferSize: Int, byteBufferSize: Int = defaultBufferSize,
	                    flatten: Boolean = false)
	                   (implicit exc: ExecutionContext) =
		bufferFile(path, UnwrapArray, valueBufferSize, byteBufferSize, flatten)
	/**
	  * Parses the contents of a json file by utilizing an asynchronous buffer.
	  * The buffer will be filled automatically as soon as data is read from the file,
	  * but only 'valueBufferSize' parsed values will ever be buffered to memory at once.
	  *
	  * Please note that the returned buffer MUST be consumed in order for the file to be closed.
	  * Failure to process buffered contents may lead to the file becoming inaccessible to other applications.
	  *
	  * @param path Path to the json file to read
	  * @param mode Mode which determines how read data is interpreted.
	  *             Use [[UnwrapArray]] when asynchronously reading a json array.
	  *             Use [[ValueStream]] when reading 0-n json values, separated by whitespace.
	  *
	  * @param valueBufferSize Maximum number of values that may be placed in the buffer at once.
	  *                        A higher value may use more memory, but may result in faster processing.
	  *
	  * @param byteBufferSize Number of bytes read from the stream at once. Default = 1024.
	  * @param flattenJsonArrays Whether consecutive json value arrays should be merged together (i.e. flattened).
	  *                          Only applicable when 'mode' = UnwrapArray.
	  *                          Default = false.
	  * @param exc Implicit execution context.
	  * @return Returns 2 values:
	  *             1. An interface for reading buffered json values
	  *             1. A future which resolves into a success or failure,
	  *                once all data has been read or if a read or parsing failure occurs.
	  *
	  *         NOTICE: The returned future will NOT COMPLETE until ALL data from the file has been buffered or read.
	  *                 Since the buffer has a maximum capacity,
	  *                 reading the collected data may be necessary for the returned future to complete
	  *                 and the file to be closed.
	  */
	def bufferFile(path: Path, mode: Mode, valueBufferSize: Int, byteBufferSize: Int = defaultBufferSize,
	               flattenJsonArrays: Boolean = false)
	              (implicit exc: ExecutionContext) =
	{
		// Opens the targeted file (may fail)
		Try { new FileInputStream(path.toFile) } match {
			// Case: Successfully opened the file => Processes its contents asynchronously using a buffer
			case Success(fileStream) =>
				val (buffer, parseFuture) = this.buffer(fileStream, mode, valueBufferSize, byteBufferSize,
					flattenJsonArrays)
				// Keeps the file open until buffering has completed.
				// Once all data has been read, closes the file (ignores failures)
				parseFuture.onComplete { _ => Try { fileStream.close() } }
			
				buffer -> parseFuture
			
			// Case: File-opening failed
			//       => Wraps the failure into the expected return types (no data + immediate failure)
			case Failure(error) => EmptyInput -> Future.successful(Failure(error))
		}
	}
	
	/**
	  * Processes streamed json values by utilizing an asynchronous buffer.
	  * Expects the consecutive values to be separated with whitespace.
	  *
	  * The buffer will be filled automatically as soon as data is received, but only 'valueBufferSize' parsed values
	  * will ever be buffered to memory at once.
	  *
	  * Please note that the returned buffer MUST be consumed in order for the stream to be fully read.
	  * Please keep the stream open until all data has been processed.
	  *
	  * @param stream Json data stream to read
	  * @param valueBufferSize Maximum number of values that may be placed in the buffer at once.
	  *                        A higher value may use more memory, but may result in faster processing.
	  *
	  * @param byteBufferSize Number of bytes read from the stream at once. Default = 1024.
	  * @param exc Implicit execution context.
	  * @return Returns 2 values:
	  *             1. An interface for reading buffered json values
	  *             1. A future which resolves into a success or failure,
	  *                once all data has been read or if a read or parsing failure occurs.
	  *
	  *         NOTICE: The returned future will NOT COMPLETE until ALL data from the stream has been buffered or read.
	  *                 Since the buffer has a maximum capacity,
	  *                 reading the collected data may be necessary for the returned future to complete.
	  *
	  *                 It is advisable not to close the 'stream' until this future resolves.
	  */
	def bufferStreamedValues(stream: InputStream, valueBufferSize: Int, byteBufferSize: Int = defaultBufferSize)
	                        (implicit exc: ExecutionContext) =
		buffer(stream, ValueStream, valueBufferSize, byteBufferSize)
	/**
	  * Processes streamed json value array data by utilizing an asynchronous buffer.
	  * The buffer will be filled automatically as soon as data is received, but only 'valueBufferSize' parsed values
	  * will ever be buffered to memory at once.
	  *
	  * Please note that the returned buffer MUST be consumed in order for the stream to be fully read.
	  * Please keep the stream open until all data has been processed.
	  *
	  * @param stream Json data stream to read
	  * @param valueBufferSize Maximum number of values that may be placed in the buffer at once.
	  *                        A higher value may use more memory, but may result in faster processing.
	  *
	  * @param byteBufferSize Number of bytes read from the stream at once. Default = 1024.
	  * @param flatten Whether consecutive json value arrays should be merged together (i.e. flattened).
	  *                Default = false.
	  * @param exc Implicit execution context.
	  * @return Returns 2 values:
	  *             1. An interface for reading buffered json values
	  *             1. A future which resolves into a success or failure,
	  *                once all data has been read or if a read or parsing failure occurs.
	  *
	  *         NOTICE: The returned future will NOT COMPLETE until ALL data from the stream has been buffered or read.
	  *                 Since the buffer has a maximum capacity,
	  *                 reading the collected data may be necessary for the returned future to complete.
	  *
	  *                 It is advisable not to close the 'stream' until this future resolves.
	  */
	def bufferStreamedArray(stream: InputStream, valueBufferSize: Int, byteBufferSize: Int = defaultBufferSize,
	                        flatten: Boolean = false)
	                        (implicit exc: ExecutionContext) =
		buffer(stream, UnwrapArray, valueBufferSize, byteBufferSize, flatten)
	/**
	  * Processes streamed json data by utilizing an asynchronous buffer.
	  * The buffer will be filled automatically as soon as data is received, but only 'valueBufferSize' parsed values
	  * will ever be buffered to memory at once.
	  *
	  * Please note that the returned buffer MUST be consumed in order for the stream to be fully read.
	  * Please keep the stream open until all data has been processed.
	  *
	  * @param stream Json data stream to read
	  * @param mode Mode which determines how read data is interpreted.
	  *             Use [[UnwrapArray]] when asynchronously reading a json array.
	  *             Use [[ValueStream]] when reading 0-n json values, separated by whitespace.
	  *
	  * @param valueBufferSize Maximum number of values that may be placed in the buffer at once.
	  *                        A higher value may use more memory, but may result in faster processing.
	  *
	  * @param byteBufferSize Number of bytes read from the stream at once. Default = 1024.
	  * @param flattenJsonArrays Whether consecutive json value arrays should be merged together (i.e. flattened).
	  *                          Only applicable when 'mode' = UnwrapArray.
	  *                          Default = false.
	  * @param exc Implicit execution context.
	  * @return Returns 2 values:
	  *             1. An interface for reading buffered json values
	  *             1. A future which resolves into a success or failure,
	  *                once all data has been read or if a read or parsing failure occurs.
	  *
	  *         NOTICE: The returned future will NOT COMPLETE until ALL data from the stream has been buffered or read.
	  *                 Since the buffer has a maximum capacity,
	  *                 reading the collected data may be necessary for the returned future to complete.
	  *
	  *                 It is advisable not to close the 'stream' until this future resolves.
	  */
	def buffer(stream: InputStream, mode: Mode, valueBufferSize: Int, byteBufferSize: Int = defaultBufferSize,
	           flattenJsonArrays: Boolean = false)
	          (implicit exc: ExecutionContext) =
	{
		val parser = AsyncParser[Value](mode, multiValue = flattenJsonArrays)
		// Uses an asynchronous thread to push values into the buffer
		val buffer = SysErrLogger.use { implicit log => new TwoThreadBuffer[Value](valueBufferSize) }
		val finalFuture = Future {
			// Keeps pushing the read values to the buffer as long as there are some available
			val result = processStream(stream, parser, byteBufferSize)(buffer.output.push).flatMap { _ =>
				// Once all bytes have been accepted by the parser, makes sure all data has been parsed / processed.
				parser.finish() match {
					case Right(finalValues) =>
						buffer.output.push(finalValues)
						Success(())
					case Left(error) => Failure(error)
				}
			}
			// Declares all data as read
			buffer.output.close()
			result
		}
		// Provides the input interface for reading the values (which is required for the stream reading to complete)
		buffer.input -> finalFuture
	}
	
	private def processStream[U](stream: InputStream, parser: AsyncParser[Value], bufferSize: Int)
	                            (receive: scala.collection.Seq[Value] => U) =
	{
		// Buffers the input stream
		val buffer = new Array[Byte](bufferSize)
		// As long as there is more data to read and no errors, continues to process the buffered bytes using the parser
		Iterator
			.continually {
				// Attempts to read the next chunk
				Try { stream.read(buffer, 0, bufferSize) }.flatMap { bytesRead =>
					// Case: End of file => Terminates
					if (bytesRead == -1)
						Success(false)
					// Case: Read more => passes the read data to the parser to absorb
					else
						parser.absorb(buffer) match {
							// Case: Parsing succeeded => Passes the generated values forward and continues reading
							case Right(values) =>
								receive(values)
								Success(true)
							// Case: Parsing failed => Terminates
							case Left(error) => Failure(error)
						}
				}
			}
			.takeTo {
				// Case: Successful iteration => Continues unless the end of file is reached
				case Success(hasMore) => !hasMore
				// Case: Failure during reading or parsing => terminates
				case Failure(_) => true
			}
			.last.map { _ => () }
	}
}
