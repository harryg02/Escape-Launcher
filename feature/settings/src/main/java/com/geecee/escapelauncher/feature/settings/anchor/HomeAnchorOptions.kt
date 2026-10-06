package com.geecee.escapelauncher.feature.settings.anchor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.geecee.escapelauncher.core.ui.R
import com.geecee.escapelauncher.core.ui.composables.EscapeHeader
import com.geecee.escapelauncher.core.ui.composables.SettingsButton
import com.geecee.escapelauncher.core.ui.composables.SettingsSpacer

/**
 * Lets the user write the optional line shown on the home screen, with an optional number to call
 *
 * @param goBack When back button is pressed
 */
@Composable
fun HomeAnchorOptions(
    goBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeAnchorViewModel = hiltViewModel()
) {
    var text by rememberSaveable { mutableStateOf("") }
    var phoneNumber by rememberSaveable { mutableStateOf("") }
    var loaded by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!loaded) {
            val anchor = viewModel.loadHomeAnchor()
            text = anchor.text
            phoneNumber = anchor.phoneNumber
            loaded = true
        }
    }

    Box(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            item(key = "header") { EscapeHeader(goBack, stringResource(R.string.home_anchor)) }

            item(key = "description") {
                Text(
                    text = stringResource(R.string.home_anchor_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(16.dp))
            }

            item(key = "text") {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(stringResource(R.string.home_anchor_text)) },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
            }

            item(key = "phone") {
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text(stringResource(R.string.home_anchor_phone)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
            }

            item(key = "save") {
                SettingsButton(
                    label = stringResource(R.string.save),
                    isTopOfGroup = true,
                    onClick = {
                        viewModel.saveHomeAnchor(text, phoneNumber)
                        goBack()
                    }
                )
            }

            item(key = "remove") {
                SettingsButton(
                    label = stringResource(R.string.remove),
                    isBottomOfGroup = true,
                    onClick = {
                        viewModel.clearHomeAnchor()
                        text = ""
                        phoneNumber = ""
                    }
                )
            }

            item(key = "spacer1") { SettingsSpacer() }
            item(key = "spacer2") { SettingsSpacer() }
        }
    }
}
