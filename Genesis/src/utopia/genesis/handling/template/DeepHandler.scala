package utopia.genesis.handling.template
import utopia.flow.event.listener.ChangeListener
import utopia.flow.event.model.ChangeEvent
import utopia.flow.operator.Identity
import utopia.flow.view.mutable.eventful.ResettableFlag
import utopia.flow.view.template.eventful.FlagLike

/**
  * An abstract Handler implementation which extends the Handleable trait
  * @author Mikko Hilpinen
  * @since 01/02/2024, v3.6
  */
abstract class DeepHandler[A <: Handleable2](initialItems: IterableOnce[A] = Vector.empty)
	extends AbstractHandler2[A](initialItems) with Handleable2
{
	// ATTRIBUTES   --------------------
	
	// Manually updates the handling state
	private val handleConditionPointer = ResettableFlag(initialValue = true)
	// A listener assigned to all handled items in order to track their handling status
	private lazy val updateStateListener = ChangeListener { e: ChangeEvent[Boolean] =>
		if (e.newValue != handleConditionPointer.value)
			handleConditionPointer.value = items.exists { _.handleCondition.value }
	}
	// Contains true when at least one of the items is consistently handled
	// (which makes more precise state-listening unnecessary)
	private val containsUnconditionalFlag =
		itemsPointer.readOnly.map { _.exists { _.handleCondition.existsFixed(Identity) } }
	
	override val handleCondition: FlagLike = handleConditionPointer.view
	
	
	// INITIAL CODE --------------------
	
	containsUnconditionalFlag.addContinuousListenerAndSimulateEvent(true) { e =>
		// Case: At least one item is continuously active => Won't listen for individual changes
		if (e.newValue) {
			items.foreach { _.handleCondition.removeListener(updateStateListener) }
			handleConditionPointer.set()
		}
		// Case: None of the items are continuously active => Alters the state based on individual changes
		else {
			// Case: No items at this time => Won't need to be handled
			if (items.isEmpty)
				handleConditionPointer.reset()
				
			// Whenever items get added or removed, attaches or detaches the state-listener, also
			itemsPointer.addListenerAndSimulateEvent(Vector.empty) { e =>
				val (changes, _) = e.values.separateMatching
				changes.first.foreach { _.handleCondition.removeListener(updateStateListener) }
				changes.second.foreach { _.handleCondition.addListener(updateStateListener) }
				handleConditionPointer.value = items.exists { _.handleCondition.value }
			}
		}
	}
}
