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
 *  Created by nicolamcornelio on 2/17/22, 2:54 PM
 */

package it.ministerodellasalute.verificaC19.ui

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import it.ministerodellasalute.verificaC19.databinding.ScanModeChoiceLayoutBinding

class ScanModeAdapter(
    private var adapterList: List<FirstActivity.ScanModeChoice>,
    var mSelectedItem: Int
) : RecyclerView.Adapter<ScanModeAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ScanModeChoiceLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(adapterList[position], position, mSelectedItem)
    }

    override fun getItemCount() = adapterList.size

    inner class ViewHolder(private val binding: ScanModeChoiceLayoutBinding) : RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("NotifyDataSetChanged")
        fun bind(
            scanMode: FirstActivity.ScanModeChoice,
            position: Int,
            selectedPosition: Int
        ) {
            binding.scanModeRadioButton.text = scanMode.name
            binding.scanModeShortDescriptionTextView.text = scanMode.shortDescription
            binding.scanModeLongDescriptionTextView.text = scanMode.longDescription
            binding.scanModeRadioButton.isChecked = scanMode.isChecked

            if (selectedPosition == -1 && position == 0 || (selectedPosition == position)) {
                binding.scanModeRadioButton.isChecked = true
                binding.scanModeLongDescriptionCardView.visibility = View.VISIBLE
            } else {
                binding.scanModeRadioButton.isChecked = selectedPosition == position
                binding.scanModeLongDescriptionCardView.visibility = View.GONE
            }

            binding.scanModeRadioButton.setOnClickListener {
                mSelectedItem = adapterPosition
                notifyDataSetChanged()
            }
        }
    }
}