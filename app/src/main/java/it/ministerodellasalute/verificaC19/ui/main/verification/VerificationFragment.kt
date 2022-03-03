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
 */

package it.ministerodellasalute.verificaC19.ui.main.verification

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import it.ministerodellasalute.verificaC19.R
import it.ministerodellasalute.verificaC19.databinding.DoubleScanResultBinding
import it.ministerodellasalute.verificaC19.databinding.FragmentVerificationBinding
import it.ministerodellasalute.verificaC19.ui.base.isDebug
import it.ministerodellasalute.verificaC19.ui.compounds.QuestionCompound
import it.ministerodellasalute.verificaC19sdk.VerificaDownloadInProgressException
import it.ministerodellasalute.verificaC19sdk.VerificaMinSDKVersionException
import it.ministerodellasalute.verificaC19sdk.VerificaMinVersionException
import it.ministerodellasalute.verificaC19sdk.model.*
import it.ministerodellasalute.verificaC19sdk.util.FORMATTED_VALIDATION_DATE
import it.ministerodellasalute.verificaC19sdk.util.TimeUtility.formatDateOfBirth
import it.ministerodellasalute.verificaC19sdk.util.TimeUtility.parseTo

@ExperimentalUnsignedTypes
@AndroidEntryPoint
class VerificationFragment : Fragment(), View.OnClickListener {

    private val args by navArgs<VerificationFragmentArgs>()
    private val viewModel by viewModels<VerificationViewModel>()

    private var _binding: FragmentVerificationBinding? = null
    private val binding get() = _binding!!
    private lateinit var certificateModel: CertificateViewBean

    private var userName: String = ""
    private var callback: OnBackPressedCallback? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVerificationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.closeButton.setOnClickListener(this)
        binding.noTestAvailableButton.setOnClickListener(this)
        viewModel.certificate.observe(viewLifecycleOwner) { certificate ->
            certificate?.let {
                certificateModel = it

                callback = object : OnBackPressedCallback(true) {
                    override fun handleOnBackPressed() {
                        findNavController().popBackStack()
                    }
                }

                setOnBackPressed(callback)
                if (
                    viewModel.getTotemMode() &&
                    (certificate.certificateStatus == CertificateStatus.VALID) &&
                    !viewModel.getDoubleScanFlag()
                ) setOnBackTimer()
                setPersonData(it.person, it.dateOfBirth)
                binding.closeButton.visibility = View.VISIBLE
                setupCertStatusView(it)
                setupTimeStamp(it)

                binding.validationDate.visibility = View.VISIBLE
            }
        }
        viewModel.inProgress.observe(viewLifecycleOwner) {
            binding.progressBar.isVisible = it
        }
        try {
            viewModel.init(args.qrCodeText, true)
        } catch (e: VerificaMinSDKVersionException) {
            Log.d("VerificationFragment", "Min SDK Version Exception")
            createForceUpdateDialog(getString(R.string.updateMessage))
        } catch (e: VerificaMinVersionException) {
            Log.d("VerificationFragment", "Min App Version Exception")
            createForceUpdateDialog(getString(R.string.updateMessage))
        } catch (e: VerificaDownloadInProgressException) {
            Log.d("VerificationFragment", "Download In Progress Exception")
            createForceUpdateDialog(getString(R.string.messageDownloadStarted))
        }
    }

    private fun setupCertStatusView(cert: CertificateViewBean) {
        cert.certificateStatus?.let {
            setPersonDetailsVisibility(it)
            setDoubleScanButtons(it)
            setScanModeText()

            checkDoubleScanConditions(it)
        }
    }

    private fun checkDoubleScanConditions(it: CertificateStatus) {
        if (viewModel.getDoubleScanFlag() && it != CertificateStatus.NOT_EU_DCC) {
            binding.doubleScanResultsContainer.visibility = View.VISIBLE

            addDoubleScanResult(R.drawable.ic_valid_cert, R.string.certificateValid)

            if (it.isANonValidCertificate()) {
                addDoubleScanResult(R.drawable.ic_invalid, R.string.certificateTestNotValid)
                setValidationLayout(CertificateStatus.NOT_VALID)
                viewModel.setDoubleScanFlag(false)
            } else if (it == CertificateStatus.VALID) {
                addDoubleScanResult(R.drawable.ic_valid_cert, R.string.certificateTestValid)
                viewModel.setDoubleScanFlag(false)
                if (viewModel.getUserName() != userName) {
                    addDoubleScanResult(R.drawable.ic_invalid, R.string.userDataDoesNotMatch)
                    setValidationLayout(CertificateStatus.NOT_VALID)
                } else {
                    setValidationLayout(CertificateStatus.VALID)
                    if (viewModel.getTotemMode()) setOnBackTimer()
                }
            }
            viewModel.setUserName("")
        } else {
            setValidationLayout(it)
        }
    }

    private fun setOnBackTimer() {
        Handler(Looper.getMainLooper()).postDelayed({
            findNavController().navigate(R.id.action_verificationFragment_to_codeReaderFragment)
        }, 5000)
    }

    private fun addDoubleScanResult(icon: Int, text: Int) {
        val container = binding.doubleScanResultsContainer
        val binding = DoubleScanResultBinding.inflate(layoutInflater, container, false)
        binding.validationText.text = getString(text)
        binding.validationIcon.setImageResource(icon)
        container.addView(binding.root)
    }

    private fun setValidationLayout(it: CertificateStatus) {
        setBackgroundColor(it)
        setValidationIcon(it)
        setValidationMainText(it)
        setValidationSubTextVisibility(it)
        setValidationSubText(it)
        setLinkViews(it)
    }

    private fun setDoubleScanButtons(status: CertificateStatus) {
        if (status == CertificateStatus.TEST_NEEDED && !viewModel.getDoubleScanFlag()) {
            binding.scanTestButton.visibility = View.VISIBLE
            binding.noTestAvailableButton.visibility = View.VISIBLE
            binding.closeButton.visibility = View.GONE

            binding.subtitleText.updateLayoutParams<ConstraintLayout.LayoutParams> {
                topToBottom = binding.noTestAvailableButton.id
                bottomMargin = 64
            }

            binding.scanTestButton.setOnClickListener {
                viewModel.setDoubleScanFlag(true)
                it.findNavController().navigate(R.id.action_verificationFragment_to_codeReaderFragment)
            }

            val callback = object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {}
            }
            setOnBackPressed(callback)

        } else if (viewModel.getDoubleScanFlag()) {
            binding.scanTestButton.visibility = View.GONE
            binding.noTestAvailableButton.visibility = View.GONE

            binding.questionContainer.updateLayoutParams<ConstraintLayout.LayoutParams> {
                topToBottom = binding.doubleScanResultsContainer.id
                topMargin = 32
            }
        }
    }

    private fun setScanModeText() {
        val chosenScanMode = when (viewModel.getScanMode()) {
            ScanMode.STANDARD -> getString(R.string.scan_mode_3G_header)
            ScanMode.STRENGTHENED -> getString(R.string.scan_mode_2G_header)
            ScanMode.BOOSTER -> getString(R.string.scan_mode_booster_header)
            ScanMode.SCHOOL -> getString(R.string.scan_mode_school_header)
            ScanMode.WORK -> getString(R.string.scan_mode_work_header)
            ScanMode.ENTRY_ITALY -> getString(R.string.scan_mode_entry_italy_header)
            ScanMode.DOUBLE_SCAN -> getString(R.string.scan_mode_booster_header)
        }
        binding.scanModeText.text = chosenScanMode
    }

    private fun setupTimeStamp(cert: CertificateViewBean) {
        binding.validationDate.text = getString(
            R.string.label_validation_timestamp, cert.timeStamp?.parseTo(
                FORMATTED_VALIDATION_DATE
            )
        )
        binding.validationDate.visibility = View.VISIBLE
    }

    private fun setLinkViews(certStatus: CertificateStatus) {
        binding.questionContainer.removeAllViews()
        val ruleSet = viewModel.getRuleSet()
        val questionMap: Map<String, String> = when (certStatus) {
            CertificateStatus.VALID -> mapOf(ruleSet.getValidFaqText() to ruleSet.getValidFaqLink())
            CertificateStatus.NOT_VALID_YET -> mapOf(ruleSet.getNotValidYetFaqText() to ruleSet.getNotValidYetFaqLink())
            CertificateStatus.NOT_VALID, CertificateStatus.EXPIRED, CertificateStatus.REVOKED -> mapOf(
                ruleSet.getNotValidFaqText() to ruleSet.getNotValidFaqLink()
            )
            CertificateStatus.TEST_NEEDED -> mapOf(ruleSet.getVerificationNeededFaqText() to ruleSet.getVerificationNeededFaqLink())
            CertificateStatus.NOT_EU_DCC -> mapOf(ruleSet.getNotEuDgcFaqText() to ruleSet.getNotEuDgcFaqLink())
        }
        questionMap.forEach {
            val compound = QuestionCompound(context)
            compound.setupWithLabels(it.key, it.value)
            binding.questionContainer.addView(compound)
        }
        binding.questionContainer.clipChildren = false
    }

    private fun setValidationSubTextVisibility(certStatus: CertificateStatus) {
        binding.subtitleText.visibility = when (certStatus) {
            CertificateStatus.NOT_EU_DCC -> {
                binding.questionContainer.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    bottomToTop = binding.validationDate.id
                    bottomMargin = 32
                }
                View.GONE
            }
            else -> View.VISIBLE
        }
    }

    private fun setValidationSubText(certStatus: CertificateStatus) {
        binding.subtitleText.text =
            when (certStatus) {
                CertificateStatus.VALID -> getString(R.string.subtitle_text)
                CertificateStatus.TEST_NEEDED, CertificateStatus.NOT_VALID, CertificateStatus.EXPIRED, CertificateStatus.NOT_VALID_YET -> getString(R.string.subtitle_text_notvalid)
                else -> getString(R.string.subtitle_text_technicalError)
            }
    }

    private fun setValidationMainText(certStatus: CertificateStatus) {
        binding.certificateValid.text = when (certStatus) {
            CertificateStatus.VALID -> getString(R.string.certificateValid)
            CertificateStatus.NOT_EU_DCC -> getString(R.string.certificateNotDCC)
            CertificateStatus.REVOKED -> if (isDebug()) getString(R.string.certificateRevoked) else getString(
                R.string.certificateNonValid
            )
            CertificateStatus.NOT_VALID -> getString(R.string.certificateNonValid)
            CertificateStatus.EXPIRED -> getString(R.string.certificateExpired)
            CertificateStatus.TEST_NEEDED -> getString(R.string.certificateValidTestNeeded)
            CertificateStatus.NOT_VALID_YET -> getString(R.string.certificateNonValidYet)
        }
    }

    private fun setValidationIcon(certStatus: CertificateStatus) {
        binding.checkmark.background =
            ContextCompat.getDrawable(
                requireContext(), when (certStatus) {
                    CertificateStatus.VALID -> R.drawable.ic_valid_cert
                    CertificateStatus.NOT_VALID_YET -> R.drawable.ic_not_valid_yet
                    CertificateStatus.NOT_EU_DCC -> R.drawable.ic_technical_error
                    CertificateStatus.TEST_NEEDED -> R.drawable.ic_warning
                    else -> R.drawable.ic_invalid
                }
            )
    }

    private fun setPersonDetailsVisibility(certStatus: CertificateStatus) {
        binding.containerPersonDetails.visibility = when (certStatus) {
            CertificateStatus.VALID, CertificateStatus.REVOKED, CertificateStatus.TEST_NEEDED, CertificateStatus.NOT_VALID, CertificateStatus.EXPIRED, CertificateStatus.NOT_VALID_YET -> View.VISIBLE
            else -> View.GONE
        }
    }

    private fun setBackgroundColor(certStatus: CertificateStatus) {
        binding.verificationBackground.setBackgroundColor(
            ContextCompat.getColor(
                requireContext(),
                when (certStatus) {
                    CertificateStatus.VALID -> R.color.green
                    CertificateStatus.TEST_NEEDED -> R.color.orange
                    else -> R.color.red_bg
                }
            )
        )
    }

    private fun setPersonData(person: PersonModel?, dateOfBirth: String?) {
        if (person?.familyName.isNullOrEmpty()) {
            binding.nameStandardisedText.text =
                person?.standardisedFamilyName.plus(" ").plus(person?.standardisedGivenName).plus(" ").plus(person?.givenName)
        } else {
            binding.nameStandardisedText.text = person?.familyName.plus(" ").plus(person?.givenName)
        }
        binding.birthdateText.text = dateOfBirth?.formatDateOfBirth().orEmpty()

        userName = binding.nameStandardisedText.text.toString()

        if (certificateModel.certificateStatus == CertificateStatus.TEST_NEEDED && !viewModel.getDoubleScanFlag()) {
            viewModel.setUserName(userName)
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.close_button -> findNavController().popBackStack()
            R.id.no_test_available_button -> {
                binding.questionContainer.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    topToBottom = binding.doubleScanResultsContainer.id
                    topMargin = 32
                }

                viewModel.setDoubleScanFlag(true)
                checkDoubleScanConditions(CertificateStatus.NOT_VALID)
                viewModel.setDoubleScanFlag(false)

                binding.closeButton.visibility = View.VISIBLE
                binding.scanTestButton.visibility = View.GONE
                binding.noTestAvailableButton.visibility = View.GONE

                val callback = object : OnBackPressedCallback(true) {
                    override fun handleOnBackPressed() {
                        findNavController().popBackStack()
                    }
                }
                setOnBackPressed(callback)
            }
        }
    }

    private fun setOnBackPressed(callback: OnBackPressedCallback?) {
        callback?.let {
            activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner, callback)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun createForceUpdateDialog(message: String) {
        val builder = this.activity?.let { AlertDialog.Builder(requireContext()) }
        builder!!.setTitle(getString(R.string.updateTitle))
        builder.setMessage(message)
        builder.setPositiveButton(getString(R.string.ok)) { _, _ ->
            findNavController().popBackStack()
        }
        val dialog = builder.create()
        dialog.setCanceledOnTouchOutside(false)
        dialog.setCancelable(false)
        dialog.show()
    }

    override fun onDestroy() {
        viewModel.setDoubleScanFlag(false)
        super.onDestroy()
    }
}
