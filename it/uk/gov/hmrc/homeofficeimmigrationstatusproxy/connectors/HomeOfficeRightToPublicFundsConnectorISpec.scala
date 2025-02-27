/*
 * Copyright 2023 HM Revenue & Customs
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

package connectors

import cats.implicits._
import connectors.ErrorCodes._
import models._
import org.scalatest.Inside.inside
import org.scalatest.concurrent.ScalaFutures
import play.api.http.Status._
import stubs.HomeOfficeRightToPublicFundsStubs
import support.AppBaseISpec
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, RequestId}

import java.time.LocalDate
import scala.concurrent.ExecutionContext

class HomeOfficeRightToPublicFundsConnectorISpec
    extends AppBaseISpec
    with HomeOfficeRightToPublicFundsStubs
    with ScalaFutures {

  implicit val hc: HeaderCarrier    = HeaderCarrier()
  implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]

  lazy val connector: HomeOfficeRightToPublicFundsConnector =
    app.injector.instanceOf[HomeOfficeRightToPublicFundsConnector]

  val dummyCorrelationId          = "some-correlation-id"
  val dummyRequestId              = Some(RequestId("request-id"))
  val dummyOAuthToken: OAuthToken = OAuthToken("FOO0123456789", "SomeTokenType")

  val request = DateOfBirth(LocalDate.parse("2001-01-31"))
    .map(StatusCheckByNinoRequest(_, "Jane", "Doe", Nino("RJ301829A")))
    .toOption
    .get
  val mrzRequest: StatusCheckByMrzRequest = (
    DocumentNumber("1234567890"),
    DateOfBirth(LocalDate.parse("2001-01-31")),
    Nationality("USA")
  ).mapN((docNumber, dob, nat) => StatusCheckByMrzRequest(DocumentType.Passport, docNumber, dob, nat)).toOption.get

  "token" should {
    "return valid oauth token" in {
      givenOAuthTokenGranted()
      val result: OAuthToken = connector.token(dummyCorrelationId, dummyRequestId).futureValue
      result.access_token shouldBe "FOO0123456789"
    }

    "return valid oauth token without refresh token" in {
      givenOAuthTokenGrantedWithoutRefresh()
      val result: OAuthToken = connector.token(dummyCorrelationId, dummyRequestId).futureValue
      result.access_token shouldBe "FOO0123456789"
    }

    "raise exception if token denied" in {
      givenOAuthTokenDenied()
      val result = intercept[RuntimeException](connector.token(dummyCorrelationId, dummyRequestId).futureValue)
      result.getMessage should include("Upstream4xxResponse")
    }
  }

  "statusPublicFundsByNino" should {

    "return status when range provided" in {
      givenStatusCheckResultWithRangeExample(RequestType.Nino)
      val range = Some(StatusCheckRange(Some(LocalDate.parse("2019-07-15")), Some(LocalDate.parse("2019-04-15"))))
      val request = DateOfBirth(LocalDate.parse("2001-01-31"))
        .map(StatusCheckByNinoRequest(_, "Jane", "Doe", Nino("RJ301829A"), range))
        .toOption
        .get

      val result: Either[StatusCheckErrorResponseWithStatus, StatusCheckResponse] =
        connector.statusPublicFundsByNino(request, dummyCorrelationId, dummyRequestId, dummyOAuthToken).futureValue

      result.toOption.get shouldBe responseBodyWithStatusObject
    }

    "return status when no range provided" in {
      givenStatusCheckResultNoRangeExample(RequestType.Nino)

      val result: Either[StatusCheckErrorResponseWithStatus, StatusCheckResponse] =
        connector.statusPublicFundsByNino(request, dummyCorrelationId, dummyRequestId, dummyOAuthToken).futureValue

      result.toOption.get shouldBe responseBodyWithStatusObject
    }

    "return check error when 400 response ERR_REQUEST_INVALID" in {
      givenStatusCheckErrorWhenMissingInputField(RequestType.Nino)

      val result: Either[StatusCheckErrorResponseWithStatus, StatusCheckResponse] =
        connector.statusPublicFundsByNino(request, dummyCorrelationId, dummyRequestId, dummyOAuthToken).futureValue

      inside(result) { case Left(error) =>
        error.statusCode                  shouldBe BAD_REQUEST
        error.errorResponse.error.errCode shouldBe ERR_REQUEST_INVALID
      }
    }

    "return check error when 404 response ERR_NOT_FOUND" in {
      givenStatusCheckErrorWhenStatusNotFound(RequestType.Nino)

      val result: Either[StatusCheckErrorResponseWithStatus, StatusCheckResponse] =
        connector.statusPublicFundsByNino(request, dummyCorrelationId, dummyRequestId, dummyOAuthToken).futureValue

      inside(result) { case Left(error) =>
        error.statusCode                  shouldBe NOT_FOUND
        error.errorResponse.error.errCode shouldBe ERR_NOT_FOUND
      }
    }

    "return check error when 409 response ERR_CONFLICT" in {
      givenStatusCheckErrorWhenConflict(RequestType.Nino)

      val result: Either[StatusCheckErrorResponseWithStatus, StatusCheckResponse] =
        connector.statusPublicFundsByNino(request, dummyCorrelationId, dummyRequestId, dummyOAuthToken).futureValue

      inside(result) { case Left(error) =>
        error.statusCode                  shouldBe CONFLICT
        error.errorResponse.error.errCode shouldBe ERR_CONFLICT
      }
    }

    "return check error when 400 response ERR_VALIDATION" in {
      givenStatusCheckErrorWhenDOBInvalid(RequestType.Nino)

      val result: Either[StatusCheckErrorResponseWithStatus, StatusCheckResponse] =
        connector.statusPublicFundsByNino(request, dummyCorrelationId, dummyRequestId, dummyOAuthToken).futureValue

      inside(result) { case Left(error) =>
        error.statusCode                  shouldBe BAD_REQUEST
        error.errorResponse.error.errCode shouldBe ERR_VALIDATION
      }
    }

    "return unknown error if other 4xx response" in {
      givenStatusPublicFundsByNinoStub(TOO_MANY_REQUESTS, validNinoRequestBody, "")

      val result: Either[StatusCheckErrorResponseWithStatus, StatusCheckResponse] =
        connector.statusPublicFundsByNino(request, dummyCorrelationId, dummyRequestId, dummyOAuthToken).futureValue

      inside(result) { case Left(error) =>
        error.statusCode                  shouldBe TOO_MANY_REQUESTS
        error.errorResponse.error.errCode shouldBe ERR_UNKNOWN
      }
    }

    "return unknown error if 5xx response" in {
      givenStatusPublicFundsByNinoStub(INTERNAL_SERVER_ERROR, validNinoRequestBody, "")

      val result: Either[StatusCheckErrorResponseWithStatus, StatusCheckResponse] =
        connector.statusPublicFundsByNino(request, dummyCorrelationId, dummyRequestId, dummyOAuthToken).futureValue

      inside(result) { case Left(error) =>
        error.statusCode                  shouldBe INTERNAL_SERVER_ERROR
        error.errorResponse.error.errCode shouldBe ERR_UNKNOWN
      }
    }

  }

  "statusPublicFundsByMrz" should {

    "return status when range provided" in {
      givenStatusCheckResultWithRangeExample(RequestType.Mrz)
      val range = Some(StatusCheckRange(Some(LocalDate.parse("2019-07-15")), Some(LocalDate.parse("2019-04-15"))))

      val mrzRequest: StatusCheckByMrzRequest = (
        DocumentNumber("1234567890"),
        DateOfBirth(LocalDate.parse("2001-01-31")),
        Nationality("USA")
      ).mapN((docNumber, dob, nat) => StatusCheckByMrzRequest(DocumentType.Passport, docNumber, dob, nat, range))
        .toOption
        .get

      val result: Either[StatusCheckErrorResponseWithStatus, StatusCheckResponse] =
        connector.statusPublicFundsByMrz(mrzRequest, dummyCorrelationId, dummyRequestId, dummyOAuthToken).futureValue

      result.toOption.get shouldBe responseBodyWithStatusObject
    }

    "return status when no range provided" in {
      givenStatusCheckResultNoRangeExample(RequestType.Mrz)

      val result: Either[StatusCheckErrorResponseWithStatus, StatusCheckResponse] =
        connector.statusPublicFundsByMrz(mrzRequest, dummyCorrelationId, dummyRequestId, dummyOAuthToken).futureValue

      result.toOption.get shouldBe responseBodyWithStatusObject
    }

    "return check error when 400 response ERR_REQUEST_INVALID" in {
      givenStatusCheckErrorWhenMissingInputField(RequestType.Mrz)

      val result: Either[StatusCheckErrorResponseWithStatus, StatusCheckResponse] =
        connector.statusPublicFundsByMrz(mrzRequest, dummyCorrelationId, dummyRequestId, dummyOAuthToken).futureValue

      inside(result) { case Left(error) =>
        error.statusCode                  shouldBe BAD_REQUEST
        error.errorResponse.error.errCode shouldBe ERR_REQUEST_INVALID
      }
    }

    "return check error when 404 response ERR_NOT_FOUND" in {
      givenStatusCheckErrorWhenStatusNotFound(RequestType.Mrz)

      val result: Either[StatusCheckErrorResponseWithStatus, StatusCheckResponse] =
        connector.statusPublicFundsByMrz(mrzRequest, dummyCorrelationId, dummyRequestId, dummyOAuthToken).futureValue

      inside(result) { case Left(error) =>
        error.statusCode                  shouldBe NOT_FOUND
        error.errorResponse.error.errCode shouldBe ERR_NOT_FOUND
      }
    }

    "return check error when 409 response ERR_CONFLICT" in {
      givenStatusCheckErrorWhenConflict(RequestType.Mrz)

      val result: Either[StatusCheckErrorResponseWithStatus, StatusCheckResponse] =
        connector.statusPublicFundsByMrz(mrzRequest, dummyCorrelationId, dummyRequestId, dummyOAuthToken).futureValue

      inside(result) { case Left(error) =>
        error.statusCode                  shouldBe CONFLICT
        error.errorResponse.error.errCode shouldBe ERR_CONFLICT
      }
    }

    "return check error when 400 response ERR_VALIDATION" in {
      givenStatusCheckErrorWhenDOBInvalid(RequestType.Mrz)

      val result: Either[StatusCheckErrorResponseWithStatus, StatusCheckResponse] =
        connector.statusPublicFundsByMrz(mrzRequest, dummyCorrelationId, dummyRequestId, dummyOAuthToken).futureValue

      inside(result) { case Left(error) =>
        error.statusCode                  shouldBe BAD_REQUEST
        error.errorResponse.error.errCode shouldBe ERR_VALIDATION
      }
    }

    "return unknown error if other 4xx response" in {
      givenStatusPublicFundsByMrzStub(TOO_MANY_REQUESTS, validMrzRequestBody, "")

      val result: Either[StatusCheckErrorResponseWithStatus, StatusCheckResponse] =
        connector.statusPublicFundsByMrz(mrzRequest, dummyCorrelationId, dummyRequestId, dummyOAuthToken).futureValue

      inside(result) { case Left(error) =>
        error.statusCode                  shouldBe TOO_MANY_REQUESTS
        error.errorResponse.error.errCode shouldBe ERR_UNKNOWN
      }
    }

    "return unknown error if 5xx response" in {
      givenStatusPublicFundsByMrzStub(INTERNAL_SERVER_ERROR, validMrzRequestBody, "")

      val result: Either[StatusCheckErrorResponseWithStatus, StatusCheckResponse] =
        connector.statusPublicFundsByMrz(mrzRequest, dummyCorrelationId, dummyRequestId, dummyOAuthToken).futureValue

      inside(result) { case Left(error) =>
        error.statusCode                  shouldBe INTERNAL_SERVER_ERROR
        error.errorResponse.error.errCode shouldBe ERR_UNKNOWN
      }
    }

  }

}
