/*
 *  ---license-start
 *  eu-digital-green-certificates / dgca-verifier-app-android
 *  ---
 *  Copyright (C) 2022 T-Systems International GmbH and all other contributors
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
 *  Created by nicolamcornelio on 5/2/22, 4:33 PM
 */

package it.ministerodellasalute.verificaC19.ui

import android.app.AlertDialog
import android.content.Context
import android.text.method.LinkMovementMethod
import android.widget.TextView

class DialogCaller(private val context: Context) {
    private lateinit var title: String
    private lateinit var message: CharSequence
    private lateinit var positiveText: String
    private var negativeText: String
    private lateinit var positiveOnClickListener: () -> Unit
    private var negativeOnClickListener: () -> Unit
    private var linksEnabled: Boolean

    init {
        negativeText = ""
        negativeOnClickListener = {}
        linksEnabled = false
    }

    fun setTitle(title: String) = apply { this.title = title }

    fun setMessage(message: CharSequence) = apply { this.message = message }

    fun setPositiveOnClickListener(positiveText: String, action: () -> Unit) = apply {
        this.positiveText = positiveText
        this.positiveOnClickListener = action
    }

    fun setNegativeOnClickListener(negativeText: String, action: () -> Unit) = apply {
        this.negativeText = negativeText
        this.negativeOnClickListener = action
    }

    fun enableLinks() = apply { linksEnabled = true }

    fun show() {
        val builder =
            AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(positiveText) { _, _ ->
                    positiveOnClickListener()
                }
                .setNegativeButton(negativeText) { _, _ ->
                    negativeOnClickListener()
                }
        val dialog = builder.create()
        dialog.setCanceledOnTouchOutside(false)
        dialog.setCancelable(false)
        dialog.show()

        if (linksEnabled) {
            val alertMessage = dialog.findViewById(android.R.id.message) as TextView
            alertMessage.movementMethod = LinkMovementMethod.getInstance()
        }
    }
}