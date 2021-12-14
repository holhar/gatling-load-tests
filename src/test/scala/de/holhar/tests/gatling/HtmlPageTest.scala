package de.holhar.tests.gatling

import io.gatling.core.Predef._
import io.gatling.core.structure.ScenarioBuilder
import io.gatling.http.Predef._
import io.gatling.http.protocol.HttpProtocolBuilder

class HtmlPageTest extends Simulation {

  val baseUrl = "http://localhost:8888"

  val httpProtocol: HttpProtocolBuilder = http
    .baseUrl(baseUrl)
    .disableFollowRedirect
    .disableAutoReferer
    .acceptHeader("*/*")
    .acceptEncodingHeader("gzip, deflate")
    .acceptLanguageHeader("de,en-US;q=0.7,en;q=0.3")
    .doNotTrackHeader("1")
    .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:87.0) Gecko/20100101 Firefox/87.0")

  val headers_cssFiles = Map(
    "Accept" -> "text/css,*/*;q=0.1",
    "Cache-Control" -> "no-cache",
    "Pragma" -> "no-cache",
    "Referer" -> "http://localhost:8888/path/to/resource?queryParam1=foo&queryParam2=bar")

  val headers_jsFiles = Map(
    "Cache-Control" -> "no-cache",
    "Pragma" -> "no-cache",
    "Referer" -> "http://localhost:8888/path/to/resource?queryParam1=foo&queryParam2=bar")

  val headers_fonts = Map(
    "Accept" -> "application/font-woff2;q=1.0,application/font-woff;q=0.9,*/*;q=0.8",
    "Accept-Encoding" -> "identity",
    "Cache-Control" -> "no-cache",
    "Pragma" -> "no-cache",
    "Referer" -> "http://localhost:8888/public/<static-resources-version>/css/main.css")

  val getAndSubmitForm: ScenarioBuilder = scenario("HtmlPageTest")
    .exec(http("get form")
      .get("/path/to/resource?queryParam1=foo&queryParam2=bar")
      .headers(Map(
        "Accept" -> "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
        "Cache-Control" -> "no-cache",
        "Pragma" -> "no-cache",
        "Upgrade-Insecure-Requests" -> "1"))
      // Retrieve session id and csrf token and save it in session to make it available for the succeeding submit request
      .check(headerRegex("Set-Cookie", "(SESSION=.*); Path.*$").saveAs("SESSION"))
      .check(regex("_csrf\" type=\"hidden\" value=\"(.*)\"\\/>").saveAs("CSRF_TOKEN")))
    .exec(http("get form mainCss")
      .get("/<static-resources-version>/css/main.css")
      .headers(headers_cssFiles))
    .exec(http("get form webComponentsJs")
      .get("/<static-resources-version>/js/web-components.js")
      .headers(headers_jsFiles))
    .exec(http("get form bundleJs")
      .get("/<static-resources-version>/js/bundle.js")
      .headers(headers_jsFiles))
    .exec { session =>
      println(session("SESSION").as[String])
      println(session("CSRF_TOKEN").as[String])
      session
    }
    .pause(1)
    .exec(http("submit form")
      .post("/path/to/resource?queryParam1=foo&queryParam2=bar")
      .headers(Map(
        "Accept" -> "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
        "Cache-Control" -> "no-cache",
        "Origin" -> "http://localhost:8888",
        "Pragma" -> "no-cache",
        "Referer" -> "http://localhost:8888/path/to/resource?queryParam1=foo&queryParam2=bar",
        "Upgrade-Insecure-Requests" -> "1",
        "Cookie" -> "${SESSION}"))
      .formParam("firstName", "max")
      .formParam("lastName", "power")
      .formParam("email", "max@power.com")
      .formParam("_csrf", "${CSRF_TOKEN}"))

  setUp(getAndSubmitForm.inject(rampUsers(250).during(60))).protocols(httpProtocol)
}