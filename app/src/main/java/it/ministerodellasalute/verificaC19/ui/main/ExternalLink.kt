/*
 *  ---license-start
 *  eu-digital-green-certificates / dgca-verifier-app-android
 *  ---
 *  Copyright (C) 2021 T-Systems International GmbH and all other contributors
 *  ---
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  ---license-end
 *
 */

package it.ministerodellasalute.verificaC19.ui.main

import it.ministerodellasalute.verificaC19.BuildConfig


object ExternalLink {
    const val FAQ_URL = "${BuildConfig.BASE_LINK_URL}/web/app.html"
    const val PRIVACY_POLICY_URL = "${BuildConfig.BASE_LINK_URL}/web/pn.html"
    const val VERIFICATION_FAQ_URL = "${BuildConfig.BASE_LINK_URL}/web/faq.html#verifica19"
}