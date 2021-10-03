package utopia.flow.util

import CollectionExtensions._
import FileExtensions._
import utopia.flow.datastructure.mutable.PollableOnce

import java.nio.file.Path
import scala.util.{Failure, Success, Try}

/**
  * An iterator that recursively goes through all the files within a directory and its sub-directories
  * @author Mikko Hilpinen
  * @since 11.9.2021, v1.11.2
  */
class RecursiveDirectoryIterator(origin: Path) extends Iterator[Try[Path]]
{
	// ATTRIBUTES   --------------------------
	
	private val originContainer = PollableOnce(origin)
	private lazy val cachedChildren = origin.children match
	{
		case Success(children) =>
			// Stops iterating on a failure
			Right(children.iterator.flatMap { new RecursiveDirectoryIterator(_) }.takeTo { _.isFailure })
		case Failure(error) => Left(PollableOnce(error))
	}
	
	
	// IMPLEMENTED  -------------------------
	
	override def hasNext = originContainer.nonConsumed || (cachedChildren match {
		case Right(iterator) => iterator.hasNext
		case Left(error) => error.nonConsumed
	})
	
	override def next() = originContainer.poll() match {
		case Some(origin) => Success(origin)
		case None =>
			cachedChildren match
			{
				case Right(iterator) => iterator.next()
				case Left(error) => Failure(error.get())
			}
	}
}
