# akka-sse #

akka-sse adds support for [Server-Sent Events](http://www.w3.org/TR/eventsource/#event-stream-interpretation SSE specification) (SSE) to akka-http.

## Installation

akka-sse depends on akka-http 1.0-M2.

Grab it while it's hot:

``` scala
resolvers += "hseeberger at bintray" at "http://dl.bintray.com/hseeberger/maven"

libraryDependencies ++= List(
  "de.heikoseeberger" %% "akka-sse" % "0.2.1",
  ...
)
```

## Usage

First, mix `SseMarshalling` into your classes/actors defining routes (`akka.http.server.Route`), e.g.:

``` scala
class HttpService
    extends Actor
    with ScalaRoutingDSL
    with SseMarshalling { ... }
```

Then, define an implicit view from your domain events to `Sse.Message`, e.g.:

``` scala
implicit def flowEventToSseMessage(event: Flow.Event): Sse.Message =
  event match {
    case messageAdded: Flow.MessageAdded =>
      val data = PrettyPrinter(jsonWriter[Flow.MessageAdded].write(messageAdded))
      Sse.Message(data, Some("added"))
  }
```

Finally, simply complete a request to get an SSE stream with a `akka.stream.scaladsl.Source` of your domain events, e.g.:

``` scala
private def messages: Route =
  path("messages") {
    get {
      complete {
        Source(ActorPublisher[Flow.Event](createFlowEventPublisher()))
      }
    }
  }
```

A complete demo using akka-sse can is [Reactive Flows](https://github.com/hseeberger/reactive-flows).
Have fun, and please report any issues, suggestions, complaints.

## Contribution policy ##

Contributions via GitHub pull requests are gladly accepted from their original author. Along with any pull requests, please state that the contribution is your original work and that you license the work to the project under the project's open source license. Whether or not you state this explicitly, by submitting any copyrighted material via pull request, email, or other means you agree to license the material under the project's open source license and warrant that you have the legal authority to do so.

## License ##

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
