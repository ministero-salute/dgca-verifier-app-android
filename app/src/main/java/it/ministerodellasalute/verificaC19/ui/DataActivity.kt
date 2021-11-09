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
 *  Created by nicolamcornelio on 18/10/2021, 14:12
 */

package it.ministerodellasalute.verificaC19.ui

import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import it.ministerodellasalute.verificaC19.R
import it.ministerodellasalute.verificaC19.databinding.ActivityDataBinding
import it.ministerodellasalute.verificaC19sdk.model.VerificationViewModel
import it.ministerodellasalute.verificaC19sdk.util.FORMATTED_DATE_LAST_SYNC
import it.ministerodellasalute.verificaC19sdk.util.TimeUtility.parseTo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlin.properties.Delegates

@AndroidEntryPoint
class DataActivity : AppCompatActivity(), View.OnClickListener {

    lateinit var binding: ActivityDataBinding
    private val viewModel by viewModels<VerificationViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDataBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setData()

        binding.backImage.setOnClickListener(this)
        binding.backText.setOnClickListener(this)
    }

    private fun setData() {
        CoroutineScope(Dispatchers.Main).launch {
            val value = async {
                viewModel.getKidsCount()
            }
            val kidsCount = value.await()
            binding.kidsCountValue.text = kidsCount.toString()

            val valueAllKids = async {
                viewModel.getAllKids()
            }
            val kidsList = valueAllKids.await()
            var stringBuilder = StringBuilder()
            for (item in kidsList){
                stringBuilder.append(item.kid+"\n")
            }
            binding.kidsListValue.text = stringBuilder.toString()
        }
        binding.resumeTokenValue.text = viewModel.getResumeToken().toString()
        binding.dateLastFetchValue.text = viewModel.getDateLastFetch().parseTo(FORMATTED_DATE_LAST_SYNC)
        binding.validationRulesValue.movementMethod = ScrollingMovementMethod()
        binding.validationRulesValue.text = viewModel.callGetValidationRules().map { "[" + it.name + "\n" + it.type + "\n" + it.value + "]"}.joinToString("\n")

    }

    override fun onClick(v: View?) {
        if (v?.id == R.id.back_image || v?.id == R.id.back_text) {
            finish()
        }
    }
}
