package net.harutiro.uwbanchorsystem.presenter.components


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import net.harutiro.uwbanchorsystem.R

@Composable
fun ProgressCycle(
    message: String = stringResource(R.string.searching),
    contentDescription: String = stringResource(R.string.loading_content_description),
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
            Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(8.dp)
                .semantics {
                    isTraversalGroup = true
                    this.contentDescription = contentDescription
                },
    ) {
        CircularProgressIndicator(
            modifier =
                Modifier.semantics {
                    this.contentDescription = contentDescription
                },
        )
        Text(
            text = message,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}