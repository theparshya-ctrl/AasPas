package com.aaspas.customer.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.aaspas.customer.R
import com.aaspas.customer.presentation.navigation.BottomNavItem

@Composable
fun AasPasBottomNavigation(
    currentRoute: String,
    onNavigate: (BottomNavItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(modifier = modifier) {
        BottomNavItem.entries.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = { onNavigate(item) },
                icon = {
                    Icon(
                        imageVector = when (item) {
                            BottomNavItem.Home -> Icons.Filled.Home
                            BottomNavItem.Search -> Icons.Filled.Search
                            BottomNavItem.Favorites -> Icons.Filled.Favorite
                            BottomNavItem.Profile -> Icons.Filled.Person
                        },
                        contentDescription = stringResource(item.labelRes),
                    )
                },
                label = { Text(text = stringResource(item.labelRes)) },
            )
        }
    }
}
