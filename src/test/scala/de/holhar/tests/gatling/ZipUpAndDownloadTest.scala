package de.holhar.tests.gatling

import scala.concurrent.duration._
import io.gatling.core.Predef._
import io.gatling.http.Predef._

import java.nio.file.{Files, Paths}
import io.gatling.core.scenario.Simulation
import io.gatling.core.session.Expression
import io.gatling.core.structure.ScenarioBuilder
import io.gatling.http.protocol.HttpProtocolBuilder

class ZipUpAndDownloadTest extends Simulation {

  val baseUrl = "http://localhost:8888"

  val bearerToken: String = System.getProperty("bearerToken")

  val httpProtocol: HttpProtocolBuilder = http
    .baseUrl(baseUrl)
    .userAgentHeader("curl/7.54.0")

  val pushBundleScenario: ScenarioBuilder = scenario("ZipUpAndDownloadTest - push zip").repeat(4) {
    exec(getBearerToken)
      .exec(buildPatchData)
      .exec(http("submitBundle")
        .patch("/api/bundle/submit")
        .headers(Map(
          "Authorization" -> "Bearer ${bearerToken}",
          "Accept" -> "*/*",
          "Cache-Control" -> "no-cache",
          "Content-Type" -> "application/zip",
          "Pragma" -> "no-cache"))
        .body(ByteArrayBody("${patchData}")))
      .pause(10000.milliseconds)
      .exec(http("approveBundle")
        .put("/api/bundle/approve")
        .headers(Map(
          "Authorization" -> "Bearer ${bearerToken}",
          "Accept" -> "*/*",
          "Cache-Control" -> "no-cache",
          "Connection" -> "keep-alive",
          "Pragma" -> "no-cache")))
      .pause(20000.milliseconds)
      .exec(http("rollbackBundle")
        .put("/api/bundle/rollback")
        .headers(Map(
          "Authorization" -> "Bearer ${bearerToken}",
          "Accept" -> "*/*",
          "Cache-Control" -> "no-cache",
          "Connection" -> "keep-alive",
          "Pragma" -> "no-cache")))
      .pause(20000.milliseconds)
  }

  val getBundleScenario: ScenarioBuilder = scenario("ZipUpAndDownloadTest - get zip").repeat(4) {
    exec(getBearerToken)
      .exec(http("getBundle")
        .get("/api/bundle")
        .headers(Map(
          "Authorization" -> "Bearer ${bearerToken}",
          "Accept" -> "*/*",
          "Cache-Control" -> "no-cache",
          "Pragma" -> "no-cache")))
  }

  val getFileScenario: ScenarioBuilder = scenario("ZipUpAndDownloadTest - get file").repeat(4) {
    exec(getBearerToken)
      .exec(http("getFile")
        .get("/api/files/test1.txt")
        .headers(Map(
          "Authorization" -> "Bearer ${bearerToken}",
          "Accept" -> "*/*",
          "Cache-Control" -> "no-cache",
          "Pragma" -> "no-cache")))
  }

  setUp(
    pushBundleScenario.inject(atOnceUsers(1)).protocols(httpProtocol),
    getBundleScenario.inject(rampUsers(6).during(60.seconds)).protocols(httpProtocol),
    getFileScenario.inject(rampUsers(1000).during(60.seconds)).protocols(httpProtocol)
  )

  def buildPatchData: Expression[Session] = session => {
    val patchData: Array[Byte] = Files.readAllBytes(Paths.get("src/test/resources/test.zip"))
    session.set("patchData", patchData)
  }

  def getBearerToken: Expression[Session] = session => {
    session.set("bearerToken", bearerToken)
  }
}
