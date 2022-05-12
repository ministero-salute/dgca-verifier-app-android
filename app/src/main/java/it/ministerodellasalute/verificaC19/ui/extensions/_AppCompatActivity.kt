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

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import it.ministerodellasalute.verificaC19sdk.util.Utility

fun AppCompatActivity.openBrowser(url: String) {
    val browserIntent =
        Intent(Intent.ACTION_VIEW, Uri.parse(url))
    startActivity(browserIntent)
}

fun AppCompatActivity.isOnline() = Utility.isOnline(this)