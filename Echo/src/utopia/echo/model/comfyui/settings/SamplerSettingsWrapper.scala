package utopia.echo.model.comfyui.settings

import utopia.flow.util.Mutate

/**
 * A common trait for classes that implement [[SamplerSettings]] by wrapping one.
 * @tparam Repr Type of the implementing class
 * @author Mikko Hilpinen
 * @since 07.08.2025, v1.4
 */
trait SamplerSettingsWrapper[+Repr <: SamplerSettings] extends SamplerSettings with SamplerSettingsLike[Repr]
{
	// ABSTRACT ---------------------------
	
	/**
	 * @return The wrapped sampler settings
	 */
	protected def settings: SamplerSettings
	
	/**
	 * @param settings New sampler settings to assign
	 * @return A copy of this item with the specified settings applied
	 */
	def withSettings(settings: SamplerSettings): Repr
	
	
	// IMPLEMENTED  -----------------------
	
	override def sampler: String = settings.sampler
	override def scheduler: String = settings.scheduler
	
	override def steps: Int = settings.steps
	override def cfg: Double = settings.cfg
	override def denoiseRatio: Double = settings.denoiseRatio
	
	override def withSampler(sampler: String): Repr = mapSettings { _.withSampler(sampler) }
	override def withScheduler(scheduler: String): Repr = mapSettings { _.withScheduler(scheduler) }
	
	override def withSteps(steps: Int): Repr = mapSettings { _.withSteps(steps) }
	override def withCfg(cfg: Double): Repr = mapSettings { _.withCfg(cfg) }
	override def withDenoiseRatio(ratio: Double): Repr = mapSettings { _.withDenoiseRatio(ratio) }
	
	
	// OTHER    -----------------------------
	
	def mapSettings(f: Mutate[SamplerSettings]) = withSettings(f(settings))
}
