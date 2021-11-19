package utopia.vault.coder.main

import utopia.bunnymunch.jawn.JsonBunny
import utopia.flow.datastructure.immutable.Model
import utopia.flow.util.FileExtensions._

import java.nio.file.Path
import scala.util.{Failure, Success}

/**
  * Used for accessing user-specified root paths based on their alias. The paths are defined in a separate config file.
  * @author Mikko Hilpinen
  * @since 19.11.2021, v1.4
  */
object Roots
{
	// ATTRIBUTES   -------------------------------
	
	private lazy val path: Path = "config.json"
	private lazy val model = {
		if (path.notExists)
			Model.empty
		else
			JsonBunny.munchPath(path) match {
				case Success(readVal) => readVal("roots").getModel
				case Failure(error) =>
					println(s"Warning: Failure while reading configurations from $path: ${error.getMessage}")
					Model.empty
			}
	}
	
	
	// OTHER    ----------------------------------
	
	/**
	  * @param alias Alias of the targeted root path
	  * @return A root path registered with that alias. None if no such path has been registered.
	  */
	def apply(alias: String) = model(alias).string.map { p => p: Path }
}
