package utopia.vault.coder.model.data

import utopia.flow.collection.value.typeless.Model
import utopia.flow.datastructure.immutable.ModelDeclaration
import utopia.flow.generic.{FromModelFactoryWithSchema, ModelConvertible, StringType}
import utopia.flow.generic.ValueConversions._
import utopia.flow.util.CollectionExtensions._
import utopia.flow.util.FileExtensions._

import java.nio.file.Path

object ProjectPaths extends FromModelFactoryWithSchema[ProjectPaths]
{
	override val schema = ModelDeclaration("models" -> StringType, "output" -> StringType, "src" -> StringType)
	
	override protected def fromValidatedModel(model: Model) = {
		val root = model("root").string.map { s => s: Path }
		def path(name: String) = {
			val str = model(name).getString
			root match {
				case Some(root) => root/str
				case None => str: Path
			}
		}
		apply(path("models"), path("output"), path("src"),
			model("alt_src").string.map { str =>
				root match {
					case Some(root) => root/str
					case None => str: Path
				}
			})
	}
	
	/**
	  * @param root The project directory
	  * @return Project paths within that directory, assuming default setup
	  */
	def apply(root: Path): ProjectPaths = apply(root/"data/models", root/"data/coder-build", root/"src")
}

/**
  * Contains references to paths that are used for reading project files
  * @author Mikko Hilpinen
  * @since 29.6.2022, v1.5.1
  */
case class ProjectPaths(modelsDirectory: Path, outputDirectory: Path, src: Path, altSrc: Option[Path] = None)
	extends ModelConvertible
{
	override def toModel = {
		val (common, models, other) = modelsDirectory.commonParentWith(Vector(outputDirectory, src) ++ altSrc)
		Model(Vector("root" -> common.map { _.toString }, "models" -> models.toString, "output" -> other.head.toString,
			"src" -> other(1).toString, "alt_src" -> other.getOption(2).map { _.toString }))
	}
}
