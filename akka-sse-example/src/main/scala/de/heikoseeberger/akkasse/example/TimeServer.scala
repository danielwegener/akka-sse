/*
 * Copyright 2015 Heiko Seeberger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.heikoseeberger.akkasse.example

import java.time.format.DateTimeFormatter
import java.time.{ZoneId, LocalTime, ZonedDateTime, Instant}

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl.Source
import akka.stream.{ActorMaterializer, Materializer}
import de.heikoseeberger.akkasse.{EventPublisher, EventStreamMarshalling, ServerSentEvent}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

object TimeServer {

  object TimeEventPublisher {
    def props: Props = Props(new TimeEventPublisher)
  }

  class TimeEventPublisher extends EventPublisher[Instant](10, 1.second) {
    import context.dispatcher

    context.system.scheduler.schedule(2.seconds, 2.seconds, self, "now")

    override protected def receiveEvent = {
      case "now" => onEvent(Instant.now())
    }
  }

  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem()
    implicit val mat = ActorMaterializer()
    import system.dispatcher
    Http().bindAndHandle(route(system), "127.0.0.1", 9000)
    Console.println("Open in browser: http://127.0.0.1:9000/index.html")
  }

  def route(system: ActorSystem)(implicit ec: ExecutionContext, mat: Materializer) = {
    import Directives._
    import EventStreamMarshalling._
    get {
      path( "index.html") {
        getFromResource("index.html")
      } ~
      optionalHeaderValueByName("Last-Event-ID") { eventId =>
        complete {
          val timeEventPublisher = system.actorOf(TimeEventPublisher.props)


          Source.apply[ServerSentEvent] { () =>
            eventId.toSeq.flatMap {
              evid => evid.toLong.to(System.currentTimeMillis).by(1000L)
                .map(Instant.ofEpochMilli)
                .map(dateTimeToServerSentEvent)
            }.toIterator
          } concat Source(ActorPublisher[ServerSentEvent](timeEventPublisher))
        }
      }
    }
  }


  implicit def dateTimeToServerSentEvent(time: Instant): ServerSentEvent = ServerSentEvent(
    data = DateTimeFormatter.ISO_LOCAL_TIME.format(ZonedDateTime.ofInstant(time, ZoneId.systemDefault())), id = Some(time.toEpochMilli.toString)
  )
}
