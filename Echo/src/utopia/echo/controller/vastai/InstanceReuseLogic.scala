package utopia.echo.controller.vastai

import utopia.echo.controller.vastai.InstanceReuseLogic.{AlternativeReuseLogic, FilteredReuseLogic}
import utopia.echo.model.vastai.instance.VastAiInstance
import utopia.flow.util.Mutate

import scala.language.implicitConversions

object InstanceReuseLogic
{
	// COMPUTED ----------------------------
	
	/**
	 * @return A reuse logic that never reuses Vast AI instances.
	 */
	def never = NeverReuse
	
	
	// IMPLICIT ----------------------------
	
	/**
	 * @param f A function that receives a collection of potentially available Vast AI instances
	 *          and finds one that may be reused, if possible.
	 * @return A reuse logic based on the specified function.
	 */
	implicit def apply(f: Seq[VastAiInstance] => Option[VastAiInstance]): InstanceReuseLogic =
		new _InstanceReuseLogic(f)
	
	
	// NESTED   ----------------------------
	
	object NeverReuse extends InstanceReuseLogic
	{
		// ATTRIBUTES   --------------------
		
		override val reuses: Boolean = false
		
		
		// IMPLEMENTED  --------------------
		
		override def findReusableFrom(instances: Seq[VastAiInstance]): Option[VastAiInstance] = None
	}
	
	object ReuseCheapest extends InstanceReuseLogic
	{
		override def reuses: Boolean = true
		
		override def findReusableFrom(instances: Seq[VastAiInstance]): Option[VastAiInstance] =
			instances.minByOption { _.cost.total }
	}
	
	private class AlternativeReuseLogic(primary: InstanceReuseLogic, secondary: InstanceReuseLogic)
		extends InstanceReuseLogic
	{
		override def reuses: Boolean = primary.reuses || secondary.reuses
		
		override def findReusableFrom(instances: Seq[VastAiInstance]): Option[VastAiInstance] =
			primary.findReusableFrom(instances).orElse(secondary.findReusableFrom(instances))
	}
	
	private class FilteredReuseLogic(delegate: InstanceReuseLogic, filter: Mutate[Seq[VastAiInstance]])
		extends InstanceReuseLogic
	{
		override def reuses: Boolean = delegate.reuses
		
		override def findReusableFrom(instances: Seq[VastAiInstance]): Option[VastAiInstance] = {
			val remainder = filter(instances)
			if (remainder.isEmpty)
				None
			else
				delegate.findReusableFrom(remainder)
		}
	}
	
	private class _InstanceReuseLogic(f: Seq[VastAiInstance] => Option[VastAiInstance]) extends InstanceReuseLogic
	{
		// ATTRIBUTES   --------------------
		
		override val reuses: Boolean = true
		
		
		// IMPLEMENTED  --------------------
		
		override def findReusableFrom(instances: Seq[VastAiInstance]): Option[VastAiInstance] = f(instances)
	}
}

/**
 * Used for determining whether and how to reuse Vast AI instances.
 * @author Mikko Hilpinen
 * @since 04.10.2026, v1.6
 */
trait InstanceReuseLogic
{
	// ABSTRACT ---------------------------
	
	/**
	 * @return Whether instance-reusing is ever applicable
	 */
	def reuses: Boolean
	
	/**
	 * @param instances Instances to select from
	 * @return An instance that should be reused next. None if none of these instances should be reused here.
	 */
	def findReusableFrom(instances: Seq[VastAiInstance]): Option[VastAiInstance]
	
	
	// COMPUTED ---------------------------
	
	/**
	 * @return Copy of this logic that only includes reusable instances.
	 *         I.e. excludes instances that are offline, for example.
	 */
	def onlyReusable = filter { _.filter { _.status.current.value.instanceMayBeReused } }
	
	
	// OTHER    ---------------------------
	
	/**
	 * @param alternative An alternative reuse logic to apply in cases when this one doesn't find reusable instances.
	 * @return A reuse logic that combines both these logics.
	 */
	def ||(alternative: InstanceReuseLogic): InstanceReuseLogic = new AlternativeReuseLogic(this, alternative)
	
	/**
	 * @param label A label
	 * @return A copy of this reuse logic that requires that the instance has the specified label (exactly).
	 *         If the specified label was empty, yields this logic as is.
	 */
	def onlyLabel(label: String) =
		if (label.isEmpty) this else filter { _.filter { _.label == label } }
	
	/**
	 * @param f A filtering function to apply to the instances presented to this reuse logic
	 * @return A copy of this logic that applies pre-filtering using the specified function.
	 */
	def filter(f: Mutate[Seq[VastAiInstance]]): InstanceReuseLogic = new FilteredReuseLogic(this, f)
}
