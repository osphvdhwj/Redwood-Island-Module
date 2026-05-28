package com.example.dynamicisland.ui

import android.app.Activity
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dynamicisland.ui.design.RedwoodDesignSystem
import com.example.dynamicisland.ui.design.RedwoodTheme
import com.google.accompanist.drawablepainter.rememberDrawablePainter

class AppPickerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val appList = packageManager.getInstalledApplications(0)
            .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 || it.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0 }
            .sortedBy { it.loadLabel(packageManager).toString() }

        setContent {
            RedwoodTheme {
                Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                    Text(
                        "Select an Application",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(16.dp)
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        items(appList) { app ->
                            AppPickerItem(appInfo = app) {
                                val resultIntent = Intent().apply {
                                    putExtra("package_name", app.packageName)
                                }
                                setResult(Activity.RESULT_OK, resultIntent)
                                finish()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppPickerItem(appInfo: ApplicationInfo, onClick: () -> Unit) {
    val pm = androidx.compose.ui.platform.LocalContext.current.packageManager
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = rememberDrawablePainter(drawable = appInfo.loadIcon(pm)),
            contentDescription = null,
            modifier = Modifier.size(40.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = appInfo.loadLabel(pm).toString(),
            color = Color.White,
            fontSize = 16.sp
        )
    }
}
