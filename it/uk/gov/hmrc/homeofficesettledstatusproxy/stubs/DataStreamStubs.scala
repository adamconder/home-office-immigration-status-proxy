package uk.gov.hmrc.homeofficesettledstatusproxy.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import uk.gov.hmrc.homeofficesettledstatusproxy.support.WireMockSupport
import org.scalatest.concurrent.Eventually
import org.scalatest.time.{Millis, Seconds, Span}
import uk.gov.hmrc.homeofficesettledstatusproxy.support.WireMockSupport

trait DataStreamStubs extends Eventually {
  me: WireMockSupport =>

  override implicit val patienceConfig: PatienceConfig =
    PatienceConfig(scaled(Span(5, Seconds)), scaled(Span(500, Millis)))

  def givenAuditConnector(): Unit = {
    stubFor(post(urlPathEqualTo(auditUrl)).willReturn(aResponse().withStatus(204)))
    stubFor(post(urlPathEqualTo(auditUrl + "/merged")).willReturn(aResponse().withStatus(204)))
  }

  private def auditUrl = "/write/audit"

}
