import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.{HttpResponse, HttpRequest}
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.stream.{ActorMaterializer, Materializer}

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContextExecutor

import spray.json.DefaultJsonProtocol

/**
 * Created with IntelliJ IDEA.
 * User: terry
 * Date: 8/13/15
 * Time: 7:57 PM
 *
 */

case class WeatherReport(zip : String, highTemp : Float, lowTemp : Float, current : Float, conditions : String)
case class Error(status : Int, message : String)

object Protocols extends DefaultJsonProtocol {
  implicit val weatherReportFormat = jsonFormat5(WeatherReport.apply)
  implicit val errorFormat = jsonFormat2(Error.apply)
}

object WeatherCache {
  val cache = new ListBuffer[WeatherReport]

  cache += WeatherReport("85021", 112.3f, 85.6f, 100.8f, "clear")

  def findByZip(zip : String) = cache.find(_.zip == zip)
  def addReport(report : WeatherReport) = cache += report

}

trait WeatherService extends Cors {
  import Protocols._
  implicit val system : ActorSystem
  implicit def executor : ExecutionContextExecutor
  implicit val materializer : Materializer

  def config : Config
  val logger : LoggingAdapter

  val routes = {
    logRequestResult("weather") {
      cors {
        pathPrefix("current") {
          (get & path(Segment)) { zip =>
            complete {
              val badRequestError = NotFound -> Error(status = 404, s"Zip $zip was not found!")
              WeatherCache.findByZip(zip).fold[ToResponseMarshallable](badRequestError)(report => report)
            }
          }
        } ~
          (post & entity(as[WeatherReport])) { report =>
            complete {
              WeatherCache.addReport(report)
              Created -> s"Report added for ${report.zip}"
            }
          }
      }
    }
  }
}

object WeatherApi extends App with WeatherService {
  override implicit val system = ActorSystem("WeaterApi")
  override implicit val executor = system.dispatcher
  override implicit val materializer = ActorMaterializer()

  override val config = ConfigFactory.load()
  override val logger = Logging(system, getClass)

  Http().bindAndHandle(routes, config.getString("http.interface"), config.getInt("http.port"))

}
