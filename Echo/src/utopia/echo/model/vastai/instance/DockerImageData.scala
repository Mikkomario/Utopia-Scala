package utopia.echo.model.vastai.instance

import utopia.echo.model.vastai.instance.offer.RunType
import RunType.Ssh
import utopia.flow.collection.immutable.Empty
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.immutable.ModelDeclaration
import utopia.flow.generic.model.mutable.DataType.StringType
import utopia.flow.generic.model.template.HasPropertiesLike.HasProperties

import scala.util.Try

object DockerImageData extends FromModelFactory[DockerImageData]
{
	// ATTRIBUTES   ----------------------
	
	private val schema = ModelDeclaration("image_uuid" -> StringType, "image_runtype" -> StringType)
	
	
	// IMPLEMENTED  ----------------------
	
	override def apply(model: HasProperties): Try[DockerImageData] = schema.validate(model).flatMap { model =>
		RunType.forKey(model("image_runtype").getString).map { runType =>
			apply(model("image_uuid").getString, runType, model("image_args").getVector.map { _.getString })
		}
	}
}

/**
 * Contains basic information about a docker image used in instance-creation
 * @param uuid ID of this image
 * @param runType How the container is launched (ssh, jupyter, etc.)
 * @param args Arguments passed to the container
 * @author Mikko Hilpinen
 * @since 24.02.2026, v1.5
 */
case class DockerImageData(uuid: String, runType: RunType = Ssh, args: Seq[String] = Empty)
