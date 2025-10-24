package utopia.echo.model.request.tts.piper

import utopia.echo.model.request.tts.piper.TtsParams.{defaultLengthScale, defaultNoiseScale, defaultNoiseWidthScale}
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.SureFromModelFactory
import utopia.flow.generic.model.immutable.{Constant, Model}
import utopia.flow.generic.model.template.ModelConvertible
import utopia.flow.generic.model.template.HasPropertiesLike.HasProperties
import utopia.flow.operator.ScopeUsable
import utopia.flow.util.Mutate

object TtsParams extends SureFromModelFactory[TtsParams]
{
	// ATTRIBUTES   ----------------------
	
	/**
	 * The assumed default length scale value (1.0).
	 * Used in mapping functions.
	 */
	lazy val defaultLengthScale = 1.0
	/**
	 * The assumed default noise scale (0.667).
	 * Used in mapping functions.
	 */
	lazy val defaultNoiseScale = 0.667
	/**
	 * The assumed default noise width scale (0.8).
	 * Used in mapping functions.
	 */
	lazy val defaultNoiseWidthScale = 0.8
	
	/**
	 * An empty parameter-set
	 */
	lazy val empty = apply()
	
	
	// IMPLEMENTED  ----------------------
	
	override def parseFrom(model: HasProperties): TtsParams =
		apply(model("length_scale").double, model("noise_scale").double, model("noise_w_scale").double,
			model("voice").string.map(Voice.apply), model("speaker_id").int match {
				case Some(id) => Some(Right(id))
				case None => model("speaker").string.map(Left.apply)
			})
}

/**
 * Parameters used for customizing the text-to-speech process
 * @param lengthScale Phoneme length scaling factor used, controls the speaking speed.
 *                    Larger values make the speech slower while lower make it faster.
 *                    Typical range is between 0.7 and 1.3.
 *                    Default = None = Keep model default.
 * @param noiseScale Controls the amount of random noise, affecting voice variability.
 *                   Lower values result in a monotone voice. Higher values result in a more varied / expressive voice.
 *                   Typical range is between 0.2 and 0.6.
 *                   Default = None = Keep model default.
 * @param noiseWidthScale Controls the phoneme length randomness.
 *                        Lower values yield more consistent (but possibly flat) results.
 *                        Higher values yield more varied / human-like results, but may cause mispronunciations.
 *                        Typical range is between 0.5 and 1.0.
 *                        Default = None = Keep model default.
 * @param voice The voice model to use. Default = None = Keep API default.
 * @param speaker Speaker name (Left) or speaker ID (Right).
 *                Only used in multi-speaker models.
 *                Default = None = The default speaker.
 * @author Mikko Hilpinen
 * @since 28.09.2025, v1.4
 */
case class TtsParams(lengthScale: Option[Double] = None, noiseScale: Option[Double] = None,
                     noiseWidthScale: Option[Double] = None, voice: Option[Voice] = None,
                     speaker: Option[Either[String, Int]] = None)
	extends ModelConvertible with ScopeUsable[TtsParams]
{
	// COMPUTED -----------------------------
	
	/**
	 * @return A copy of these settings with fast speech (length = 0.7)
	 */
	def fast = 0.7
	/**
	 * @return A copy of these settings with slow speech (length = 1.3)
	 */
	def slow = 1.3
	/**
	 * @return A copy of these settings with (25%) faster speech
	 */
	def faster = fasterBy(0.25)
	/**
	 * @return A copy of these settings with (20%) slower speech
	 */
	def slower = slowerBy(0.2)
	
	/**
	 * @return A copy of these settings with low noise (0.3)
	 */
	def withConsistentTone = withNoiseScale(0.3)
	/**
	 * @return A copy of these settings with high noise (0.5)
	 */
	def withVariedTone = withNoiseScale(0.5)
	/**
	 * @return A copy of these settings with (20%) lower noise.
	 */
	def moreConsistentTone = mapNoiseScale { s => (s * 0.8) max 0.1 }
	/**
	 * @return A copy of these settings with (25%) higher noise.
	 */
	def moreExpressive = mapNoiseScale { s => (s * 1.25) max 1.0 }
	
	/**
	 * @return A copy of these settings with consistent timing (0.7)
	 */
	def withConsistentTiming = withNoiseWidthScale(0.7)
	/**
	 * @return A copy of these settings with varied timing (0.9)
	 */
	def withVariedTiming = withNoiseWidthScale(0.9)
	/**
	 * @return A copy of these settings with more consistent timing
	 */
	def moreConsistentTiming = mapNoiseWidthScale { s => (s - 0.1) max 0.3 }
	/**
	 * @return A copy of these settings with more varied timing
	 */
	def moreVariedTiming = mapNoiseWidthScale { s => (s + 0.1) min 1.0 }
	
	
	// IMPLEMENTED  ---------------------
	
	override def self: TtsParams = this
	
	override def toModel: Model = (Model.from("voice" -> voice.map { _.name },
		"length_scale" -> lengthScale, "noise_scale" -> noiseScale, "noise_w_scale" -> noiseWidthScale) ++
		speaker.map {
			case Left(name) => Constant("speaker", name)
			case Right(id) => Constant("speaker_id", id)
		}).withoutEmptyValues
	
	
	// OTHER    -------------------------
	
	/**
	 * @param scale Length scale to assign, where lower (e.g. 0.7) is faster and higher (e.g. 1.3) is slower.
	 * @return Copy of these parameters with the specified length scaling
	 */
	def withLengthScale(scale: Double) = copy(lengthScale = Some(scale))
	/**
	 * @param scale Noise scale to assign, where higher values (e.g. 0.8) are more random / varied
	 *              and lower values (e.g. 0.3) are more consistent / monotone.
	 * @return Copy of these parameters with the specified noise scaling.
	 */
	def withNoiseScale(scale: Double) = copy(noiseScale = Some(scale))
	/**
	 * @param scale Noise width scale to assign.
	 *              Higher values (e.g. 0.9) introduce more randomness to phoneme lengths,
	 *              while lower values (e.g. 0.6) result in a more consistent output.
	 * @return Copy of these parameters with the specified noise width scale
	 */
	def withNoiseWidthScale(scale: Double) = copy(noiseWidthScale = Some(scale))
	
	def mapLengthScale(f: Mutate[Double]) = withLengthScale(f(lengthScale.getOrElse(defaultLengthScale)))
	def mapNoiseScale(f: Mutate[Double]) = withNoiseScale(f(noiseScale.getOrElse(defaultNoiseScale)))
	def mapNoiseWidthScale(f: Mutate[Double]) =
		withNoiseWidthScale(f(noiseWidthScale.getOrElse(defaultNoiseWidthScale)))
	
	/**
	 * @param increase Amount of speed increase added.
	 *                 E.g. 0.2 would result in about 20% faster speech.
	 * @return Copy of these parameters with modified length scale.
	 */
	def fasterBy(increase: Double) = mapLengthScale { _ * (1 - increase) }
	/**
	 * @param decrease Amount of speed decrease to apply.
	 *                 E.g. 0.2 would result in about 20% slower speech.
	 * @return Copy of these parameters with modified length scale.
	 */
	def slowerBy(decrease: Double) = mapLengthScale { _ * (1 + decrease) }
	
	/**
	 * @param linearAdjustment Noise scale adjustment (linear).
	 *                         E.g. 0.1 will increase noise by 0.1 (e.g. from 0.5 to 0.6)
	 * @return Copy of these parameters with adjusted noise scale
	 */
	def increaseNoiseBy(linearAdjustment: Double) = mapNoiseScale { _ + linearAdjustment }
	def multiplyNoiseBy(mod: Double) = mapNoiseScale { _ * mod }
	
	/**
	 * @param voice Voice model to use
	 * @return Copy of these parameters, using the specified voice model
	 */
	def withVoice(voice: Voice) = copy(voice = Some(voice))
	
	/**
	 * @param speaker Speaker identifier: Either the speaker name (Left) or speaker ID (right).
	 * @return Copy of these parameters with the specified speaker.
	 */
	def withSpeaker(speaker: Either[String, Int]) = copy(speaker = Some(speaker))
	/**
	 * @param speakerName Name of the speaker to use in a multi-speaker model.
	 * @return Copy of these parameters with the specified speaker.
	 */
	def withSpeaker(speakerName: String): TtsParams = withSpeaker(Left(speakerName))
	/**
	 * @param speaker ID of the speaker to use in a multi-speaker model.
	 * @return Copy of these parameters with the specified speaker.
	 */
	def withSpeakerId(speaker: Int) = withSpeaker(Right(speaker))
}