package utopia.echo.model.vastai.instance

import utopia.echo.model.vastai.instance.offer.RunType
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.immutable.ModelDeclaration
import utopia.flow.generic.model.mutable.DataType.StringType
import utopia.flow.generic.model.template.HasPropertiesLike.HasProperties

import scala.util.Try

object ImageDetails extends FromModelFactory[ImageDetails]
{
	// ATTRIBUTES   ---------------------
	
	private val schema = ModelDeclaration("image_uuid" -> StringType, "image_runtype" -> StringType)
	
	
	// IMPLEMENTED  ---------------------
	
	override def apply(model: HasProperties): Try[ImageDetails] = schema.validate(model).flatMap { model =>
		RunType.forKey(model("image_runtype").getString).map { runType =>
			apply(model("image_uuid").getString, model("image_args").getVector.map { _.getString }, runType)
		}
	}
}

/**
 * Contains information about an image applied to a Vast AI instance
 * @param name Name / UUID of the applied docker image
 * @param args Arguments passed to the container
 * @param runType Applied run-type
 * @author Mikko Hilpinen
 * @since 04.10.2026, v1.6
 */
case class ImageDetails(name: String, args: Seq[String], runType: RunType)