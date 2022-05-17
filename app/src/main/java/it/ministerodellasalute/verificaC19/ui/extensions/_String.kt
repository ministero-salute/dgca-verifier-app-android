/*
 *  license-start
 *  
 *  Copyright (C) 2021 Ministero della Salute and all other contributors
 *  
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
*/

package it.ministerodellasalute.verificaC19.ui.extensions

import android.text.Html
import android.text.SpannableString
import android.text.util.Linkify
import androidx.core.text.HtmlCompat

fun String.linkify() = SpannableString(Html.fromHtml(this, HtmlCompat.FROM_HTML_MODE_LEGACY)).also {
    Linkify.addLinks(it, Linkify.ALL)
}
