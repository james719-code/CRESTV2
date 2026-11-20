package com.bdbshs.crest.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.bdbshs.crest.branding.CrestSizes

/**
 * Product controls use the shared CREST tokens, while retaining Material's
 * native semantics, focus/ripples, disabled states and font scaling.
 */
@Composable
fun CrestButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.small,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit
) = Button(onClick, modifier.heightIn(min = CrestSizes.buttonMinimum), enabled, shape, colors,
    elevation, border, contentPadding, interactionSource, content)

@Composable
fun CrestOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.small,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = BorderStroke(1.dp, if (enabled) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit
) = OutlinedButton(onClick, modifier.heightIn(min = CrestSizes.buttonMinimum), enabled, shape, colors,
    elevation, border, contentPadding, interactionSource, content)

@Composable
fun CrestTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.small,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit
) = TextButton(onClick, modifier.heightIn(min = CrestSizes.touchMinimum), enabled, shape, colors,
    elevation, border, contentPadding, interactionSource, content)

@Composable
fun CrestTonalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.small,
    colors: ButtonColors = ButtonDefaults.filledTonalButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit
) = FilledTonalButton(onClick, modifier.heightIn(min = CrestSizes.buttonMinimum), enabled, shape, colors,
    elevation, border, contentPadding, interactionSource, content)

@Composable
fun CrestFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null
) = FilterChip(
    selected = selected,
    onClick = onClick,
    label = label,
    modifier = modifier.heightIn(min = CrestSizes.touchMinimum),
    enabled = enabled,
    leadingIcon = leadingIcon,
    trailingIcon = trailingIcon,
    shape = MaterialTheme.shapes.small,
    colors = FilterChipDefaults.filterChipColors(
        containerColor = MaterialTheme.colorScheme.surface,
        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
        selectedTrailingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
    ),
    border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
)

@Composable
fun CrestCard(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    colors: CardColors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation: CardElevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    border: BorderStroke? = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    content: @Composable ColumnScope.() -> Unit
) = Card(modifier, shape, colors, elevation, border, content)

@Composable
fun CrestOutlinedCard(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    colors: CardColors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation: CardElevation = CardDefaults.outlinedCardElevation(),
    border: BorderStroke = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    content: @Composable ColumnScope.() -> Unit
) = OutlinedCard(modifier, shape, colors, elevation, border, content)

@Composable
fun CrestOutlinedCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.medium,
    colors: CardColors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation: CardElevation = CardDefaults.outlinedCardElevation(),
    border: BorderStroke = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable ColumnScope.() -> Unit
) = OutlinedCard(onClick, modifier, enabled, shape, colors, elevation, border, interactionSource, content)

@Composable
fun CrestTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource? = null,
    shape: Shape = MaterialTheme.shapes.small,
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors()
) = OutlinedTextField(
    value = value, onValueChange = onValueChange, modifier = modifier,
    enabled = enabled, readOnly = readOnly, textStyle = textStyle, label = label,
    placeholder = placeholder, leadingIcon = leadingIcon, trailingIcon = trailingIcon,
    prefix = prefix, suffix = suffix, supportingText = supportingText, isError = isError,
    visualTransformation = visualTransformation, keyboardOptions = keyboardOptions,
    keyboardActions = keyboardActions, singleLine = singleLine, maxLines = maxLines,
    minLines = minLines, interactionSource = interactionSource, shape = shape, colors = colors
)
