package utopia.echo.controller.vastai

import utopia.echo.model.unit.ByteCountExtensions._
import utopia.echo.controller.client.VastAiApiClient
import utopia.echo.model.enumeration.ServiceState
import utopia.echo.model.unit.ByteCount
import utopia.echo.model.vastai.instance.offer.Offer
import utopia.echo.model.vastai.instance.{NewInstanceFoundation, VastAiInstance}
import utopia.echo.model.vastai.settings.VastAiServiceSettings
import utopia.echo.model.vastai.settings.VastAiServiceSettings.VastAiServiceSettingsWrapper
import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.context.Scheduler
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.mutable.Settable
import utopia.flow.view.template.eventful.{Changing, Flag}

import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions
import scala.util.{Failure, Success}

object VastAiSshProcess
{
	// ATTRIBUTES   ------------------------
	
	/**
	 * A factory used for constructing new Vast AI SSH processes. Applies default settings.
	 */
	val factory = VastAiSshProcessFactory()
	
	
	// IMPLICIT ----------------------------
	
	implicit def objectAsFactory(o: VastAiSshProcess.type): VastAiSshProcessFactory = o.factory
	
	
	// NESTED   ----------------------------
	
	case class VastAiSshProcessFactory(selectOffer: SelectOffer = SelectOffer.cheapestFirst.head(),
	                                   requiredDiskSpace: ByteCount = 5.gib,
	                                   override val serviceSettings: VastAiServiceSettings = VastAiServiceSettings.default)
		extends VastAiServiceSettingsWrapper[VastAiSshProcessFactory]
	{
		// IMPLEMENTED  --------------------
		
		override def withSettings(settings: VastAiServiceSettings): VastAiSshProcessFactory =
			copy(serviceSettings = settings)
			
		
		// OTHER    ------------------------
		
		/**
		 * @param select Select offer logic to use
		 * @return Copy of this factory using the specified select offer logic
		 */
		def selectingOfferUsing(select: SelectOffer) = copy(selectOffer = select)
		/**
		 * @param requiredSpace Disk space to require
		 * @return Copy of this factory requiring the specified amount of disk space
		 */
		def requiringDiskSpace(requiredSpace: ByteCount) =
			copy(requiredDiskSpace = requiredSpace)
		
		/**
		 * Creates a new Vast AI SSH process
		 * @param chooseImage A function that receives the accepted offer and specifies the image/template to use
		 * @param exc Implicit execution context
		 * @param scheduler Implicit scheduler interface
		 * @param log Implicit logging interface
		 * @param vastAiClient Implicit Vast AI client to use
		 * @return A new Vast AI SSH process. Not started yet.
		 */
		def apply(chooseImage: Offer => NewInstanceFoundation)
		         (implicit exc: ExecutionContext, scheduler: Scheduler, log: Logger, vastAiClient: VastAiApiClient) =
			new VastAiSshProcess(selectOffer, requiredDiskSpace, serviceSettings)(chooseImage)
	}
}

/**
 * A process that acquires a Vast AI instance and provides SSH connection to it.
 * May be used for debugging / troubleshooting, for example.
 * @author Mikko Hilpinen
 * @since 19.11.2026, v1.6
 */
class VastAiSshProcess(selectOffer: SelectOffer, requiredDiskSpace: ByteCount, settings: VastAiServiceSettings)
                      (_chooseImage: Offer => NewInstanceFoundation)
                      (implicit exc: ExecutionContext, scheduler: Scheduler, log: Logger, vastAiClient: VastAiApiClient)
	extends VastAiServiceProcess[SshExecutor, SshExecutor, Unit](selectOffer, requiredDiskSpace, settings)
{
	// IMPLEMENTED  ------------------------
	
	override protected def chooseImage(acceptedOffer: Offer): (NewInstanceFoundation, ServiceState) =
		_chooseImage(acceptedOffer) -> ServiceState.Running
	override protected def onInstanceReuse(instance: VastAiInstance): ServiceState = ServiceState.Running
	
	override protected def initializeService(ssh: SshExecutor): (SshExecutor, String) = ssh -> ""
	override protected def attemptServiceConnection(service: SshExecutor, deprecationView: View[Boolean]): Future[Either[Boolean, Unit]] =
		service("ls").runAsync(captureStdOutLines = 24).future.map {
			case Success(_) => Right(())
			case Failure(error) =>
				log(error, "Failed to SSH to the instance")
				Left(false)
		}
	
	override protected def initializeClient(service: SshExecutor, setupResult: Unit, stopFlag: Flag): SshExecutor =
		service
	
	override protected def monitorRequestTimeouts(client: SshExecutor, service: SshExecutor, timedOutFlag: Settable,
	                                              continueView: View[Boolean]): Unit = ()
	
	override protected def stopService(service: SshExecutor, client: Option[SshExecutor]): (Future[Any], Changing[Int]) =
		Future.unit -> Fixed(0)
}
