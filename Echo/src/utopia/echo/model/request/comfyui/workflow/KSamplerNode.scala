package utopia.echo.model.request.comfyui.workflow

import utopia.echo.model.enumeration.comfyui.NodeClass
import utopia.echo.model.enumeration.comfyui.NodeClass.KSampler
import utopia.flow.generic.model.immutable.Model
import utopia.flow.generic.casting.ValueConversions._

import scala.util.Random

/**
 * Used for denoising a latent image based on a positive and a negative conditioning / prompt.
 *
 * @param name Name of this node
 * @param model Reference to the model used for denoising the input latent.
 * @param positive Reference to the conditioning describing the attributes you want to include in the image.
 * @param negative Reference to the conditioning describing the attributes you want to exclude from the image.
 * @param latentImage Reference to the latent image to denoise.
 * @param sampler The name of the algorithm used when sampling,
 *                this can affect the quality, speed, and style of the generated output.
 *
 *                See [[http://localhost:8188/object_info/KSampler]] for a full list of available values.
 * @param scheduler The scheduler controls how noise is gradually removed to form the image.
 *
 *                  Valid options are (at 4.8.2025): [
 *                  "normal", "karras", "exponential", "sgm_uniform", "simple", "ddim_uniform",
 *                  "beta", "linear_quadratic", "kl_optimal"
 *                  ]
 *
 * @param steps The number of steps used in the denoising process. Default = 20.
 *              For stable diffusion 1.5, 20-40 should be OK.
 * @param cfg The Classifier-Free Guidance scale balances creativity and adherence to the prompt.
 *            Higher values result in images more closely matching the prompt
 *            however too high values will negatively impact quality.
 *
 *            Default = 8.
 *            For stable diffusion 1.5, 5-9 should be a reasonable range.
 *            The accepted range is between 0 and 100.
 * @param denoiseRatio The amount of denoising applied [0,1], lower values will maintain the structure of the initial
 *                     image allowing for image to image sampling.
 *
 *                     Default = 1.0 = Fully denoise (i.e. replace) the latent image.
 *
 * @param seed The random seed used for creating the noise. Default = random.
 * @author Mikko Hilpinen
 * @since 04.08.2025, v1.4
 */
case class KSamplerNode(name: String, model: OutputRef, positive: OutputRef, negative: OutputRef,
                        latentImage: OutputRef, sampler: String = "euler", scheduler: String = "normal",
                        steps: Int = 20, cfg: Double = 8.0, denoiseRatio: Double = 1.0, seed: Long = Random.nextLong)
	extends WorkflowNode
{
	override val classType: NodeClass = KSampler
	
	override lazy val input: Model = Model.from(
		"model" -> model, "seed" -> seed, "steps" -> steps, "cfg" -> cfg, "sampler_name" -> sampler,
		"scheduler" -> scheduler, "positive" -> positive, "negative" -> negative, "latent_image" -> latentImage,
		"denoise" -> denoiseRatio)
	
	/**
	 * The denoised latent.
	 */
	lazy val latent = output
}
