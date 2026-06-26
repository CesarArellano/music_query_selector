package com.cesarArellano.music_query_selector.interfaces

interface PermissionManagerInterface {
    fun permissionStatus() : Boolean
    fun requestPermission()
    fun retryRequestPermission()
}