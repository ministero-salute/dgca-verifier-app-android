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
 *  Created by nicolamcornelio on 07/10/2021, 11:49
 */

package it.ministerodellasalute.verificaC19.ui

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import it.ministerodellasalute.verificaC19.BuildConfig
import it.ministerodellasalute.verificaC19.R
import it.ministerodellasalute.verificaC19.WhiteLabelApplication
import it.ministerodellasalute.verificaC19.databinding.ActivitySettingsBinding
import it.ministerodellasalute.verificaC19.ui.main.ExternalLink
import it.ministerodellasalute.verificaC19sdk.model.VerificationViewModel

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var binding: ActivitySettingsBinding
    private val viewModel by viewModels<VerificationViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSwitchesValue()
        setButtonsListener()
    }

    private fun setButtonsListener() {
        binding.backImage.setOnClickListener(this)
        binding.backText.setOnClickListener(this)
        binding.totemSwitch.setOnClickListener(this)
        binding.faqCard.setOnClickListener(this)
        binding.privacyPolicyCard.setOnClickListener(this)
        binding.resetButton.setOnClickListener(this)
        binding.viewDataButton.setOnClickListener(this)
    }

    private fun setSwitchesValue() {
        binding.totemSwitch.isChecked = viewModel.getTotemMode()
    }

    override fun onClick(v: View?) {
        if (v?.id == R.id.back_image || v?.id == R.id.back_text) {
            finish()
        } else if (v?.id == R.id.totem_switch) {
            viewModel.setTotemMode(binding.totemSwitch.isChecked)
        } else if (v?.id == R.id.faq_card) {
            val browserIntent =
                Intent(Intent.ACTION_VIEW, Uri.parse(ExternalLink.FAQ_URL))
            startActivity(browserIntent)
        } else if (v?.id == R.id.privacy_policy_card) {
            val browserIntent =
                Intent(Intent.ACTION_VIEW, Uri.parse(ExternalLink.PRIVACY_POLICY_URL))
            startActivity(browserIntent)
        } else if (v?.id == R.id.reset_button) {
            showAlertDialog()
        } else if (v?.id == R.id.view_data_button) {
            val intent = Intent(this, DataActivity::class.java)
            startActivity(intent)
        }
    }

    private fun showAlertDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("Tutti i dati memorizzati nell'app verranno cancellati. Quest'operazione è irreversibile. Confermare?")
            .setTitle("Attenzione")
            .setCancelable(false)
            .setPositiveButton("Sì") { dialog, id ->
                resetAndRestart()
            }
            .setNegativeButton("No") { dialog, id ->
                dialog.dismiss()
            }
        val alert = builder.create()
        alert.show()
    }

    private fun resetAndRestart() {
        WhiteLabelApplication.dataResetted = true
        viewModel.nukeData()
        WhiteLabelApplication().setWorkManager()
        finish()
    }
}
