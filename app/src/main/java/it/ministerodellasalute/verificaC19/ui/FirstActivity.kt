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

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.text.util.Linkify
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.core.text.bold
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import it.ministerodellasalute.verificaC19.BuildConfig
import it.ministerodellasalute.verificaC19.R
import it.ministerodellasalute.verificaC19.databinding.ActivityFirstBinding
import it.ministerodellasalute.verificaC19.ui.base.doOnDebug
import it.ministerodellasalute.verificaC19.ui.extensions.addTransparency
import it.ministerodellasalute.verificaC19.ui.extensions.hide
import it.ministerodellasalute.verificaC19.ui.extensions.removeTransparency
import it.ministerodellasalute.verificaC19.ui.extensions.show
import it.ministerodellasalute.verificaC19.ui.main.ExternalLink
import it.ministerodellasalute.verificaC19.ui.main.Extras
import it.ministerodellasalute.verificaC19.ui.main.MainActivity
import it.ministerodellasalute.verificaC19sdk.model.FirstViewModel
import it.ministerodellasalute.verificaC19sdk.model.ScanMode
import it.ministerodellasalute.verificaC19sdk.model.drl.DownloadState
import it.ministerodellasalute.verificaC19sdk.util.ConversionUtility
import it.ministerodellasalute.verificaC19sdk.util.FORMATTED_DATE_LAST_SYNC
import it.ministerodellasalute.verificaC19sdk.util.TimeUtility.parseTo
import it.ministerodellasalute.verificaC19sdk.util.Utility

@AndroidEntryPoint
class FirstActivity : AppCompatActivity(), View.OnClickListener, DialogInterface.OnDismissListener {

    private lateinit var binding: ActivityFirstBinding

    private val viewModel by viewModels<FirstViewModel>()

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                openQrCodeReader()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFirstBinding.inflate(layoutInflater)

        setContentView(binding.root)
        setSecureWindowFlags()
        setOnClickListeners()
        setupUI()
        observeLiveData()
    }

    private fun observeLiveData() {
        observeDownloadStatus()
        observeSyncStatus()
        observeScanMode()
        doOnDebug {
            observeDebugInfo()
        }
    }

    private fun observeDownloadStatus() {
        viewModel.downloadStatus.observe(this) {
            when (it) {
                is DownloadState.Complete -> {
                    renderCompleteState()
                }
                is DownloadState.RequiresConfirm -> {
                    renderRequiresConfirmState(it.totalSize)
                }
                is DownloadState.Downloading -> {
                    renderDownloadingState()
                }
                is DownloadState.ResumeAvailable -> {
                    renderResumeAvailableState()
                }
                is DownloadState.DownloadAvailable -> {
                    renderDownloadAvailableState()
                }
                else -> {}
            }
        }

    }

    private fun renderDownloadingState() {
        updateDownloadedPackagesCount()
        showDownloadProgressViews()
    }

    private fun renderCompleteState() {
            viewModel.getDateLastSync().let { date ->
                binding.dateLastSyncText.text = getString(
                    R.string.lastSyncDate,
                    if (date == -1L) getString(R.string.notAvailable) else date.parseTo(
                        FORMATTED_DATE_LAST_SYNC
                    )
                )
            }
            binding.qrButton.removeTransparency()
            hideDownloadProgressViews()
    }

    private fun renderResumeAvailableState() {
        binding.resumeDownload.show()
        binding.dateLastSyncText.text = getString(R.string.incompleteDownload)
        showDownloadProgressViews()
        updateDownloadedPackagesCount()
        binding.qrButton.addTransparency()
    }

    private fun observeDebugInfo() {
        viewModel.debugInfoLiveData.observe(this) {
            it?.let { binding.debugButton.show() }
        }
    }

    private fun observeScanMode() {
        viewModel.scanMode.observe(this) {
            setScanModeButtonText(it)
        }
    }

    private fun observeSyncStatus() {
        viewModel.fetchStatus.observe(this) {
            if (it) {
                binding.qrButton.addTransparency()
            } else {
                viewModel.setDateLastSync(System.currentTimeMillis())
                viewModel.startDrlFlow()
            }
        }
    }

    private fun setupUI() {
        val string = getString(R.string.version, BuildConfig.VERSION_NAME)
        val spannableString = SpannableString(string).also {
            it.setSpan(StyleSpan(Typeface.NORMAL), 0, it.length, 0)
        }
        binding.versionText.text = spannableString
        binding.dateLastSyncText.text = getString(R.string.loading)
    }

    private fun setSecureWindowFlags() {
        if (!BuildConfig.DEBUG) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        }
    }

    private fun setOnClickListeners() {
        binding.qrButton.setOnClickListener(this)
        binding.settings.setOnClickListener(this)
        binding.scanModeButton.setOnClickListener(this)
        binding.privacyPolicyCard.setOnClickListener(this)
        binding.faqCard.setOnClickListener(this)
        binding.initDownload.setOnClickListener(this)
        binding.debugButton.setOnClickListener(this)
        binding.resumeDownload.setOnClickListener(this)
        binding.circleInfoContainer.setOnClickListener(this)
    }

    private fun setScanModeButtonText(currentScanMode: ScanMode?) {
        if (!viewModel.hasScanModeBeenChosen()) {
            val s = SpannableStringBuilder()
                .bold { append(getString(R.string.label_choose_scan_mode)) }
            binding.scanModeButton.text = s
        } else {
            val chosenScanMode =
                when (currentScanMode) {
                    ScanMode.STANDARD -> getString(R.string.scan_mode_3G_header)
                    ScanMode.STRENGTHENED -> getString(R.string.scan_mode_2G_header)
                    ScanMode.BOOSTER -> getString(R.string.scan_mode_booster_header)
                    ScanMode.ENTRY_ITALY -> getString(R.string.scan_mode_entry_italy_header)
                    else -> getString(R.string.scan_mode_3G_header)
                }
            binding.scanModeButton.text = chosenScanMode
        }
    }

    private fun startDownload() {
        prepareForDownload()
        showDownloadProgressViews()
        binding.initDownload.hide()
        binding.dateLastSyncText.text = getString(R.string.updatingRevokedPass)
        viewModel.startDrlFlow()
    }

    private fun createCheckConnectionAlertDialog() {
        DialogCaller()
            .setTitle(getString(R.string.no_internet_title))
            .setMessage(getString(R.string.no_internet_message))
            .setPositiveText(getString(R.string.ok_label))
            .setPositiveOnClickListener { _, _ -> }
            .show(this)
    }


    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_DENIED
        ) {
            createPermissionAlert()
        } else {
            openQrCodeReader()
        }
    }

    private fun createPermissionAlert() {
        try {
            DialogCaller()
                .setTitle(getString(R.string.privacyTitle))
                .setMessage(getString(R.string.privacy))
                .setPositiveText(getString(R.string.next))
                .setPositiveOnClickListener { _, _ -> requestPermissionLauncher.launch(Manifest.permission.CAMERA) }
                .setNegativeText(getString(R.string.back))
                .setNegativeOnClickListener { _, _ -> }
                .show(this)
        } catch (e: Exception) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun renderRequiresConfirmState(totalSize: Float) {
        try {
            DialogCaller()
                .setTitle(
                    getString(
                        R.string.titleDownloadAlert,
                        ConversionUtility.byteToMegaByte(totalSize)
                    )
                )
                .setMessage(
                    getString(
                        R.string.messageDownloadAlert,
                        ConversionUtility.byteToMegaByte(totalSize)
                    )
                )
                .setPositiveText(getString(R.string.label_download))
                .setPositiveOnClickListener { _, _ ->
                    if (Utility.isOnline(this)) {
                        startDownload()
                    } else {
                        createCheckConnectionAlertDialog()
                        renderDownloadAvailableState()
                    }
                }
                .setNegativeText(getString(R.string.after_download))
                .setNegativeOnClickListener { _, _ ->
                    viewModel.setDownloadStatus(DownloadState.DownloadAvailable)
                }
                .show(this)
        } catch (e: Exception) {
            Log.i("RequiresConfirmException", e.message.toString())
        }
    }

    private fun prepareForDownload() {
        viewModel.resetCurrentRetry()
        viewModel.setShouldInitDownload(true)
        viewModel.setDownloadAsAvailable()
    }

    private fun renderDownloadAvailableState() {
        binding.resumeDownload.hide()
        binding.initDownload.show()
        binding.qrButton.addTransparency()
        hideDownloadProgressViews()
        binding.dateLastSyncText.text = when (viewModel.getDrlStateIT().totalSizeInByte + viewModel.getDrlStateEU().totalSizeInByte) {
            0L -> {
                hideDownloadProgressViews()
                getString(
                    R.string.label_download_alert_simple
                )
            }
            else ->
                getString(
                    R.string.label_download_alert_complete,
                    ConversionUtility.byteToMegaByte(viewModel.getDrlStateIT().totalSizeInByte.toFloat() + viewModel.getDrlStateEU().totalSizeInByte.toFloat())
                )
        }
    }

    override fun onResume() {
        super.onResume()
        setScanModeButtonText(viewModel.getChosenScanMode())
        checkAppMinimumVersion()
    }

    private fun checkAppMinimumVersion() {
        viewModel.getAppMinVersion().let {
            if (Utility.versionCompare(
                    it,
                    BuildConfig.VERSION_NAME
                ) > 0 || viewModel.isSDKVersionObsolete()
            ) {
                createForceUpdateDialog()
            }
        }
    }

    private fun openQrCodeReader() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

    private fun openSettings() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.qrButton -> {
                if (viewModel.getDateLastSync() == -1L) {
                    createNoSyncAlertDialog()
                    return
                } else if (!viewModel.hasScanModeBeenChosen()) {
                    viewModel.getRuleSet()?.getErrorScanModePopup()?.run {
                        createNoScanModeChosenAlert()
                    } ?: run { createNoSyncAlertDialog() }
                    return
                }

                if (viewModel.isDrlOutdatedOrNull()) {
                    createNoDrlAlertDialog()
                    return
                }
                checkCameraPermission()
            }

            R.id.settings -> openSettings()

            R.id.scan_mode_button -> {
                viewModel.getRuleSet()?.run {
                    ScanModeDialogFragment(viewModel.getRuleSet()!!).show(supportFragmentManager, "SCAN_MODE_DIALOG_FRAGMENT")
                } ?: run {
                    createNoSyncAlertDialog()
                }
            }

            R.id.circle_info_container -> {
                viewModel.getRuleSet()?.getBaseScanModeDescription()?.run {
                    createScanModeInfoAlert()
                } ?: run { createNoSyncAlertDialog() }
            }

            R.id.privacy_policy_card -> {
                val browserIntent =
                    Intent(Intent.ACTION_VIEW, Uri.parse(ExternalLink.PRIVACY_POLICY_URL))
                startActivity(browserIntent)
            }

            R.id.faq_card -> {
                val browserIntent =
                    Intent(Intent.ACTION_VIEW, Uri.parse(ExternalLink.FAQ_URL))
                startActivity(browserIntent)
            }

            R.id.init_download -> {
                if (Utility.isOnline(this)) {
                    startDownload()
                } else {
                    createCheckConnectionAlertDialog()
                }
            }

            R.id.debugButton -> {
                val debugInfoIntent = Intent(this, DebugInfoActivity::class.java)
                debugInfoIntent.putExtra(
                    Extras.DEBUG_INFO,
                    Gson().toJson(viewModel.debugInfoLiveData.value)
                )
                startActivity(debugInfoIntent)
            }

            R.id.resumeDownload -> {
                if (Utility.isOnline(this)) {
                    viewModel.setResumeAsAvailable()
                    viewModel.setShouldInitDownload(true)
                    binding.resumeDownload.hide()
                    binding.dateLastSyncText.text = getString(R.string.updatingRevokedPass)
                    viewModel.startDrlFlow()
                } else {
                    createCheckConnectionAlertDialog()
                }
            }
        }
    }

    private fun createNoScanModeChosenAlert() {
        val string =
            SpannableString(Html.fromHtml(viewModel.getRuleSet()?.getErrorScanModePopup(), HtmlCompat.FROM_HTML_MODE_LEGACY)).also {
                Linkify.addLinks(it, Linkify.ALL)
            }

        DialogCaller()
            .setTitle(getString(R.string.noKeyAlertTitle))
            .setMessage(string)
            .setPositiveText(getString(R.string.ok))
            .setPositiveOnClickListener { _, _ -> }
            .enableLinks()
            .show(this)
    }

    private fun createScanModeInfoAlert() {
        val string = SpannableString(Html.fromHtml(viewModel.getRuleSet()?.getInfoScanModePopup(), HtmlCompat.FROM_HTML_MODE_LEGACY)).also {
            Linkify.addLinks(it, Linkify.ALL)
        }

        DialogCaller()
            .setTitle(getString(R.string.label_scan_mode_types))
            .setMessage(string)
            .setPositiveText(getString(R.string.ok))
            .setPositiveOnClickListener { _, _ -> }
            .enableLinks()
            .show(this)
    }

    private fun createNoSyncAlertDialog() {
        DialogCaller()
            .setTitle(getString(R.string.noKeyAlertTitle))
            .setMessage(getString(R.string.noKeyAlertMessage))
            .setPositiveText(getString(R.string.ok))
            .setPositiveOnClickListener { _, _ -> }
            .show(this)
    }

    private fun createNoDrlAlertDialog() {
        DialogCaller()
            .setTitle(getString(R.string.noKeyAlertTitle))
            .setMessage(getString(R.string.noKeyAlertMessageForDrl))
            .setPositiveText(getString(R.string.ok))
            .setPositiveOnClickListener { _, _ -> }
            .show(this)
    }

    private fun createForceUpdateDialog() {
        DialogCaller()
            .setTitle(getString(R.string.updateTitle))
            .setMessage(getString(R.string.updateMessage))
            .setPositiveText(getString(R.string.updateLabel))
            .setPositiveOnClickListener { _, _ -> openGooglePlay() }
            .show(this)
    }

    private fun openGooglePlay() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
        } catch (e: ActivityNotFoundException) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                )
            )
        }
    }


    private fun hideDownloadProgressViews() {
        binding.updateProgressBar.hide()
        binding.chunkCount.hide()
        binding.chunkSize.hide()
    }

    private fun showDownloadProgressViews() {
        binding.updateProgressBar.show()
        binding.chunkCount.show()
        binding.chunkSize.show()
    }

    override fun onBackPressed() {
        moveTaskToBack(true)
    }

    private fun updateDownloadedPackagesCount() {
        val lastDownloadedChunk = viewModel.getDrlStateIT().currentChunk.toInt() + viewModel.getDrlStateEU().currentChunk.toInt()
        val lastChunk = viewModel.getDrlStateIT().totalChunk.toInt() + viewModel.getDrlStateEU().totalChunk.toInt()
        val dataAmountDownloaded =
            (viewModel.getDrlStateIT().currentChunk * viewModel.getDrlStateIT().sizeSingleChunkInByte) +
                    (viewModel.getDrlStateEU().currentChunk * viewModel.getDrlStateEU().sizeSingleChunkInByte)

        binding.updateProgressBar.max = lastChunk
        binding.updateProgressBar.progress = lastDownloadedChunk
        binding.chunkCount.text = getString(R.string.chunk_count, lastDownloadedChunk, lastChunk)
        binding.chunkSize.text = getString(
            R.string.chunk_size,
            ConversionUtility.byteToMegaByte(dataAmountDownloaded.toFloat()),
            ConversionUtility.byteToMegaByte(viewModel.getDrlStateIT().totalSizeInByte.toFloat() + viewModel.getDrlStateEU().totalSizeInByte.toFloat())
        )
    }

    override fun onDismiss(dialog: DialogInterface?) {
        setScanModeButtonText(viewModel.getChosenScanMode())
    }
}
