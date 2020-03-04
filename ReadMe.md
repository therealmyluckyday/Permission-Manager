# ReadMe - Build

## Android

Projet pour les permissions

Comment utiliser :
Appeler la méthode executeFunctionWithPermissionNeeded

Dans le basefragment et/ou baseactivity 
 override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionManager.onRequestPermissionsResult(this, requestCode, permissions, grantResults)
    }
