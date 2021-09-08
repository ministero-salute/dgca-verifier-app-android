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

package it.ministerodellasalute.verificaC19.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import it.ministerodellasalute.verificaC19.ui.main.VerificationActivity

class BarcodeBroadcastReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION = "it.ministerodellasalute.verificaC19.decode_action"
        const val CATEGORY = "it.ministerodellasalute.verificaC19.decode_category"
        const val BARCODE_STRING = "it.ministerodellasalute.verificaC19.barcode_string"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action.equals(ACTION)) {
            val barcodeString: String? = intent?.extras?.getString(BARCODE_STRING)
            if (barcodeString != null && barcodeString.isNotEmpty()) {
                val verificationIntent = Intent(context, VerificationActivity::class.java)
                verificationIntent.putExtra("qrCodeText", barcodeString)
                verificationIntent.putExtra("finishOnClose", true)
                context?.startActivity(verificationIntent)
            }
        }
    }
}