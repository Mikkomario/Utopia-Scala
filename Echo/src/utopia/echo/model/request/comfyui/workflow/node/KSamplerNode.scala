package utopia.echo.model.request.comfyui.workflow.node

import utopia.echo.model.enumeration.comfyui.NodeClass
import utopia.echo.model.enumeration.comfyui.NodeClass.KSampler
import utopia.echo.model.request.comfyui.Seed
import utopia.echo.model.request.comfyui.workflow.{OutputRef, SamplerSettings}
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Model

/**
 * Used for denoising a latent image based on a positive and a negative conditioning / prompt.
 *
 * @param name Name of this node
 * @param model Reference to the model used for denoising the input latent.
 * @param positive Reference to the conditioning describing the attributes you want to include in the image.
 * @param negative Reference to the conditioning describing the attributes you want to exclude from the image.
 * @param latentInput Reference to the latent image to denoise.
 * @param seed The random seed used for creating the noise.
 * @param settings Settings for configuring this sampler
 * @author Mikko Hilpinen
 * @since 04.08.2025, v1.4
 */
case class KSamplerNode(name: String, model: OutputRef, positive: OutputRef, negative: OutputRef,
                        latentInput: OutputRef)
                       (implicit settings: SamplerSettings, seed: Seed)
	extends WorkflowNode
{
	// ATTRIBUTES   -----------------------
	
	override val classType: NodeClass = KSampler
	
	/**
	 * The denoised latent.
	 */
	lazy val latentOutput = output
	
	
	// IMPLEMENTED  ----------------------
	
	override def input: Model = Model.from(
		"model" -> model, "seed" -> seed.next(), "steps" -> settings.steps, "cfg" -> settings.cfg,
		"sampler_name" -> settings.sampler, "scheduler" -> settings.scheduler, "positive" -> positive,
		"negative" -> negative, "latent_image" -> latentInput, "denoise" -> settings.denoiseRatio)
}
