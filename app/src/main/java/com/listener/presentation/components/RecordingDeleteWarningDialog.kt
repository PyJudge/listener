package com.listener.presentation.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.listener.presentation.theme.ListenerTheme

@Composable
fun RecordingDeleteWarningDialog(
    recordingCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("녹음 삭제 경고") },
        text = {
            Text(
                "전사 설정을 변경하면 학습 구간이 재계산됩니다.\n\n" +
                "현재 저장된 녹음 ${recordingCount}개가 삭제됩니다.\n\n" +
                "계속하시겠습니까?"
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("삭제하고 변경")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun RecordingDeleteWarningDialogPreview() {
    ListenerTheme {
        RecordingDeleteWarningDialog(
            recordingCount = 5,
            onConfirm = {},
            onDismiss = {}
        )
    }
}
