package utopia.echo.model.comfyui.settings

import utopia.flow.util.Mutate

/**
 * Common trait for sampler settings interfaces that may be copied
 *
 * @author Mikko Hilpinen
 * @since 07.08.2025, v1.4
 */
trait SamplerSettingsLike[+Repr]
{
	// ABSTRACT -------------------------
	
	/**
	 * @return The name of the algorithm used when sampling,
	 *         this can affect the quality, speed, and style of the generated output.
	 *
	 *         See [[http://localhost:8188/object_info/KSampler]] for a full list of available values.
	 */
	def sampler: String
	/**
	 * @return The name of the scheduler to use.
	 *         The scheduler controls how noise is gradually removed to form the image.
	 *
	 *         Valid options are (at 4.8.2025): [
	 *         "normal", "karras", "exponential", "sgm_uniform", "simple", "ddim_uniform",
	 *         "beta", "linear_quadratic", "kl_optimal"
	 *         ]
	 */
	def scheduler: String
	
	/**
	 * @return The number of steps used in the denoising process. Default = 20.
	 *         For stable diffusion 1.5, 20-40 should be OK.
	 */
	def steps: Int
	/**
	 * @return The Classifier-Free Guidance scale, which balances creativity and adherence to the prompt.
	 *         Higher values result in images more closely matching the prompt
	 *         however too high values will negatively impact quality.
	 *
	 *         Default = 8.
	 *         For stable diffusion 1.5, 5-9 should be a reasonable range.
	 *         The accepted range is between 0 and 100.
	 */
	def cfg: Double
	/**
	 * @return The amount of denoising applied [0,1], lower values will maintain the structure of the initial
	 *         image allowing for image to image sampling.
	 *
	 *         Default = 1.0 = Fully denoise (i.e. replace) the image.
	 */
	def denoiseRatio: Double
	
	def withSampler(sampler: String): Repr
	def withScheduler(scheduler: String): Repr
	
	def withSteps(steps: Int): Repr
	def withCfg(cfg: Double): Repr
	def withDenoiseRatio(ratio: Double): Repr
	
	
	// COMPUTED ------------------------
	
	/**
	 * @return A copy of these settings with 100% denoising ratio,
	 *         meaning that the original image will be fully replaced.
	 */
	def fullyDenoising = withDenoiseRatio(1.0)
	
	
	// OTHER    ------------------------
	
	def mapSampler(f: Mutate[String]) = withSampler(f(sampler))
	def mapScheduler(f: Mutate[String]) = withScheduler(f(scheduler))
	
	def mapSteps(f: Mutate[Int]) = withSteps(f(steps))
	def mapCfg(f: Mutate[Double]) = withCfg(f(cfg))
	def mapDenoiseRatio(f: Mutate[Double]) = withDenoiseRatio(f(denoiseRatio))
}
