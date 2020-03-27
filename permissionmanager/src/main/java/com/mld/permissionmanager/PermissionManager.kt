package com.mld.permissionmanager

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.annotation.StyleRes
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

const val ENABLE_BT_REQUEST_CODE = 101
const val ENABLE_LOCATION_REQUEST_CODE = 102

class PermissionManager(private val context: Context, @StyleRes private val theme : Int) {

    val locationBackgroundPermission = Manifest.permission.ACCESS_BACKGROUND_LOCATION
    val locationPermission = Manifest.permission.ACCESS_FINE_LOCATION
    val bluetoothPermission = Manifest.permission.BLUETOOTH_ADMIN
    val cameraPermission = Manifest.permission.CAMERA
    val changeNetworkStatePermission = Manifest.permission.CHANGE_NETWORK_STATE

    var autoIncrementRequestCode = 0
        get() {
            return ++field
        }

    private val requestPermissionsDataMap = mutableMapOf<Int, RequestPermissionData>()


    /** For Activities **/
    fun executeFunctionWithPermissionNeeded(
        activity: Activity,
        permission: String,
        onGranted: () -> Unit,
        onRefused: (() -> Unit)? = null,
        requestCode: Int = autoIncrementRequestCode,
        refusedText: String = context.getString(R.string.permission_default_explanation),
        alwaysDenyText: String = context.getString(R.string.permission_default_always_deny)
    ) {
        executeFunctionWithPermissionNeeded(
            activity,
            arrayOf(permission),
            onGranted,
            onRefused,
            requestCode,
            refusedText,
            alwaysDenyText
        )

    }
    /** For Fragments **/
    fun executeFunctionWithPermissionNeeded(
        fragment: Fragment,
        permission: String,
        onGranted: () -> Unit,
        onRefused: (() -> Unit)? = null,
        requestCode: Int = autoIncrementRequestCode,
        refusedText: String = context.getString(R.string.permission_default_explanation),
        alwaysDenyText: String = context.getString(R.string.permission_default_always_deny)
    ) {
        executeFunctionWithPermissionNeeded(
            fragment,
            arrayOf(permission),
            onGranted,
            onRefused,
            requestCode,
            refusedText,
            alwaysDenyText
        )

    }

    /** For Activities with permission Array **/
    fun executeFunctionWithPermissionNeeded(
        activity: Activity,
        permissions: Array<String>,
        onGranted: () -> Unit,
        onRefused: (() -> Unit)? = null,
        requestCode: Int = autoIncrementRequestCode,
        refusedText: String = context.getString(R.string.permission_default_explanation),
        alwaysDenyText: String = context.getString(R.string.permission_default_always_deny)
    ) {
        executeFunctionWithPermissionNeeded(
            activity,
            permissions,
            onGranted,
            requestCode,
            {
                showRefusedPermissionDialog(activity, refusedText, {
                    executeFunctionWithPermissionNeeded(
                        activity,
                        permissions,
                        onGranted,
                        onRefused,
                        requestCode,
                        refusedText,
                        alwaysDenyText
                    )
                },onRefused)
            },
            { showAlwaysDenyPermissionDialog(activity, alwaysDenyText, onRefused) }
        )
    }

    /** For Fragments with permission Array **/
    fun executeFunctionWithPermissionNeeded(
        fragment: Fragment,
        permissions: Array<String>,
        onGranted: () -> Unit,
        onRefused: (() -> Unit)? = null,
        requestCode: Int = autoIncrementRequestCode,
        refusedText: String = context.getString(R.string.permission_default_explanation),
        alwaysDenyText: String = context.getString(R.string.permission_default_always_deny)
    ) {
        executeFunctionWithPermissionNeeded(
            fragment,
            permissions,
            onGranted,
            requestCode,
            {
                showRefusedPermissionDialog(fragment, refusedText, {
                    executeFunctionWithPermissionNeeded(
                        fragment,
                        permissions,
                        onGranted,
                        onRefused,
                        requestCode,
                        refusedText,
                        alwaysDenyText
                    )
                },onRefused)
            },
            { showAlwaysDenyPermissionDialog(fragment.requireContext(), alwaysDenyText, onRefused) }
        )
    }

    private fun executeFunctionWithPermissionNeeded(
        activity: Activity,
        permissions: Array<String>,
        onGranted: () -> Unit,
        requestCode: Int,
        onRefused: () -> Unit,
        onAlwaysDeny: () -> Unit
    ) {
        if (!isGrantedPermissions(permissions)) {
            requestPermission(
                activity,
                permissions,
                onGranted,
                requestCode,
                onRefused,
                onAlwaysDeny
            )
        } else {
            onGranted()
        }
    }

    private fun executeFunctionWithPermissionNeeded(
        fragment: Fragment,
        permissions: Array<String>,
        onGranted: () -> Unit,
        requestCode: Int,
        onRefused: () -> Unit,
        onAlwaysDeny: () -> Unit
    ) {
        if (!isGrantedPermissions(permissions)) {
            requestPermission(
                fragment,
                permissions,
                onGranted,
                requestCode,
                onRefused,
                onAlwaysDeny
            )
        } else {
            onGranted()
        }
    }

    private fun requestPermission(
        fragment: Fragment,
        permissions: Array<String>,
        onGranted: () -> Unit,
        requestCode: Int,
        onRefused: (() -> Unit)? = null,
        onAlwaysDeny: (() -> Unit)? = null
    ) {
        requestPermissionsDataMap.put(
            requestCode,
            RequestPermissionData(
                onGranted,
                onRefused,
                onAlwaysDeny
            )
        )
        fragment.requestPermissions(permissions, requestCode)
    }

    private fun requestPermission(
        activity: Activity,
        permissions: Array<String>,
        onGranted: () -> Unit,
        requestCode: Int,
        onRefused: (() -> Unit)? = null,
        onAlwaysDeny: (() -> Unit)? = null
    ) {
        requestPermissionsDataMap.put(
            requestCode,
            RequestPermissionData(
                onGranted,
                onRefused,
                onAlwaysDeny
            )
        )
        ActivityCompat.requestPermissions(activity, permissions, requestCode)
    }

    /** Result for Activity **/
    fun onRequestPermissionsResult(
        activity: Activity,
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        val permissionsData = requestPermissionsDataMap.get(requestCode)
        requestPermissionsDataMap.remove(requestCode)
        if (permissionsData != null) {
            val isGranted = isGrantedPermissions(permissions)
            if (grantResults.isNotEmpty() && isGranted) {
                permissionsData.onGranted.invoke()
            } else {
                if(!permissions.isEmpty()){
                    val permission = getFirstRefusedPermission(permissions)
                    permission?.let {
                        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                            permissionsData.onRefused?.invoke()
                        } else {
                            permissionsData.onAlwaysDeny?.invoke()
                        }
                    }
                }
            }
        }
    }

    /** Result for Fragment **/
    fun onRequestPermissionsResult(
        fragment: Fragment,
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        val permissionsData = requestPermissionsDataMap.get(requestCode)
        requestPermissionsDataMap.remove(requestCode)
        if (permissionsData != null) {
            val isGranted = isGrantedPermissions(permissions)
            if (grantResults.isNotEmpty() && isGranted) {
                permissionsData.onGranted.invoke()
            } else {
                if(!permissions.isEmpty()) {
                    val permission = getFirstRefusedPermission(permissions)
                    permission?.let {
                        if (fragment.shouldShowRequestPermissionRationale(permission)) {
                            permissionsData.onRefused?.invoke()
                        } else {
                            permissionsData.onAlwaysDeny?.invoke()
                        }
                    }
                }
            }
        }
    }

    private fun getFirstRefusedPermission(permissions: Array<out String>) : String? {
        for (permission in permissions) {
           if(ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
               return permission
           }
        }
        return null
    }

    fun isGrantedPermissions(permissions: Array<out String>): Boolean {
        var isGranted = true
        for (permission in permissions) {
            isGranted = isGranted && ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
        return isGranted
    }

    private fun showRefusedPermissionDialog(
        activity: Activity,
        explanation: String,
        onAcceptClick: () -> Unit,
        onRefusedClick: (() -> Unit)? = null
    ) {
        AlertDialog.Builder(activity, theme)
            .setMessage(explanation)
            .setPositiveButton(android.R.string.yes) { dialog, which ->
                onAcceptClick()
            }
            .setNegativeButton(android.R.string.cancel){ _, _ ->
                onRefusedClick?.invoke()
            }
            .show()
    }

    private fun showRefusedPermissionDialog(fragment: Fragment, explanation: String, onAcceptClick: () -> Unit, onRefusedClick: (() -> Unit)? = null) {
        AlertDialog.Builder(fragment.requireContext(), theme)
            .setMessage(explanation)
            .setPositiveButton(android.R.string.yes) { dialog, which ->
                onAcceptClick()
            }
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                onRefusedClick?.invoke()
            }
            .show()
    }

    private fun showAlwaysDenyPermissionDialog(context: Context, explanation: String, onRefused: (() -> Unit)? = null) {

        AlertDialog.Builder(context, theme)
            .setMessage(explanation)
            .setPositiveButton(android.R.string.yes) { dialog, which ->
                val intent = Intent()
                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS

                val uri = Uri.fromParts("package", context.packageName, null)
                intent.data = uri

                context.startActivity(intent)
            }
            .setNegativeButton(android.R.string.cancel){ _, _ ->
                onRefused?.invoke()
            }
            .show()
    }

    private data class RequestPermissionData(
        val onGranted: () -> Unit,
        val onRefused: (() -> Unit)? = null,
        val onAlwaysDeny: (() -> Unit)? = null
    )
}
