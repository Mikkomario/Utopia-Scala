package utopia.echo.model.request.vastai

import utopia.access.model.enumeration.Method
import utopia.access.model.enumeration.Method.Put
import utopia.annex.controller.ApiClient
import utopia.annex.model.request.ApiRequest
import utopia.annex.model.response.RequestResult
import utopia.disciple.model.error.RequestFailedException
import utopia.disciple.model.request.Body
import utopia.echo.model.request.vastai.AcceptOffer.AcceptOfferResponseParser
import utopia.echo.model.vastai.instance.offer.RunType
import RunType.Ssh
import utopia.flow.collection.immutable.Empty
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.immutable.{Constant, Model, Value}
import utopia.flow.generic.model.template.HasPropertiesLike.HasProperties
import utopia.flow.util.{NotEmpty, UncertainBoolean}
import utopia.flow.util.StringExtensions._
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.eventful.AlwaysFalse

import scala.concurrent.Future
import scala.util.{Failure, Try}

object AcceptOffer
{
	// NESTED   -------------------------
	
	private object AcceptOfferResponseParser extends FromModelFactory[Int]
	{
		override def apply(model: HasProperties): Try[Int] = {
			if (model("success").booleanOr(true))
				model.tryGet("new_contract") { _.tryInt }
			else
				Failure(new RequestFailedException(s"Instance creation failed: Response body: $model"))
		}
	}
}

/**
 * A request used for accepting an offer and creating an instance / contract.
 * Yields an instance ID on success.
 * @param offerId ID of the offer to accept
 * @param templateHashId Content-based hash ID of a template to use as base configuration.
 *
 *                       Template values are merged with or overridden by request parameters.
 *                       When using a template, the 'image' field is optional as the template provides it.
 * @param image Docker image to use for the instance. Optional only if 'templateHashId' is specified.
 *              E.g. "vastai/base-image:@vastai-automatic-tag"
 * @param runType Launch mode for the instance. Default = SSH.
 * @param env Environment variables and port mappings to apply.
 *            When using a template, request env is merged with template env -
 *            existing keys are retained, new keys are appended, conflicting keys use the request value.
 * @param label Custom name for the instance (optional)
 * @param bidPrice Bid price per machine (in $/hour). Only for interruptible instances.
 * @param args Arguments to pass to the image entrypoint. Specified either as a string (Left) or as an array (Right).
 * @param startCommands Commands to run when instance starts.
 *                      E.g. ["env | grep _ >> /etc/environment", "echo 'starting up'"]
 * @param imageCredentials Docker registry credentials, if needed
 * @param jupyterDirectory Directory to launch Jupyter from. E.g. "/home/notebooks".
 * @param cancelIfUnavailable Whether to cancel if instance cannot start immediately.
 *                            Defaults to false for interruptibles.
 *                            Defaults to true for on-demand with target_state='running'
 * @param isVirtualMachine Whether this is a VM instance.
 * @param useUtf8 Whether to set locale to C.UTF-8
 * @param useUtf8InPython Whether to set python's locale to C.UTF-8
 * @param useJupyterLab Whether to launch instance with jupyter lab instead of notebook
 * @param targetStopped Whether the desired initial state of the instance is "stopped". Default = false.
 * @author Mikko Hilpinen
 * @since 24.02.2026, v1.5
 */
case class AcceptOffer(offerId: Int, templateHashId: String, image: String = "", runType: RunType = Ssh,
                       env: Model = Model.empty, label: String = "", bidPrice: Option[Double] = None,
                       args: Either[String, Seq[String]] = Right(Empty), startCommands: Seq[String] = Empty,
                       imageCredentials: String = "", jupyterDirectory: String = "",
                       deprecatedView: View[Boolean] = AlwaysFalse,
                       cancelIfUnavailable: UncertainBoolean = UncertainBoolean,
                       isVirtualMachine: UncertainBoolean = UncertainBoolean,
                       useUtf8: UncertainBoolean = UncertainBoolean,
                       useUtf8InPython: UncertainBoolean = UncertainBoolean,
                       useJupyterLab: UncertainBoolean = UncertainBoolean, targetStopped: Boolean = false)
	extends ApiRequest[Int]
{
	// ATTRIBUTES   -------------------------
	
	override val method: Method = Put
	override val path: String = s"asks/$offerId"
	override val pathParams: Model = Model.empty
	
	
	// IMPLEMENTED  -------------------------
	
	override def body: Either[Value, Body] = {
		val argsProp = args match {
			case Left(args) => args.ifNotEmpty.map { Constant("args_str", _) }
			case Right(args) => NotEmpty(args).map { Constant("args", _) }
		}
		Left(Model
			.from(
				"image" -> image, "template_hash_id" -> templateHashId, "label" -> label, "runtype" -> runType.key,
				"target_state" -> (if (targetStopped) "stopped" else "running"),
				"price" -> bidPrice.map { p => (p max 0.001) min 128 }, "env" -> env,
				"cancel_unavail" -> cancelIfUnavailable, "vm" -> isVirtualMachine,
				"onstart" -> startCommands.mkString("; "), "use_jupyter_lab" -> useJupyterLab,
				"jupyter_dir" -> jupyterDirectory, "python_utf8" -> useUtf8InPython, "lang_utf8" -> useUtf8,
				"image_login" -> imageCredentials
			)
			.withoutEmptyValues ++ argsProp)
	}
	
	override def deprecated: Boolean = deprecatedView.value
	
	override def send(prepared: ApiClient.PreparedRequest): Future[RequestResult[Int]] =
		prepared.getOne(AcceptOfferResponseParser)
}