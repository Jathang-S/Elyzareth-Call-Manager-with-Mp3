package com.example

import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.telecom.InCallService
import androidx.test.core.app.ApplicationProvider
import com.example.telecom.DefaultDialerManager
import com.example.telecom.ElyzarethInCallService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DefaultDialerQualificationTest {

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun testElyzarethInCallServiceExtendsInCallService() {
        // Verify class inheritance
        assertTrue(
            "ElyzarethInCallService must extend android.telecom.InCallService",
            InCallService::class.java.isAssignableFrom(ElyzarethInCallService::class.java)
        )
    }

    @Test
    fun testDefaultDialerManagerProducesRoleOrChangeIntent() {
        val roleIntent = DefaultDialerManager.createRequestRoleIntent(context)
        assertNotNull("Role request intent must not be null", roleIntent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ uses RoleManager
            val roleName = roleIntent?.getStringExtra("android.app.role.extra.ROLE_NAME")
            if (roleName != null) {
                assertEquals("Intent must request ROLE_DIALER", RoleManager.ROLE_DIALER, roleName)
            }
        }
    }

    @Test
    fun testInCallServiceDeclaredInManifestWithRequiredPermissionAndMetaData() {
        val packageManager = context.packageManager
        val componentName = ComponentName(context, ElyzarethInCallService::class.java)

        val serviceInfo = packageManager.getServiceInfo(
            componentName,
            PackageManager.GET_META_DATA
        )

        assertNotNull("ElyzarethInCallService must be registered in AndroidManifest.xml", serviceInfo)
        assertTrue("ElyzarethInCallService must be exported for Telecom to bind", serviceInfo.exported)
        assertEquals(
            "Service must be protected by android.permission.BIND_INCALL_SERVICE",
            "android.permission.BIND_INCALL_SERVICE",
            serviceInfo.permission
        )

        val metaData = serviceInfo.metaData
        assertNotNull("Service must contain meta-data", metaData)
        val hasInCallUi = metaData.getBoolean("android.telecom.IN_CALL_SERVICE_UI", false)
        assertTrue(
            "Service must declare android.telecom.IN_CALL_SERVICE_UI = true",
            hasInCallUi
        )
    }

    @Test
    fun testMainActivityResolvesActionDial() {
        val packageManager = context.packageManager
        val dialIntent = Intent(Intent.ACTION_DIAL).apply {
            data = android.net.Uri.parse("tel:5551234")
            setPackage(context.packageName)
        }

        val resolveInfo = packageManager.resolveActivity(dialIntent, PackageManager.MATCH_DEFAULT_ONLY)
        assertNotNull("MainActivity must resolve ACTION_DIAL with tel: scheme", resolveInfo)
        assertEquals(
            MainActivity::class.java.name,
            resolveInfo?.activityInfo?.name
        )
    }
}
