package io.github.iainhull.bug

import scala.concurrent.duration._
import scala.util.{Failure, Success}

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.util.Timeout
import com.typesafe.scalalogging.StrictLogging

object TestActor extends StrictLogging {

  val maxMessages = 100000000
  val maxVerifyAttempts = 10
  val stormSize = 1000


  sealed trait Command

  final case class Storm(start: Int) extends Command

  final case class TriggerVerify(start: Int, attempt: Int) extends Command

  final case class DoVerify(start: Int, attempt: Int) extends Command

  case object Stop extends Command

  def apply(persistentActor: ActorRef[PersistentActor.Command]): Behavior[Command] = {
    LoggingBehaviorInterceptor(logger) {
      Behaviors.setup { context =>

        logger.info("Created")

        implicit val timeout = Timeout(5.seconds)

        context.ask[PersistentActor.Command, PersistentActor.State](persistentActor)(s => PersistentActor.GetState(s)) {
          case Success(_) => Storm(0)
          case Failure(_) => Stop
        }

        Behaviors.withTimers { timers =>
          Behaviors.receiveMessage {
            case Storm(start) =>
              logger.info(s"Starting Storm $start")
              start to (start + stormSize) foreach { i =>
                persistentActor ! PersistentActor.RegisterValue(i)
              }
              context.self ! TriggerVerify(start, 0)
              logger.info(s"Stopping Storm $start")
              Behaviors.same

            case TriggerVerify(start, attempt) if start > maxMessages || attempt > maxVerifyAttempts =>
              logger.info(s"Abandoning verify $start, $attempt")
              context.self ! Stop
              Behaviors.same

            case TriggerVerify(start, attempt) =>
              val v = DoVerify(start, attempt)
              timers.startSingleTimer(v, v, 150.millis)
              Behaviors.same

            case DoVerify(start, attempt) =>
              logger.info(s"Starting verify $start, $attempt")
              context.ask[PersistentActor.Command, PersistentActor.State](persistentActor)(s => PersistentActor.GetState(s)) {
                case Success(PersistentActor.State(values)) =>
                  val sent = (start to (start + stormSize)).toSet
                  val missing = sent -- values
                  if (missing.isEmpty) {
                    logger.info("Verify succeeded")
                    Storm(start + stormSize)
                  } else {
                    logger.info(s"Verify failed $missing")
                    TriggerVerify(start, attempt + 1)
                  }
                case Failure(t) =>
                  logger.error(s"Ask failure $t")
                  Stop
              }
              Behaviors.same

            case Stop =>
              logger.info("Stop")
              Behavior.stopped
          }
        }
      }
    }
  }
}
