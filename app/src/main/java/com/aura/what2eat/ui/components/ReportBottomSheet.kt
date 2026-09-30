package com.aura.what2eat.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.what2eat.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportBottomSheet(
    dishId: String,
    dishName: String,
    onDismiss: () -> Unit,
    onSubmitReport: (reason: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedCategory by remember { mutableStateOf("Inappropriate Content") }
    var additionalNotes by remember { mutableStateOf("") }
    var isSubmitted by remember { mutableStateOf(false) }

    val reportCategories = listOf(
        "Inappropriate Content",
        "Wrong Recipe / Steps",
        "Spam / Advertisement",
        "Other Issue"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceWhite,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = "Report",
                        tint = PrimaryOrange,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Report Dish Suggestion",
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = DarkText
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextMuted
                    )
                }
            }

            Text(
                text = "Help us keep What2Eat safe and clean by reporting issues with \"$dishName\".",
                fontFamily = NunitoFontFamily,
                fontSize = 13.sp,
                color = TextMuted,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Reason Options
            reportCategories.forEach { category ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedCategory = category }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (selectedCategory == category),
                        onClick = { selectedCategory = category },
                        colors = RadioButtonDefaults.colors(selectedColor = PrimaryOrange)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = category,
                        fontFamily = NunitoFontFamily,
                        fontWeight = if (selectedCategory == category) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 14.sp,
                        color = DarkText
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Notes text field
            OutlinedTextField(
                value = additionalNotes,
                onValueChange = { additionalNotes = it },
                label = { Text("Additional details (optional)", fontFamily = NunitoFontFamily) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryOrange,
                    unfocusedBorderColor = CardBorder
                ),
                minLines = 2
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Submit Button
            Button(
                onClick = {
                    val fullReason = "$selectedCategory: $additionalNotes".trim()
                    onSubmitReport(fullReason)
                    isSubmitted = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
            ) {
                Text(
                    text = if (isSubmitted) "Report Submitted ✓" else "Submit Report",
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = SurfaceWhite
                )
            }
        }
    }
}
