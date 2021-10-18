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
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import it.ministerodellasalute.verificaC19.databinding.ActivityDataBinding
import it.ministerodellasalute.verificaC19sdk.model.VerificationViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlin.properties.Delegates

@AndroidEntryPoint
class DataActivity : AppCompatActivity() {

    lateinit var binding: ActivityDataBinding
    private val viewModel by viewModels<VerificationViewModel>()

    private var kidsCount: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDataBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setData()
    }

    private fun setData() {
        CoroutineScope(Dispatchers.Main).launch {
            val value = async {
                viewModel.getKidsCount()
            }
            val kidsCount = value.await()
            binding.kidsCountValue.text = kidsCount.toString()
        }
        binding.resumeTokenValue.text = viewModel.getResumeToken().toString()
        binding.dateLastFetchValue.text = viewModel.getDateLastFetch().toString()
        binding.validationRulesValue.text = viewModel.callGetValidationRules().toString()
    }
}
