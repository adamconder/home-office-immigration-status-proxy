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

import com.google.inject.AbstractModule
import play.api.{Configuration, Environment, Logging}
import uk.gov.hmrc.auth.core.AuthConnector
import connectors.MicroserviceAuthConnector

class MicroserviceModule(val environment: Environment, val configuration: Configuration)
    extends AbstractModule with Logging {

  override def configure(): Unit = {
    val appName = "home-office-immigration-status-proxy"
    logger.info(s"Starting microservice : $appName : in mode : ${environment.mode}")

    bind(classOf[AuthConnector]).to(classOf[MicroserviceAuthConnector])
  }
}
