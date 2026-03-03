package utopia.echo.model.vastai.instance

import utopia.flow.collection.immutable.Empty
import utopia.flow.generic.model.immutable.Model

/**
 * Specifies what image or template should be used in Vast AI instance-creation
 * @param imageOrTemplate Either name of the used image, such as "vastai/base-image:@vastai-automatic-tag",
 *                        or the hash ID of the used Vast AI template.
 * @param env Environment variables and port mappings to apply.
 *            When using a template, request env is merged with template env -
 *            existing keys are retained, new keys are appended, conflicting keys use the request value.
 * @param startCommand Command to run when the instance starts.
 *                     E.g. "env | grep _ >> /etc/environment", "echo 'starting up'"
 * @param args Arguments to pass to the image entrypoint. Specified either as a string (Left) or as an array (Right).
 * @param dockerCredentials Docker registry credentials, if needed
 * @param isTemplate True if 'imageOrTemplate' refers to a template hash ID. False if an image is referenced (default).
 * @author Mikko Hilpinen
 * @since 03.03.2026, v1.5
 */
case class NewInstanceFoundation(imageOrTemplate: String, env: Model = Model.empty, startCommand: String = "",
                                 args: Either[String, Seq[String]] = Right(Empty),
                                 dockerCredentials: String = "", isTemplate: Boolean = false)
{
	def image = if (isTemplate) "" else imageOrTemplate
	def templateHashId = if (isTemplate) imageOrTemplate else ""
}