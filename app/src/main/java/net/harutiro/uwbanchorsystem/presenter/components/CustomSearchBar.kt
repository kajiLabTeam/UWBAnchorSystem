package net.harutiro.uwbanchorsystem.presenter.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun CustomSearchBar(
    inputText: String = "",
    onValueChange: (String) -> Unit = {},
    searchAction: (String) -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    // キーボードアクションを定義
    val keyboardActions =
        KeyboardActions(
            onSearch = {
                searchAction(inputText)
                keyboardController?.hide()
            },
        )

    TextField(
        value = inputText,
        onValueChange = onValueChange,
        placeholder = { Text("キーワードで検索") },
        leadingIcon = {
            Icon(
                Icons.Sharp.Search,
                contentDescription = "検索",
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(40.dp),
                )
                .semantics {
                    contentDescription = "検索フィールド"
                },
        colors =
            TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedIndicatorColor = MaterialTheme.colorScheme.primaryContainer,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.primaryContainer,
            ),
        keyboardOptions =
            KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Search,
            ),
        keyboardActions = keyboardActions,
        maxLines = 1,
        singleLine = true,
    )
}

@Preview
@Composable
fun CustomSearchBarPreview() {
    CustomSearchBar(
        inputText = "",
        onValueChange = {},
        searchAction = {},
    )
}
