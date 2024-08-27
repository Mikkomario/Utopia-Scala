package utopia.flow.event.listener

import utopia.flow.event.model.ChangeResponse.Detach
import utopia.flow.event.model.{ChangeEvent, ChangeResponse}
import utopia.flow.operator.enumeration.End
import utopia.flow.operator.enumeration.End.{First, Last}
import utopia.flow.util.logging.Logger
import utopia.flow.view.mutable.Pointer
import utopia.flow.view.mutable.eventful.ResettableFlag
import utopia.flow.view.template.eventful.Changing

object ConditionalChangeReaction
{
	/**
	  * Activates a new conditional change reaction
	  * @param origin The pointer to which this reaction applies
	  * @param condition A pointer that contains true while this reaction should be active
	  *                  (i.e. reactions should occur) and false while reactions should be disabled.
	  * @param priority The priority given to this listener/reaction in the origin pointer.
	  *                 Default = Last = standard priority.
	  * @param simulatedInitialValue Initial value to 'simulate' for the 'origin' pointer for the purposes of
	  *                              the initial call of 'effect'.
	  *
	  *                              If defined as something other than the 'origin' pointer's current value,
	  *                              generates a change event where that value is "simulated" as the previous
	  *                              'origin' state.
	  *
	  *                              In cases where 'conditionPointer' initially contains false,
	  *                              this simulated change event is delayed.
	  *
	  *                              Set to None (default) in cases where you don't want the initial reaction
	  *                              to occur.
	  * @param effect A function called whenever the 'origin' pointer changes while the 'conditionPointer' contains true.
	  *          This function is also called in cases where the 'origin' pointer had changed its value while
	  *          this reaction was disabled.
	  *
	  *          Returns DetachmentChoice.detach when this reaction should permanently cease.
	  * @tparam A Type of values held within the origin pointer
	  */
	def apply[A](origin: Changing[A], condition: Changing[Boolean], priority: End, simulatedInitialValue: Option[A])
	            (effect: ChangeListener[A]): Unit =
	{
		// Optimizes the cases where the listening condition is fixed
		condition.fixedValue match {
			// Case: Fixed listening condition =>
			// Either continually listens to the origin pointer, or skips the listener/reaction altogether
			case Some(fixedState) =>
				if (fixedState)
					origin.addListenerAndPossiblySimulateEvent(simulatedInitialValue)(effect)
			// Case: Changing listening condition
			case None => new ConditionalChangeReaction[A](origin, condition, effect, priority, simulatedInitialValue)
		}
	}
	
	/**
	  * Activates a new conditional change reaction
	  * @param origin                The pointer to which this reaction applies
	  * @param condition             A pointer that contains true while this reaction should be active
	  *                              (i.e. reactions should occur) and false while reactions should be disabled.
	  * @param priority The priority given to this listener/reaction in the origin pointer.
	  *                 Default = Last = standard priority.
	  * @param effect                     A function called whenever the 'origin' pointer changes while the 'conditionPointer' contains true.
	  *                              This function is also called in cases where the 'origin' pointer had changed its value while
	  *                              this reaction was disabled.
	  *
	  *                              Returns DetachmentChoice.detach when this reaction should permanently cease.
	  * @tparam A Type of values held within the origin pointer
	  */
	def apply[A](origin: Changing[A], condition: Changing[Boolean], priority: End = Last)(effect: ChangeListener[A]): Unit =
		apply[A](origin, condition, priority, None)(effect)
	
	/**
	  * Activates a new conditional change reaction
	  * @param origin                The pointer to which this reaction applies
	  * @param condition             A pointer that contains true while this reaction should be active
	  *                              (i.e. reactions should occur) and false while reactions should be disabled.
	  * @param simulatedInitialValue Initial value to 'simulate' for the 'origin' pointer for the purposes of
	  *                              the initial call of 'effect'.
	  *
	  *                              If set to something other than the 'origin' pointer's current value,
	  *                              generates a change event where that value is "simulated" as the previous
	  *                              'origin' state.
	  *
	  *                              In cases where 'conditionPointer' initially contains false,
	  *                              this simulated change event is delayed.
	  *
	  * @param priority The priority given to this listener/reaction in the origin pointer.
	  *                 Default = Last = standard priority.
	  * @param effect                A function called whenever the 'origin' pointer changes while the 'conditionPointer' contains true.
	  *                              This function is also called in cases where the 'origin' pointer had changed its value while
	  *                              this reaction was disabled.
	  *
	  *                              Returns DetachmentChoice.detach when this reaction should permanently cease.
	  * @tparam A Type of values held within the origin pointer
	  */
	def simulatingInitialValue[A](origin: Changing[A], condition: Changing[Boolean], simulatedInitialValue: A,
	                              priority: End = Last)
	                             (effect: ChangeListener[A]) =
		apply[A](origin, condition, priority, Some(simulatedInitialValue))(effect)
}

/**
  * Listens to a changing item as long as another tracked state allows it.
  * After becoming inactive due to a change in the secondary pointer,
  * may then reattach itself to the original pointer when this change is reversed.
  *
  * The effect is that, while the external state allows it, this listener functions exactly like
  * a normal, continuous, change listener would.
  * However, a normal change listener is typically not able to reattach itself after detachment while this
  * version allows for temporary detachments.
  *
  * @author Mikko Hilpinen
  * @since 24.7.2023, v2.2
  *
  * @tparam A Type of values held within the origin pointer
  *
  * @constructor Creates a new conditional change reaction,
  *              which immediately starts listening/reacting to the 'origin' pointer (if appropriate)
  * @param origin The pointer to which this instance should react
  * @param conditionPointer A pointer that contains true while this reaction should be active
  *                         (i.e. reactions should occur) and false while reactions should be disabled.
  * @param effect A function called whenever the 'origin' pointer changes while the 'conditionPointer' contains true.
  *          This function is also called in cases where the 'origin' pointer had changed its value while
  *          this reaction was disabled.
  *
  *          Returns DetachmentChoice.detach when this reaction should permanently cease.
  *
  * @param priority              The priority given to this listener/reaction in the origin pointer.
  *                              Default = Last = standard priority.
  * @param simulatedInitialValue Initial value to 'simulate' for the 'origin' pointer for the purposes of
  *                              the initial call of 'effect'.
  *
  *                              If defined as something other than the 'origin' pointer's current value,
  *                              generates a change event where that value is "simulated" as the previous
  *                              'origin' state.
  *
  *                              In cases where 'conditionPointer' initially contains false,
  *                              this simulated change event is delayed.
  *
  *                              Set to None (default) in cases where you don't want the initial reaction
  *                              to occur.
  */
class ConditionalChangeReaction[A](origin: Changing[A], conditionPointer: Changing[Boolean],
                                   effect: ChangeListener[A], priority: End = Last,
                                   simulatedInitialValue: Option[A] = None)
{
	// ATTRIBUTES   ------------------------
	
	private implicit val log: Logger = origin.listenerLogger
	
	// Contains true while this listener has been scheduled to detach
	// The detachment is appropriated upon a change event received from the 'origin' pointer
	// This flag is set when the 'listenConditionPointer' is set to false
	private val detachmentQueuedFlag = ResettableFlag()
	
	// Contains a value while detached
	// Used for simulating a change event upon reattachment
	private val memorizedValuePointer = Pointer.empty[A]()
	
	// Contains true after this listener has permanently ended its listening functions
	// due to a DetachmentChoice received from 'effect'
	private var ended = false
	
	
	// INITIAL CODE ---------------------
	
	// Starts listening to the 'origin' pointer, if appropriate
	if (conditionPointer.value)
		simulatedInitialValue match {
			case Some(simulation) =>
				origin.addListenerAndSimulateEvent(simulation, isHighPriority = priority == First)(Delegate)
			case None => origin.addListenerOfPriority(priority)(Delegate)
		}
	else
		memorizedValuePointer.value = simulatedInitialValue
	
	// Starts listening to the listening condition
	conditionPointer.addListener { e =>
		// Case: Listening has already permanently ceased => Detaches from this pointer, also
		if (ended)
			Detach
		// Case: Listening may still be performed => Reacts to the change
		else {
			// Case: Listening should reactivate
			if (e.newValue) {
				// Case: Listening had not yet stopped => Cancels the previously queued detachment
				if (detachmentQueuedFlag.reset())
					memorizedValuePointer.clear()
				// Case: Detachment had been appropriated already => Reattaches to the 'origin' pointer
				else
					memorizedValuePointer.pop() match {
						// Case: Simulated value had been queued (expected) =>
						// Uses that to possibly generate a new change event
						case Some(memorized) =>
							origin.addListenerAndSimulateEvent(memorized, isHighPriority = priority == First)(Delegate)
						// Case: No simulated value queued (unexpected) => Starts listening again
						case None => origin.addListenerOfPriority(priority)(Delegate)
					}
			}
			// Case: Listening should deactivate => Queues a detachment
			else
				detachmentQueuedFlag.set()
			
			ChangeResponse.continueIf(!ended && origin.mayChange)
		}
	}
	
	
	// NESTED   -------------------------
	
	// Doesn't expose the ChangeListener interface, therefore uses a private object for that function
	private object Delegate extends ChangeListener[A]
	{
		override def onChangeEvent(event: ChangeEvent[A]): ChangeResponse = {
			// Case: Detachment was queued => Actuates it
			if (detachmentQueuedFlag.reset()) {
				// Remembers the last informed state for event simulation upon reattachment
				memorizedValuePointer.value = Some(event.oldValue)
				Detach
			}
			// Case: No detachment queued => Reacts to this change by forwarding it to the 'effect'
			else {
				val choice = effect.onChangeEvent(event)
				// If the user (i.e. 'effect') made the choice to detach,
				// stops listening to the listening condition as well and considers this listener permanently removed
				if (choice.shouldDetach)
					ended = true
				choice
			}
		}
	}
}
