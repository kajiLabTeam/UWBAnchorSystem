package net.harutiro.uwbanchorsystem.presenter.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessAlarms
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/***
 * テキストフィールドのコンポーネント
 *
 * この記事を参考にして記述をしました
 * https://medium.com/@dimasoktanugraha47/mastering-android-jetpack-compose-textfield-and-validation-3abd17c75952
 */
@Composable
fun TextFieldValidation(
    value: String,
    placeholder: String = "",
    onChange: (String) -> Unit,
    isError: Boolean,
    icon: ImageVector,
    errorMessage: String,
    isPassword: Boolean = false,
    imeAction: ImeAction = ImeAction.Next,
    keyboardType: KeyboardType = KeyboardType.Text,
    label: String,
    modifier: Modifier = Modifier
) {

    var showPassword by rememberSaveable { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.Start,
        modifier = modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                onChange(it)
            },
            label = {
                Text(text = label)
            },
            placeholder = {
                Text(text = placeholder)
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium,
            leadingIcon = {
                Icon(
                    icon,
                    contentDescription = "Text FieldInput",
                    tint = Color.Gray,
                    modifier = Modifier
                        .size(24.dp)
                )
            },
            trailingIcon = {
                if (isPassword){
                    Icon(
                        if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (showPassword) "Show Password" else "Hide Password",
                        tint = Color.Gray,
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { showPassword = !showPassword }
                    )
                }else {
                    null
                }
            },
            modifier = Modifier
                .fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = imeAction
            ),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedTextColor = Color.Gray,
                unfocusedBorderColor = Color.Gray,
                focusedTextColor = Color.Blue,
                focusedBorderColor = Color.Blue,
                errorBorderColor = Color.Red,
            ),
            shape = RoundedCornerShape(10.dp),
            visualTransformation = if (isPassword){
                if (showPassword) VisualTransformation.None else PasswordVisualTransformation()
            } else { VisualTransformation.None },
            isError = isError
        )
        if (isError){
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.labelSmall,
                color = Color.Red,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 2.dp),
                textAlign = TextAlign.Start
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TextFieldValidationPreview() {
    var password by rememberSaveable { mutableStateOf("") }

    TextFieldValidation(
        value = password,
        placeholder = "", // hintメッセージ
        label = "ラベル",
        onChange = {
            password = it
        },
        isError = true,
        icon = Icons.Filled.AccessAlarms,
        errorMessage = "Error Message",
        isPassword = false
    )
}
