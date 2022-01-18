/*
 * Copyright 2022 HM Revenue & Customs
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

package controllers

import akka.stream.Materializer
import com.google.inject.Inject
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

class FakeAuthAction @Inject()(implicit materializer: Materializer) extends AuthAction {
  override def parser: BodyParser[AnyContent] = new BodyParsers.Default()

  override def invokeBlock[A](
    request: Request[A],
    block: Request[A] => Future[Result]): Future[Result] =
    block(request)

  override protected def executionContext: ExecutionContext = ???
}
