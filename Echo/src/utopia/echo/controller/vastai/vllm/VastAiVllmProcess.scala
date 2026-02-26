package utopia.echo.controller.vastai.vllm

import utopia.annex.util.RequestResultExtensions._
import utopia.echo.controller.client.VastAiApiClient
import utopia.echo.controller.vastai.VastAiProcess
import utopia.echo.model.llm.LlmDesignator
import utopia.echo.model.request.vastai.{AcceptOffer, GetOffers}
import utopia.echo.model.unit.ByteCountExtensions._
import utopia.echo.model.unit.ByteCount
import utopia.echo.model.vastai.instance.offer.OfferType.OnDemand
import utopia.echo.model.vastai.instance.offer.RunType.DirectSsh
import utopia.echo.model.vastai.instance.offer.{Offer, OfferProperty, OfferType, SearchFilter}
import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.process.{Delay, Process}
import utopia.flow.async.process.ShutdownReaction.SkipDelay
import utopia.flow.collection.immutable.Empty
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Model
import utopia.flow.operator.sign.Sign
import utopia.flow.time.{Duration, Now}
import utopia.flow.util.logging.Logger
import utopia.flow.util.result.TryExtensions._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

/**
 * A process for setting up and managing a vLLM server on a rented Vast AI instance
 * @author Mikko Hilpinen
 * @since 26.02.2026, v1.5
 */
// Template doc: https://cloud.vast.ai/template/readme/728eda674fd3c3810d227c9668d899bd
// Use the full model path (e.g., meta-llama/Llama-3.1-8B-Instruct)
class VastAiVllmProcess(vllmTemplateHashId: String, modelSize: ByteCount, additionalReservedDisk: ByteCount = 8.gb,
                        offerSearchFilters: Seq[SearchFilter] = Empty,
                        offerOrdering: Seq[(OfferProperty[_], Sign)] = Empty,
                        offerLimit: Option[Int] = None, offerType: OfferType = OnDemand, maxContextSize: Int = 8192,
                        gpuUtilizationRate: Double = 0.8, setupTimeout: Duration = Duration.infinite,
                        instanceLabel: String = "", imageCredentials: String = "")
                       (selectOffer: Seq[Offer] => Future[Try[Offer]])
                       (implicit exc: ExecutionContext, log: Logger, client: VastAiApiClient, llm: LlmDesignator)
	extends Process(shutdownReaction = Some(SkipDelay))
{
	// ATTRIBUTES   ------------------------
	
	override protected val isRestartable: Boolean = false
	
	private val vastAiProcess = VastAiProcess() { hurryFlag =>
		// Requests for offers
		client.send(GetOffers(modelSize + additionalReservedDisk,
				offerSearchFilters, offerOrdering, offerLimit, offerType))
			// Selects one offer
			.tryFlatMap(selectOffer)
			.flatMapOrFail { offer =>
				// Accepts the offer, requesting a new instance
				// Assumes that vLLM template (or similar) is used
				val quantizationParam = {
					if (llm.llmName.contains("AWQ"))
						" --quantization awq_marlin"
					else
						""
				}
				client.send(AcceptOffer(offer.id, vllmTemplateHashId, runType = DirectSsh,
					env = Model.from(
						"VLLM_MODEL" -> llm.llmName,
						"VLLM_ARGS" -> s"--max-model-len $maxContextSize --gpu-memory-utilization $gpuUtilizationRate$quantizationParam"
					),
					label = instanceLabel, imageCredentials = imageCredentials, deprecatedView = hurryFlag,
					cancelIfUnavailable = true))
			}
			.toTryFuture
	}
	
	
	// IMPLEMENTED  ----------------------
	
	override protected def runOnce(): Unit = {
		val startTime = Now.toInstant
		
		// Sets up a Vast AI instance and waits for it to load (or for timeout or stop() call)
		vastAiProcess.runAsync()
		val instanceLoadedFuture = vastAiProcess.liveInstanceFuture.flatMap {
			case Success(instance) => instance.loadedFuture
			case Failure(_) =>
				// TODO: Capture or log the error, or change state, or something
				Future.successful(false)
		}
		val loadedOrTimedOutFuture = {
			if (setupTimeout.isFinite)
				instanceLoadedFuture.raceWith(Delay(setupTimeout)(false))
			else
				instanceLoadedFuture
		}
		val stopFuture = hurryFlag.future
		// Case: Instance successfully loaded => Sets up port-forwarding in order to access the vLLM API
		if (loadedOrTimedOutFuture.raceWith(stopFuture.map { _ => false }).waitFor()
			.logWithMessage("Failure while waiting for instance to load, timeout or stop").getOrElse(false))
		{
			// TODO: Set up SSH port forwarding
			
			// Waits until GET /models yields successful results
			// TODO: Implement
			???
		}
		
		// Destroys the Vast AI instance
		vastAiProcess.stop().waitFor().logWithMessage("Failure while waiting for the Vast AI instance to be destroyed")
	}
	
	
	// OTHER    ---------------------
	
	private def waitUntilGetModelsSucceeds() = {
		// TODO: Implement
	}
}
