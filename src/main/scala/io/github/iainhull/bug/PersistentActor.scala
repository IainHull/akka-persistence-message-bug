package io.github.iainhull.bug

import scala.concurrent.duration._

import akka.actor.typed.{ActorRef, Behavior, SupervisorStrategy}
import akka.actor.typed.scaladsl.Behaviors
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}
import akka.persistence.typed.PersistenceId
import com.typesafe.scalalogging.StrictLogging

object PersistentActor extends StrictLogging {

  sealed trait Command

  case class RegisterValue(value: Int) extends Command

  case class GetState(sender: ActorRef[State]) extends Command

  sealed trait Event

  case class ValueRegistered(value: Int) extends Event

  case class State(values: Set[Int])

  def apply(): Behavior[Command] = {
    LoggingBehaviorInterceptor(logger) {
      Behaviors.setup { context =>
        EventSourcedBehavior[Command, Event, State](
          persistenceId = PersistenceId(context.self.path.name),
          emptyState = State(Set()),
          commandHandler = commandHandler,
          eventHandler = eventHandler
        ).receiveSignal {
          case (_, signal) =>
            logger.info(s"Received signal $signal")
        }
          .snapshotWhen(snapshotPredicate)
          .onPersistFailure(SupervisorStrategy.restartWithBackoff(minBackoff = 2.seconds, maxBackoff = 20.seconds, randomFactor = 0.1))
      }
    }
  }

  def commandHandler(state: State, command: Command): Effect[Event, State] = {
    logger.info(s"Received command $command")
    command match {
      case RegisterValue(value) if !state.values.contains(value) =>
        Effect.persist[Event, State](ValueRegistered(value))
      case RegisterValue(_) =>
        Effect.none
      case GetState(sender) =>
        sender ! state
        Effect.none
    }
  }

  def eventHandler(state: State, event: Event): State = {
    event match {
      case ValueRegistered(value) =>
        state.copy(values = state.values + value)
    }
  }

  def snapshotPredicate(state: State, event: Event, id: Long): Boolean = {
    val ret = state.values.size % 10 == 0 || id % 10 == 0
    if (ret) logger.info(s"Lets snapshot ${state.values.size} $event $id")
    ret
  }
}
