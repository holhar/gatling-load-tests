package de.holhar.tests.gatling

import scala.concurrent.duration._
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.core.session.Expression
import io.gatling.core.structure.ScenarioBuilder
import io.gatling.http.protocol.HttpProtocolBuilder

class RestApiTest extends Simulation {

  val baseUrl = "http://localhost:8888"

  val bearerToken: String = System.getProperty("bearerToken")

  val httpProtocol: HttpProtocolBuilder = http
    .baseUrl(baseUrl)
    .inferHtmlResources()
    .acceptHeader("*/*")
    .contentTypeHeader("application/json")
    .userAgentHeader("curl/7.54.0")

  val getAndPostDataScenario: ScenarioBuilder = scenario("RestApiTest - get and post data")
    .exec(http("get user data")
      .get("/user/1")
      .check(status.is(200))
      .check(jsonPath("$.array[0].field[1].name").is("Max Power"))
      .check(jsonPath("$.array[0].field[1].etag").saveAs("ETAG")))
    .exec { session =>
      // Save ETAG value in session to make it available for succeeding requests
      println(session("ETAG").as[String])
      session
    }
    .pause(1)
    .exec(http("post data")
      .post("/user/1")
      .body(StringBody(
        """{
          |  "some": "data",
          |  "array": [
          |      {
          |          "another": "object",
          |          "field": [
          |              {
          |                  "name": "Max Super Power"
          |              }
          |          ]
          |      }
          |  ]
          |}""".stripMargin
      )).asJson
      .check(status.is(200))
      .check(jsonPath("$.array[0].field[1].name").is("Max Super Power")))

  setUp(
    getAndPostDataScenario.inject(rampUsers(250).during(60.seconds)).protocols(httpProtocol),
  )

  def getBearerToken: Expression[Session] = session => {
    session.set("bearerToken", bearerToken)
  }
}
