package utopia.echo.model.request.comfyui.workflow

import utopia.flow.util.Mutate

object SamplerSettings
{
	/**
	 * The default sampler settings
	 */
	lazy val default = apply()
}

/**
 * Used for specifying how KSampler is to perform
 *
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
 * @author Mikko Hilpinen
 * @since 05.08.2025, v1.4
 */
case class SamplerSettings(sampler: String = "euler", scheduler: String = "normal", steps: Int = 20, cfg: Double = 8.0,
                           denoiseRatio: Double = 1.0)
{
	def mapDenoiseRatio(f: Mutate[Double]) = copy(denoiseRatio = f(denoiseRatio))
}